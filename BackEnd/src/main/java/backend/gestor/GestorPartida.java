package backend.gestor;

import java.sql.SQLException;
import java.util.List;

import backend.JDBC.PartidaJDBC;
import backend.JDBC.JugadorJDBC;
import backend.VO.Partida;
import backend.VO.Jugador;


public class GestorPartida {

    private final PartidaJDBC partidaJdbc;
    private final JugadorJDBC jugadorJdbc;

    public GestorPartida() {
        this.partidaJdbc = new PartidaJDBC();
        this.jugadorJdbc = new JugadorJDBC();
    }

    public int registrarPartida(Partida partida) throws SQLException {
        partidaJdbc.terminarPartidasEnCurso(partida.getJ1(), partida.getJ2());
        return partidaJdbc.registrarPartida(partida);
    }

    public Partida buscarPorId(int idPartida) throws SQLException {
        return partidaJdbc.buscarPorId(idPartida);
    }

    public List<Partida> buscarPartidasJugadorPublicas(String nombreUS) throws SQLException {
        return partidaJdbc.buscarPartidasJugadorPublicas(nombreUS);
    }

    public List<Partida> buscarPartidasJugadorPrivadas(String miNombre, String nombreUS) throws SQLException {
        if (!jugadorJdbc.sonAmigos(miNombre, nombreUS)) {
            return List.of();
        }
        return partidaJdbc.buscarPartidasJugadorPrivadas(miNombre, nombreUS);
    }

    public boolean updateEstado(int idPartida, String nuevoEstado) throws SQLException {
        return partidaJdbc.updateEstado(idPartida, nuevoEstado);
    }

    public boolean updateTurno(int idPartida, int turno) throws SQLException {
        return partidaJdbc.updateTurno(idPartida, turno);
    }

    public boolean updateTiempo(int idPartida, int nuevoTiempo) throws SQLException {
        return partidaJdbc.updateTiempo(idPartida, nuevoTiempo);
    }

    public void terminarPartidasEnCurso(String j1, String j2) {
        partidaJdbc.terminarPartidasEnCurso(j1, j2);
    }

    public boolean updatePosFichas1(int idPartida, String nuevoF1) throws SQLException {
        return partidaJdbc.updatePosFichas1(idPartida, nuevoF1);
    }

    public boolean updatePosFichas2(int idPartida, String nuevoF2) throws SQLException {
        return partidaJdbc.updatePosFichas2(idPartida, nuevoF2);
    }

    public boolean updateMuertesFichas1(int idPartida, int nuevoF1) throws SQLException {
        return partidaJdbc.updateMuertesFichas1(idPartida, nuevoF1);
    }

    public boolean updateMuertesFichas2(int idPartida, int nuevoF2) throws SQLException {
        return partidaJdbc.updateMuertesFichas2(idPartida, nuevoF2);
    }

    public boolean updateTrampaJ1(int idPartida, String pos) throws SQLException {
        return partidaJdbc.updateTrampaJ1(idPartida, pos);
    }

    public boolean updateTrampaJ2(int idPartida, String pos) throws SQLException {
        return partidaJdbc.updateTrampaJ2(idPartida, pos);
    }

    public boolean updateJ1(int idPartida, String nuevoJ1) throws SQLException {
        return partidaJdbc.updateJ1(idPartida, nuevoJ1);
    }

    public boolean updateJ2(int idPartida, String nuevoJ2) throws SQLException {
        return partidaJdbc.updateJ2(idPartida, nuevoJ2);
    }

    public boolean updateGanadorJ1(int idPartida, boolean nuevoGanadorJ1) throws SQLException {
        return partidaJdbc.updateGanadorJ1(idPartida, nuevoGanadorJ1);
    }

    public boolean updateGanadorJ2(int idPartida, boolean nuevoGanadorJ2) throws SQLException {
        return partidaJdbc.updateGanadorJ2(idPartida, nuevoGanadorJ2);
    }

    public boolean finalizarPartida(int idPartida, String ganador, String perdedor, boolean esJ1Ganador) throws SQLException {
       
        boolean estadoOk = partidaJdbc.updateEstado(idPartida, "TERMINADA");
        // primero marcamos partida como terminada, actualizamos ganador y perdedir, buscamos ganador para sumarle
        // una partida ganada más, y con el perdedor lo mismo, pero con partidas perdidas
        if (esJ1Ganador) {
            partidaJdbc.updateGanadorJ1(idPartida, true);
        } else {
            partidaJdbc.updateGanadorJ2(idPartida, true);
        }

        Jugador jGanador = jugadorJdbc.buscarJugador(ganador);
        Jugador jPerdedor = jugadorJdbc.buscarJugador(perdedor);

        if (jGanador != null) {
            jugadorJdbc.updatePartidasJugadas(ganador, jGanador.getPartidasJugadas() + 1);
            jugadorJdbc.updatePartidasGanadas(ganador, jGanador.getPartidasGanadas() + 1);
        }
        if (jPerdedor != null) {
            jugadorJdbc.updatePartidasJugadas(perdedor, jPerdedor.getPartidasJugadas() + 1);
        }

        return estadoOk;
    }

    public void borrar(int idPartida) throws SQLException {
        partidaJdbc.borrar(idPartida);
    }
}