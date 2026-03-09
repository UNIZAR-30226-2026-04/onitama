import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import VO.Jugador;

public class ClienteTestPartida extends WebSocketClient {

    public ClienteTestPartida(String url) throws URISyntaxException {
        super(new URI(url));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println(">>> Conectado al servidor");
        
        //Simulamos que el jugador ya esta registrado
        Jugador prueba = new Jugador("correo_" + (int)(Math.random()*100), "Jugador_" + (int)(Math.random()*100), "prueba");
        prueba.registrarse();
        enviarBuscarPartida(prueba.getNombre(), prueba.getPuntos());
    }

    @Override
    public void onMessage(String message) {
        System.out.println("\n[SERVIDOR]: " + message);
        JSONObject obj = new JSONObject(message);
        
        if (obj.getString("tipo").equals("PARTIDA_ENCONTRADA")) {
            System.out.println("¡Partida confirmada contra " + obj.getString("oponente") + "!");
            System.out.println("Tus cartas: " + obj.getJSONArray("cartas_jugador"));
            System.out.println("Escribe 'mover' para simular un movimiento.");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(">>> Conexión cerrada: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println(">>> ERROR: " + ex.getMessage());
    }

    // Método para enviar el JSON de búsqueda
    public void enviarBuscarPartida(String nombre, int puntos) {
        JSONObject json = new JSONObject();
        json.put("tipo", "BUSCAR_PARTIDA");
        json.put("nombre", nombre);
        json.put("puntos", puntos);
        send(json.toString());
        System.out.println("Enviado: BUSCAR_PARTIDA como " + nombre);
    }

    // Método para enviar el JSON de movimiento
    public void enviarMovimiento(String carta, int filaO, int colO, int filaD, int colD, int equipo) {
        JSONObject json = new JSONObject();
        json.put("tipo", "MOVER");
        json.put("carta", carta);
        json.put("fila_origen", filaO);
        json.put("col_origen", colO);
        json.put("fila_destino", filaD);
        json.put("col_destino", colD);
        json.put("equipo", equipo);
        send(json.toString());
        System.out.println("Enviado: MOVER con carta " + carta);
    }

    public static void main(String[] args) {
        try {
            ClienteTestPartida cliente = new ClienteTestPartida("ws://localhost:8080");
            cliente.connect();

            //Bucle para interactuar por consola
            Scanner sc = new Scanner(System.in);
            while (true) {
                String input = sc.nextLine();
                if (input.equalsIgnoreCase("mover")) {
                    //Simulación de un movimiento, va a dar error porque la carta no existe -> esto se debe probar con el frontend
                    cliente.enviarMovimiento("CartaBase", 0, 0, 1, 1, 1);
                } else if (input.equalsIgnoreCase("salir")) {
                    cliente.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}