package DAO;

import java.sql.SQLException;
import java.util.List;

import VO.Partida;

public interface PartidaDAO {
    int registrarPartida(Partida partida) throws SQLException;
    Partida buscarPorId(int idPartida) throws SQLException;
    List<Partida> buscarPartidasJugadorPublicas(String nombreUS) throws SQLException;
    List<Partida> buscarPartidasJugadorPrivadas(String miNombre, String nombreUS) throws SQLException;
    boolean updateEstado(int ID, String nuevoEstado) throws SQLException;
    boolean updateTurno(int ID, int turno) throws SQLException;
    boolean updateTiempo(int ID, int nuevoTiempo) throws SQLException;
    void terminarPartidasEnCurso(String j1, String j2);
    boolean updatePosFichas1(int ID, String nuevoF1) throws SQLException;
    boolean updatePosFichas2(int ID, String nuevoF2) throws SQLException;
    boolean updateMuertesFichas1(int ID, int nuevoF1) throws SQLException;
    boolean updateMuertesFichas2(int ID, int nuevoF2) throws SQLException;
    boolean updateTrampaJ1(int ID, String pos) throws SQLException;
    boolean updateTrampaJ2(int ID, String pos) throws SQLException;
    boolean updateJ1(int ID, String nuevoJ1) throws SQLException;
    boolean updateJ2(int ID, String nuevoJ2) throws SQLException;
    boolean updateGanadorJ1(int ID, boolean nuevoGanadorJ1) throws SQLException;
    boolean updateGanadorJ2(int ID, boolean nuevoGanadorJ2) throws SQLException;
    void borrar(int IDPartida) throws SQLException;
}