import VO.Autenticacion;
import VO.Autenticacion.ResultadoValidacion;
import VO.Jugador;

/**
 * Clase de prueba para demostrar el uso de la clase Autenticacion.
 * 
 * Este test demuestra:
 * 1. Hashear contraseñas con BCrypt
 * 2. Verificar contraseñas
 * 3. Validar fortaleza de contraseñas
 * 4. Integración con la clase Jugador
 * 
 * IMPORTANTE: Necesitas tener la librería jBCrypt en el classpath para ejecutar esto.
 * Ver backend/DEPENDENCIAS.md para instrucciones de instalación.
 */
public class testAutenticacion {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   TEST DE AUTENTICACIÓN - SISTEMA ONITAMA                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // ========================================
        // TEST 1: HASHEAR CONTRASEÑAS
        // ========================================
        System.out.println("📝 TEST 1: HASHEAR CONTRASEÑAS");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        String password1 = "MiSuperPassword123!";
        String hash1 = Autenticacion.hashearPassword(password1);
        String hash2 = Autenticacion.hashearPassword(password1); // Mismo password
        
        System.out.println("Password original: " + password1);
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("¿Son iguales los hashes? " + hash1.equals(hash2));
        System.out.println("✅ Resultado esperado: false (cada hash tiene salt único)\n");
        
        
        // ========================================
        // TEST 2: VERIFICAR CONTRASEÑAS
        // ========================================
        System.out.println("🔐 TEST 2: VERIFICAR CONTRASEÑAS");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        // Verificar con contraseña correcta
        boolean correcta = Autenticacion.verificarPassword("MiSuperPassword123!", hash1);
        System.out.println("Verificar 'MiSuperPassword123!' contra hash1: " + correcta);
        System.out.println("✅ Resultado esperado: true\n");
        
        // Verificar con contraseña incorrecta
        boolean incorrecta = Autenticacion.verificarPassword("PasswordIncorrecto", hash1);
        System.out.println("Verificar 'PasswordIncorrecto' contra hash1: " + incorrecta);
        System.out.println("✅ Resultado esperado: false\n");
        
        // Verificar que ambos hashes funcionan con la misma contraseña
        boolean hash2Valido = Autenticacion.verificarPassword("MiSuperPassword123!", hash2);
        System.out.println("Verificar 'MiSuperPassword123!' contra hash2: " + hash2Valido);
        System.out.println("✅ Resultado esperado: true\n");
        
        
        
        
        // ========================================
        // TEST 3: VALIDAR FORTALEZA 
        // ========================================
        System.out.println("🔓 TEST 3: VALIDAR FORTALEZA ");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        String[] passwordsBasicas = {
            "abc",           // Muy corta
            "",              // Vacia
            "            ",  // sin numeros ni letras
            "123123133131313", // sin letras
            "abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345abcde12345",
            "abcdefgh",      // Sin números
            "abcdefgh1",     // ✅ VÁLIDA (tiene letra + número)
            "Password1",     // ✅ VÁLIDA
            "mipass123"      // ✅ VÁLIDA
        };
        
        for (String pwd : passwordsBasicas) {
            ResultadoValidacion resultado = Autenticacion.validarFortalezaPassword(pwd);
            System.out.println(String.format("%-20s → %s", "\"" + pwd + "\"", resultado));
        }
        System.out.println();
        
        
        // ========================================
        // TEST 4: INTEGRACIÓN CON JUGADOR
        // ========================================
        System.out.println("👤 TEST 4: INTEGRACIÓN CON CLASE JUGADOR");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        try {
            // Simular registro de usuario
            String correo = "test@onitama.com";
            String nombre = "TestPlayer";
            String passwordOriginal = "MySecurePass123!";
            
            // PASO 1: Validar fortaleza de contraseña
            ResultadoValidacion validacion = Autenticacion.validarFortalezaPassword(passwordOriginal);
            
            if (!validacion.esValida) {
                System.out.println("❌ Contraseña rechazada: " + validacion.mensaje);
                return;
            }
            
            System.out.println("✅ Contraseña válida: " + validacion.mensaje);
            
            // PASO 2: Hashear contraseña
            String passwordHasheado = Autenticacion.hashearPassword(passwordOriginal);
            System.out.println("Password hasheado: " + passwordHasheado);
            
            // PASO 3: Crear jugador con contraseña hasheada
            Jugador jugador = new Jugador(correo, nombre, passwordHasheado, 0, 0, 0, 0);
            System.out.println("✅ Jugador creado: " + jugador.getNombre());
            
            // PASO 4: Simular inicio de sesión
            System.out.println("\n--- Simulando inicio de sesión ---");
            String passwordIntento = "MySecurePass123!"; // El usuario escribe esto
            String hashAlmacenado = jugador.getContrasenya(); // Obtenido de la BD
            
            if (Autenticacion.verificarPassword(passwordIntento, hashAlmacenado)) {
                System.out.println("✅ Login exitoso para: " + jugador.getNombre());
            } else {
                System.out.println("❌ Login fallido - Contraseña incorrecta");
            }
            
            // PASO 5: Intento con contraseña incorrecta
            System.out.println("\n--- Intento con contraseña incorrecta ---");
            String passwordIncorrecto = "WrongPassword123!";
            
            if (Autenticacion.verificarPassword(passwordIncorrecto, hashAlmacenado)) {
                System.out.println("❌ ERROR: Login exitoso con contraseña incorrecta!");
            } else {
                System.out.println("✅ Login correctamente rechazado");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
        
        
    
        
        // ========================================
        // TEST 5: MANEJO DE ERRORES
        // ========================================
        System.out.println("⚠️  TEST 5: MANEJO DE ERRORES");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        try {
            Autenticacion.hashearPassword(null);
            System.out.println("❌ ERROR: No lanzó excepción con password null");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Excepción correcta: " + e.getMessage());
        }
        
        try {
            Autenticacion.hashearPassword("");
            System.out.println("❌ ERROR: No lanzó excepción con password vacío");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Excepción correcta: " + e.getMessage());
        }
        
        try {
            Autenticacion.verificarPassword("test", "hash_invalido");
            System.out.println("❌ ERROR: No lanzó excepción con hash inválido");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Excepción correcta: " + e.getMessage());
        }
        
        System.out.println();
        
        
        // ========================================
        // RESUMEN
        // ========================================
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   ✅ TODOS LOS TESTS COMPLETADOS                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("\n💡 IMPORTANTE:");
        System.out.println("   - NUNCA guarder contraseñas en texto plano en la BD");
        System.out.println("   - SIEMPRE usar Autenticacion.hashearPassword() antes de guardar");
        System.out.println("   - SIEMPRE usar Autenticacion.verificarPassword() para login");
        System.out.println("   - Considera validar fortaleza antes de aceptar passwords");
    }
}
