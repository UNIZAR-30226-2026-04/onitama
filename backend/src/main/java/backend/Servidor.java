package backend;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.Map;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.SQLException;

import backend.VO.Partida;
import backend.VO.CartaMov;
import backend.VO.CartaAccion;
import backend.VO.Posicion;
import backend.VO.Tablero;
import backend.VO.Jugador;
import backend.VO.Skin;
import backend.VO.Autenticacion;
import backend.VO.Notificacion;

import backend.gestor.GestorNotificaciones;
import backend.gestor.GestorSkin;
import backend.gestor.GestorJugador;
import backend.gestor.GestorPartida;
import backend.gestor.GestorCartasMov;
import backend.gestor.GestorCartasAccion;
import backend.gestor.GestorEmail;

//POR HACER:
// -> El xml que querias hacer: PRIORIDAD ALTA <-- Puedes empezar con esto si quieres 
// todavía por commitear, añado primero las actualizaciones a servidor.java
// -> Solicitudes de amistad: PRIORIDAD ALTA
// he terminado rechazar que he vsto que era lo que faltaba
// -> Solicitudes de partida privadas: PRIORIDAD MEDIA (Antes hay que hacer lo anterior)
// he planteado las solicitudes de partida privadas, también en el readme

// -> Reanudar/Pausar una partida privada: PRIORIDAD MEDIA (Antes hay que hacer lo anterior)
// -> Cartas de Accion: PRIORIDAD BAJA (No lo necesitamos para la primera entrega)


// NOTAS: 

// gestionarBusquedaPartida crea InfoJugador sin avatarId porque en el momento no tiene
// el objeto jugador, entonces podemos o pasarle null o hacer una consulta para saber su avatar.
// lo he dejado comentado lo que sería una consulta en gestionarBsquedaPartida.

class InfoJugador {
    WebSocket ws;
    String nombre;
    int puntos;
    //nuevo 
    String avatarId;

    public InfoJugador(WebSocket w, String nom, int pt, String avatar) {
        ws = w;
        nombre = nom;
        puntos = pt;
        avatarId = avatar;
    }
}

class Pareja {
    InfoJugador p1, p2;
    Partida partida;
    int eleccion;

    // NUEVO
    // constructor para reanudar partidas pausadas, y así en lugar de crear
    // una partida nueva en BD, recibe Partida ya cargada desde la base en 
    // gestionarRespuestaReanudar y la asigna directamente.
    // Así los dos jugadores vuelven a estar emparejados en memoria con su
    // partida guardada, sin tocar la base ni reasignar cartas.
    public Pareja(InfoJugador _p1, InfoJugador _p2, Partida partidaExistente) {
        p1 = _p1;
        p2 = _p2;
        partida = partidaExistente;
    }

    public Pareja(InfoJugador _p1, InfoJugador _p2, String tipo) {
        eleccion = 0; //Para las cartas de accion
        p1 = _p1;
        p2 = _p2;

        // Limpiamos partidas fantasma (abandonos por desconexión) antes de intentar crear una nueva
        new GestorPartida().terminarPartidasEnCurso(_p1.nombre, _p2.nombre);

        partida = new Partida(0, "JUGANDOSE", 0, tipo, null, null, 0, 0, _p1.nombre, _p2.nombre, false, false, 0, null, null);
        if (!partida.registrarPartida()) {
            throw new RuntimeException("No se pudo registrar la partida: " + _p1.nombre + " o " + _p2.nombre + " ya tiene una partida activa (JUGANDOSE) en la BD.");
        }
        partida.asignarCartas();
        partida.repartirCartas();
    }

    public boolean buscar(WebSocket _p1) {
        return p1.ws == _p1 || p2.ws == _p1;
    }

    // PREVIAMENTE if p1.ws return p1, pero eso devolvía el mismo jugador,
    // entonces lo he cambiado para que devuelva el oponente.
    public InfoJugador getOponente(WebSocket _p1) {
        if (p1.ws == _p1) {
            return p2;
        } else if (p2.ws == _p1) {
            return p1;
        } else {
            return null;
        }
    }
}

// BORRO PARTIDA

public class Servidor extends WebSocketServer {
    private List<InfoJugador> conectados;
    private List<InfoJugador> buscando_partida;
    private List<Pareja> parejas;
    private ExecutorService hilos = Executors.newFixedThreadPool(50);
    private Semaphore mutex = new Semaphore(1);
    private Semaphore mutexParejas = new Semaphore(1);
    private Semaphore mutexConectados = new Semaphore(1);
    private ScheduledExecutorService temporizador = Executors.newScheduledThreadPool(10);
    private Map<Integer, ScheduledFuture<?>> esperaPartida = new ConcurrentHashMap<>();
    // para mapear idNotificacion → timer activo y poder cancelarlo cuando el amigo
    // acepta o rechaza
    // concurrenthashmap y no hashmap porque el servidor es multihilo --> varios
    // mensajes pueden
    // llegar a la vez y HashMap no es seguro cuando se usan concurrencias.

    // ScheduledFuture<?> es el handle que devuelve temporizador.schedule() al
    // programar una tarea,
    // se guarda aquí únicamente para poder llamar a cancel() si B responde antes de
    // que expire,
    // evitando que se mande ERROR_NO_UNIDO cuando ya no hace falta.
    // El <?> indica que no nos importa el tipo de retorno de la tarea, porque solo
    // vamos a cancelarla.
    // NUEVO: AÑADIMOS SEGUNDO MAPA DE TIMERS PARA PODER REANUDAR PARTIDAS
    private Map<Integer, ScheduledFuture<?>> timersInvitacion = new ConcurrentHashMap<>();
    private Map<Integer, ScheduledFuture<?>> timersReanudar = new ConcurrentHashMap<>();

    private void cartasPartida(Pareja pj, JSONArray mazoJ1, JSONArray mazoJ2, JSONArray cola) {
        List<CartaMov> cartas = pj.partida.getCartasMovimiento();

        for (CartaMov carta : cartas) {
            JSONArray arrayMovimientos = new JSONArray();
            List<Posicion> movs = carta.getListaMovimientos();

            for (Posicion p : movs) {
                JSONObject punto = new JSONObject();
                punto.put("x", p.getX());
                punto.put("y", p.getY());
                arrayMovimientos.put(punto);
            }

            JSONObject JSONCarta = new JSONObject();
            JSONCarta.put("nombre", carta.getNombre());
            JSONCarta.put("movimientos", arrayMovimientos);

            if (carta.perteneceAlEquipo(1)) {
                mazoJ1.put(JSONCarta);
            } else if (carta.perteneceAlEquipo(2)) {
                mazoJ2.put(JSONCarta);
            } else {
                cola.put(JSONCarta);
            }
        }
    }

    // para poder recuperar cartas de acción en partidas privadas reanudadas
    private void cartasAccionPartida(Pareja pj, JSONArray cartasJ1, JSONArray cartasJ2) {
        List<CartaAccion> cartas = pj.partida.getCartasAccion();

        for (CartaAccion carta : cartas) {
            JSONObject JSONCarta = new JSONObject();
            JSONCarta.put("nombre", carta.getNombre());
            JSONCarta.put("accion", carta.getAccion());
            JSONCarta.put("estado", carta.getEstado());
            JSONCarta.put("equipo", carta.getEquipo());

            if (carta.getEquipo() == 1) {
                cartasJ1.put(JSONCarta);
            } else if (carta.getEquipo() == 2) {
                cartasJ2.put(JSONCarta);
            }
        }
    }

    // Con 'iniciar' construimos los JSON de tipo PARTIDA_ENCONTRADA o PARTIDA_PRIVADA_ENCONTRADA
    public void iniciar(Pareja pj, String msgTipoPartida) {

        JSONArray mazoJ1 = new JSONArray();
        JSONArray mazoJ2 = new JSONArray();
        JSONArray cola = new JSONArray();
        cartasPartida(pj, mazoJ1, mazoJ2, cola);

        JSONObject msg1 = new JSONObject(); // el que espera a jugar
        msg1.put("tipo", msgTipoPartida);
        msg1.put("partida_id", pj.partida.getIDPartida());
        msg1.put("equipo", 1);
        msg1.put("oponente", pj.p2.nombre);
        msg1.put("oponentePt", pj.p2.puntos);
        msg1.put("cartas_jugador", mazoJ1);
        msg1.put("cartas_oponente", mazoJ2);
        msg1.put("carta_siguiente", cola);
        msg1.put("oponente_avatar_id", pj.p2.avatarId);
        msg1.put("tablero_eq1", pj.partida.getPos_Fichas_Eq1());
        msg1.put("tablero_eq2", pj.partida.getPos_Fichas_Eq2());
        msg1.put("turno", pj.partida.getTurno());

        JSONObject msg2 = new JSONObject(); // al que le emparejan a j1
        msg2.put("tipo", msgTipoPartida);
        msg2.put("partida_id", pj.partida.getIDPartida());
        msg2.put("equipo", 2);
        msg2.put("oponente", pj.p1.nombre);
        msg2.put("oponentePt", pj.p1.puntos);
        msg2.put("cartas_jugador", mazoJ2);
        msg2.put("cartas_oponente", mazoJ1);
        msg2.put("carta_siguiente", cola);
        msg2.put("oponente_avatar_id", pj.p1.avatarId);
        msg2.put("tablero_eq1", pj.partida.getPos_Fichas_Eq1());
        msg2.put("tablero_eq2", pj.partida.getPos_Fichas_Eq2());
        msg2.put("turno", pj.partida.getTurno());

        pj.p1.ws.send(msg1.toString());
        pj.p2.ws.send(msg2.toString());
        System.out.println("Partida " + pj.partida.getIDPartida() + " iniciada.");

        // Envío las 3 cartas de la cola para que el frontend gestione la rotación por
        // su cuenta,
        // porque si solo enviáramos la carta visible, el servidor tendría que guardar
        // las demás
        // en memoria y el frontend tendría que pedirle la siguiente cada vez que gasta
        // una,
        // que acumularía bastante petición y respuesta. Con esto evitamos mensajes
        // extra.

    }

    /** Ejecuta el tiempo agotado: antes del primer movimiento no hay ganador; después pierde quien debía mover. */
    private void ejecutarTiempoAgotado(Pareja pj) {
        if (pj.partida.getTurno() == 0) {
            boolean esPrivada = "PRIVADA".equalsIgnoreCase(pj.partida.getTipo());
            System.out.println("TIEMPO_AGOTADO partida=" + pj.partida.getIDPartida()
                + " sin_movimientos tipo=" + pj.partida.getTipo());

            if (esPrivada) {
                pj.partida.pausarPartida();
                JSONObject msg = new JSONObject()
                    .put("tipo", "PARTIDA_PAUSADA")
                    .put("motivo", "TIEMPO_AGOTADO");
                if (pj.p1.ws.isOpen()) pj.p1.ws.send(msg.toString());
                if (pj.p2.ws.isOpen()) pj.p2.ws.send(msg.toString());
            } else {
                pj.partida.cancelarPartida();
                JSONObject msg = new JSONObject()
                    .put("tipo", "PARTIDA_CANCELADA")
                    .put("motivo", "TIEMPO_AGOTADO");
                if (pj.p1.ws.isOpen()) pj.p1.ws.send(msg.toString());
                if (pj.p2.ws.isOpen()) pj.p2.ws.send(msg.toString());
            }

            try {
                mutexParejas.acquire();
                parejas.remove(pj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutexParejas.release();
            }
            return;
        }

        int equipoSinMover = pj.partida.getTurno() % 2 + 1;
        int equipoGanador = (equipoSinMover == 1) ? 2 : 1;
        System.out.println("TIEMPO_AGOTADO partida=" + pj.partida.getIDPartida()
            + " equipo_sin_mover=" + equipoSinMover);

        pj.partida.abandonarPartida(equipoSinMover);

        JSONObject msgVictoria = new JSONObject()
            .put("tipo", "VICTORIA")
            .put("motivo", "TIEMPO_AGOTADO")
            .put("equipo_responsable", equipoSinMover);
        JSONObject msgDerrota = new JSONObject()
            .put("tipo", "DERROTA")
            .put("motivo", "TIEMPO_AGOTADO")
            .put("equipo_responsable", equipoSinMover);

        if (equipoGanador == 1) {
            if (pj.p1.ws.isOpen()) pj.p1.ws.send(msgVictoria.toString());
            if (pj.p2.ws.isOpen()) pj.p2.ws.send(msgDerrota.toString());
        } else {
            if (pj.p1.ws.isOpen()) pj.p1.ws.send(msgDerrota.toString());
            if (pj.p2.ws.isOpen()) pj.p2.ws.send(msgVictoria.toString());
        }

        try {
            mutexParejas.acquire();
            parejas.remove(pj);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release();
        }
    }

    /**
     * Inicia (o reinicia) el timer de respaldo para el turno indicado.
     * Se dispara 10 s después de que el frontend debería haber enviado TIEMPO_AGOTADO,
     * como seguro ante desconexiones o clientes maliciosos.
     */
    private void iniciarTimerTurno(Pareja pj, int turnoActual) {
        final int idPartida = pj.partida.getIDPartida();
        final int equipoQueDebeMover = turnoActual % 2 + 1;

        ScheduledFuture<?> timerAntiguo = esperaPartida.remove(idPartida);
        if (timerAntiguo != null) timerAntiguo.cancel(false);

        final Pareja pjFinal = pj;

        // 130 s = 120 s del contador visual + 10 s de margen de red
        ScheduledFuture<?> timerNuevo = temporizador.schedule(() -> {
            esperaPartida.remove(idPartida);
            System.out.println("TIMER RESPALDO disparado partida=" + idPartida
                + " equipo_sin_mover=" + equipoQueDebeMover);
            ejecutarTiempoAgotado(pjFinal);
        }, 130, TimeUnit.SECONDS);

        esperaPartida.put(idPartida, timerNuevo);
    }

    /** El cliente avisa que su contador visual llegó a 0. */
    private void gestionarTiempoAgotado(WebSocket conn) {
        Pareja pj = null;
        try {
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                if (pareja.buscar(conn)) { pj = pareja; break; }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release();
        }
        if (pj == null) return;

        // Cancelar el timer de respaldo antes de actuar
        ScheduledFuture<?> timer = esperaPartida.remove(pj.partida.getIDPartida());
        if (timer != null) timer.cancel(false);

        ejecutarTiempoAgotado(pj);
    }

    private void gestionarBusquedaPartida(WebSocket conn, JSONObject obj) {

        String nombre = obj.getString("nombre");
        int puntos = obj.getInt("puntos");

            // consultamos la base para obtener el avatarId del jugador
            String avatarId = null;
            try {
                Jugador j = new GestorJugador().buscarJugador(nombre);
                if (j != null) avatarId = j.getAvatarId();
            } catch (SQLException e) {
                System.err.println("Error al obtener avatarId en búsqueda: " + e.getMessage());
            }

        InfoJugador oponente = null;
        Pareja pj = null;

        try {
            mutex.acquire(); // WAIT

            for (InfoJugador j : buscando_partida) {
                int dif = Math.abs(j.puntos - puntos);
                if (dif <= 100 && !j.nombre.equals(nombre)) {
                    oponente = j;
                    break;
                }
            }

            // if gestiona caso de match y else si no ha habido match
            if (oponente != null) {
                buscando_partida.remove(oponente);
                InfoJugador nuevoJugador = new InfoJugador(conn, nombre, puntos, avatarId);
                pj = new Pareja(oponente, nuevoJugador, "PUBLICA");

                try {
                    mutexParejas.acquire();// WAIT
                    parejas.add(pj);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutexParejas.release(); // SIGNAL
                }

            } else {
                InfoJugador jugador = new InfoJugador(conn, nombre, puntos, avatarId);
                buscando_partida.add(jugador);
                System.out.println(nombre + " está esperando a unirse a una partida.");
                hilos.submit(() -> {
                    volverABuscar(conn, jugador);
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release(); // SIGNAL
        }

        if (oponente != null) {
            iniciar(pj, "PARTIDA_ENCONTRADA");
            System.out.println("Partida creada: " + nombre + " VS " + oponente.nombre);
        }
    }

    private void volverABuscar(WebSocket conn, InfoJugador jug) {
        AtomicInteger segBuscando = new AtomicInteger(0);
        ScheduledFuture<?>[] tareaLoop = new ScheduledFuture<?>[1];

        tareaLoop[0] = temporizador.scheduleAtFixedRate(() -> {

            InfoJugador oponenteEncontrado = null;
            Pareja pj = null;
            int tiempoActual = segBuscando.addAndGet(10); // Sumamos 10 segundos atomicamente debido a que dentro del
                                                          // temporizador no se puede modificar variables primitivas de
                                                          // java

            try {
                mutex.acquire(); // WAIT

                // Si no estamos en la lista, es que ya nos han cogido como pareja
                if (!buscando_partida.contains(jug)) {
                    tareaLoop[0].cancel(false);
                    return; // Salimos de la ejecución
                }

                boolean hayGente = buscando_partida.size() > 1;
                boolean prioridad = tiempoActual > 20 && hayGente;

                if (prioridad) {
                    // Sacamos al primero que no sea él mismo (el que ha estado más tiempo
                    // esperando)
                    oponenteEncontrado = buscando_partida.get(0);
                    if (jug.equals(oponenteEncontrado) && buscando_partida.size() > 1) {
                        oponenteEncontrado = buscando_partida.get(1);
                    }
                } else {
                    for (InfoJugador j : buscando_partida) {
                        if (jug.equals(j))
                            continue; // No emparejarse consigo mismo
                        int dif = Math.abs(j.puntos - jug.puntos);
                        if (dif <= 100 && !j.nombre.equals(jug.nombre)) {
                            oponenteEncontrado = j;
                            break;
                        }
                    }
                }

                // Si encontramos a alguien, los sacamos de la lista
                if (oponenteEncontrado != null) {
                    buscando_partida.remove(jug);
                    buscando_partida.remove(oponenteEncontrado);
                    pj = new Pareja(oponenteEncontrado, jug, "PUBLICA");
                    try {
                        mutexParejas.acquire();// WAIT
                        parejas.add(pj);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        mutexParejas.release(); // SIGNAL
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutex.release(); // SIGNAL
            }

            if (oponenteEncontrado != null) {
                System.out.println("Partida creada tardía: " + jug.nombre + " VS " + oponenteEncontrado.nombre);
                iniciar(pj, "PARTIDA_ENCONTRADA");

                tareaLoop[0].cancel(false);
            } else {
                System.out.println(jug.nombre + " sigue buscando... (" + tiempoActual + "s)");
            }

        }, 5, 5, TimeUnit.SECONDS); // Se repite cada 5 segundos
    }

    private void gestionarPartida(WebSocket conn, JSONObject obj) {
        // buscamos partida en la que esta el jugador
        Pareja pj = null;
        try {
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                // Usamos el método 'buscar' de la clase Pareja para ver si este jugador está
                // aquí
                if (pareja.buscar(conn)) {
                    // una vez encontremos el jugador, actualizamos el valor de p para trabajar con
                    // él y con el movimiento
                    // que tiene que hacer
                    pj = pareja;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); // SIGNAL
        }

        if (pj != null) {

            // Cancelar el timer del turno anterior al recibir un nuevo movimiento
            final int idPartida = pj.partida.getIDPartida();
            ScheduledFuture<?> timerAntiguo = esperaPartida.remove(idPartida);
            if (timerAntiguo != null) timerAntiguo.cancel(false);

            final Pareja pjFinal = pj;

            InfoJugador oponente = pj.getOponente(conn);

            // Verificamos que el oponente exista y que su conexión esté abierta
            if (oponente != null && oponente.ws.isOpen()) {
                String carta = obj.getString("carta");
                int YO = obj.getInt("fila_origen");
                int XO = obj.getInt("col_origen");
                int YD = obj.getInt("fila_destino");
                int XD = obj.getInt("col_destino");
                int equipo = obj.getInt("equipo");

                // El que te pase el mensaje te dira su equipo
                // 0 -> Movimiento realizado con exito
                // 1 -> equipo 1 gana
                // 2 -> equipo 2 gana
                // -1 -> carta no existente en la partida
                // -2 -> movimiento no valido
                int estado = -2;

                if (XO > -1 && YO > -1 && XD > -1 && YD > -1) {
                    Tablero tb = pj.partida.getTablero();
                    estado = pj.partida.moverFicha(equipo, tb.getPosicion(XO, YO), tb.getPosicion(XD, YD), carta);
                    Posicion trampaAct = pj.partida.trampaActivada;
                    if (trampaAct != null) {
                        JSONObject msgTrampa = new JSONObject();
                        msgTrampa.put("tipo", "TRAMPA_ACTIVADA");
                        msgTrampa.put("columna", trampaAct.getX());
                        msgTrampa.put("fila", trampaAct.getY());
                        conn.send(msgTrampa.toString());
                        oponente.ws.send(msgTrampa.toString());
                    }
                }

                JSONObject msg = new JSONObject();

                if (estado >= 0) {
                    // almacenamos cambios en bd para todos los movimientos exitosos
                    try {
                        pj.partida.actualizarBD();
                    } catch (Exception e) {
                        System.err.println("Error al actualizar BD después de movimiento: " + e.getMessage());
                    }

                    if (estado == 0) {
                        msg.put("tipo", "MOVER");
                        msg.put("col_origen", XO);
                        msg.put("fila_origen", YO);
                        msg.put("col_destino", XD);
                        msg.put("fila_destino", YD);
                        msg.put("carta", carta);
                        msg.put("equipo", equipo);
                        if (pj.partida.trampaActivada != null) {
                            msg.put("trampa_activada", true);
                            pj.partida.trampaActivada = null; // reset
                        }
                    } else if (estado == 1) {
                        JSONObject msgEq1 = new JSONObject();
                        msgEq1.put("motivo", "FIN_PARTIDA");
                        msgEq1.put("equipo_responsable", equipo);
                        msgEq1.put("tipo", "VICTORIA");
                        
                        JSONObject msgEq2 = new JSONObject();
                        msgEq2.put("motivo", "FIN_PARTIDA");
                        msgEq2.put("equipo_responsable", equipo);
                        msgEq2.put("tipo", "DERROTA");

                        // Persistir fin de partida y puntuaciones (incluye victorias por trampa/trono/captura).
                        pj.partida.finalizarPartida();

                        try {
                            mutexParejas.acquire();
                            parejas.remove(pjFinal);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            mutexParejas.release();
                        }
                        if (equipo == 1) {
                            // conn es Eq1 (gana)
                            conn.send(msgEq1.toString());
                            if(oponente.ws.isOpen()) oponente.ws.send(msgEq2.toString());
                        } else {
                            // conn es Eq2 (pierde)
                            conn.send(msgEq2.toString());
                            if(oponente.ws.isOpen()) oponente.ws.send(msgEq1.toString());
                        }
                        ScheduledFuture<?> timerFinal = esperaPartida.remove(idPartida);
                        if (timerFinal != null) {
                            timerFinal.cancel(false);
                        }
                        System.out.println("Partida finalizada con victoria del eq1 " + pj.partida.getIDPartida());
                        return;
                    } else if (estado == 2) {
                        JSONObject msgEq1 = new JSONObject();
                        msgEq1.put("motivo", "FIN_PARTIDA");
                        msgEq1.put("equipo_responsable", equipo);
                        msgEq1.put("tipo", "DERROTA");
                        
                        JSONObject msgEq2 = new JSONObject();
                        msgEq2.put("motivo", "FIN_PARTIDA");
                        msgEq2.put("equipo_responsable", equipo);
                        msgEq2.put("tipo", "VICTORIA");

                        // Persistir fin de partida y puntuaciones (incluye victorias por trampa/trono/captura).
                        pj.partida.finalizarPartida();

                        try {
                            mutexParejas.acquire();
                            parejas.remove(pjFinal);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            mutexParejas.release();
                        }

                        if (equipo == 2) {
                            // conn es Eq2 (gana)
                            conn.send(msgEq2.toString());
                            if(oponente.ws.isOpen()) oponente.ws.send(msgEq1.toString());
                        } else {
                            // conn es Eq1 (pierde)
                            conn.send(msgEq1.toString());
                            if(oponente.ws.isOpen()) oponente.ws.send(msgEq2.toString());
                        }
                        ScheduledFuture<?> timerFinal = esperaPartida.remove(idPartida);
                        if (timerFinal != null) {
                            timerFinal.cancel(false);
                        }
                        System.out.println("Partida finalizada con victoria del eq2 " + pj.partida.getIDPartida());
                        return;
                    }

                    // Enviamos el mensaje de MOVER
                    if (estado == 0) {
                        oponente.ws.send(msg.toString());
                        if (msg.has("trampa_activada")) {
                            conn.send(msg.toString());
                        }
                    }
                    System.out.println("Movimiento reenviado en la partida " + pj.partida.getIDPartida());

                    // Partida terminada por victoria: quitar la pareja para que ABANDONAR/MOVER
                    // no coincidan con una partida antigua si el mismo jugador empieza otra
                    // después.
                    if (estado == 1 || estado == 2) {
                        // actualizamos todo al final, para no encadenar consultas antes que enviar mensaje de movimiento al oponente
                        pj.partida.finalizarPartida();
                        try {
                            mutexParejas.acquire();
                            parejas.remove(pj);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            mutexParejas.release();
                        }
                    }

                } else {
                    if (estado == -2) {
                        msg.put("tipo", "MOVIMIENTO_INVALIDO");
                        System.out.println("Movimiento invalido en la partida " + pj.partida.getIDPartida());
                    } else {
                        msg.put("tipo", "CARTA_INVALIDA");
                        System.out.println("Carta no presente en la partida en la partida " + pj.partida.getIDPartida());
                    }

                    conn.send(msg.toString());
                }

                // Reiniciar timer para quien deba mover ahora (turno avanzado si movimiento fue válido)
                iniciarTimerTurno(pj, pj.partida.getTurno());
            }
        } else {
            // Si no encontramos partida, es un error (el jugador intentó mover sin estar
            // emparejado)
            System.out.println("Error: Movimiento recibido de un jugador sin partida activa.");
        }
    }

    public void abandonarPartida(WebSocket conn, JSONObject obj) {
        int equipoAbandona = obj.getInt("equipo");
        Pareja pj = null;
        try {
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                if (pareja.buscar(conn)) {
                    pj = pareja;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); // SIGNAL
        }

        if (pj != null) { 
            final int idPartida = pj.partida.getIDPartida();

            ScheduledFuture<?> timerAntiguo = esperaPartida.remove(idPartida);
            if (timerAntiguo != null) {
                timerAntiguo.cancel(false);
            }

            if (pj.partida.getTurno() == 0) {
                pj.partida.cancelarPartida();
                JSONObject msgCancelada = new JSONObject();
                msgCancelada.put("tipo", "PARTIDA_CANCELADA");
                msgCancelada.put("motivo", "ABANDONO");
                msgCancelada.put("equipo_responsable", equipoAbandona);

                if (pj.p1.ws.isOpen()) pj.p1.ws.send(msgCancelada.toString());
                if (pj.p2.ws.isOpen()) pj.p2.ws.send(msgCancelada.toString());

                try {
                    mutexParejas.acquire();
                    parejas.remove(pj);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutexParejas.release(); // SIGNAL
                }
                return;
            }

            InfoJugador oponente = pj.getOponente(conn);
            JSONObject msg = new JSONObject();

            if (oponente != null && oponente.ws.isOpen()) {
                msg.put("tipo", "VICTORIA");
                msg.put("motivo", "ABANDONO");
                msg.put("equipo_responsable", equipoAbandona);
                oponente.ws.send(msg.toString());
            }

            pj.partida.abandonarPartida(equipoAbandona); // Actualizamos la partida en la BD con el abandono

            try {
                mutexParejas.acquire();
                parejas.remove(pj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutexParejas.release(); // SIGNAL
            }
        }
    }

    public void iniciarSesion(WebSocket conn, JSONObject obj) {
        GestorJugador gestorJugador = new GestorJugador();
        String nombre = obj.getString("nombre");
        try {
            Jugador j = gestorJugador.buscarJugador(nombre);

            if (j == null || estaConectado(nombre)) {
                conn.send(new JSONObject().put("tipo", "ERROR_SESION_USS").toString());
            } else if (!Autenticacion.verificarPassword(obj.getString("password"), j.getContrasenya())) {
                conn.send(new JSONObject().put("tipo", "ERROR_SESION_PSSWD").toString());
            } else {
                try {
                    mutexConectados.acquire();
                    // Inicialmente sí se registra con 0 puntos, pero si inicia sesión, tienen que
                    // ser j.getPuntos(), no?
                    // nuevo
                    conectados.add(new InfoJugador(conn, nombre, j.getPuntos(), j.getAvatarId())); // Inicialmente el jugador se registra
                                                                                  // con 0 puntos
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutexConectados.release();
                }
                JSONObject msg = new JSONObject();
                msg.put("tipo", "INICIO_SESION_EXITOSO");
                msg.put("nombre", j.getNombre());
                msg.put("correo", j.getCorreo());
                msg.put("puntos", j.getPuntos());
                msg.put("partidas_ganadas", j.getPartidasGanadas());
                msg.put("partidas_jugadas", j.getPartidasJugadas());
                msg.put("cores", j.getCores());
                // nuevo
                msg.put("skin_activa", j.getSkinActiva());
                msg.put("avatar_id", j.getAvatarId());
                conn.send(msg.toString());

                // no hemos considerado que mientras el jugador esté desconectado, no le llegan
                // las notifs (ya que
                // el ws en el frontend se cierra después de hacer registro, login, jugar)
                // las notifs pendientes se envían aquí porque el frontend usa ws de manera
                // puntual->
                // abre el WS para login y lo cierra nada más recibir INICIO_SESION_EXITOSO.
                // No hay ningún momento en que el servidor tenga conexión abierta con el
                // jugador
                // fuera de una búsqueda o partida activa, así que aprovechamos el login para
                // volcar
                // todo lo que se perdió mientras estaba desconectado.
                GestorNotificaciones gestorNotif = new GestorNotificaciones();
                try {
                    List<Notificacion> pendientes = gestorNotif.obtenerPendientes(nombre);
                    for (Notificacion n : pendientes) {
                        JSONObject notif = new JSONObject();
                        notif.put("tipo", n.getTipo());
                        notif.put("remitente", n.getRemitente());
                        notif.put("idNotificacion", n.getIdNotificacion());
                        notif.put("fecha_ini", n.getFechaCreacion().toString());
                        notif.put("fecha_fin", n.getFechaExpiracion() != null ? n.getFechaExpiracion().toString() : "");
                        conn.send(notif.toString());
                    }
                } catch (SQLException e) {
                    System.err.println("Error al obtener notificaciones pendientes: " + e.getMessage());
                }
            }

        } catch (java.sql.SQLException e) {
            System.err.println("Error de Base de Datos al iniciar sesión: " + e.getMessage());
            JSONObject errorInfo = new JSONObject();
            errorInfo.put("tipo", "ERROR_BD");
            conn.send(errorInfo.toString());
        }
    }

    private boolean estaConectado(String nombre) {
        boolean encontrado = false;
        try {
            mutexConectados.acquire();
            for (InfoJugador j : conectados) {
                if (j.nombre.equals(nombre)) {
                    encontrado = true;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexConectados.release();
        }
        return encontrado;
    }

    private InfoJugador buscarJugadorConectado(String nombre) {
        InfoJugador encontrado = null;
        try {
            mutexConectados.acquire();
            for (InfoJugador j : conectados) {
                if (j.nombre.equals(nombre)) {
                    encontrado = j;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexConectados.release();
        }
        return encontrado;
    }

    private WebSocket buscarConexion(String nombre) {
        WebSocket ws = null;
        try {
            mutexConectados.acquire();
            for (InfoJugador j : conectados) {
                if (j.nombre.equals(nombre)) {
                    ws = j.ws;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexConectados.release();
        }
        return ws;
    }

    private void notificarAmistad(WebSocket conn, JSONObject obj) {
        String remitente = obj.getString("remitente");
        String destinatario = obj.getString("destinatario");
        JSONObject msg = new JSONObject();
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(10, ChronoUnit.DAYS); // La solicitud de amistad expira en 10 días
        Timestamp fecha_ini = Timestamp.from(ahora);
        Timestamp fecha_fin = Timestamp.from(expiracion);

        Notificacion n = new Notificacion(0, Notificacion.TIPO_SOLICITUD_AMISTAD, remitente, destinatario,
                Notificacion.ESTADO_PENDIENTE, fecha_ini, fecha_fin, null);
        if (n.registrarNotificacion()) {
            System.out.println("Solicitud de amistad registrada: " + remitente + " -> " + destinatario);
            WebSocket ws = buscarConexion(destinatario);
            if (ws != null && ws.isOpen()) {
                msg.put("tipo", "SOLICITUD_AMISTAD");
                msg.put("remitente", remitente);
                msg.put("fecha_ini", ahora.toString());
                msg.put("fecha_fin", expiracion.toString());
                msg.put("idNotificacion", n.getIdNotificacion());
                ws.send(msg.toString());
            }
        } else {
            System.out.println("Error al registrar solicitud de amistad: " + remitente + " -> " + destinatario);
            msg.put("tipo", "ERROR_SOLICITUD_AMISTAD");
            msg.put("destinatario", destinatario);
            conn.send(msg.toString());
        }
    }

    private void aceptarAmistad(WebSocket conn, JSONObject obj) {
        GestorNotificaciones gestorNotif = new GestorNotificaciones();
        try {
            // modificado: no se recibía AMISTAD_ACEPTADA mientras remitente está desconectado
            //gestorNotif.borrar(obj.getInt("idNotificacion")); DUDA, ARREGLAR
            GestorJugador gestorJugador = new GestorJugador();
            if (gestorJugador.insertarAmistad(obj.getString("remitente"), obj.getString("destinatario"))) {
               // actualizamos estdo a ACEPTADA en lugar de borrar en la bd
                gestorNotif.actualizarEstado(obj.getInt("idNotificacion"), Notificacion.ESTADO_ACEPTADA); 
                System.out.println("Amistad registrada entre " + obj.getString("remitente") + " y "
                        + obj.getString("destinatario"));
                WebSocket ws = buscarConexion(obj.getString("remitente"));
                if (ws != null && ws.isOpen()) {
                    JSONObject msg = new JSONObject();
                    msg.put("tipo", "AMISTAD_ACEPTADA");
                    msg.put("amigo", obj.getString("destinatario"));
                    ws.send(msg.toString());
                }
                // que la confirmación llegue siempre al remitente esté o no conectado
                JSONObject msg2 = new JSONObject();
                msg2.put("tipo", "AMISTAD_ACEPTADA");
                msg2.put("amigo", obj.getString("remitente"));
                conn.send(msg2.toString());
            }
        } catch (SQLException e) {
            System.err.println("Error al aceptar amistad: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        }
    }

    // nuevo: rechazar solicitud de amistad eliminando la notificación de la BD
    private void rechazarAmistad(WebSocket conn, JSONObject obj) {
        GestorNotificaciones gestorNotif = new GestorNotificaciones();
        try {
            int idNotificacion = obj.getInt("idNotificacion");
            Notificacion notif = gestorNotif.obtenerPorId(idNotificacion);

            if (notif != null) {
                gestorNotif.borrar(idNotificacion);

                // notificamos al que envía
                WebSocket ws = buscarConexion(notif.getRemitente());
                if (ws != null && ws.isOpen()) {
                    JSONObject msg = new JSONObject();
                    msg.put("tipo", "AMISTAD_RECHAZADA");
                    msg.put("usuario", notif.getDestinatario());
                    ws.send(msg.toString());
                }

                // confirmación al que rechaza
                conn.send(new JSONObject().put("tipo", "RECHAZO_EXITOSO").toString());

            } else {
                conn.send(new JSONObject().put("tipo", "ERROR_NOTIFICACION_NO_ENCONTRADA").toString());
            }
        } catch (SQLException e) {
            System.err.println("Error al rechazar amistad: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        }
    }

    // nuevo: envío de solicitud de partida privada
    private void gestionarInvitacionPartida(WebSocket conn, JSONObject obj) {
        String remitente = obj.getString("remitente");
        String destinatario = obj.getString("destinatario");

        // comprobamos si el destinatario está conectado
        WebSocket wsB = buscarConexion(destinatario);
        if (wsB == null || !wsB.isOpen()) {
            conn.send(new JSONObject().put("tipo", "ERROR_DESCONECTADO").toString());
            return;
        }

        // reamos la notificación en BD
        GestorNotificaciones gestor = new GestorNotificaciones();
        int idNotificacion;
        try {
            idNotificacion = gestor.enviarInvitacionPartida(remitente, destinatario);
            if (idNotificacion == -1) {
                conn.send(new JSONObject().put("tipo", "ERROR_DESCONECTADO").toString());
                return;
            }
        } catch (SQLException e) {
            System.err.println("Error al crear invitación: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_DESCONECTADO").toString());
            return;
        }

        // enviamos la invitación a B
        // !!!!!!!!!! este mensaje solo llega si el destinatario tiene el ws abierto en
        // este momento
        // (buscando partida o jugando, inicio sesión/registro). Si está desconectado o
        // simplemente
        // navegando por la app, el ws está cerrado y el mensaje se pierde.
        // Las notifs pendientes se recuperan al hacer login, pero los mensajes de error
        // (ERROR_NO_UNIDO, INVITACION_RECHAZADA) como no se guardan en BD, se pierden
        // si el WS no está abierto.
        // lo mismo pasa cuando queramos enviar notif a wsA en rechazarInvitacion() y
        // aceptarInvitacion()
        JSONObject msg = new JSONObject();
        msg.put("tipo", "INVITACION_PARTIDA");
        msg.put("remitente", remitente);
        msg.put("idNotificacion", idNotificacion);
        wsB.send(msg.toString());

        // empieza el timer de 2 minutos
        // si nadie lo cancela antes, mandamos ERROR_NO_UNIDO a A
        final int idNotifFinal = idNotificacion;
        ScheduledFuture<?>[] timer = new ScheduledFuture<?>[1];
        timer[0] = temporizador.schedule(() -> {
            timersInvitacion.remove(idNotifFinal);
            // arreglado: al terminar el timer, actualiza el estado a RECHAZADA
            // para que aceptarInvitacion() rechace cualquier intento de aceptar
            // después de los 2 minutos.
            try {
                gestor.actualizarEstado(idNotifFinal, Notificacion.ESTADO_RECHAZADA);
            } catch (SQLException e) {
                System.err.println("Error al marcar invitación expirada: " + e.getMessage());
            }
            WebSocket wsA = buscarConexion(remitente);
            if (wsA != null && wsA.isOpen()) {
                wsA.send(new JSONObject().put("tipo", "ERROR_NO_UNIDO").toString());
            }
        }, 2, TimeUnit.MINUTES);

        timersInvitacion.put(idNotificacion, timer[0]);
        // Informar al remitente del id de notificación (para que pueda cancelar)
        conn.send(new JSONObject().put("tipo", "NOTIFICACION_ENVIADA").put("idNotificacion", idNotificacion).toString());
        System.out.println(
                "Invitación privada: " + remitente + " -> " + destinatario + " (notif " + idNotificacion + ")");
    }

    // nuevo: invitación a partida privada aceptada
    private void aceptarInvitacion(WebSocket conn, JSONObject obj) {
        int idNotificacion = obj.getInt("idNotificacion");

        // cancelamos el timer para que no se mande ERROR_NO_UNIDO una vez B ha aceptado
        // (tal y como en SECUENCIAS)
        ScheduledFuture<?> timer = timersInvitacion.remove(idNotificacion);
        if (timer != null) {
            timer.cancel(false);
        }

        GestorNotificaciones gestor = new GestorNotificaciones();
        try {
            Notificacion notif = gestor.obtenerPorId(idNotificacion);
            if (notif == null) {
                System.err.println("aceptarInvitacion: notificación no encontrada id=" + idNotificacion);
                conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
                return;
            }
            // arreglado: verifica que la notificación esté PENDIENTE y
            // si está RECHAZADA (porque expiró), rechaza la aceptación con ERROR_NO_UNIDO. 
            if (!Notificacion.ESTADO_PENDIENTE.equals(notif.getEstado())) {
                conn.send(new JSONObject().put("tipo", "ERROR_NO_UNIDO").toString());
                return;
            }
            if (notif.haExpirado()) {
                gestor.actualizarEstado(idNotificacion, Notificacion.ESTADO_RECHAZADA);
                conn.send(new JSONObject().put("tipo", "ERROR_NO_UNIDO").toString());
                return;
            }
            System.out.println("Invitación aceptada: " + notif.getRemitente() + " -> " + notif.getDestinatario());

            // actualizamos el estado de la notificación a ACEPTADA
            gestor.actualizarEstado(idNotificacion, Notificacion.ESTADO_ACEPTADA);
            InfoJugador j1 = buscarJugadorConectado(notif.getRemitente());
            InfoJugador j2 = buscarJugadorConectado(notif.getDestinatario());

            if (j1 == null || j2 == null) {
                System.err.println("aceptarInvitacion: jugador no encontrado en conectados (j1=" + j1 + ", j2=" + j2 + ")");
                conn.send(new JSONObject().put("tipo", "ERROR_DESCONECTADO").toString());
                return;
            }

            Pareja pj = new Pareja(j1, j2, "PRIVADA");
            try {
                mutexParejas.acquire();
                parejas.add(pj);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutexParejas.release();
            }
            System.out.println("Partida privada iniciada con id: " + pj.partida.getIDPartida());
            iniciar(pj, "PARTIDA_PRIVADA_ENCONTRADA");

        } catch (SQLException e) {
            System.err.println("Error SQL al aceptar invitación: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        } catch (Exception e) {
            System.err.println("Error inesperado al aceptar invitación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // nueva: invitación a partida privada rechazada
    private void rechazarInvitacion(WebSocket conn, JSONObject obj) {
        int idNotificacion = obj.getInt("idNotificacion");

        // se cancela el timer (como en SECUENCIAS)
        ScheduledFuture<?> timer = timersInvitacion.remove(idNotificacion);
        if (timer != null) {
            timer.cancel(false);
        }

        // rechazamos la notificación en BD
        GestorNotificaciones gestor = new GestorNotificaciones();
        try {
            Notificacion notif = gestor.obtenerPorId(idNotificacion);
            if (notif == null) {
                conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
                return;
            }

            gestor.rechazarNotificacion(idNotificacion, notif.getDestinatario());

            // avisamos al remitente
            WebSocket wsA = buscarConexion(notif.getRemitente());
            if (wsA != null && wsA.isOpen()) {
                wsA.send(new JSONObject().put("tipo", "INVITACION_RECHAZADA").toString());
            }

            System.out.println("Invitación rechazada por " + notif.getDestinatario());

        } catch (SQLException e) {
            System.err.println("Error al rechazar invitación: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        }
    }

    public void registrarJugador(WebSocket conn, JSONObject obj) {
        String correo = obj.getString("correo");
        String nombre = obj.getString("nombre");
        String contrasena = obj.getString("password");

        // nuevo añadimos avatar a la hora de registrar (skin no, va por defecto en inserción en base de datos)
        // y añadimos campo avatar al constructor
        String avatar_id = obj.isNull("avatar_id") ? null : obj.optString("avatar_id", null);
        Jugador prueba = new Jugador(correo, nombre, contrasena, avatar_id); // El constructor ya hashea la contraseña internamente
        if (prueba.registrarse() && !estaConectado(nombre)) {
            System.out.println("Jugador registrado");
            try {
                mutexConectados.acquire();
                conectados.add(new InfoJugador(conn, nombre, 0, avatar_id)); // Inicialmente el jugador se registra con 0 puntos
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutexConectados.release();
            }
            conn.send(new JSONObject().put("tipo", "REGISTRO_EXITOSO").toString());
        } else {
            System.out.println("Jugador NO registrado o ya conectado");
            conn.send(new JSONObject().put("tipo", "REGISTRO_ERRONEO").toString());
        }
    }

    public void cancelarBusqueda(WebSocket conn) {
        try {
            mutex.acquire(); // WAIT
            InfoJugador jugadorBorrar = null;
            for (InfoJugador j : buscando_partida) {
                if (j.ws == conn) {
                    jugadorBorrar = j;
                }
            }
            if (jugadorBorrar != null) {
                buscando_partida.remove(jugadorBorrar);
                conn.send(new JSONObject().put("tipo", "CANCELAR_EXITO").toString());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release(); // SIGNAL
        }
    }

    public void buscarJugadores(WebSocket conn, JSONObject obj) {
        String raiz = obj.getString("raiz");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            List<Jugador> jugadores = gestorJugador.buscarJugadoresPorRaiz(raiz);
            if (jugadores.isEmpty()) {
                conn.send(new JSONObject().put("tipo", "NO_ENCONTRADOS").toString());
                return;
            }
            JSONArray infoJugadores = new JSONArray();
            for (Jugador j : jugadores) {
                JSONObject info = new JSONObject();
                info.put("nombre", j.getNombre());
                info.put("puntos", j.getPuntos());
                info.put("avatar_id", j.getAvatarId());
                infoJugadores.put(info);
            }

            JSONObject msg = new JSONObject();
            msg.put("tipo", "INFORMACION_JUGADORES");
            msg.put("info", infoJugadores);
            conn.send(msg.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error al buscar por raiz a jugadores: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "NO_ENCONTRADOS").toString());
        }
    }

    public void buscarAmigos(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            List<Jugador> jugadores = gestorJugador.sacarAmigos(usuario);
            if (jugadores.isEmpty()) {
                conn.send(new JSONObject().put("tipo", "NO_AMIGOS").toString());
                return;
            }
            JSONArray infoJugadores = new JSONArray();
            for (Jugador j : jugadores) {
                JSONObject info = new JSONObject();
                info.put("nombre", j.getNombre());
                info.put("puntos", j.getPuntos());
                info.put("avatar_id", j.getAvatarId());
                infoJugadores.put(info);
            }

            JSONObject msg = new JSONObject();
            msg.put("tipo", "INFORMACION_AMIGOS");
            msg.put("info", infoJugadores);
            conn.send(msg.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error al buscar amigos: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_AMIGOS").toString());
        }
    }

    public void borrarAmigo(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        String amigo = obj.getString("amigo");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            JSONObject msg = new JSONObject();
            if (gestorJugador.borrarAmigo(usuario, amigo)) {
                msg.put("tipo", "AMIGO_BORRADO");
                conn.send(msg.toString());
            } else {
                msg.put("tipo", "ERROR_AL_BORRAR_AMIGO");
                conn.send(msg.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error al buscar amigos: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_AL_BORRAR_AMIGO").toString());
        }
    }

    public void setTrampa(WebSocket conn, JSONObject obj){
        int equipo = obj.getInt("equipo");
        int fila = obj.getInt("fila");
        int columna = obj.getInt("columna");
        Pareja pj = null;
        try {
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                if (pareja.buscar(conn)) {
                    pj = pareja;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); // SIGNAL
        }

        if(pj != null){
            int estado = pj.partida.setTrampa(equipo, fila, columna);
            if (estado == -1) {
                JSONObject msg = new JSONObject();
                msg.put("tipo", "TRAMPA_INVALIDA");
                conn.send(msg.toString());
            }else if(estado == 1){
                //Iniciamos la seleccion de cartas de Accion para J1
                JSONObject msg1 = new JSONObject();
                msg1.put("tipo", "SELECCIONE_CARTA_ACCION"); 
                JSONArray cartasJSON1 = new JSONArray();
                for (CartaAccion ca : pj.partida.getCartasAccion()) {
                    if (ca.getEquipo() == -1) {
                        JSONObject cartaJSON = new JSONObject();
                        cartaJSON.put("nombre", ca.getNombre());
                        cartaJSON.put("accion", ca.getAccion());
                        cartasJSON1.put(cartaJSON);
                    }
                }
                msg1.put("cartas_accion", cartasJSON1);
                pj.p1.ws.send(msg1.toString());

                //Iniciamos la seleccion de cartas de Accion para J2
                JSONObject msg2 = new JSONObject();
                msg2.put("tipo", "SELECCIONE_CARTA_ACCION"); 
                JSONArray cartasJSON2 = new JSONArray();
                for (CartaAccion ca : pj.partida.getCartasAccion()) {
                    if (ca.getEquipo() == -2) {
                        JSONObject cartaJSON = new JSONObject();
                        cartaJSON.put("nombre", ca.getNombre());
                        cartaJSON.put("accion", ca.getAccion());
                        cartasJSON2.put(cartaJSON);
                    }
                }
                msg2.put("cartas_accion", cartasJSON2);
                pj.p2.ws.send(msg2.toString());
            }
        }
    }

    public void jugarCartaAccion(WebSocket conn, JSONObject obj){
        String nomCartaAcc = obj.getString("cartaAccion");
        int equipo = obj.getInt("equipo");
        int x = obj.getInt("x");
        int y = obj.getInt("y");
        String cartaRobar = obj.getString("cartaRobar");
        int xOp = obj.getInt("x_op");
        int yOp = obj.getInt("y_op");
        Pareja pj = null;
        try {
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                if (pareja.buscar(conn)) {
                    pj = pareja;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); // SIGNAL
        }

        if(pj != null){
            final int idPartida = pj.partida.getIDPartida();

            // Cancelar timer anterior al recibir acción de carta
            ScheduledFuture<?> timerAntiguo = esperaPartida.remove(idPartida);
            if (timerAntiguo != null) timerAntiguo.cancel(false);

            int estado = pj.partida.jugarAccion(nomCartaAcc, x, y, equipo, xOp, yOp, cartaRobar);
            if(estado < 0){
                JSONObject msg = new JSONObject();
                msg.put("tipo", "CARTA_ACCION_INVALIDA");
                conn.send(msg.toString());
                // Acción inválida: el turno no avanzó, reiniciar timer para el mismo jugador
                iniciarTimerTurno(pj, pj.partida.getTurno());
            }else if(estado != 0){
                //Uno de los dos se quedo sin movimientos
                pj.partida.finalizarPartida();
                JSONObject msg1 = new JSONObject();
                JSONObject msg2 = new JSONObject();
                esperaPartida.remove(idPartida);
                try {
                    mutexParejas.acquire();
                    parejas.remove(pj);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutexParejas.release();
                }
                
                msg1.put("tipo", "DERROTA");
                msg1.put("motivo", "SIN_MOV");
                msg1.put("equipo_responsable", (equipo==1) ? 2 : 1);
                msg2.put("tipo", "VICTORIA");
                msg2.put("motivo", "SIN_MOV");
                msg2.put("equipo_responsable", (equipo==1) ? 2 : 1);

                InfoJugador oponente = pj.getOponente(conn);
                oponente.ws.send((equipo == estado) ? msg1.toString() : msg2.toString());
                conn.send((equipo == estado) ? msg2.toString() : msg1.toString());
                // Juego terminado: no iniciar nuevo timer
            }else{
                estado = pj.partida.eraTrampa(nomCartaAcc);
                if(estado == 1){
                    //Uno de los dos se quedo sin movimientos
                    pj.partida.finalizarPartida();
                    JSONObject msg1 = new JSONObject();
                    JSONObject msg2 = new JSONObject();
                    esperaPartida.remove(idPartida);
                    try {
                        mutexParejas.acquire();
                        parejas.remove(pj);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        mutexParejas.release();
                    }
                    
                    msg1.put("tipo", "DERROTA");
                    msg1.put("motivo", "REY_EN_TRAMPA");
                    msg1.put("equipo_responsable", equipo);
                    msg2.put("tipo", "VICTORIA");
                    msg2.put("motivo", "REY_EN_TRAMPA");
                    msg2.put("equipo_responsable", equipo);

                    InfoJugador oponente = pj.getOponente(conn);
                    oponente.ws.send(msg2.toString());
                    conn.send(msg1.toString());
                    // Juego terminado: no iniciar nuevo timer
                }else if(estado == 2){
                    JSONObject msgPeonMuerto = new JSONObject();
                    msgPeonMuerto.put("tipo", "PEON_MUERTO");
                    msgPeonMuerto.put("pos_x", x);
                    msgPeonMuerto.put("pos_y", y);

                    InfoJugador oponente = pj.getOponente(conn);
                    oponente.ws.send(msgPeonMuerto.toString());
                    conn.send(msgPeonMuerto.toString());

                    // Aunque el peón muera por trampa, la carta de acción se ha jugado y
                    // el rival debe sincronizar cambio de turno/estado de carta.
                    String accionTipo = "";
                    for (CartaAccion ca : pj.partida.getCartasAccion()) {
                        if (ca.getNombre().equals(nomCartaAcc)) {
                            accionTipo = ca.getAccion();
                            break;
                        }
                    }
                    JSONObject msgAccion = new JSONObject();
                    msgAccion.put("tipo", "CARTA_ACCION_JUGADA");
                    msgAccion.put("carta_accion", nomCartaAcc);
                    msgAccion.put("accion", accionTipo);
                    msgAccion.put("x", x);
                    msgAccion.put("y", y);
                    msgAccion.put("x_op", xOp);
                    msgAccion.put("y_op", yOp);
                    msgAccion.put("carta_robar", cartaRobar);
                    oponente.ws.send(msgAccion.toString());

                    try {
                        pj.partida.actualizarBD();
                    } catch (Exception e) {
                        System.err.println("Error al actualizar BD después de PEON_MUERTO: " + e.getMessage());
                    }

                    // Reiniciar timer para quien deba mover ahora
                    iniciarTimerTurno(pj, pj.partida.getTurno());
                }else{
                    String accionTipo = "";
                    for (CartaAccion ca : pj.partida.getCartasAccion()) {
                        if (ca.getNombre().equals(nomCartaAcc)) {
                            accionTipo = ca.getAccion();
                            break;
                        }
                    }
                    JSONObject msg = new JSONObject();
                    msg.put("tipo", "CARTA_ACCION_JUGADA");
                    msg.put("carta_accion", nomCartaAcc);
                    msg.put("accion", accionTipo);
                    msg.put("x", x);
                    msg.put("y", y);
                    msg.put("x_op", xOp);
                    msg.put("y_op", yOp);
                    msg.put("carta_robar", cartaRobar);
                    pj.getOponente(conn).ws.send(msg.toString()); //Avisamos al oponente de la carta que se ha jugado
                    // actualizamos turno y estado de cartas en bd
                    try {
                        pj.partida.actualizarBD();
                    } catch (Exception e) {
                        System.err.println("Error al actualizar BD después de jugar acción: " + e.getMessage());
                    }
                    // Reiniciar timer para quien deba mover ahora
                    iniciarTimerTurno(pj, pj.partida.getTurno());
                }
            }
        }
    }

    public void seleccionarCartaAccion(WebSocket conn, JSONObject obj){
        String carta = obj.getString("carta");
        int equipo = obj.getInt("equipo");
        Pareja pj = null;
        try {
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                if (pareja.buscar(conn)) {
                    pj = pareja;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); // SIGNAL
        }

        if(pj != null){
            boolean estado = pj.partida.setEquipoCartaAccion(carta, equipo);
            if(!estado){
                JSONObject msg = new JSONObject();
                msg.put("tipo", "CARTA_ACCION_INVALIDA");
                conn.send(msg.toString());
            }else{
                pj.eleccion++;
                if(pj.eleccion == 2){
                    JSONObject msg1 = new JSONObject();
                    JSONObject msg2 = new JSONObject();
                    msg1.put("tipo", "PARTIDA_LISTA"); 
                    msg2.put("tipo", "PARTIDA_LISTA");
                    JSONArray cartasJSON1 = new JSONArray();
                    JSONArray cartasJSON2 = new JSONArray();
                    for (CartaAccion ca : pj.partida.getCartasAccion()) {
                        if (ca.getEquipo()==1) {
                            JSONObject cartaJSON1 = new JSONObject();
                            cartaJSON1.put("nombre", ca.getNombre());
                            cartaJSON1.put("accion", ca.getAccion());
                            cartasJSON1.put(cartaJSON1);
                        } else if (ca.getEquipo()==2) {
                            JSONObject cartaJSON2 = new JSONObject();
                            cartaJSON2.put("nombre", ca.getNombre());
                            cartaJSON2.put("accion", ca.getAccion());
                            cartasJSON2.put(cartaJSON2);
                        }
                    }
                    msg1.put("cartas_accion", cartasJSON1);
                    msg2.put("cartas_accion", cartasJSON2);
                    // msg1 siempre a p1 (equipo 1) y msg2 siempre a p2 (equipo 2)
                    pj.p1.ws.send(msg1.toString());
                    pj.p2.ws.send(msg2.toString());
                    iniciarTimerTurno(pj, pj.partida.getTurno());
                }
            }
        }
    }

    public void solicitarPartidas(WebSocket conn, JSONObject obj, String tipo) {
        String usuario = obj.getString("usuario");
        GestorPartida gestorPartida = new GestorPartida();
        JSONObject msg = new JSONObject();
        if (tipo.equals("PRIVADA")) {
            String amigo = obj.getString("amigo");
            try {
                List<Partida> partidas = gestorPartida.buscarPartidasJugadorPrivadas(usuario, amigo);
                msg.put("tipo", "PARTIDAS_PRIVADAS");
                msg.put("oponente", amigo);
                JSONArray partidasJSON = new JSONArray();
                for (Partida p : partidas) {
                    JSONObject partidaJSON = new JSONObject();
                    String estado = p.getEstado();
                    partidaJSON.put("partida_id", p.getIDPartida());
                    partidaJSON.put("oponente", amigo);
                    partidaJSON.put("estado", estado);
                    partidaJSON.put("tiempo", p.getTiempo());
                    if (p.isEs_Ganador_J1()) {
                        partidaJSON.put("ganador", p.getJ1());
                    } else if (p.isEs_Ganador_J2()) {
                        partidaJSON.put("ganador", p.getJ2());
                    } else if (estado.equals("FINALIZADA")) {
                        partidaJSON.put("ganador", "Empate");
                    } else {
                        partidaJSON.put("ganador", "NO_HAY");
                    }
                    partidasJSON.put(partidaJSON);
                }
                msg.put("partidas", partidasJSON);
                conn.send(msg.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("SQL State: " + e.getSQLState());
                System.err.println("Error al buscar partidas privadas: " + e.getMessage());
                conn.send(new JSONObject().put("tipo", "ERROR_AL_BUSCAR_PARTIDAS_PRIV").toString());
            }
        } else if (tipo.equals("PUBLICA")) {
            try {
                List<Partida> partidas = gestorPartida.buscarPartidasJugadorPublicas(usuario);
                msg.put("tipo", "PARTIDAS_PUBLICAS");
                JSONArray partidasJSON = new JSONArray();
                for (Partida p : partidas) {
                    JSONObject partidaJSON = new JSONObject();
                    String oponente;
                    if (usuario.equals(p.getJ1())) {
                        oponente = p.getJ2();
                    } else {
                        oponente = p.getJ1();
                    }
                    String estado = p.getEstado();
                    partidaJSON.put("partida_id", p.getIDPartida());
                    partidaJSON.put("oponente", oponente);
                    partidaJSON.put("estado", estado);
                    partidaJSON.put("tiempo", p.getTiempo());
                    if (p.isEs_Ganador_J1()) {
                        partidaJSON.put("ganador", p.getJ1());
                    } else if (p.isEs_Ganador_J2()) {
                        partidaJSON.put("ganador", p.getJ2());
                    } else if (estado.equals("FINALIZADA")) {
                        partidaJSON.put("ganador", "Empate");
                    } else {
                        partidaJSON.put("ganador", "NO_HAY");
                    }
                    partidasJSON.put(partidaJSON);
                }
                msg.put("partidas", partidasJSON);
                conn.send(msg.toString());
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("SQL State: " + e.getSQLState());
                System.err.println("Error al buscar partidas publicas: " + e.getMessage());
                conn.send(new JSONObject().put("tipo", "ERROR_AL_BUSCAR_PARTIDAS_PUB").toString());
            }
        }
    }

    public Servidor(int puerto) {
        super(new InetSocketAddress(puerto));
        conectados = new ArrayList<>();
        buscando_partida = new ArrayList<>();
        parejas = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Jugador conectado al servidor -> " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Jugador desconectado -> " + conn.getRemoteSocketAddress());
        try {
            mutex.acquire(); // WAIT
            buscando_partida.removeIf(jugador -> jugador.ws == conn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release(); // SIGNAL
        }

        // elimnamos al jugador de conectados para que pueda volver a iniciar sesión,
        // porque
        // sin esto, al cerrar el WS de login el jugador queda como ausente,
        // estaConectado() seguiría devolviendo true y el login fallaría
        try {
            mutexConectados.acquire();
            conectados.removeIf(jugador -> jugador.ws == conn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexConectados.release();
        }
    }

// VERSIÓN PARA JAVA 14 O SUPERIORES QUE CON -> EVITA FALL THROUGH EN LUGAR DE CASE "HOLA": funcion(); break;
        @Override
        public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);

        hilos.submit(() -> {
            JSONObject obj = new JSONObject(message);
            String tipoMSG = obj.getString("tipo");
            switch (tipoMSG) {
                case "BUSCAR_PARTIDA"         -> gestionarBusquedaPartida(conn, obj);
                case "MOVER"                  -> gestionarPartida(conn, obj);
                case "INICIAR_SESION"         -> iniciarSesion(conn, obj);
                case "REGISTRARSE"            -> registrarJugador(conn, obj);
                case "ABANDONAR"              -> abandonarPartida(conn, obj);
                case "CANCELAR"               -> cancelarBusqueda(conn);
                case "SOLICITUD_AMISTAD"      -> notificarAmistad(conn, obj);
                case "ACEPTAR_AMISTAD"        -> aceptarAmistad(conn, obj);
                case "RECHAZAR_AMISTAD"       -> rechazarAmistad(conn, obj);
                case "INVITACION_PARTIDA"     -> gestionarInvitacionPartida(conn, obj);
                case "ACEPTAR_INVITACION"     -> aceptarInvitacion(conn, obj);
                case "RECHAZAR_INVITACION"    -> rechazarInvitacion(conn, obj);
                case "OBTENER_PERFIL"         -> obtenerPerfil(conn, obj);
                case "BUSCAR_JUGADORES"       -> buscarJugadores(conn, obj);
                case "OBTENER_AMIGOS"         -> buscarAmigos(conn, obj);
                case "BORRAR_AMIGO"           -> borrarAmigo(conn, obj);
                case "SOLICITAR_PARTIDAS_PUB" -> solicitarPartidas(conn, obj, "PUBLICA");
                case "SOLICITAR_PARTIDAS_PRIV"-> solicitarPartidas(conn, obj, "PRIVADA");
                case "OBTENER_CARTAS"         -> obtenerCartas(conn, obj);
                case "OBTENER_CARTAS_ACCION"  -> obtenerCartasAccion(conn, obj);
                case "CANCELAR_NOTIFICACION"  -> cancelarNotificacion(conn, obj);
                case "SOLICITAR_PAUSA"        -> gestionarSolicitudPausa(conn, obj);
                case "ACEPTAR_PAUSA"          -> gestionarRespuestaPausa(conn, obj, true);
                case "RECHAZAR_PAUSA"         -> gestionarRespuestaPausa(conn, obj, false);
                case "SOLICITAR_REANUDAR"     -> gestionarSolicitudReanudar(conn, obj);
                case "ACEPTAR_REANUDAR"       -> gestionarRespuestaReanudar(conn, obj, true);
                case "RECHAZAR_REANUDAR"      -> gestionarRespuestaReanudar(conn, obj, false);
                case "PONER_TRAMPA"           -> setTrampa(conn, obj);
                case "CARTA_ACCION"           -> seleccionarCartaAccion(conn, obj);
                case "JUGAR_CARTA_ACCION"     -> jugarCartaAccion(conn, obj);
                case "TIEMPO_AGOTADO"         -> gestionarTiempoAgotado(conn);
                case "OBTENER_TIENDA_SKINS"   -> obtenerTiendaSkins(conn, obj);
                case "COMPRAR_SKIN"           -> comprarSkin(conn, obj);
                case "ACTIVAR_SKIN"           -> activarSkin(conn, obj);
                case "RECUPERAR_CONTRASENA"   -> recuperarContrasena(conn, obj);
                case "CAMBIAR_CONTRASENA"     -> cambiarContrasena(conn, obj);
                case "CAMBIAR_AVATAR"         -> cambiarAvatar(conn, obj);
            }
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor iniciado, puerto -> " + getPort());
    }

    private void obtenerPerfil(WebSocket conn, JSONObject obj) {
        try {
            String nombre = obj.getString("nombre");
            Jugador j = new GestorJugador().buscarJugador(nombre);
            if (j == null) {
                System.out.println("OBTENER_PERFIL: jugador no encontrado -> " + nombre);
                return;
            }
            JSONObject msg = new JSONObject();
            msg.put("tipo", "PERFIL_ACTUALIZADO");
            msg.put("nombre", j.getNombre());
            msg.put("correo", j.getCorreo());
            msg.put("puntos", j.getPuntos());
            msg.put("partidas_ganadas", j.getPartidasGanadas());
            msg.put("partidas_jugadas", j.getPartidasJugadas());
            msg.put("cores", j.getCores());
            // nuevo
            msg.put("skin_activa", j.getSkinActiva());
            msg.put("avatar_id", j.getAvatarId());
            System.out.println("PERFIL_ACTUALIZADO enviado -> " + nombre
                    + " | puntos=" + j.getPuntos()
                    + " | cores=" + j.getCores()
                    + " | ganadas=" + j.getPartidasGanadas()
                    + " | jugadas=" + j.getPartidasJugadas());
            conn.send(msg.toString());
        } catch (Exception e) {
            System.err.println("Error al obtener perfil: " + e.getMessage());
        }
    }

    private void obtenerCartas(WebSocket conn, JSONObject obj) {
        try {
            GestorCartasMov gestorCartas = new GestorCartasMov();
            List<CartaMov> cartasData = gestorCartas.sacarCartas();
            JSONArray arregloCartas = new JSONArray();
            for (CartaMov c : cartasData) {
                JSONObject cJson = new JSONObject();
                cJson.put("nombre", c.getNombre());
                cJson.put("puntos_necesarios", c.getPuntosMin());
                arregloCartas.put(cJson);
            }
            JSONObject msg = new JSONObject();
            msg.put("tipo", "LISTA_CARTAS");
            msg.put("cartas", arregloCartas);
            conn.send(msg.toString());
        } catch (SQLException e) {
            System.err.println("Error al obtener cartas: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        }
    }

    private void obtenerCartasAccion(WebSocket conn, JSONObject obj) {
        try {
            GestorCartasAccion gestorCartas = new GestorCartasAccion();
            List<CartaAccion> cartasData = gestorCartas.sacarCartas();
            JSONArray arregloCartas = new JSONArray();
            for (CartaAccion c : cartasData) {
                JSONObject cJson = new JSONObject();
                cJson.put("nombre", c.getNombre());
                cJson.put("puntos_necesarios", c.getPuntosMin());
                cJson.put("descripcion", c.getAccion());
                arregloCartas.put(cJson);
            }
            JSONObject msg = new JSONObject();
            msg.put("tipo", "LISTA_CARTAS_ACCION");
            msg.put("cartas", arregloCartas);
            conn.send(msg.toString());
        } catch (SQLException e) {
            System.err.println("Error al obtener cartas de accion: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        }
    }

    private void cancelarNotificacion(WebSocket conn, JSONObject obj) {
        try {
            int idNotificacion = obj.getInt("idNotificacion");

            // Cancelar timer de invitación o de reanudar si existe
            ScheduledFuture<?> timer = timersInvitacion.remove(idNotificacion);
            if (timer == null) timer = timersReanudar.remove(idNotificacion);
            if (timer != null) timer.cancel(false);

            // Marcar notificación como rechazada en BD
            GestorNotificaciones gestorNotif = new GestorNotificaciones();
            Notificacion notif = gestorNotif.obtenerPorId(idNotificacion);
            if (notif != null) {
                gestorNotif.actualizarEstado(idNotificacion, Notificacion.ESTADO_RECHAZADA);
                // Notificar al destinatario que fue cancelada
                WebSocket wsDestino = buscarConexion(notif.getDestinatario());
                if (wsDestino != null && wsDestino.isOpen()) {
                    wsDestino.send(new JSONObject().put("tipo", "NOTIFICACION_CANCELADA").put("idNotificacion", idNotificacion).toString());
                }
            }
            System.out.println("Notificación " + idNotificacion + " cancelada por el remitente.");
        } catch (Exception e) {
            System.err.println("Error al cancelar notificación: " + e.getMessage());
        }
    }

    private void gestionarSolicitudPausa(WebSocket conn, JSONObject obj) {
        try {
            String remitente = obj.getString("remitente");
            String destinatario = obj.getString("destinatario");
            int idPartida = obj.getInt("idPartida");

            // Cancelar el timer de turno mientras se espera la respuesta de pausa
            ScheduledFuture<?> timerPausa = esperaPartida.remove(idPartida);
            if (timerPausa != null) timerPausa.cancel(false);

            GestorNotificaciones gestor = new GestorNotificaciones();
            int idNotif = gestor.enviarSolicitudPausa(remitente, destinatario, idPartida);

            WebSocket wsDestino = buscarConexion(destinatario);
            if (wsDestino != null && wsDestino.isOpen()) {
                JSONObject aviso = new JSONObject();
                aviso.put("tipo", "SOLICITUD_PAUSA");
                aviso.put("remitente", remitente);
                aviso.put("idNotificacion", idNotif);
                wsDestino.send(aviso.toString());
                System.out.println("Solicitud pausa: " + remitente + " -> " + destinatario + " (partida " + idPartida + ")");
            }

        } catch (SQLException e) {
            System.err.println("Error al gestionar solicitud de pausa: " + e.getMessage());
        }
    }

    private Pareja buscarParejaPorNombre(String nombre) {
        try {
            mutexParejas.acquire();
            for (Pareja pj : parejas) {
                if (pj.p1.nombre.equals(nombre) || pj.p2.nombre.equals(nombre)) {
                    return pj;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release();
        }
        return null;
    }

    private void gestionarRespuestaPausa(WebSocket conn, JSONObject obj, boolean aceptar) {
        try {
            int idNotificacion = obj.getInt("idNotificacion");
            String nombreQuienResponde = obj.getString("nombre");

            GestorNotificaciones gestor = new GestorNotificaciones();
            Notificacion notif = gestor.obtenerPorId(idNotificacion);
            if (notif == null) return;

            if (aceptar) {
                boolean ok = gestor.aceptarNotificacion(idNotificacion, nombreQuienResponde);
                if (ok) {
                    Pareja pj = buscarParejaPorNombre(notif.getRemitente());
                    if (pj == null) pj = buscarParejaPorNombre(notif.getDestinatario());
                    if (pj != null) {
                        // Cancelar timer de turno por si aún estaba activo
                        ScheduledFuture<?> timer = esperaPartida.remove(pj.partida.getIDPartida());
                        if (timer != null) timer.cancel(false);
                        boolean pausada = pj.partida.pausarPartida(); // cambia estado a PAUSADA y lo persiste
                        if (!pausada) {
                            System.err.println("pausarPartida falló para partida " + pj.partida.getIDPartida() + " (estado=" + pj.partida.getEstado() + "). Forzando estado PAUSADA.");
                            // Forzar actualización directa en BD por si hay inconsistencia de mayúsculas
                            try { new GestorPartida().updateEstado(pj.partida.getIDPartida(), "PAUSADA"); } catch (Exception ex) { ex.printStackTrace(); }
                        }
                        pj.partida.actualizarBD();  // guarda tablero, fichas y turno
                        try {
                            mutexParejas.acquire();
                            parejas.remove(pj); // liberamos de memoria
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            mutexParejas.release();
                        }
                        JSONObject msg = new JSONObject().put("tipo", "PARTIDA_PAUSADA");
                        pj.p1.ws.send(msg.toString());
                        pj.p2.ws.send(msg.toString());
                        System.out.println("Partida " + pj.partida.getIDPartida() + " pausada.");
                    }
                }
            } else {
                gestor.rechazarNotificacion(idNotificacion, nombreQuienResponde);
                WebSocket wsRemitente = buscarConexion(notif.getRemitente());
                System.out.println("Pausa rechazada por " + notif.getDestinatario());
                if (wsRemitente != null && wsRemitente.isOpen()) {
                    wsRemitente.send(new JSONObject().put("tipo", "PAUSA_RECHAZADA").toString());
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al gestionar respuesta de pausa: " + e.getMessage());
        }
    }

    private void gestionarSolicitudReanudar(WebSocket conn, JSONObject obj) {
        try {
            String remitente = obj.getString("remitente");
            String destinatario = obj.getString("destinatario");
            int idPartida = obj.getInt("idPartida");

            WebSocket wsB = buscarConexion(destinatario);
            if (wsB == null || !wsB.isOpen()) {
                conn.send(new JSONObject().put("tipo", "ERROR_DESCONECTADO").toString());
                return;
            }

            GestorNotificaciones gestor = new GestorNotificaciones();
            int idNotif = gestor.enviarSolicitudReanudar(remitente, destinatario, idPartida);

            JSONObject aviso = new JSONObject();
            aviso.put("tipo", "SOLICITUD_REANUDAR");
            aviso.put("remitente", remitente);
            aviso.put("idNotificacion", idNotif);
            aviso.put("idPartida", idPartida);
            wsB.send(aviso.toString());

            // timer de los 2 minutos esperando a que el amigo se una, como en partida privada desde 0
            final int idNotifFinal = idNotif;
            ScheduledFuture<?>[] timer = new ScheduledFuture<?>[1];
            timer[0] = temporizador.schedule(() -> {
                timersReanudar.remove(idNotifFinal);
                WebSocket wsA = buscarConexion(remitente);
                if (wsA != null && wsA.isOpen()) {
                    wsA.send(new JSONObject().put("tipo", "ERROR_NO_UNIDO").toString());
                }
            }, 2, TimeUnit.MINUTES);

            timersReanudar.put(idNotif, timer[0]);
            // Informar al remitente del id de notificación (para que pueda cancelar)
            conn.send(new JSONObject().put("tipo", "NOTIFICACION_ENVIADA").put("idNotificacion", idNotif).toString());
            System.out.println("Solicitud reanudar: " + remitente + " -> " + destinatario + " (notif " + idNotif + ")");

        } catch (SQLException e) {
            System.err.println("Error al gestionar solicitud de reanudar: " + e.getMessage());
        }
    }

    private void gestionarRespuestaReanudar(WebSocket conn, JSONObject obj, boolean aceptar) {
        try {
            int idNotificacion = obj.getInt("idNotificacion");
            String nombreQuienResponde = obj.getString("nombre");

            // Cancelar timer
            ScheduledFuture<?> timer = timersReanudar.remove(idNotificacion);
            if (timer != null) timer.cancel(false);

            GestorNotificaciones gestor = new GestorNotificaciones();
            Notificacion notif = gestor.obtenerPorId(idNotificacion);
            if (notif == null) return;

            if (aceptar) {
                boolean ok = gestor.aceptarNotificacion(idNotificacion, nombreQuienResponde);
                System.out.println("gestionarRespuestaReanudar: aceptarNotificacion ok=" + ok);
                if (ok) {
                    // cargamos partida de la base
                    GestorPartida gestorPartida = new GestorPartida();
                    Integer idPartida = notif.getIdPartida();
                    System.out.println("gestionarRespuestaReanudar: buscando partida id=" + idPartida);
                    Partida partidaGuardada = idPartida != null ? gestorPartida.buscarPorId(idPartida) : null;
                    if (partidaGuardada == null) {
                        System.err.println("gestionarRespuestaReanudar: partida no encontrada id=" + idPartida);
                        conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
                        return;
                    }
                    partidaGuardada.cargarCartas(); // cargar cartasM y cartasA desde BD

                    // Importante: al reanudar, los equipos NO dependen de quién solicita
                    // reanudar. Deben respetar J1/J2 guardados en la partida original.
                    InfoJugador j1 = buscarJugadorConectado(partidaGuardada.getJ1());
                    InfoJugador j2 = buscarJugadorConectado(partidaGuardada.getJ2());
                    System.out.println("gestionarRespuestaReanudar: eq1=" + (j1 != null ? j1.nombre : "null") + ", eq2=" + (j2 != null ? j2.nombre : "null"));
                    if (j1 == null || j2 == null) {
                        System.err.println("gestionarRespuestaReanudar: jugador no conectado");
                        conn.send(new JSONObject().put("tipo", "ERROR_DESCONECTADO").toString());
                        return;
                    }
                    Pareja pj = new Pareja(j1, j2, partidaGuardada);
                    try {
                        mutexParejas.acquire();
                        parejas.add(pj);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        mutexParejas.release();
                    }

                    // PARTIDA_PRIVADA_ENCONTRADA con el tablero guardado
                    JSONArray mazoJ1 = new JSONArray();
                    JSONArray mazoJ2 = new JSONArray();
                    JSONArray cola = new JSONArray();
                    cartasPartida(pj, mazoJ1, mazoJ2, cola);

                    JSONArray cartasAccionJ1 = new JSONArray();
                    JSONArray cartasAccionJ2 = new JSONArray();
                    cartasAccionPartida(pj, cartasAccionJ1, cartasAccionJ2);

                    JSONObject msg1 = new JSONObject();
                    msg1.put("tipo", "PARTIDA_PRIVADA_ENCONTRADA");
                    msg1.put("partida_id", partidaGuardada.getIDPartida());
                    msg1.put("equipo", 1);
                    msg1.put("oponente", j2.nombre);
                    msg1.put("oponentePt", j2.puntos);
                    msg1.put("oponente_avatar_id", j2.avatarId);
                    msg1.put("cartas_jugador", mazoJ1);
                    msg1.put("cartas_oponente", mazoJ2);
                    msg1.put("carta_siguiente", cola);
                    msg1.put("tablero_eq1", partidaGuardada.getPos_Fichas_Eq1());
                    msg1.put("tablero_eq2", partidaGuardada.getPos_Fichas_Eq2());
                    msg1.put("turno", partidaGuardada.getTurno());
                    msg1.put("trampa_j1_pos", partidaGuardada.getTrampaPosJ1());
                    msg1.put("trampa_j2_pos", partidaGuardada.getTrampaPosJ2());
                    msg1.put("cartas_accion_jugador", cartasAccionJ1);
                    msg1.put("cartas_accion_oponente", cartasAccionJ2);

                    JSONObject msg2 = new JSONObject();
                    msg2.put("tipo", "PARTIDA_PRIVADA_ENCONTRADA");
                    msg2.put("partida_id", partidaGuardada.getIDPartida());
                    msg2.put("equipo", 2);
                    msg2.put("oponente", j1.nombre);
                    msg2.put("oponentePt", j1.puntos);
                    msg2.put("oponente_avatar_id", j1.avatarId);
                    msg2.put("cartas_jugador", mazoJ2);
                    msg2.put("cartas_oponente", mazoJ1);
                    msg2.put("carta_siguiente", cola);
                    msg2.put("tablero_eq1", partidaGuardada.getPos_Fichas_Eq1());
                    msg2.put("tablero_eq2", partidaGuardada.getPos_Fichas_Eq2());
                    msg2.put("turno", partidaGuardada.getTurno());
                    msg2.put("trampa_j1_pos", partidaGuardada.getTrampaPosJ1());
                    msg2.put("trampa_j2_pos", partidaGuardada.getTrampaPosJ2());
                    msg2.put("cartas_accion_jugador", cartasAccionJ2);
                    msg2.put("cartas_accion_oponente", cartasAccionJ1);

                    j1.ws.send(msg1.toString());
                    j2.ws.send(msg2.toString());
                    System.out.println("Partida " + partidaGuardada.getIDPartida() + " reanudada: " + notif.getRemitente() + " VS " + notif.getDestinatario());

                    // Iniciar timer para el turno actual al reanudar
                    iniciarTimerTurno(pj, partidaGuardada.getTurno());
                }
            } else {
                gestor.rechazarNotificacion(idNotificacion, nombreQuienResponde);
                WebSocket wsRemitente = buscarConexion(notif.getRemitente());
                if (wsRemitente != null && wsRemitente.isOpen()) {
                    wsRemitente.send(new JSONObject().put("tipo", "ERROR_NO_UNIDO").toString());
                }
                System.out.println("Reanudar rechazado por " + notif.getDestinatario());
            }

        } catch (SQLException e) {
            System.err.println("Error SQL al gestionar respuesta de reanudar: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error inesperado al gestionar respuesta de reanudar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void obtenerTiendaSkins(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            GestorSkin gestorSkin = new GestorSkin();

            Jugador j = gestorJugador.buscarJugador(usuario);
            if (j == null) {
                conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
                return;
            }

            List<Skin> todasLasSkins = gestorSkin.sacarSkinDisp();
            List<Skin> skinsDelJugador = gestorSkin.sacarSkinJugador(usuario);

            JSONArray skinsJSON = new JSONArray();
            for (Skin s : todasLasSkins) {
                boolean owned = s.getNombre().equals("Skin0") || skinsDelJugador.stream()
                        .anyMatch(sk -> sk.getNombre().equals(s.getNombre()));
                boolean esActiva = s.getNombre().equals(j.getSkinActiva());
                JSONObject skinJSON = new JSONObject();
                skinJSON.put("skin_id", s.getNombre());
                skinJSON.put("precio", s.getPrecio());
                skinJSON.put("owned", owned);
                skinJSON.put("es_activa", esActiva);
                skinsJSON.put(skinJSON);
            }

            JSONObject msg = new JSONObject();
            msg.put("tipo", "TIENDA_SKINS");
            msg.put("usuario", usuario);
            msg.put("cores", j.getCores());
            msg.put("skin_activa", j.getSkinActiva());
            msg.put("skins", skinsJSON);
            conn.send(msg.toString());
            System.out.println("TIENDA_SKINS enviado a " + usuario);

        } catch (SQLException e) {
            System.err.println("Error al obtener tienda skins: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "ERROR_BD").toString());
        }
    }

    private void comprarSkin(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        String skinId = obj.getString("skin_id");
        try {
            GestorSkin gestorSkin = new GestorSkin();
            String resultado = gestorSkin.comprarSkin(skinId, usuario);

            // ahora que se gestiona toda la lógica con gestores, solo toca enviar el mensaje
            // correspondiente a raíz de lo que devuelve la llamada a comprarSkin, método
            // implementado en gestorSKin
            if (resultado.equals("OK")) {
                Jugador jActualizado = new GestorJugador().buscarJugador(usuario);
                int nuevosCores = jActualizado != null ? jActualizado.getCores() : -1;
                JSONObject msg = new JSONObject();
                msg.put("tipo", "COMPRA_SKIN_OK");
                msg.put("skin_id", skinId);
                msg.put("cores", nuevosCores);
                conn.send(msg.toString());
                System.out.println("COMPRA_SKIN_OK: " + usuario + " compró " + skinId + " | cores restantes: " + nuevosCores);
            } else {
                conn.send(new JSONObject()
                        .put("tipo", "COMPRA_SKIN_ERROR")
                        .put("skin_id", skinId)
                        .put("codigo", resultado)
                        .toString());
            }

        } catch (SQLException e) {
            System.err.println("Error al comprar skin: " + e.getMessage());
            conn.send(new JSONObject()
                    .put("tipo", "COMPRA_SKIN_ERROR")
                    .put("skin_id", skinId)
                    .put("codigo", "ERROR_BD")
                    .toString());
        }
    }

    private void activarSkin(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        String skinId = obj.getString("skin_id");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            GestorSkin gestorSkin = new GestorSkin();

            // Comprobar que la skin existe
            if (gestorSkin.buscarSkin(skinId) == null) {
                conn.send(new JSONObject()
                        .put("tipo", "ACTIVAR_SKIN_ERROR")
                        .put("skin_id", skinId)
                        .put("codigo", "SKIN_NO_EXISTE")
                        .toString());
                return;
            }

            // updateSkinActiva ya comprueba internamente que el jugador posee la skin
            boolean ok = gestorJugador.updateSkinActiva(usuario, skinId);
            if (!ok) {
                conn.send(new JSONObject()
                        .put("tipo", "ACTIVAR_SKIN_ERROR")
                        .put("skin_id", skinId)
                        .put("codigo", "NO_POSEIDA")
                        .toString());
                return;
            }

            JSONObject msg = new JSONObject();
            msg.put("tipo", "SKIN_ACTIVADA");
            msg.put("skin_activa", skinId);
            conn.send(msg.toString());
            System.out.println("SKIN_ACTIVADA: " + usuario + " activó " + skinId);

        } catch (SQLException e) {
            System.err.println("Error al activar skin: " + e.getMessage());
            conn.send(new JSONObject()
                    .put("tipo", "ACTIVAR_SKIN_ERROR")
                    .put("skin_id", skinId)
                    .put("codigo", "ERROR_BD")
                    .toString());
        }
    }

    /**
     * RECUPERAR_CONTRASENA – El cliente envía el correo del jugador.
     * El servidor:
     *   1. Busca el jugador por correo en la BD.
     *   2. Si no existe → responde CORREO_NO_ENCONTRADO.
     *   3. Si existe   → genera contraseña aleatoria, la hashea, actualiza la BD
     *                    y envía un email con JavaMail.
     *
     * Mensaje cliente:  { "tipo": "RECUPERAR_CONTRASENA", "correo": "user@ejemplo.com" }
     * Respuesta OK:     { "tipo": "CONTRASENA_ENVIADA" }
     * Respuesta error:  { "tipo": "CORREO_NO_ENCONTRADO" }
     */
    private void recuperarContrasena(WebSocket conn, JSONObject obj) {
        String correo = obj.optString("correo", "").trim();
        if (correo.isEmpty()) {
            conn.send(new JSONObject().put("tipo", "CORREO_NO_ENCONTRADO").toString());
            return;
        }

        try {
            GestorJugador gestorJugador = new GestorJugador();
            Jugador jugador = gestorJugador.buscarJugadorPorCorreo(correo);
            System.out.println("RECUPERAR_CONTRASENA: buscando correo '" + correo + "' → " + (jugador != null ? "encontrado (" + jugador.getNombre() + ")" : "NO encontrado"));

            if (jugador == null) {
                conn.send(new JSONObject().put("tipo", "CORREO_NO_ENCONTRADO").toString());
                return;
            }

            // Generar contraseña aleatoria en texto plano
            String nuevaContrasena = GestorEmail.generarContrasennaAleatoria();

            // Hashear con bcrypt (mismo proceso que en el registro)
            String hash = org.mindrot.jbcrypt.BCrypt.hashpw(nuevaContrasena, org.mindrot.jbcrypt.BCrypt.gensalt());

            // Actualizar la BD
            boolean actualizado = gestorJugador.updateContrasenya(jugador.getNombre(), hash);
            if (!actualizado) {
                System.err.println("RECUPERAR_CONTRASENA: fallo al actualizar contraseña de " + jugador.getNombre());
                conn.send(new JSONObject().put("tipo", "CORREO_NO_ENCONTRADO").toString());
                return;
            }

            // Enviar el email en un hilo separado para no bloquear el hilo WS
            final String nombreFinal   = jugador.getNombre();
            final String correoFinal   = correo;
            final String nuevaPswFinal = nuevaContrasena;
            new Thread(() -> {
                try {
                    System.out.println("RECUPERAR_CONTRASENA: enviando email a " + correoFinal + "…");
                    GestorEmail.enviarContrasennaReset(correoFinal, nuevaPswFinal, nombreFinal);
                    conn.send(new JSONObject().put("tipo", "CONTRASENA_ENVIADA").toString());
                    System.out.println("RECUPERAR_CONTRASENA: OK → email enviado a " + correoFinal);
                } catch (javax.mail.MessagingException e) {
                    System.err.println("RECUPERAR_CONTRASENA [MessagingException]: " + e.getMessage());
                    conn.send(new JSONObject().put("tipo", "ERROR_EMAIL").toString());
                } catch (Exception e) {
                    System.err.println("RECUPERAR_CONTRASENA [email thread error]: " + e.getMessage());
                    conn.send(new JSONObject().put("tipo", "ERROR_EMAIL").toString());
                }
            }, "email-sender").start();

        } catch (java.sql.SQLException e) {
            System.err.println("RECUPERAR_CONTRASENA [SQLException]: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "CORREO_NO_ENCONTRADO").toString());
        } catch (Exception e) {
            System.err.println("RECUPERAR_CONTRASENA [Exception inesperada]: " + e.getClass().getSimpleName() + " – " + e.getMessage());
            e.printStackTrace();
            conn.send(new JSONObject().put("tipo", "CORREO_NO_ENCONTRADO").toString());
        }
    }

    private void cambiarContrasena(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        // vienen hasheadas de cliente
        String hashAntigua = obj.getString("contrasena_actual");
        String hashNueva = obj.getString("contrasena_nueva");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            Jugador jugador = gestorJugador.buscarJugador(usuario);

            if (jugador == null) {
                conn.send(new JSONObject().put("tipo", "CAMBIO_CONTRASENA_ERROR")
                        .put("codigo", "USUARIO_NO_EXISTE").toString());
                return;
            }

            // se verigica la contraseña actual usando bcrypt, igual que en iniciarSesion
            if (!jugador.verificarPassword(hashAntigua)) {
                conn.send(new JSONObject().put("tipo", "CAMBIO_CONTRASENA_ERROR")
                        .put("codigo", "CONTRASENA_INCORRECTA").toString());
                return;
            }

            // hashNueva llega en texto plano desde el cliente entonces la hasheamos aquí como en registro
            String nuevoHash = org.mindrot.jbcrypt.BCrypt.hashpw(hashNueva, org.mindrot.jbcrypt.BCrypt.gensalt());
            boolean ok = gestorJugador.updateContrasenya(usuario, nuevoHash);

            if (ok) {
                conn.send(new JSONObject().put("tipo", "CONTRASENA_CAMBIADA").toString());
                System.out.println("CAMBIAR_CONTRASENA: OK para " + usuario);
            } else {
                conn.send(new JSONObject().put("tipo", "CAMBIO_CONTRASENA_ERROR")
                        .put("codigo", "ERROR_BD").toString());
            }

        } catch (SQLException e) {
            System.err.println("CAMBIAR_CONTRASENA [SQLException]: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "CAMBIO_CONTRASENA_ERROR")
                    .put("codigo", "ERROR_BD").toString());
        }
    }

    private void cambiarAvatar(WebSocket conn, JSONObject obj) {
        String usuario = obj.getString("usuario");
        String nuevoAvatarId = obj.getString("avatar_id");
        try {
            GestorJugador gestorJugador = new GestorJugador();
            boolean ok = gestorJugador.updateAvatar(usuario, nuevoAvatarId);

            if (ok) {
                JSONObject msg = new JSONObject();
                msg.put("tipo", "AVATAR_CAMBIADO");
                msg.put("avatar_id", nuevoAvatarId);
                conn.send(msg.toString());
            } else {
                conn.send(new JSONObject().put("tipo", "CAMBIO_AVATAR_ERROR")
                        .put("codigo", "ERROR_BD").toString());
            }

        } catch (SQLException e) {
            System.err.println("CAMBIAR_AVATAR [SQLException]: " + e.getMessage());
            conn.send(new JSONObject().put("tipo", "CAMBIO_AVATAR_ERROR")
                    .put("codigo", "ERROR_BD").toString());
        }
    }

    public static void main(String[] args) {
        Servidor s = new Servidor(8080);
        s.start();
    }
}