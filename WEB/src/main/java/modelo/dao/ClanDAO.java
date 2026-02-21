package modelo.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import modelo.vo.ClanVO;

public interface ClanDAO {
	List<ClanVO> readByName(String j) throws SQLException;
    void create(ClanVO clan, String Creador) throws SQLException;
    Integer idCorrespondiente(String creador) throws SQLException;
    Optional<ClanVO> readById(int id) throws SQLException;
    List<ClanVO> top100() throws SQLException;
    void update(ClanVO clan) throws SQLException;
    void delete(int id) throws SQLException;
}