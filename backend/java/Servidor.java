import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import VO.Partida;
import VO.CartaMov;
import VO.Posicion;

//Hay que añadir el .jar de json

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
    List<InfoJugador> buscando_partida;
    List<Pareja> parejas;

    
    private void cartasPartida(Pareja pj, JSONArray mazoJ1, JSONArray mazoJ2, JSONArray cola){
        List<CartaMov> cartas = pj.partida.getCartasMovimiento();

        for (CartaMov carta : cartas){
            if(carta.getEstado() == "EQ1"){
                mazoJ1.put(carta.getNombre());
            }else if(carta.getEstado() == "EQ2"){
                mazoJ2.put(carta.getNombre());
            }else{
                cola.put(carta.getNombre());
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
        // todavía no hemos matcheado

        for (InfoJugador j : buscando_partida){
            int dif = j.puntos - puntos;
            if (dif >= -100 && dif <= 100) { 
                oponente = j;
                break;
            }
        }
        
        // if gestiona caso de match y else si no ha habido match
        if (oponente != null){
            buscando_partida.remove(oponente);
            InfoJugador nuevoJugador = new InfoJugador(conn, nombre, puntos);
            Pareja pj = new Pareja(oponente, nuevoJugador);

            parejas.add(pj);
            iniciar(pj);

            System.out.println("Partida creada: " + nombre + " VS " + oponente.nombre);
        } else {
            buscando_partida.add(new InfoJugador(conn, nombre, puntos));
            System.out.println(nombre + " está esperando a unirse a una partida.");
        }
    }

    private void gestionarPartida(WebSocket conn, JSONObject obj) {
        // buscamos partida en la que esta el jugador
        Pareja pj = null;
        for (Pareja pareja : parejas) {
                // Usamos el método 'buscar' de la clase Pareja para ver si este jugador está aquí
            if (pareja.buscar(conn)) {
                // una vez encontremos el jugador, actualizamos el valor de p para trabajar con él y con el movimiento
                // que tiene que hacer
                pj = pareja;
                break;
            }
        }
        
        //LANZAR SUBRUTINA EN LA VERSION NO POCHA

        if (pj != null) {
            InfoJugador oponente = pj.getOponente(conn);

            // Verificamos que el oponente exista y que su conexión esté abierta
            if (oponente != null && oponente.ws.isOpen()) {
                String carta = obj.getString("carta");
                int XO = obj.getInt("fila_origen");
                int YO = obj.getInt("col_origen");
                int XD = obj.getInt("fila_destino");
                int YD = obj.getInt("col_destino");
                int equipo = obj.getInt("equipo");
                
                //El que te pase el mensaje te dira su equipo
                // 0 -> Movimiento realizado con exito
                // 1 -> equipo 1 gana
                // 2 -> equipo 2 gana
                // -1 -> carta no existente en la partida
                // -2 -> movimiento no valido
                int estado = pj.partida.moverFicha(equipo, new Posicion(XO,YO,null), new Posicion(XD,YD, null), carta);
                pj.partida.rotarCartas(carta, equipo);
                
                JSONObject msg = new JSONObject();

                if(estado >= 0){    
                    if(estado == 0){
                        msg.put("tipo", "MOVER");
                        msg.put("col_origen", YO);
                        msg.put("fila_origen", XO);
                        msg.put("col_destino", YD);
                        msg.put("fila_destino", XD);
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
                    if(estado == -1){
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
        System.out.println("Jugador desconectado del servidor -> " + conn.getRemoteSocketAddress());
        // Si el jugador se desconecta del servidor, igual conviene quitarlo de la lista de jugadores
        // que esperan jugar una partida porque puede ser que el server haya recibido BUSCAR_PARTIDA, lo
        // haya metido a esperar y se haya salido de la página por quedarse esperando, por si acaso no vayamos
        // a unir jugadores activos con otros que quedan fantasmas en la lista.

        // He buscado en google 'java remove element from list with condition' y me ha salido el removeIf, de la interfaz Collection
        // pero que lo implementa ArrayList entonces no nos hacen falta imports.
        buscando_partida.removeIf(jugador->jugador.ws == conn); // eliminamos de la lista de jugadres que buscan partida al que haya enviado onClose al server y cuya conxión sea conn
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);

        JSONObject obj = new JSONObject(message);
        String tipoMSG = obj.getString("tipo");


        
        if(tipoMSG.equals("BUSCAR_PARTIDA")){
            gestionarBusquedaPartida(conn, obj);
            
        } else if (tipoMSG.equals("MOVER")) {
            gestionarPartida(conn, obj);
        }

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