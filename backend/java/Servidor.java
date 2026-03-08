import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.ArrayList;

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

    public Pareja(InfoJugador _p1, InfoJugador _p2){
        p1 = _p1;
        p2 = _p2;
    }

    public boolean buscar(WebSocket _p1){
        return p1.ws == _p1 || p2.ws == _p1;
    }

    public InfoJugador getPareja(WebSocket _p1){
        if(p1.ws == _p1){
            return p1;
        }else if(p2.ws == _p1){
            return p2;
        }else{
            return null;
        }
    }
}

public class Servidor extends WebSocketServer {
    List<InfoJugador> buscando_partida;
    List<Pareja> parejas;

    public Servidor(int puerto) {
        super(new InetSocketAddress(puerto));
        conexiones = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Jugador conectado al servidor -> " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Jugador desconectado del servidor -> " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);

        JSONObject obj = new JSONObject(message);
        String tipoMSG = obj.getString("tipo");

        if(tipoMSG.equals("BUSCAR_PARTIDA")){
            String jugadorNom = obj.getString("tipo");
            int jugadorPt = obj.getInt("puntos");

            //Miramos si en la lista hay alguien esperando
            if(buscando_partida.isEmpty()){
                buscando_partida.add(new InfoJugador(conn, jugadorNom, jugadorPt)); //Si no hay alguine -> le metemos en la lista y NO mandamos msg
            }else{
                //Si hay gente esperando -> miramos si hay alguno con una diferencia de puntos menor a 100
                boolean encontrado = false;
                for (InfoJugador j : buscando_partida){
                    int dif = j.puntos - jugadorPt;
                    if(dif >= -100 ||dif <=100){
                        encontrado = true;

                        //LOS MOVIMIENTOS NO SON REALES, FALTARIA CREAR LA PARTIDA Y QUE SE GENEREN LAS CARTAS
                        JSONArray movimientosJ1 = new JSONArray();
                        lista.put("Tigre");
                        lista.put("Dragon");

                        JSONArray movimientosJ2 = new JSONArray();
                        lista.put("Rana");
                        lista.put("Conejo");

                        JSONArray siguientes = new JSONArray();
                        lista.put("Oso");
                        lista.put("Elefante");
                        lista.put("Cobra");

                        //Creamos msg
                        JSONObject respuestaJ1 = new JSONObject();
                        respuestaJ1.put("tipo", "PARTIDA_ENCONTRADA");
                        respuestaJ1.put("partida_id", "123");
                        respuestaJ1.put("equipo", 1);
                        respuestaJ1.put("oponente", j.nombre);
                        respuestaJ1.put("oponentePt", j.puntos);
                        respuestaJ1.put("cartas_jugador", movimientosJ1);
                        respuestaJ1.put("cartas_oponente", movimientosJ2);
                        respuestaJ1.put("carta_siguiente", siguientes);
                        JSONObject respuestaJ2 = new JSONObject();
                        respuestaJ2.put("tipo", "PARTIDA_ENCONTRADA");
                        respuestaJ2.put("partida_id", "123");
                        respuestaJ2.put("equipo", 2);
                        respuestaJ2.put("oponente", jugadorNom);
                        respuestaJ2.put("oponentePt", jugadorPt);
                        respuestaJ2.put("cartas_jugador", movimientosJ2);
                        respuestaJ2.put("cartas_oponente", movimientosJ1);
                        respuestaJ2.put("carta_siguiente", siguientes);

                        //Avisamos a ambos
                        conn.send(respuestaJ1.toString());
                        j.ws.send(respuestaJ2.toString());
                    }
                }
                if(!encontrado){
                    buscando_partida.add(new InfoJugador(conn, jugadorNom, jugadorPt)); //Si no hay nadie esperamos y lo añadimos a la lista
                }
            }
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