package gestor;
 
import java.sql.SQLException;
import java.util.List;
 
import JDBC.SkinJDBC;
import JDBC.JugadorJDBC;
import VO.Skin;
import VO.Jugador;

public class GestorJugador {

    private final JugadorJDBC jugadorJdbc;
    private final SkinJDBC skinJdbc;

    public GestorJugador() {
        this.jugadorJdbc = new JugadorJDBC();
        this.skinJdbc = new SkinJDBC();
    }

    public boolean registrarse(Jugador jugador) throws SQLException {
        if (jugadorJdbc.buscarJugador(jugador.getNombre()) != null) {
            return false;
        }
        return jugadorJdbc.registrarse(jugador);
    }

    public Jugador buscarJugador(String nombreUS) throws SQLException {
        return jugadorJdbc.buscarJugador(nombreUS);
    }

    public List<Jugador> buscarJugadoresPorRaiz(String raiz) throws SQLException {
        return jugadorJdbc.buscarJugadoresPorRaiz(raiz);
    }

    public void borrar(String nombreUS) throws SQLException {
        jugadorJdbc.borrar(nombreUS);
    }

    public List<Jugador> sacarJugadoresDisp() throws SQLException {
        return jugadorJdbc.sacarJugadoresDisp();
    }


    public boolean updatePuntos(String nombreUS, int nuevosPuntos) throws SQLException {
        return jugadorJdbc.updatePuntos(nombreUS, nuevosPuntos);
    }

    public boolean updateContrasenya(String nombreUS, String psswd) throws SQLException {
        return jugadorJdbc.updateContrasenya(nombreUS, psswd);
    }

    public boolean updateCorreo(String nombreUS, String nuevoCorreo) throws SQLException {
        return jugadorJdbc.updateCorreo(nombreUS, nuevoCorreo);
    }

    public boolean updateCores(String nombreUS, int nuevosCores) throws SQLException {
        return jugadorJdbc.updateCores(nombreUS, nuevosCores);
    }

    public boolean updatePartidasGanadas(String nombreUS, int nuevasPartidasGanadas) throws SQLException {
        return jugadorJdbc.updatePartidasGanadas(nombreUS, nuevasPartidasGanadas);
    }

    public boolean updatePartidasJugadas(String nombreUS, int nuevasPartidasJugadas) throws SQLException {
        return jugadorJdbc.updatePartidasJugadas(nombreUS, nuevasPartidasJugadas);
    }

    public boolean updateAvatar(String nombreUS, String nuevoAvatarId) throws SQLException {
        return jugadorJdbc.updateAvatar(nombreUS, nuevoAvatarId);
    }

    public boolean updateSkinActiva(String nombreUS, String nuevaSkin) throws SQLException {
        if (!nuevaSkin.equals("Skin0")) {
            List<Skin> skinsDelJugador = skinJdbc.sacarSkinJugador(nombreUS);
            boolean posee = skinsDelJugador.stream()
                    .anyMatch(sk -> sk.getNombre().equals(nuevaSkin));
            if (!posee) {
                return false;
            }
        }
        return jugadorJdbc.updateSkinActiva(nombreUS, nuevaSkin);
    }

    public boolean desbloquearSkin(String nombreUS, String skin) throws SQLException {
        return jugadorJdbc.desbloquearSkin(nombreUS, skin);
    }

    public boolean insertarAmistad(String jugadorA, String jugadorB) throws SQLException {
        return jugadorJdbc.insertarAmistad(jugadorA, jugadorB);
    }

    public boolean sonAmigos(String jugadorA, String jugadorB) throws SQLException {
        return jugadorJdbc.sonAmigos(jugadorA, jugadorB);
    }

    public List<Jugador> sacarAmigos(String nombreUS) throws SQLException {
        return jugadorJdbc.sacarAmigos(nombreUS);
    }

    public boolean borrarAmigo(String miNombre, String nombreAmigo) throws SQLException {
        return jugadorJdbc.borrarAmigo(miNombre, nombreAmigo);
    }

    public Jugador buscarJugadorPorCorreo(String correo) throws SQLException {
        return jugadorJdbc.buscarJugadorPorCorreo(correo);
    }
}