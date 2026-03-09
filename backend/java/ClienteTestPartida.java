import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import VO.Posicion;
import VO.Jugador;

//Clase auxiliar para representar la carta en el cliente
class CartaCliente {
    String nombre;
    List<Posicion> movimientos = new ArrayList<>();

    public CartaCliente(String nombre, JSONArray movsJson) {
        this.nombre = nombre;
        for (int i = 0; i < movsJson.length(); i++) {
            JSONObject m = movsJson.getJSONObject(i);
            this.movimientos.add(new Posicion(m.getInt("x"), m.getInt("y"), null));
        }
    }
}

public class ClienteTestPartida extends WebSocketClient {
    private List<CartaCliente> misCartas = new ArrayList<>();
    private int miEquipo = 0;

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

    // Método para enviar el JSON de búsqueda
    public void enviarBuscarPartida(String nombre, int puntos) {
        JSONObject json = new JSONObject();
        json.put("tipo", "BUSCAR_PARTIDA");
        json.put("nombre", nombre);
        json.put("puntos", puntos);
        send(json.toString());
        System.out.println("Enviado: BUSCAR_PARTIDA como " + nombre);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("MSG:: " + message);
        JSONObject obj = new JSONObject(message);
        String tipo = obj.getString("tipo");

        if (tipo.equals("PARTIDA_ENCONTRADA")) {
            miEquipo = obj.getInt("equipo");
            misCartas.clear();

            JSONArray cartasJson = obj.getJSONArray("cartas_jugador");
            for (int i = 0; i < cartasJson.length(); i++) {
                JSONObject c = cartasJson.getJSONObject(i);
                //Guardamos las cartas
                misCartas.add(new CartaCliente(c.getString("nombre"), c.getJSONArray("movimientos")));
            }
            mostrarMano();
        }
    }

    private void mostrarMano() {
        System.out.println("\nTUS CARTAS DE MOVIMIENTOS: ");
        for (CartaCliente c : misCartas) {
            System.out.print("Carta: " + c.nombre + ", Movimientos: ");
            for (Posicion p : c.movimientos) {
                System.out.print("(" + p.getX() + "," + p.getY() + ") ");
            }
            System.out.println();
        }
    }

    public void realizarMovimientoSimulado() {
        if (misCartas.isEmpty()) return;

        CartaCliente carta = misCartas.get(0);

        //Elegimos la primera carta de la lista
        Posicion mov = carta.movimientos.get(0);

        JSONObject json = new JSONObject();
        json.put("tipo", "MOVER");
        json.put("carta", carta.nombre);
        json.put("equipo", miEquipo);
        json.put("fila_origen", 0);
        json.put("col_origen", 0);
        json.put("fila_destino", mov.getY());
        json.put("col_destino", mov.getX());

        send(json.toString());
        System.out.println(">>> Movimiento enviado: " + carta.nombre + " hacia (" + mov.getX() + "," + mov.getY() + ")");
    }

    @Override 
    public void onClose(int c, String r, boolean rem) { 
        System.out.println("Desconectado."); 
    }

    @Override 
    public void onError(Exception e) { 
        e.printStackTrace(); 
    }

    public static void main(String[] args) throws URISyntaxException {
        ClienteTestPartida cliente = new ClienteTestPartida("ws://localhost:8080");
        cliente.connect();
        Scanner sc = new Scanner(System.in);
        while(true) {
            String in = sc.nextLine();
            if(in.equals("mover")){
                cliente.realizarMovimientoSimulado();
            }
        }
    }
}