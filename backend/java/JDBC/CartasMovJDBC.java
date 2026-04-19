package JDBC;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import VO.CartaMov;
import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import DAO.CartasMovDAO;

public final class CartasMovJDBC implements CartasMovDAO {

    private final DataSource dataSource;

    public CartasMovJDBC() {
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
    
    public boolean crearCarta(CartaMov movimiento) throws SQLException {
        final String sql = "INSERT INTO Cartas_Mov (Nombre, Movimientos, Puntos_min, img) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, movimiento.getNombre());
            ps.setString(2, movimiento.getMovimientos());
            ps.setInt(3, movimiento.getPuntosMin());
            ps.setString(4, movimiento.getImg());
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
            
        } catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public CartaMov buscarMovimiento(String nombre) throws SQLException {
        final String sql = "SELECT * FROM Cartas_Mov WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return montarMovimiento(rs);
            }
        }
    }

    public boolean updateMovimientos(String nombre, String nuevosMov) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Mov SET Movimientos = ? WHERE Nombre = ?")) { 
            p.setString(1, nuevosMov); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updatePuntosMin(String nombre, int puntos) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Mov SET Puntos_min = ? WHERE Nombre = ?")) { 
            p.setInt(1, puntos); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public boolean updateImg(String nombre, String img) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Cartas_Mov SET img = ? WHERE Nombre = ?")) { 
            p.setString(1, img); 
            p.setString(2, nombre); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<CartaMov> sacarCartas() throws SQLException {
        final String sql = "SELECT * FROM Cartas_Mov";

        List<CartaMov> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarMovimiento(rs));
                }
            }
        }
        return cartas;
    }
    
    public List<CartaMov> sacarCartasPartida(int IDPartida) throws SQLException {
        final String sql = "SELECT c.Nombre, c.Movimientos, c.Puntos_min, c.img, p.Estado FROM Cartas_Mov c, Partida_Cartas_Mov p WHERE c.Nombre = p.ID_Carta_Mov AND p.ID_Partida = ?";

        List<CartaMov> cartas = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, IDPartida); 

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    cartas.add(montarMovimientoPartida(rs));
                }
            }
        }
        return cartas;
    }

    public boolean updateEstadoEnPartida(int IDPartida, String nombreCarta, String estado) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Partida_Cartas_Mov SET Estado = ? WHERE ID_Carta_Mov = ? AND ID_Partida = ?")) { 
            p.setString(1, estado); 
            p.setString(2, nombreCarta); 
            p.setInt(3, IDPartida); 
            p.executeUpdate(); 
            return true;
        }catch (SQLException e) {
            return false; // Si hay una excepción, asumimos que no se creó
        }
    }

    public List<CartaMov> asignar7CartasPartida(int IDPartida, int puntosMin) throws SQLException {
        List<CartaMov> disponibles = sacarCartas(); //IMPORTANTE: Tiene que haber mas de 7 cartas para que funcione
        List<CartaMov> usables = new ArrayList<>();
        //Eliminamos de la lista las cartas que sobrepasen el minimo IMPORTANTE: Poner al menos 4 cartas que no necesiten un minimos de puntos para jugarse
        for(CartaMov carta : disponibles){
            if(carta.getPuntosMin() <= puntosMin){
                usables.add(carta);
            }
        }
        if (usables.isEmpty()) {
            System.out.println("No hay cartas de movimiento disponibles para " + puntosMin + " puntos.");
            return null; // Devolvemos null si por alguna razon no me hicisteis caso en el comentario de la linea 138
        }
        Collections.shuffle(usables);
        for(int i = 0; i<7; i++){
            if(!asignarCartaPartida(IDPartida, usables.get(i).getNombre())){
                return null;
            }
        }
        return usables.subList(0, 7);
    }

    public boolean asignarCartaPartida(int IDPartida, String nombreCarta) throws SQLException {
        final String sql = "INSERT INTO Partida_Cartas_Mov (ID_Partida, ID_Carta_Mov, Estado) VALUES (?, ?, ?)";
        
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
        final String sql = "DELETE FROM Cartas_Mov WHERE Nombre = ?";
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo movimiento
    private CartaMov montarMovimiento(ResultSet rs) throws SQLException {
        return new CartaMov(
            rs.getString("Nombre"),
            rs.getString("Movimientos"),
            rs.getInt("Puntos_min"),
            rs.getString("img")
        );
    }

    //Metodo auxiliar que saca los campod de la BD y crea un objeto de tipo movimiento
    private CartaMov montarMovimientoPartida(ResultSet rs) throws SQLException {
        return new CartaMov(
            rs.getString("Nombre"),
            rs.getString("Movimientos"),
            rs.getInt("Puntos_min"),
            rs.getString("img"),
            rs.getString("Estado")
        );
    }
}
