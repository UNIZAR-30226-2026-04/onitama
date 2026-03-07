import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;

public class Servidor extends WebSocketServer {

    public Servidor(int puerto) {
        super(new InetSocketAddress(puerto));
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