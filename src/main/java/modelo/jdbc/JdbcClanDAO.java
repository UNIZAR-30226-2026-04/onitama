package modelo.jdbc;

import java.sql.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import modelo.dao.ClanDAO;
import modelo.vo.ClanVO;

import org.postgresql.ds.PGSimpleDataSource;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

public final class JdbcClanDAO implements ClanDAO {

    private final DataSource dataSource;

    public JdbcClanDAO() {
    	try {
            Context initialContext = new InitialContext();
            
            // "java:comp/env" es el entorno de nombres específico de esta aplicación
            Context envContext = (Context) initialContext.lookup("java:comp/env");
            
            // Busca la referencia que acabas de definir en web.xml
            // 
            this.dataSource = (DataSource) envContext.lookup("jdbc/MiDataSource");
            
        } catch (NamingException e) {
            System.err.println("ERROR FATAL: No se pudo obtener el recurso JNDI 'jdbc/MiDataSource'.");
            e.printStackTrace();
            throw new RuntimeException("Fallo al inicializar la conexión con la BD.", e);
        }
    }
    
    public void denegarReporteClan(int id) throws SQLException {
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("UPDATE CLAN SET Reportado=0 WHERE ID=?")){ p.setInt(1,id); p.executeUpdate(); }
    }
    
    public List<ClanVO> leerClanesReportados() throws SQLException {
        List<ClanVO> l = new ArrayList<>();
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("SELECT ID, Pt, Nom FROM CLAN WHERE Reportado=1"); ResultSet r=p.executeQuery()){
            while(r.next()) l.add(new ClanVO(r.getInt("ID"), r.getInt("Pt"), r.getString("Nom")));
        }
        return l;
    }
    
    public void updateNombre(int id, String n) throws SQLException {
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("UPDATE CLAN SET Nom=? WHERE ID=?")){ p.setString(1,n); p.setInt(2,id); p.executeUpdate(); }
    }

    public boolean esReportado(int id) throws SQLException {
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("SELECT Reportado FROM CLAN WHERE ID=?")){
            p.setInt(1,id); ResultSet r=p.executeQuery(); return r.next() && r.getInt(1)==1;
        }
    }
    
    public void reportarClan(int id) throws SQLException {
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("UPDATE CLAN SET Reportado=1 WHERE ID=?")){ p.setInt(1,id); p.executeUpdate(); }
    }
    
    // READ (por ID)
    @Override
    public Integer idCorrespondiente(String creador) throws SQLException {
        final String sql = "SELECT ID FROM CLAN WHERE creador = ?";
        try (Connection conn = dataSource.getConnection();
               PreparedStatement ps = conn.prepareStatement(sql)) {
            	ps.setString(1, creador);
               try (ResultSet rs = ps.executeQuery()) {
                   if (rs.next()) {
                       return rs.getInt(1);
                   } else {
                       return null;
                   }
               }
           }
    }
    
    public void quitarCreador(String Creador) throws SQLException {
        final String sql = """
            UPDATE CLAN
            SET Creador = NULL
            WHERE Creador = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Creador);
            ps.executeUpdate();
        }
    }
    
    // CREATE
    @Override
    public void create(ClanVO clan, String Creador) throws SQLException {
        final String sql = """
            INSERT INTO CLAN (Pt, Nom, Creador)
            VALUES (?, ?, ?)
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clan.getPt());
            ps.setString(2, clan.getNom());
            ps.setString(3, Creador);
            ps.executeUpdate();
        }
    }

    // READ (por ID)
    @Override
    public Optional<ClanVO> readById(int id) throws SQLException {
        final String sql = "SELECT ID, Pt, Nom FROM CLAN WHERE ID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }
        }
    }

    @Override
    public List<ClanVO> readByName(String j) throws SQLException {
        final String sql = "SELECT ID, Pt, Nom FROM CLAN WHERE Nom = ?";
        List<ClanVO> clanes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, j); 

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                	clanes.add(mapRow(rs));
                }
            }
        }
        return clanes;
    }
    
    // READ ALL
    @Override
    public List<ClanVO> top100() throws SQLException {
        final String sql = "SELECT ID, Pt, Nom FROM CLAN ORDER BY Pt DESC LIMIT 100";
        List<ClanVO> clanes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clanes.add(mapRow(rs));
            }
        }
        return clanes;
    }

    // UPDATE
    @Override
    public void update(ClanVO clan) throws SQLException {
        final String sql = """
            UPDATE CLAN
            SET Pt = ?, Nom = ?
            WHERE ID = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clan.getPt());
            ps.setString(2, clan.getNom());
            ps.setInt(3, clan.getId());
            ps.executeUpdate();
        }
    }

    // DELETE
    @Override
    public void delete(int id) throws SQLException {
        final String sql = "DELETE FROM CLAN WHERE ID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // 🔹 Método auxiliar: convierte una fila del ResultSet en un ClanVO
    private ClanVO mapRow(ResultSet rs) throws SQLException {
        return new ClanVO(
            rs.getInt("ID"),
            rs.getInt("Pt"),
            rs.getString("Nom")
        );
    }
}