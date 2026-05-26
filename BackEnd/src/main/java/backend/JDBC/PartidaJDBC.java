package backend.JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import backend.VO.Partida;

import java.util.List;
import java.util.ArrayList;

import backend.DAO.PartidaDAO;

public final class PartidaJDBC implements PartidaDAO {

    private final DataSource dataSource;

    public PartidaJDBC() {
        try {
            String url = "jdbc:postgresql://database:5432/postgres"; 
            String user = "postgres";
            String password = "postgres";
            
            org.postgresql.ds.PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
            ds.setURL(url);
            ds.setUser(user);
            ds.setPassword(password);
            this.dataSource = ds;
            
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar manualmente", e);
        }
    }

    // inserta una nueva partida y devuelve el ID generado por la BBDD, -1 si hay fallo
    public int registrarPartida(Partida partida) throws SQLException {
        final String sql = "INSERT INTO Partida (Estado, Tiempo, Tipo, Pos_Fichas_Eq1, Pos_Fichas_Eq2, FichasMuertas1, FichasMuertas2, J1, J2, Es_Ganador_J1, Es_Ganador_J2, Turno, Pos_Trampa_J1, Pos_Trampa_J2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        //Añadimos Statement.RETURN_GENERATED_KEYS al preparar la sentencia para que nos devuelva el id generado
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, partida.getEstado());
            ps.setInt(2, partida.getTiempo());
            ps.setString(3, partida.getTipo());
            ps.setString(4, partida.getPos_Fichas_Eq1());
            ps.setString(5, partida.getPos_Fichas_Eq2());
            ps.setInt(6, partida.getFichasMuertas1());
            ps.setInt(7, partida.getFichasMuertas2());
            ps.setString(8, partida.getJ1());
            ps.setString(9, partida.getJ2());
            ps.setBoolean(10, partida.isEs_Ganador_J1());
            ps.setBoolean(11, partida.isEs_Ganador_J2());
            ps.setInt(12, partida.getTurno());
            ps.setString(13, partida.getTrampaPosJ1());
            ps.setString(14, partida.getTrampaPosJ2());
            
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); //Esto devuelve el ID generado por la BD
                    }
                }
            }
            return -1; // Retornamos -1 si no se pudo insertar nada
            
        } catch (SQLException e) {
            return -1;
        }
    }

    // busca y devuelve una partida por su id, null si no existe
    public Partida buscarPorId(int idPartida) throws SQLException {
        final String sql = "SELECT * FROM Partida WHERE ID_Partida = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, idPartida);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return montarPartida(rs);
                }
            }
        }
        return null;
    }

    // devuelve las 3 últimas partidas públicas en las que ha participado un jugador
    public List<Partida> buscarPartidasJugadorPublicas(String nombreUS) throws SQLException {
        final String sql = "SELECT * FROM Partida WHERE (J1 = ? OR J2 = ?) AND Tipo = 'PUBLICA' ORDER BY ID_Partida DESC LIMIT 3";

        List<Partida> partidas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, nombreUS);
            p.setString(2, nombreUS);

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    partidas.add(montarPartida(rs));
                }
            }
        }
        return partidas;
    }

    // devuelve las últimas 3 partidas privadas jugadas entre dos jugadores concretos 
    public List<Partida> buscarPartidasJugadorPrivadas(String miNombre, String nombreUS) throws SQLException {
        final String sql = "SELECT * FROM Partida WHERE ((J1 = ? AND J2 = ?) OR (J1 = ? AND J2 = ?)) AND Tipo = 'PRIVADA' ORDER BY ID_Partida DESC LIMIT 3";

        List<Partida> partidas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, miNombre);
            p.setString(2, nombreUS);
            p.setString(3, nombreUS);
            p.setString(4, miNombre);

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    partidas.add(montarPartida(rs));
                }
            }
        }
        return partidas;
    }

    // actualiza el turno actual de una partida
    public boolean updateTurno(int ID, int turno) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Turno = ? WHERE ID_Partida = ?")) { 
            p.setInt(1, turno); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza el estado de una partida (JUGANDOSE, FINALIZADA...)
    public boolean updateEstado(int ID, String nuevoEstado) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Estado = ? WHERE ID_Partida = ?")) { 
            p.setString(1, nuevoEstado); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza el tiempo transcurrido de una partida
    public boolean updateTiempo(int ID, int nuevoTiempo) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Tiempo = ? WHERE ID_Partida = ?")) { 
            p.setInt(1, nuevoTiempo); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza la posición de las fichas del equipo 1
    public boolean updatePosFichas1(int ID, String nuevoF1) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Pos_Fichas_Eq1 = ? WHERE ID_Partida = ?")) { 
            p.setString(1, nuevoF1); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }
    
    // marca como FINALIZADA cualquier partida activa en la que participa alguno de los dos jugadores indicados
    public void terminarPartidasEnCurso(String j1, String j2) {
        final String sql = "UPDATE Partida SET Estado = 'FINALIZADA' WHERE (J1 = ? OR J2 = ? OR J1 = ? OR J2 = ?) AND Estado = 'JUGANDOSE'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, j1);
            p.setString(2, j1);
            p.setString(3, j2);
            p.setString(4, j2);
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error limpiando partidas en curso: " + e.getMessage());
        }
    }

    // actualiza la posición de las fichas del equipo 2
    public boolean updatePosFichas2(int ID, String nuevoF2) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Pos_Fichas_Eq2 = ? WHERE ID_Partida = ?")) { 
            p.setString(1, nuevoF2); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza el contador de fichas muertas del equipo 1
    public boolean updateMuertesFichas1(int ID, int nuevoF1) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET FichasMuertas1 = ? WHERE ID_Partida = ?")) { 
            p.setInt(1, nuevoF1); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza el contador de fichas muertas del equipo 2
    public boolean updateMuertesFichas2(int ID, int nuevoF2) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET FichasMuertas2 = ? WHERE ID_Partida = ?")) { 
            p.setInt(1, nuevoF2); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza el jugador 1 asignado a una partida
    public boolean updateJ1(int ID, String nuevoJ1) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET J1 = ? WHERE ID_Partida = ?")) { 
            p.setString(1, nuevoJ1); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // actualiza el jugador 2 asignado a una partida
    public boolean updateJ2(int ID, String nuevoJ2) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET J2 = ? WHERE ID_Partida = ?")) { 
            p.setString(1, nuevoJ2); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // marca en la bbdd que el jugador 1 es el ganador de la partida
    public boolean updateGanadorJ1(int ID, boolean nuevoGanadorJ1) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Es_Ganador_J1 = ? WHERE ID_Partida = ?")) { 
            p.setBoolean(1, nuevoGanadorJ1); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // marca en la bbdd que el jugador 2 es el ganador de la partida
    public boolean updateGanadorJ2(int ID, boolean nuevoGanadorJ2) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Es_Ganador_J2 = ? WHERE ID_Partida = ?")) { 
            p.setBoolean(1, nuevoGanadorJ2); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    // UPDATES DE CASILLA TRAMPA PARA QUE NO SEAN NULL AL RECUPERAR PARTIDA PRIVADA
    // registrarPartida todavía pone null en casillas trampa porque los jugadores no las han
    // elegido, entonces actualizarBD también las guardaba como null.
    // Este update se va a hacer una única vez por casilla trampa para poder recuperar las coordenadas.

    // guarda la posición de casilla trampa del jugador 1
    public boolean updateTrampaJ1(int ID, String pos) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Pos_Trampa_J1 = ? WHERE ID_Partida = ?")) { 
            p.setString(1, pos); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // guarda la posición de casilla trampa del jugador 2
    public boolean updateTrampaJ2(int ID, String pos) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida SET Pos_Trampa_J2 = ? WHERE ID_Partida = ?")) { 
            p.setString(1, pos); 
            p.setInt(2, ID); 
            p.executeUpdate(); 
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // elimina una partida por su ID
    public void borrar(int IDPartida) throws SQLException {
        final String sql = "DELETE FROM Partida WHERE ID_Partida = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, IDPartida);
            ps.executeUpdate();
        }
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo Partida
    private Partida montarPartida(ResultSet rs) throws SQLException {
        return new Partida(
            rs.getInt("ID_Partida"),
            rs.getString("Estado"),
            rs.getInt("Tiempo"),
            rs.getString("Tipo"),
            rs.getString("Pos_Fichas_Eq1"),
            rs.getString("Pos_Fichas_Eq2"),
            rs.getInt("FichasMuertas1"),
            rs.getInt("FichasMuertas2"),
            rs.getString("J1"),
            rs.getString("J2"),
            rs.getBoolean("Es_Ganador_J1"),
            rs.getBoolean("Es_Ganador_J2"),
            rs.getInt("Turno"),
            rs.getString("Pos_Trampa_J1"),
            rs.getString("Pos_Trampa_J2")
        );
    }
}
