package backend.gestor;
 
import java.sql.SQLException;
import java.util.List;
 
import backend.JDBC.SkinJDBC;
import backend.JDBC.JugadorJDBC;
import backend.VO.Skin;
import backend.VO.Jugador;

// GestorSkin actúa de capa intermedia entre las llamadas de un Value Object o del servidor a los JDBC para modificar la bbdd

public class GestorSkin {
 
    private final SkinJDBC skinJdbc;
    private final JugadorJDBC jugadorJdbc;
 
    public GestorSkin() {
        this.skinJdbc = new SkinJDBC();
        this.jugadorJdbc = new JugadorJDBC();
    }
 
    public boolean crearSkin(Skin skin) throws SQLException {
        if (skinJdbc.buscarSkin(skin.getNombre()) != null) {
            return false;
            // si ya existe skin que se llama igual
        }
        return skinJdbc.crearSkin(skin);
    }
 
    public Skin buscarSkin(String nombre) throws SQLException {
        return skinJdbc.buscarSkin(nombre);
    }
 
    public void borrar(String nombre) throws SQLException {
        skinJdbc.borrar(nombre);
    }
 
    public List<Skin> sacarSkinDisp() throws SQLException {
        return skinJdbc.sacarSkinDisp();
    }

    public String comprarSkin(String nombreSkin, String nombreJugador) throws SQLException {
    Skin skin = skinJdbc.buscarSkin(nombreSkin);
    if (skin == null) return "SKIN_NO_EXISTE";

    Jugador jugador = jugadorJdbc.buscarJugador(nombreJugador);
    if (jugador == null) return "ERROR_BD";

    List<Skin> skinsJugador = skinJdbc.sacarSkinJugador(nombreJugador);
    boolean yaComprada = skinsJugador.stream()
            .anyMatch(sk -> sk.getNombre().equals(nombreSkin));
    if (yaComprada) return "YA_COMPRADA";

    if (jugador.getCores() < skin.getPrecio()) return "CORES_INSUFICIENTES";

    int nuevosCores = jugador.getCores() - skin.getPrecio();
    boolean coresOk = jugadorJdbc.updateCores(nombreJugador, nuevosCores);
    boolean skinOk = skinJdbc.comprarSkin(nombreSkin, nombreJugador);

    if (!coresOk || !skinOk) return "ERROR_BD";

    return "OK";
    }
 
    public List<Skin> sacarSkinJugador(String nombreUS) throws SQLException {
        return skinJdbc.sacarSkinJugador(nombreUS);
    }
 
    public boolean updatePrecio(String nombre, int nuevoPrecio) throws SQLException {
        return skinJdbc.updatePrecio(nombre, nuevoPrecio);
    }
}