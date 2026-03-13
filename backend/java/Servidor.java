import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.*;
import VO.Partida;
import VO.CartaMov;
import VO.Posicion;
import VO.Tablero;
import VO.Jugador;
import VO.Autenticacion;
import JDBC.JugadorJDBC;

//POR HACER:
// -> El xml que querias hacer: PRIORIDAD ALTA <-- Puedes empezar con esto si quieres 
// -> Solicitudes de amistad: PRIORIDAD ALTA
// -> Solicitudes de partida privadas: PRIORIDAD MEDIA (Antes hay que hacer lo anterior)
// -> Reanudar/Pausar una partida privada: PRIORIDAD MEDIA (Antes hay que hacer lo anterior)
// -> Cartas de Accion: PRIORIDAD BAJA (No lo necesitamos para la primera entrega)

class InfoJugador{
    WebSocket ws;
    String nombre;
    int puntos;

    public InfoJugador(WebSocket w, String nom, int pt){
        ws = w;
        nombre = nom;
        puntos = pt;
    }
}

class Pareja{
    InfoJugador p1, p2;
    Partida partida;

    public Pareja(InfoJugador _p1, InfoJugador _p2){
        p1 = _p1;
        p2 = _p2;
        //De momento solo existe partidas Publicas
        partida = new Partida(0, "JUGANDOSE", 0, "PUBLICA", null, null, 0, 0, _p1.nombre, _p2.nombre, false, false, 0);
        partida.registrarPartida();
        partida.asignarCartas(); //Genera las cartas aleatorias
        partida.repartirCartas(); //Repartimos a los equipos sus cartas
    }

    public boolean buscar(WebSocket _p1){
        return p1.ws == _p1 || p2.ws == _p1;
    }

    // PREVIAMENTE if p1.ws return p1, pero eso devolvía el mismo jugador,
    // entonces lo he cambiado para que devuelva el oponente.
    public InfoJugador getOponente(WebSocket _p1){
        if(p1.ws == _p1){
            return p2;
        }else if(p2.ws == _p1){
            return p1;
        }else{
            return null;
        }
    }
}

//BORRO PARTIDA

public class Servidor extends WebSocketServer {
    private List<InfoJugador> buscando_partida;
    List<Pareja> parejas;
    private ExecutorService hilos = Executors.newFixedThreadPool(50);
    private Semaphore mutex = new Semaphore(1);
    private Semaphore mutexParejas = new Semaphore(1);
    ScheduledExecutorService temporizador = Executors.newScheduledThreadPool(10);
    
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

            if ("EQ1".equals(carta.getEstado())) {
                mazoJ1.put(JSONCarta);
            } else if ("EQ2".equals(carta.getEstado())) {
                mazoJ2.put(JSONCarta);
            } else {
                cola.put(JSONCarta);
            }
        }
    }

    // Con 'iniciar' construimos los JSON de tipo PARTIDA_ENCONTRADA
    public void iniciar(Pareja pj){

        JSONArray mazoJ1 = new JSONArray();
        JSONArray mazoJ2 = new JSONArray();
        JSONArray cola = new JSONArray();
        cartasPartida(pj, mazoJ1, mazoJ2, cola);

        JSONObject msg1 = new JSONObject(); // el que espera a jugar
        msg1.put("tipo", "PARTIDA_ENCONTRADA");
        msg1.put("partida_id", pj.partida.getIDPartida());
        msg1.put("equipo", 1);
        msg1.put("oponente", pj.p2.nombre);
        msg1.put("oponentePt", pj.p2.puntos);
        msg1.put("cartas_jugador", mazoJ1);
        msg1.put("cartas_oponente", mazoJ2);
        msg1.put("carta_siguiente", cola);

        JSONObject msg2= new JSONObject(); // al que le emparejan a j1
        msg2.put("tipo", "PARTIDA_ENCONTRADA");
        msg2.put("partida_id", pj.partida.getIDPartida());
        msg2.put("equipo", 2);
        msg2.put("oponente", pj.p1.nombre);
        msg2.put("oponentePt", pj.p1.puntos);
        msg2.put("cartas_jugador", mazoJ2);
        msg2.put("cartas_oponente", mazoJ1);
        msg2.put("carta_siguiente", cola);

        pj.p1.ws.send(msg1.toString());
        pj.p2.ws.send(msg2.toString());
        System.out.println("Partida "+ pj.partida.getIDPartida() +" iniciada.");

        // Envío las 3 cartas de la cola para que el frontend gestione la rotación por su cuenta,
        // porque si solo enviáramos la carta visible, el servidor tendría que guardar las demás 
        // en memoria y el frontend tendría que pedirle la siguiente cada vez que gasta una, 
        // que acumularía bastante petición y respuesta. Con esto evitamos mensajes extra.

    }

    private void gestionarBusquedaPartida(WebSocket conn, JSONObject obj){

        String nombre = obj.getString("nombre");
        int puntos = obj.getInt("puntos");

        InfoJugador oponente = null;
        Pareja pj = null;

        try{
            mutex.acquire(); //WAIT

            for (InfoJugador j : buscando_partida){
                int dif = j.puntos - puntos;
                if (dif >= -100 && dif <= 100 && !j.nombre.equals(nombre)) { 
                    oponente = j;
                    break;
                }
            }
            
            // if gestiona caso de match y else si no ha habido match
            if (oponente != null){
                buscando_partida.remove(oponente);
                InfoJugador nuevoJugador = new InfoJugador(conn, nombre, puntos);
                pj = new Pareja(oponente, nuevoJugador);

                try{
                    mutexParejas.acquire();//WAIT
                    parejas.add(pj);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutexParejas.release(); //SIGNAL
                }
                
            } else {
                InfoJugador jugador = new InfoJugador(conn, nombre, puntos);
                buscando_partida.add(jugador);
                System.out.println(nombre + " está esperando a unirse a una partida.");
                hilos.submit(() -> {
                    volverABuscar(conn, jugador);
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release(); //SIGNAL
        }

        if(oponente != null){
            iniciar(pj);
            System.out.println("Partida creada: " + nombre + " VS " + oponente.nombre);
        }
    }

    private void volverABuscar(WebSocket conn, InfoJugador jug) {
        AtomicInteger segBuscando = new AtomicInteger(0);
        ScheduledFuture<?>[] tareaLoop = new ScheduledFuture<?>[1];
        
        tareaLoop[0] = temporizador.scheduleAtFixedRate(() -> {
            
            InfoJugador oponenteEncontrado = null;
            Pareja pj = null;
            int tiempoActual = segBuscando.addAndGet(10); //Sumamos 10 segundos atomicamente debido a que dentro del temporizador no se puede modificar variables primitivas de java

            try {
                mutex.acquire(); // WAIT
                
                //Si no estamos en la lista, es que ya nos han cogido como pareja
                if (!buscando_partida.contains(jug)) {
                    tareaLoop[0].cancel(false);
                    return; // Salimos de la ejecución
                }

                boolean hayGente = buscando_partida.size() > 1;
                boolean prioridad = tiempoActual > 120 && hayGente;

                if (prioridad) {
                    // Sacamos al primero que no sea él mismo (el que ha estado más tiempo esperando)
                    oponenteEncontrado = buscando_partida.get(0);
                    if (jug.equals(oponenteEncontrado) && buscando_partida.size() > 1) {
                        oponenteEncontrado = buscando_partida.get(1);
                    }
                } else {
                    for (InfoJugador j : buscando_partida) {
                        if (jug.equals(j)) continue; // No emparejarse consigo mismo
                        
                        int dif = j.puntos - jug.puntos;
                        if (dif >= -100 && dif <= 100 && !j.nombre.equals(jug.nombre)) { 
                            oponenteEncontrado = j;
                            break;
                        }
                    }
                }

                // Si encontramos a alguien, los sacamos de la lista
                if (oponenteEncontrado != null) {
                    buscando_partida.remove(jug);
                    buscando_partida.remove(oponenteEncontrado);
                    pj = new Pareja(oponenteEncontrado, jug);
                    try{
                        mutexParejas.acquire();//WAIT
                        parejas.add(pj);
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        mutexParejas.release(); //SIGNAL
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutex.release(); // SIGNAL 
            }

            if (oponenteEncontrado != null) {
                System.out.println("Partida creada tardía: " + jug.nombre + " VS " + oponenteEncontrado.nombre);
                iniciar(pj);
                
                tareaLoop[0].cancel(false); 
            } else {
                System.out.println(jug.nombre + " sigue buscando... (" + tiempoActual + "s)");
            }

        }, 10, 10, TimeUnit.SECONDS); //Se repite cada 10 segundos
    }

    private void gestionarPartida(WebSocket conn, JSONObject obj) {
        // buscamos partida en la que esta el jugador
        Pareja pj = null;
        try{
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                // Usamos el método 'buscar' de la clase Pareja para ver si este jugador está aquí
                if (pareja.buscar(conn)) {
                    // una vez encontremos el jugador, actualizamos el valor de p para trabajar con él y con el movimiento
                    // que tiene que hacer
                    pj = pareja;
                    break;
                }
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); //SIGNAL
        }
        
        //LANZAR SUBRUTINA EN LA VERSION NO POCHA

        if (pj != null) {
            InfoJugador oponente = pj.getOponente(conn);

            // Verificamos que el oponente exista y que su conexión esté abierta
            if (oponente != null && oponente.ws.isOpen()) {
                String carta = obj.getString("carta");
                int YO = obj.getInt("fila_origen");
                int XO = obj.getInt("col_origen");
                int YD = obj.getInt("fila_destino");
                int XD = obj.getInt("col_destino");
                int equipo = obj.getInt("equipo");
                
                //El que te pase el mensaje te dira su equipo
                // 0 -> Movimiento realizado con exito
                // 1 -> equipo 1 gana
                // 2 -> equipo 2 gana
                // -1 -> carta no existente en la partida
                // -2 -> movimiento no valido
                int estado = -2;

                if(XO > -1 && YO > -1 && XD > -1 && YD > -1){
                    Tablero tb = pj.partida.getTablero();
                    estado = pj.partida.moverFicha(equipo, tb.getPosicion(XO,YO), tb.getPosicion(XD,YD), carta);
                    // rotarCartas se llama internamente en moverFicha (solo en movimientos válidos)
                    //pj.partida.rotarCartas(carta, equipo);
                }
                
                JSONObject msg = new JSONObject();

                if(estado >= 0){    
                    if(estado == 0){
                        msg.put("tipo", "MOVER");
                        msg.put("col_origen", XO);
                        msg.put("fila_origen", YO);
                        msg.put("col_destino", XD);
                        msg.put("fila_destino", YD);
                        msg.put("carta", carta);
                    }else if(estado == 1){
                        if(equipo == 1){
                            msg.put("tipo", "DERROTA");
                        }else{
                            msg.put("tipo", "VICTORIA");
                        }
                        pj.partida.actualizarBD(); //Si se termina la partida -> actualizo la base de datos
                    }else if(estado == 2){
                        if(equipo == 2){
                            msg.put("tipo", "DERROTA");
                        }else{
                            msg.put("tipo", "VICTORIA");
                        }
                        pj.partida.actualizarBD();
                    }
                    
                    // Enviamos el mensaje AL OPOONENTE
                    oponente.ws.send(msg.toString());
                    System.out.println("Movimiento reenviado en la partida " + pj.partida.getIDPartida());
                }else{
                    if(estado == -2){
                        msg.put("tipo", "MOVIMIENTO_INVALIDO");
                        System.out.println("Movimiento invalido en la partida " + pj.partida.getIDPartida());
                    }else{
                        msg.put("tipo", "CARTA_INVALIDA");
                        System.out.println("Carta no presente en la partida en la partida " + pj.partida.getIDPartida());
                    }

                    conn.send(msg.toString());
                }
            }
        } else {
            // Si no encontramos partida, es un error (el jugador intentó mover sin estar emparejado)
            System.out.println("Error: Movimiento recibido de un jugador sin partida activa.");
        }
    }

    public void abandonarPartida(WebSocket conn, JSONObject obj){
        int equipoAbandona = obj.getInt("equipo");
        Pareja pj = null;
        try{
            mutexParejas.acquire();
            for (Pareja pareja : parejas) {
                if (pareja.buscar(conn)) {
                    pj = pareja;
                    break;
                }
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutexParejas.release(); //SIGNAL
        }

        if(pj != null){
            InfoJugador oponente = pj.getOponente(conn);
            JSONObject msg = new JSONObject();

            if (oponente != null && oponente.ws.isOpen()) {
                msg.put("tipo", "VICTORIA");
                oponente.ws.send(msg.toString());
            }

            pj.partida.abandonarPartida(equipoAbandona); //Actualizamos la partida en la BD con el abandono

            try{
                mutexParejas.acquire();
                parejas.remove(pj);
            }catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mutexParejas.release(); //SIGNAL
            }
        }
    }

    public void iniciarSesion(WebSocket conn, JSONObject obj){
        JugadorJDBC jdbc = new JugadorJDBC();
        
        try {
            Jugador j = jdbc.buscarJugador(obj.getString("nombre"));
            
            if(j == null){
                conn.send(new JSONObject().put("tipo", "ERROR_SESION_USS").toString());
            }else if(!Autenticacion.verificarPassword(obj.getString("password"), j.getContrasenya())){
                conn.send(new JSONObject().put("tipo", "ERROR_SESION_PSSWD").toString());
            }else{
                JSONObject msg = new JSONObject();
                msg.put("tipo", "INICIO_SESION_EXITOSO");
                msg.put("nombre", j.getNombre());
                msg.put("correo", j.getCorreo());
                msg.put("puntos", j.getPuntos());
                msg.put("partidas_ganadas", j.getPartidasGanadas());
                msg.put("partidas_jugadas", j.getPartidasJugadas());
                msg.put("cores", j.getCores());
                conn.send(msg.toString());
            }
            
        } catch (java.sql.SQLException e) {
            System.err.println("Error de Base de Datos al iniciar sesión: " + e.getMessage());
            JSONObject errorInfo = new JSONObject();
            errorInfo.put("tipo", "ERROR_BD");
            conn.send(errorInfo.toString());
        }
    }

    public void registrarJugador(WebSocket conn, JSONObject obj){
        String correo = obj.getString("correo");
        String nombre = obj.getString("nombre");
        String contrasena = obj.getString("password");
        Jugador prueba = new Jugador(correo, nombre, contrasena); // El constructor ya hashea la contraseña internamente
        if(prueba.registrarse()){
            System.out.println("Jugador registrado");
            conn.send(new JSONObject().put("tipo", "REGISTRO_EXITOSO").toString());
        }else{
            System.out.println("Jugador NO registrado");
            conn.send(new JSONObject().put("tipo", "REGISTRO_ERRONEO").toString());
        }
    }

    public void cancelarBusqueda(WebSocket conn){
        try{
            mutex.acquire(); //WAIT
            InfoJugador jugadorBorrar = null;
            for(InfoJugador j : buscando_partida){
                if(j.ws == conn){
                    jugadorBorrar = j;
                    tareaLoop[0].cancel(true);
                }
            }
            if(jugadorBorrar != null){
                buscando_partida.remove(jugadorBorrar);
                conn.send(new JSONObject().put("tipo", "CANCELAR_EXITO").toString());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release(); //SIGNAL
        }
    }

    public Servidor(int puerto) {
        super(new InetSocketAddress(puerto));
        // conexiones = new ArrayList<>(); 
        // he comentado esto porque 'conexiones' no estaba definido en ningún
        // otro sitio de 'Servidor.java', luego he continuado con la inicialización 
        // de las dos listas previamente declaradas.
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
            mutex.acquire(); //WAIT
            buscando_partida.removeIf(jugador -> jugador.ws == conn);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release(); //SIGNAL
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);

        hilos.submit(() -> {
            JSONObject obj = new JSONObject(message);
            String tipoMSG = obj.getString("tipo");
            if(tipoMSG.equals("BUSCAR_PARTIDA")){
                gestionarBusquedaPartida(conn, obj);
            } else if (tipoMSG.equals("MOVER")) {
                gestionarPartida(conn, obj);
            } else if (tipoMSG.equals("INICIAR_SESION")){
                iniciarSesion(conn, obj);
            } else if (tipoMSG.equals("REGISTRARSE")){
                registrarJugador(conn, obj);
            } else if (tipoMSG.equals("ABANDONAR")){
                abandonarPartida(conn, obj);
            } else if (tipoMSG.equals("CANCELAR")){
                cancelarBusqueda(conn);
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

    public static void main(String[] args) {
        Servidor s = new Servidor(8080);
        s.start();
    }
}
