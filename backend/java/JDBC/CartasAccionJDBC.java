package JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import VO.CartaAccion;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public final class CartasAccionJDBC {

    private final DataSource dataSource;

    public CartasAccionJDBC() {
        try {
            String url = "jdbc:postgresql://localhost:5432/postgres"; 
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
    
    public boolean crearCarta(CartaAccion movimiento) throws SQLException {
        final String sql = "INSERT INTO Cartas_Accion (Nombre, Accion, Puntos_min) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, movimiento.getNombre());
            ps.setString(2, movimiento.getAccion());
            ps.setInt(3, movimiento.getPuntosMin());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public CartaAccion buscarAccion(String nombre) throws SQLException {
        final String sql = "SELECT * FROM Cartas_Accion WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarAccion(rs);
            }
        }
    }

    public boolean updateAccion(String nombre, String nuevaAccion) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Accion SET Accion = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevaAccion); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updatePuntosMin(String nombre, int nuevoPunt) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Accion SET Puntos_min = ? WHERE Nombre = ?")) { 
            p.setInt(1, nuevoPunt); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<CartaAccion> sacarCartas() throws SQLException {
        final String sql = "SELECT * FROM Cartas_Accion";

        List<CartaAccion> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarAccion(rs));
                }
            }
        }
        return cartas;
    }
    
    public List<CartaAccion> sacarCartasPartida(int IDPartida) throws SQLException {
        final String sql = "SELECT c.Nombre, c.Accion, c.Puntos_min, p.Estado, p.Equipo FROM Cartas_Accion c, Partida_Cartas_Accion p WHERE c.Nombre = p.ID_Carta_Accion AND ID_Partida = ?";

        List<CartaAccion> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, IDPartida); 

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarAccionPartida(rs));
                }
            }
        }
        return cartas;
    }

    public boolean asignarEquipo(int IDPartida, String nombreCarta, int equipo) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida_Cartas_Accion SET Equipo = ? WHERE ID_Carta_Accion = ? AND ID_Partida = ?")) { 
            p.setInt(1, equipo); 
            p.setString(2, nombreCarta); 
            p.setInt(3, IDPartida); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateEstadoEnPartida(int IDPartida, String nombreCarta, String estado) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida_Cartas_Accion SET Estado = ? WHERE ID_Carta_Accion = ? AND ID_Partida = ?")) { 
            p.setString(1, estado); 
            p.setString(2, nombreCarta); 
            p.setInt(3, IDPartida); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<CartaAccion> asignar4CartasPartida(int IDPartida, int puntosMin) throws SQLException {
        List<CartaAccion> disponibles = sacarCartas();
        List<CartaAccion> usables = new ArrayList<>();
        //Eliminamos de la lista las cartas que sobrepasen el minimo IMPORTANTE: Poner al menos 4 cartas que no necesiten un minimos de puntos para jugarse
        for(CartaAccion carta : disponibles){
            if(carta.getPuntosMin() <= puntosMin){
                usables.add(carta);
            }
        }
        if (usables.isEmpty()) {
            System.out.println("No hay cartas de acción disponibles para " + puntosMin + " puntos.");
            return null; // Devolvemos null si por alguna razon no me hicisteis caso en el comentario de la linea 138
        }
        Collections.shuffle(usables);
        for(int i = 0; i<4; i++){
            if(!asignarCartaPartida(IDPartida, usables.get(i).getNombre())){
                return null;
            }
            System.out.println(usables.get(i).getNombre());
        }
        return usables.subList(0, 4);
    }

    public boolean asignarCartaPartida(int IDPartida, String nombreCarta) throws SQLException {
        final String sql = "INSERT INTO Partida_Cartas_Accion (ID_Partida, ID_Carta_Accion, Estado) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, IDPartida);
            ps.setString(2, nombreCarta);
            ps.setString(3, "MAZO");
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public void borrar(String nombre) throws SQLException {
        final String sql = "DELETE FROM Cartas_Accion WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo movimiento
    private CartaAccion montarAccion(ResultSet rs) throws SQLException {
        return new CartaAccion(
            rs.getString("Nombre"),
            rs.getString("Accion"),
            rs.getInt("Puntos_min")
        );
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo movimiento
    private CartaAccion montarAccionPartida(ResultSet rs) throws SQLException {
        return new CartaAccion(
            rs.getString("Nombre"),
            rs.getString("Accion"),
            rs.getInt("Puntos_min"),
            rs.getString("Estado"),
            rs.getInt("Equipo")
        );
    }
}
