package VO;

import JDBC.JugadorJDBC;
import JDBC.SkinJDBC;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

//Faltan las skins
public class Jugador {
    private String nombre, password, correo;
    private int puntos, cores, partidasGanadas, partidasJugadas;
    private List<Jugador> amigos;
    private List<Jugador> solicitudesPendientes;
    private List<Skin> misSkines;
    private JugadorJDBC jdbc;
    private SkinJDBC jdbcSkin;
    
    //Constructor necesario para la BD (usado por JugadorJDBC.montarJugador)
    //IMPORTANTE: Este constructor espera que la contraseña YA esté hasheada
    public Jugador(String correo, String nombre, String passwordHash, int puntos, int cores, int partidasGanadas, int partidasJugadas){
        this.correo = correo;
        this.nombre = nombre;
        this.password = passwordHash; // Ya está hasheada con BCrypt
        this.puntos = puntos;
        this.cores = cores;
        this.partidasGanadas = partidasGanadas;
        this.partidasJugadas = partidasJugadas;
        amigos = new ArrayList<>();
        solicitudesPendientes = new ArrayList<>();
        misSkines = new ArrayList<>();
        jdbc = new JugadorJDBC();
        jdbcSkin = new SkinJDBC();
    }
    
    //Constructor simplificado para registro (valores por defecto)
    //IMPORTANTE: Este constructor espera la contraseña en texto plano y la hashea automáticamente
    public Jugador(String correo, String nombre, String passwordTextoPlano){
        this(correo, nombre, Autenticacion.hashearPassword(passwordTextoPlano), 0, 0, 0, 0);
    }

    public boolean registrarse(){
        try {
            return jdbc.registrarse(this);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Verifica si una contraseña en texto plano coincide con la contraseña almacenada.
     * Utiliza BCrypt para verificación segura.
     */
    public boolean verificarPassword(String passwordTextoPlano){
        try {
            return Autenticacion.verificarPassword(passwordTextoPlano, this.password);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Método estático para iniciar sesión.
     * Busca al jugador en la BD y verifica su contraseña.
     */
    public static Jugador iniciarSesion(String nombreUsuario, String passwordTextoPlano){
        try {
            JugadorJDBC jdbc = new JugadorJDBC();
            Jugador jugador = jdbc.buscarJugador(nombreUsuario);
            
            if (jugador == null) {
                return null; // Usuario no existe
            }
            
            // Verificar contraseña
            if (jugador.verificarPassword(passwordTextoPlano)) {
                return jugador; // Login exitoso
            } else {
                return null; // Contraseña incorrecta
            }
            
        } catch (SQLException e) {
            return null;
        }
    }

    public void setNombre(String nombre){
        this.nombre = nombre;
    }

    public void setCorreo(String correo){
        this.correo = correo;
    }

    public void setContrasenya(String passwordTextoPlano){
        // Hashear automáticamente la nueva contraseña
        this.password = Autenticacion.hashearPassword(passwordTextoPlano);
    }

    public void setPuntos(int puntos){
        this.puntos = puntos;
    }

    public void setCores(int cores){
        this.cores = cores;
    }

    public void setPartidasGanadas(int partidasGanadas){
        this.partidasGanadas = partidasGanadas;
    }

    public void setPartidasJugadas(int partidasJugadas){
        this.partidasJugadas = partidasJugadas;
    }

    public String getCorreo(){
        return correo;
    }
     
    public String getNombre(){
        return nombre;
    }
    
    public String getContrasenya(){
        return password;
    }
    
    public int getPuntos(){
        return puntos;
    }

    public int getCores(){
        return cores;
    }

    public int getPartidasGanadas(){
        return partidasGanadas;
    }

    public int getPartidasJugadas(){
        return partidasJugadas;
    }

    public boolean actualizarBD(){
        try {
            return jdbc.updateContrasenya(nombre, password) | jdbc.updateCorreo(nombre, correo) | jdbc.updatePuntos(nombre, puntos) | jdbc.updateCores(nombre, cores) | jdbc.updatePartidasGanadas(nombre, partidasGanadas) | jdbc.updatePartidasJugadas(nombre, partidasJugadas); //| para que se ejecuten todos
        } catch (SQLException e) {
            return false;
        }
    }

    public void cargarAmigos(){
        try {
            amigos = jdbc.sacarAmigos(nombre);
        } catch (SQLException e) {
        }
    }

    public void cargarSolicitudesPendientes(){
        try {
            solicitudesPendientes = jdbc.sacarSolicitudesPendientes(nombre);
        } catch (SQLException e) {
        }
    }

    public List<Jugador> getAmigos(){
        return amigos;
    }

    public List<Jugador> getSolicitudesPendientes(){
        return solicitudesPendientes;
    }

    public boolean solicitarAmistad(String destinatario){
        try{
            return jdbc.enviarSolicitud(nombre, destinatario);
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean anyadirAmigo(Jugador amigo){
        try {
            if(jdbc.anyadirAmigo(amigo.getNombre(), nombre)) {
                amigos.add(amigo); 
                solicitudesPendientes.remove(amigo);
                return true;
            }
            return false;    

        } catch (SQLException e) {
            return false;
        }
    }

    public boolean rechazarSolicitud(Jugador amigo){
        try {
            if (jdbc.rechazarSolicitud(amigo.getNombre(), nombre)) {
                solicitudesPendientes.remove(amigo); 
                return true;
            }
            return false;    

        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean borrarAmigo(Jugador amigo){
        try {
            if(jdbc.borrarAmigo(nombre, amigo.getNombre())) {
                amigos.remove(amigo); 
                return true;
            }
            return false;    

        } catch (SQLException e) {
            return false;
        }
    }

    public void cargarSkins(){
        try {
            misSkines = jdbcSkin.sacarSkinJugador(nombre);
        } catch (SQLException e) {
        }
    }

    public List<Skin> getSkins(){
        return misSkines;
    }

    public boolean comprarSkin(Skin nueva){
        // Verificar si el jugador tiene suficientes cores
        if (this.cores < nueva.getPrecio()) {
            return false; // No tiene suficientes cores
        }
        
        // Restar el precio de los cores
        this.cores -= nueva.getPrecio();
        
        misSkines.add(nueva); //Añadimos en la lista para evitar tener que estar cargando de la BD
        try {
            // Actualizar cores en la BD
            boolean coresActualizados = jdbc.updateCores(nombre, cores);
            // Registrar la compra de la skin
            boolean skinComprada = jdbcSkin.comprarSkin(nueva.getNombre(), nombre);
            
            return coresActualizados && skinComprada;
        } catch (SQLException e) {
            // Si falla, revertir el descuento de cores en memoria
            this.cores += nueva.getPrecio();
            misSkines.remove(nueva);
            return false;
        }
    }
    
    // Método para incrementar partidas jugadas
    public void incrementarPartidasJugadas(){
        this.partidasJugadas++;
    }
    
    // Método para registrar victoria (incrementa partidas ganadas y jugadas)
    public void registrarVictoria(int coresGanados, int puntosGanados){
        this.partidasGanadas++;
        this.partidasJugadas++;
        this.cores += coresGanados;
        this.puntos += puntosGanados;
    }
    
}