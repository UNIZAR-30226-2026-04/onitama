import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.ArrayList;

class Pareja{
    WebSocket p1, p2;

    public Pareja(WebSocket _p1, WebSocket _p2){
        p1 = _p1;
        p2 = _p2;
    }

    public boolean buscar(WebSocket _p1){
        return p1 == _p1 || p2 == _p1;
    }

    public WebSocket getPareja(WebSocket _p1){
        if(p1 == _p1){
            return p1;
        }else if(p2 == _p1){
            return p2;
        }else{
            return null;
        }
    }
}

public class Servidor extends WebSocketServer {
    List<WebSocket> buscando_partida;
    List<Pareja> parejas;

    public Servidor(int puerto) {
        super(new InetSocketAddress(puerto));
        conexiones = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conexiones.add(conn.getRemoteSocketAddress().toString());
        System.out.println("Jugador conectado al servidor -> " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Jugador desconectado del servidor -> " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);
        conn.send("{\"estado\": \"ENCONTRADA\", \"mensaje\": \"Partida encontrada, ¡a jugar!\", \"partida_id\": \"abc-123\"}");
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