package backend.DAO;

import backend.VO.Skin;
import java.sql.SQLException;
import java.util.List;

// interfaz DAO para las skins -> CRUD, compra, asignación a jugadores y precio
public interface SkinDAO {
    boolean crearSkin(Skin skin) throws SQLException;
    Skin buscarSkin(String nombre) throws SQLException;
    void borrar(String nombre) throws SQLException;
    List<Skin> sacarSkinDisp() throws SQLException;
    boolean comprarSkin(String nombreSkin, String nombreJugador) throws SQLException;
    List<Skin> sacarSkinJugador(String nombreUS) throws SQLException;
    boolean updatePrecio(String nombre, int nuevoPrecio) throws SQLException;
}