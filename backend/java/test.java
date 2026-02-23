import VO.Jugador;
import VO.Skin;
import VO.CartaMov;
import VO.Posicion;
import java.util.List;

public class test {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO TESTS INTEGRALES (JUGADOR, SKINS, CARTAS) ===\n");

        // ---------------------------------------------------------
        // 1. TESTS DE JUGADOR Y SKINS
        // ---------------------------------------------------------
        Jugador j1 = new Jugador("test@mail.com", "Ciro", "pass123", 1000);
        Skin skinEspecial = new Skin("Skin Legendaria", "#FFF", "#00F", "#F00", 500);

        System.out.print("Test Registro Jugador: ");
        System.out.println(j1.registrarse() ? "OK ✅" : "FALLO ❌");

        System.out.print("Test Comprar Skin: ");
        System.out.println(j1.comprarSkin(skinEspecial) ? "OK ✅" : "FALLO ❌");

        // ---------------------------------------------------------
        // 2. TESTS DE CARTAMOV (SERIALIZACIÓN Y REGEX)
        // ---------------------------------------------------------
        System.out.println("\n--- Test de Cartas de Movimiento ---");
        
        // Creamos una carta con un String de movimientos
        String movRaw = "(1,0),(0,-1),(-1,2)";
        CartaMov cartaTest = new CartaMov("Caballo", movRaw);

        // Verificamos que el Regex funcionó (Parsing)
        System.out.print("Test Parsing Regex (de String a Lista): ");
        if (cartaTest.getListaMovimientos().size() == 3) {
            System.out.println("OK ✅ (Se detectaron 3 movimientos)");
        } else {
            System.out.println("FALLO ❌ (Se detectaron " + cartaTest.getListaMovimientos().size() + ")");
        }

        // Verificamos el proceso inverso (Serialización a String)
        System.out.print("Test Serialización (de Lista a String): ");
        String resultadoString = cartaTest.getMovimientos();
        if (resultadoString.equals(movRaw)) {
            System.out.println("OK ✅ -> " + resultadoString);
        } else {
            System.out.println("FALLO ❌ -> Esperado: " + movRaw + " | Obtenido: " + resultadoString);
        }

        // ---------------------------------------------------------
        // 3. TESTS DE BASE DE DATOS PARA CARTAMOV
        // ---------------------------------------------------------
        System.out.print("Test Registro Carta en BD: ");
        // Nota: Asegúrate de que jdbc esté inicializado en el constructor de CartaMov
        if (cartaTest.registrarCartaMov()) {
            System.out.println("OK ✅");
        } else {
            System.out.println("FALLO ❌ (Revisa conexión o si la carta ya existe)");
        }

        System.out.print("Test Actualizar Movimientos en BD: ");
        cartaTest.addMovimiento(new Posicion(2, 2, null)); // Añadimos uno nuevo
        if (cartaTest.actualizarBD()) {
            System.out.println("OK ✅ (Ahora con 4 movimientos)");
        } else {
            System.out.println("FALLO ❌");
        }

        System.out.println("\n=== TESTS FINALIZADOS ===");
    }
}