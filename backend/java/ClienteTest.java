import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;

public class ClienteTest extends WebSocketClient {

    public ClienteTest(String url) throws URISyntaxException {
        super(new URI(url));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Conectado al servidor...");
        send("");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Respuesta del servidor: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Conexión cerrada: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Error en la conexión:");
        ex.printStackTrace();
    }

    public static void main(String[] args) {
        try {
            ClienteTest cliente = new ClienteTest("ws://localhost:8080");
            cliente.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}