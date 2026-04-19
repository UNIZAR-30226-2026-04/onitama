package VO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import JDBC.CartasMovJDBC;
import DAO.CartasMovDAO;

public class CartaMov {
    private String nombre, estado, img;
    private int puntosMin;
    private List<Posicion> movimientos;
    private CartasMovDAO dao;

    public CartaMov(String nombre, String mov, int puntosMin, String img){
        this.nombre = nombre;
        this.estado = "MAZO";
        this.puntosMin = puntosMin;
        this.img = img;
        movimientos = new ArrayList<>();
        dao = new CartasMovJDBC();
        
        // Esta expresión regular busca grupos de dígitos separados por una coma
        // \d+ coincide con uno o más números, -? para decir que pueden ser negativos
        Pattern p = Pattern.compile("(-?\\d+),(-?\\d+)");
        Matcher m = p.matcher(mov);

        while (m.find()) {
            // m.group(1) es la X, m.group(2) es la Y
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            movimientos.add(new Posicion(x, y, null));
        }
    }

    public CartaMov(String nombre, String mov, int puntosMin, String img, String estado){
        this.nombre = nombre;
        this.estado = estado;
        this.puntosMin = puntosMin;
        this.img = img;
        movimientos = new ArrayList<>();
        dao = new CartasMovJDBC();
        
        // Esta expresión regular busca grupos de dígitos separados por una coma
        // \d+ coincide con uno o más números, -? para decir que pueden ser negativos
        Pattern p = Pattern.compile("(-?\\d+),(-?\\d+)");
        Matcher m = p.matcher(mov);

        while (m.find()) {
            // m.group(1) es la X, m.group(2) es la Y
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            movimientos.add(new Posicion(x, y, null));
        }
    }

    public String getImg() {
        return img;
    }

    public void marcarEquipo(int eq) {
        estado = "EQ"+eq;
    }

    public boolean perteneceAlEquipo(int eq) {
        String est = "EQ"+eq;
        return estado.equals(est);
    }

    public boolean estaMazo() {
        return estado.equals("MAZO");
    }

    public void marcarMazo() {
        estado = "MAZO";
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getPuntosMin() {
        return puntosMin;
    }

    public void setPuntosMin(int puntosMin) {
        this.puntosMin = puntosMin;
    }

    public boolean registrarCartaMov(){
        try {
            return dao.crearCarta(this);
        } catch (SQLException e) {
            return false;
        }
    }

    public String getNombre(){
        return nombre;
    }

    public List<Posicion> getListaMovimientos(){
        return movimientos;
    }

    public void setListaMovimientos(List<Posicion> movimientos){
        this.movimientos = movimientos;
    }

    public String getMovimientos(){
        if (movimientos == null || movimientos.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < movimientos.size(); i++) {
            Posicion p = movimientos.get(i);
            
            // Construimos el par: (x,y)
            sb.append("(").append(p.getX()).append(",").append(p.getY()).append(")");

            // Añadimos la coma solo si no es el último elemento
            if (i < movimientos.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public void addMovimiento(Posicion pos){
        movimientos.add(pos);
    }

    public void removeMovimiento(Posicion pos){
        movimientos.remove(pos);
    }

    public boolean cambiarImg(){
        try {
            return dao.updateImg(nombre, img); 
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean actualizarBD(){
        try {
            return dao.updateMovimientos(nombre, getMovimientos()) | dao.updatePuntosMin(nombre, puntosMin); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean actualizarDatosPartida(int IDPartida){
        try {
            return dao.updateEstadoEnPartida(IDPartida, nombre, estado);
        } catch (SQLException e) {
            return false;
        }
    }
}