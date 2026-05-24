package backend.VO;

import java.util.ArrayList;
import java.util.List;

import backend.gestor.GestorJugador;
import backend.gestor.GestorSkin;

//Faltan las skins
public class Jugador {
    private String nombre, password, correo;
    private int puntos, cores, partidasGanadas, partidasJugadas;
    // NUEVOS ATRIBUTOS: AVATAR Y SKIN ACTUAL
    private String avatarId; 
    private String skinActiva;

    private List<Jugador> amigos;
    private List<Skin> misSkines;
    private GestorJugador gestorJugador;
    private GestorSkin gestorSkin;
    
    //Constructor necesario para la BD (usado por JugadorJDBC.montarJugador)
    //IMPORTANTE: Este constructor espera que la contraseña YA esté hasheada
    public Jugador(String correo, String nombre, String passwordHash, int puntos, int cores, int partidasGanadas, int partidasJugadas, String avatarId, String skinActiva){
        this.correo = correo;
        this.nombre = nombre;
        this.password = passwordHash; // Ya está hasheada con BCrypt
        this.puntos = puntos;
        this.cores = cores;
        this.partidasGanadas = partidasGanadas;
        this.partidasJugadas = partidasJugadas;
        // NUEVO: ASIGNAMOS AVATAR Y SKIN
        this.avatarId = avatarId;      
        this.skinActiva = skinActiva;

        amigos = new ArrayList<>();
        misSkines = new ArrayList<>();
        gestorJugador = new GestorJugador();
        gestorSkin = new GestorSkin();
    }
    
    //Constructor simplificado para registro (valores por defecto)
    //IMPORTANTE: Este constructor espera la contraseña en texto plano y la hashea automáticamente
    // añadidos avatarId y skin0 por defecto
    public Jugador(String correo, String nombre, String passwordTextoPlano, String avatarId){
        this(correo, nombre, Autenticacion.hashearPassword(passwordTextoPlano), 0, 0, 0, 0, avatarId, "Skin0");
    }

    public boolean registrarse(){
        try {
            return gestorJugador.registrarse(this);
        // he cambiado que tire Exception en lugar de SQLEx. pq debería haber abstracción e igual
        // que hemos quitado interacciones con la bbdd usando jdbc estos errores no deberían ser SQL
        } catch (Exception e) {
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
            GestorJugador gestorJugador = new GestorJugador();
            Jugador jugador = gestorJugador.buscarJugador(nombreUsuario);
            
            if (jugador == null) {
                return null; // Usuario no existe
            }
            
            // Verificar contraseña
            if (jugador.verificarPassword(passwordTextoPlano)) {
                return jugador; // Login exitoso
            } else {
                return null; // Contraseña incorrecta
            }
            
        } catch (Exception e) {
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
            return gestorJugador.updateContrasenya(nombre, password) | gestorJugador.updateCorreo(nombre, correo) | gestorJugador.updatePuntos(nombre, puntos) | gestorJugador.updateCores(nombre, cores) | gestorJugador.updatePartidasGanadas(nombre, partidasGanadas) | gestorJugador.updatePartidasJugadas(nombre, partidasJugadas); //| para que se ejecuten todos
        } catch (Exception e) {
            return false;
        }
    }

    public void cargarAmigos(){
        try {
            amigos = gestorJugador.sacarAmigos(nombre);
        } catch (Exception e) {
        }
    }

    public List<Jugador> getAmigos(){
        return amigos;
    }
    
    public boolean borrarAmigo(Jugador amigo){
        try {
            if(gestorJugador.borrarAmigo(nombre, amigo.getNombre())) {
                amigos.remove(amigo); 
                return true;
            }
            return false;    

        } catch (Exception e) {
            return false;
        }
    }

    public void cargarSkins(){
        try {
            misSkines = gestorSkin.sacarSkinJugador(nombre);
        } catch (Exception e) {
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
            boolean coresActualizados = gestorJugador.updateCores(nombre, cores);
            // Registrar la compra de la skin
            String skinComprada = gestorSkin.comprarSkin(nueva.getNombre(), nombre);
            // comprarSkin ahora devuelve String, comprobamos si vale "OK"
            return coresActualizados && "OK".equals(skinComprada);
        } catch (Exception e) {
            // Si falla, revertir el descuento de cores en memoria
            this.cores += nueva.getPrecio();
            misSkines.remove(nueva);
            return false;
        }
    }
    
    // Método para registrar partida (modifica partidas ganadas, jugadas, puntos y cores)
    public void registrarPartida(int coresGanados, int puntosGanados, boolean victoria){
        if (victoria) {
            this.partidasGanadas++;
        }
        this.partidasJugadas++;
        if (puntosGanados < 0) {
            this.puntos = Math.max(0, this.puntos + puntosGanados); // Evitamos puntos negativos
        } else {
            this.puntos += puntosGanados;
        }
        this.cores += coresGanados;
        actualizarBD();
    }

    // GETTERS Y SETTERS NUEVOS

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public String getSkinActiva() {
        return skinActiva;
    }

    public void setSkinActiva(String skinActiva) {
        this.skinActiva = skinActiva;
    }
    
}