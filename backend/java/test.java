import VO.*;
import java.util.List;

public class test {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO TEST INTEGRAL DE PARTIDA Y DATOS ===\n");

        // ---------------------------------------------------------
        // 1. CREACIÓN DE DATOS BASE (10 MOVIMIENTOS Y 8 ACCIONES)
        // ---------------------------------------------------------
        System.out.println("--- Paso 1: Poblando Base de Datos ---");
        
        // Crear 10 Cartas de Movimiento
        String[] nombresM = {"Tigre", "Cangrejo", "Mono", "Grulla", "Dragon", 
                             "Elefante", "Mantis", "Jabali", "Buey", "Cobra"};
        for (String n : nombresM) {
            CartaMov m = new CartaMov(n, "(1,1),(-1,-1)");
            m.registrarCartaMov(); // El método devuelve false si ya existe, no rompe el test
        }
        System.out.println("   [OK] 10 Cartas de Movimiento procesadas.");

        // Crear 8 Cartas de Acción (5 con 0 puntos, 3 con 1000)
        String[] nombresA = {"Salto", "Escudo", "Giro", "Refuerzo", "Carga", 
                             "Teletransporte", "Furia", "Sacrificio"};
        for (int i = 0; i < nombresA.length; i++) {
            int pts = (i < 5) ? 0 : 1000; // Las primeras 5 tienen 0 puntos
            CartaAccion a = new CartaAccion(nombresA[i], "ACCION_TIPO_" + i, pts);
            a.registrarCartaAccion();
        }
        System.out.println("   [OK] 8 Cartas de Acción procesadas (5 con 0 pts).");


        // ---------------------------------------------------------
        // 2. CREACIÓN DE JUGADORES
        // ---------------------------------------------------------
        System.out.println("\n--- Paso 2: Registrando Jugadores ---");
        Jugador j1 = new Jugador("admin@test.com", "Ciro", "pass123", 2000);
        Jugador j2 = new Jugador("rival@test.com", "Rival", "pass456", 1500);
        
        j1.registrarse();
        j2.registrarse();
        System.out.println("   Jugadores listos.");


        // ---------------------------------------------------------
        // 3. PRUEBA DE LA CLASE PARTIDA
        // ---------------------------------------------------------
        System.out.println("\n--- Paso 3: Inicializando Partida (ID: 999) ---");
        // El constructor de Partida ejecutará automáticamente:
        // - asignar8CartasPartida(999)
        // - asignar4CartasPartida(999, 1500)
        Partida partida = new Partida(999, j1, j2);


        // ---------------------------------------------------------
        // 4. VERIFICACIÓN DE RESULTADOS
        // ---------------------------------------------------------
        System.out.println("\n--- RESULTADOS DEL TEST ---");
        
        // Comprobar Movimientos (Debe haber 8)
        List<CartaMov> movsAsignados = partida.getCartasMovimiento();
        System.out.print("Cartas de Movimiento (Esperadas 8): ");
        if (movsAsignados != null && movsAsignados.size() == 8) {
            System.out.println(movsAsignados.size() + " ✅");
        } else {
            int num = (movsAsignados == null) ? 0 : movsAsignados.size();
            System.out.println(num + " ❌ (Revisa si asignar8CartasPartida funciona)");
        }

        // Comprobar Acciones (Debe haber 4)
        List<CartaAccion> accsAsignadas = partida.getCartasAccion();
        System.out.print("Cartas de Acción (Esperadas 4): ");
        if (accsAsignadas != null && accsAsignadas.size() == 4) {
            System.out.println(accsAsignadas.size() + " ✅");
            // Mostrar cuáles tocaron
            for(CartaAccion ca : accsAsignadas) {
                System.out.println("   -> " + ca.getNombre() + " (Req: " + ca.getPuntosMin() + " pts)");
            }
        } else {
            int num = (accsAsignadas == null) ? 0 : accsAsignadas.size();
            System.out.println(num + " ❌ (Revisa puntosMin o asignar4CartasPartida)");
        }

        System.out.println("\n=== TEST FINALIZADO ===");
    }
}