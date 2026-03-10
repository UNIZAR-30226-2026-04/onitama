package VO;

import org.mindrot.jbcrypt.BCrypt;
import java.util.regex.Pattern;



public class Autenticacion {
    
    //Cost factor para BCrypt (número de rondas de hashing).
    private static final int BCRYPT_COST_FACTOR = 12;
    
    //Longitud mínima de contraseña.
    private static final int PASSWORD_MIN_LENGTH = 8;
    
    //Longitud máxima de contraseña (BCrypt tiene límite de 72 bytes).
    private static final int PASSWORD_MAX_LENGTH = 72;
    
    
    
    /*
    * Hashea una contraseña usando BCrypt con el cost factor configurado.
    
    * IMPORTANTE: Cada vez que llamas a este método con la misma contraseña,
    * obtienes un hash DIFERENTE (debido al salt aleatorio).
     */
    public static String hashearPassword(String password) {
        // Validaciones
        if (password == null) {
            throw new IllegalArgumentException("La contraseña no puede ser null");
        }
        
        if (password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        
        if (password.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "La contraseña no puede exceder " + PASSWORD_MAX_LENGTH + " caracteres"
            );
        }
        
        // Generar hash con BCrypt
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST_FACTOR));
    }
    
    
    //Verifica si una contraseña en texto plano coincide con un hash BCrypt
    public static boolean verificarPassword(String passwordTextoPlano, String hashAlmacenado) {
        // Validaciones
        if (passwordTextoPlano == null || passwordTextoPlano.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser null o vacía");
        }
        
        if (hashAlmacenado == null || hashAlmacenado.isEmpty()) {
            throw new IllegalArgumentException("El hash no puede ser null o vacío");
        }
        
        // Verificar si el hash tiene el formato correcto de BCrypt
        if (!hashAlmacenado.startsWith("$2a$") && 
            !hashAlmacenado.startsWith("$2b$") && 
            !hashAlmacenado.startsWith("$2y$")) {
            throw new IllegalArgumentException("El hash no tiene formato válido de BCrypt");
        }
        
        try {
            // Verificar con BCrypt 
            return BCrypt.checkpw(passwordTextoPlano, hashAlmacenado);
        } catch (Exception e) {
            // Si hay algún error en la verificación, asumimos que no coincide
            return false;
        }
    }
    

    /**
     * Valida que una contraseña cumpla con requisitos .
     * 
     * Requisitos básicos:
     * - Longitud mínima: 8 caracteres
     * - Al menos una letra
     * - Al menos un número
     */
    public static ResultadoValidacion validarFortalezaPassword(String password) {
        // Verificar null o vacío
        if (password == null || password.isEmpty()) {
            return new ResultadoValidacion(false, "La contraseña no puede estar vacía");
        }
        
        // Verificar longitud mínima
        if (password.length() < PASSWORD_MIN_LENGTH) {
            return new ResultadoValidacion(
                false, 
                "La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres"
            );
        }
        
        // Verificar longitud máxima
        if (password.length() > PASSWORD_MAX_LENGTH) {
            return new ResultadoValidacion(
                false, 
                "La contraseña no puede exceder " + PASSWORD_MAX_LENGTH + " caracteres"
            );
        }
        
        // Verificar que contenga al menos una letra (mayúscula o minúscula)
        if (!Pattern.compile("[a-zA-Z]").matcher(password).find()) {
            return new ResultadoValidacion(
                false, 
                "La contraseña debe contener al menos una letra"
            );
        }
        
        // Verificar que contenga al menos un número
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            return new ResultadoValidacion(
                false, 
                "La contraseña debe contener al menos un número"
            );
        }
        
        // Si pasa todas las validaciones
        return new ResultadoValidacion(true, "Contraseña válida");
    }
    
    // Clase que encapsula el resultado de una validación de contraseña.
    public static class ResultadoValidacion {
        public final boolean esValida;
        public final String mensaje;
        
        public ResultadoValidacion(boolean esValida, String mensaje) {
            this.esValida = esValida;
            this.mensaje = mensaje;
        }
        
        @Override
        public String toString() {
            return esValida ? "OK: " + mensaje : "ERROR: " + mensaje;
        }
    }
}
