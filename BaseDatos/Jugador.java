package modelo.jdbc;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public final class JugadorJDBC implements JugadorDAO {

    private final DataSource dataSource;

    public JugadorJDBC() {
        try {
            Context initialContext = new InitialContext();
            // "java:comp/env" es el entorno de nombres específico de esta aplicación
            Context envContext = (Context) initialContext.lookup("java:comp/env");
            // Busca la referencia definida en context.xml / web.xml
            this.dataSource = (DataSource) envContext.lookup("jdbc/MiDataSource");
            
        } catch (NamingException e) {
            System.err.println("ERROR FATAL: No se pudo obtener el recurso JNDI 'jdbc/MiDataSource'.");
            e.printStackTrace();
            throw new RuntimeException("Fallo al inicializar la conexión con la BD.", e);
        }
    }
    
    // --- CREATE ---
    @Override
    public void create(JugadorVO jugador) throws SQLException {
        final String sql = """
            INSERT INTO Jugador (Correo, Nombre_US, Contrasena_Hash, Puntos)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugador.getCorreo());
            ps.setString(2, jugador.getNombreUS());
            ps.setString(3, jugador.getContrasenaHash());
            ps.setInt(4, jugador.getPuntos());
            ps.executeUpdate();
        }
    }

    // --- READ (Por Clave Primaria: Nombre_US) ---
    @Override
    public Optional<JugadorVO> readById(String nombreUS) throws SQLException {
        final String sql = "SELECT Correo, Nombre_US, Contrasena_Hash, Puntos FROM Jugador WHERE Nombre_US = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUS);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    // --- READ (Por Correo Único - Muy útil para Login) ---
    public Optional<JugadorVO> readByCorreo(String correo) throws SQLException {
        final String sql = "SELECT Correo, Nombre_US, Contrasena_Hash, Puntos FROM Jugador WHERE Correo = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }
    
    // --- READ ALL (Top 100 por Puntos) ---
    @Override
    public List<JugadorVO> top100() throws SQLException {
        final String sql = "SELECT Correo, Nombre_US, Contrasena_Hash, Puntos FROM Jugador ORDER BY Puntos DESC LIMIT 100";
        List<JugadorVO> jugadores = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                jugadores.add(mapRow(rs));
            }
        }
        return jugadores;
    }

    // --- UPDATE (Completo) ---
    @Override
    public void update(JugadorVO jugador) throws SQLException {
        final String sql = """
            UPDATE Jugador
            SET Correo = ?, Contrasena_Hash = ?, Puntos = ?
            WHERE Nombre_US = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jugador.getCorreo());
            ps.setString(2, jugador.getContrasenaHash());
            ps.setInt(3, jugador.getPuntos());
            ps.setString(4, jugador.getNombreUS());
            ps.executeUpdate();
        }
    }

    // --- UPDATE RÁPIDO (Solo actualizar puntos) ---
    public void updatePuntos(String nombreUS, int nuevosPuntos) throws SQLException {
        try(Connection c = dataSource.getConnection(); 
            PreparedStatement p = c.prepareStatement("UPDATE Jugador SET Puntos = ? WHERE Nombre_US = ?")) { 
            p.setInt(1, nuevosPuntos); 
            p.setString(2, nombreUS); 
            p.executeUpdate(); 
        }
    }

    // --- DELETE ---
    @Override
    public void delete(String nombreUS) throws SQLException {
        final String sql = "DELETE FROM Jugador WHERE Nombre_US = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUS);
            ps.executeUpdate();
        }
    }

    // 🔹 Método auxiliar: convierte una fila del ResultSet en un JugadorVO
    private JugadorVO mapRow(ResultSet rs) throws SQLException {
        return new JugadorVO(
            rs.getString("Correo"),
            rs.getString("Nombre_US"),
            rs.getString("Contrasena_Hash"),
            rs.getInt("Puntos")
        );
    }
}
