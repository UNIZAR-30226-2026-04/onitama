package DAO;

import VO.Jugador;
import java.sql.SQLException;
import java.util.List;

public interface JugadorDAO {
    boolean registrarse(Jugador jugador) throws SQLException;
    Jugador buscarJugador(String nombreUS) throws SQLException;
    List<Jugador> buscarJugadoresPorRaiz(String nombreUS) throws SQLException;
    void borrar(String nombreUS) throws SQLException;
    List<Jugador> sacarJugadoresDisp() throws SQLException;
    boolean updatePuntos(String nombreUS, int nuevosPuntos) throws SQLException;
    boolean updateContrasenya(String nombreUS, String psswd) throws SQLException;
    boolean updateCorreo(String nombreUS, String nuevoCorreo) throws SQLException;
    boolean updateCores(String nombreUS, int nuevosCores) throws SQLException;
    boolean updatePartidasGanadas(String nombreUS, int nuevasPartidasGanadas) throws SQLException;
    boolean updatePartidasJugadas(String nombreUS, int nuevasPartidasJugadas) throws SQLException;
    boolean updateAvatar(String nombreUS, String nuevoAvatarId) throws SQLException;
    boolean updateSkinActiva(String nombreUS, String nuevaSkin) throws SQLException;
    boolean desbloquearSkin(String miNombre, String skin) throws SQLException;
    boolean insertarAmistad(String jugadorA, String jugadorB) throws SQLException;
    boolean sonAmigos(String jugadorA, String jugadorB) throws SQLException;
    List<Jugador> sacarAmigos(String nombreUS) throws SQLException;
    boolean borrarAmigo(String miNombre, String nombreAmigo) throws SQLException;
}