import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

//Hay que añadir el .jar de json y de websocket

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

    // PREVIAMENTE ESTABA if p1.ws return p1, pero eso devolvía el mismo jugador,
    // entonces lo he cambiado para que devuelva el oponente.
    public InfoJugador getPareja(WebSocket _p1){
        if(p1.ws == _p1){
            return p2;
        }else if(p2.ws == _p1){
            return p1;
        }else{
            return null;
        }
    }
}

class Partida {

    String id;
    Pareja pareja;
    String[] cartasMov; // SERÁN 7 EN ROTACIÓN

    public Partida (String _id, Pareja _pareja){
        id = _id;
        pareja = _pareja;
        cartasMov = cartasAleatorias();
    }

    private String[] cartasAleatorias(){
        String[] todas = { "Tigre", "Dragon", "Rana", "Conejo", "Cangrejo", "Elefante", "Ganso",
        "Gallo", "Mono", "Mantis", "Caballo", "Buey", "Grulla", "Oso", "Aguila", "Cobra"};

        List<String> mazo = new ArrayList<>(); // definimos lista donde van a ir las 7 elegidas
        for (String c : todas) mazo.add(c);
        // añadimos todas y hacemos shuffle, con el método de la interfaz collections (he buscado en google
        // how to generate a random order list in java, igual hay otras opciones)
        Collections.shuffle(mazo);

        String[] cartasElegidas =  new String[] { mazo.get(0), mazo.get(1), mazo.get(2), mazo.get(3), mazo.get(4), mazo.get(5),mazo.get(6)};

        return cartasElegidas;
    }

    // Con 'iniciar' construimos los JSON de tipo PARTIDA_ENCONTRADA
    public void iniciar(){

        JSONArray mazoJ1 = new JSONArray();
        JSONArray mazoJ2 = new JSONArray();

        JSONArray cola = new JSONArray();

        mazoJ1.put(cartasMov[0]);
        mazoJ1.put(cartasMov[1]);
        mazoJ2.put(cartasMov[2]);
        mazoJ2.put(cartasMov[3]); 

        cola.put(cartasMov[4]);
        cola.put(cartasMov[5]);
        cola.put(cartasMov[6]);

        JSONObject msg1 = new JSONObject(); // el que espera a jugar
        msg1.put("tipo", "PARTIDA_ENCONTRADA");
        msg1.put("partida_id", this.id);
        msg1.put("equipo", 1);
        msg1.put("oponente", pareja.p2.nombre);
        msg1.put("oponentePt", pareja.p2.puntos);
        msg1.put("cartas_jugador", mazoJ1);
        msg1.put("cartas_oponente", mazoJ2);
        msg1.put("carta_siguiente", cola);

        JSONObject msg2= new JSONObject(); // al que le emparejan a j1
        msg2.put("tipo", "PARTIDA_ENCONTRADA");
        msg2.put("partida_id", this.id);
        msg2.put("equipo", 2);
        msg2.put("oponente", pareja.p1.nombre);
        msg2.put("oponentePt", pareja.p1.puntos);
        msg2.put("cartas_jugador", mazoJ2);
        msg2.put("cartas_oponente", mazoJ1);
        msg2.put("carta_siguiente", cola);

        pareja.p1.ws.send(msg1.toString());
        pareja.p2.ws.send(msg2.toString());
        System.out.println("Partida "+id+" iniciada.");

        // Envío las 3 cartas de la cola para que el frontend gestione la rotación por su cuenta,
        // porque si solo enviáramos la carta visible, el servidor tendría que guardar las demás 
        // en memoria y el frontend tendría que pedirle la siguiente cada vez que gasta una, 
        // que acumularía bastante petición y respuesta. Con esto evitamos mensajes extra.

    }
}

public class Servidor extends WebSocketServer {
    List<InfoJugador> buscando_partida;
    List<Partida> partidas;

    public Servidor(int puerto) {
        super(new InetSocketAddress(puerto));
        // conexiones = new ArrayList<>(); 
        // he comentado esto porque 'conexiones' no estaba definido en ningún
        // otro sitio de 'Servidor.java', luego he continuado con la inicialización 
        // de las dos listas previamente declaradas.
        buscando_partida = new ArrayList<>();
        partidas = new ArrayList<>();
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

        // hago lo mismo con parejas, porque puede salirse del juego
        partidas.removeIf(jugador -> jugador.buscar(conn)); // si recorremos la lista y uno de los jugadores cuya conexión que se acaba de cerrar ya tiene asignada una pareja para el jueg, se quita
        // pero, gestion del otro jugador?
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
                InfoJugador local = new InfoJugador(conn, nombre, puntos);
                Pareja p = new Pareja(oponente, local);
                // para el id aleatorio no sabía qué usar, le he preguntado a GML (z.ai) y me ha dicho 
                // de usar currentTimeMilli porque es simplemente una forma barata y automática de generar 
                // nombres ("etiquetas") distintos para cada juego sin que tengas que llevar un contador manual (contador++).
                // Lo que devuelve es la fecha y hora actual en milisegundos, partiendo de un valor base. 
                Partida partida = new Partida("partida-"+System.currentTimeMillis(), p);

                partidas.add(partida);
                partida.iniciar();

                System.out.println("Partida creada: " + nombre + " VS " + oponente.nombre);
            } else {
                buscando_partida.add(new InfoJugador(conn, nombre, puntos));
                System.out.println(nombre + " está esperando a unirse a una partida.");
            }
    }

    private void gestionarMovimiento(WebSocket conn, JSONObject obj) {
        // POR TERMINAR
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);

        JSONObject obj = new JSONObject(message);
        String tipoMSG = obj.getString("tipo");


        
        if(tipoMSG.equals("BUSCAR_PARTIDA")){
            gestionarBusquedaPartida(conn, obj);

            // He sacado todo el tema de generar cartas y mensajes fuera de aquí
            // y lo metido en su propia clase 'Partida'.
            // Así este método solo se dedica a buscar pareja.
            /*
            String jugadorNom = obj.getString("nombre");
            int jugadorPt = obj.getInt("puntos");

            // En lugar de crear el mensaje JSON manualmente aquí, instanciamos
            // un objeto 'Partida' qu se va a encargar de generar las cartas
            // aleatorias y enviar el mensaje 'PARTIDA_ENCONTRADA' a ambos jugadores.

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

            */
        } else if (tipoMSG.equals("MOVER")) {
            gestionarMovimiento(conn, obj);
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