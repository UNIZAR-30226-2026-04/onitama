import VO.Jugador;
import JDBC.JugadorJDBC;
import java.util.List;
import java.util.ArrayList;

public class test {
    public static void main(String[] args) {
        System.out.println("=== INICIANDO TESTS DE JUGADOR ===\n");

        // 1. Crear instancias de prueba
        Jugador j1 = new Jugador("pepe@gmail.com", "Pepe", "pass123", 100);
        Jugador j2 = new Jugador("ana@gmail.com", "Ana", "abc456", 200);

        // 2. Test de Registro
        System.out.print("Test Registro J1: ");
        if (j1.registrarse()) {
            System.out.println("OK");
        } else {
            System.out.println("FALLO (Quizás ya existe en la BD)");
        }

        System.out.print("Test Registro J2: ");
        j2.registrarse(); // Registramos al segundo para poder hacerlo amigo
        System.out.println("Hecho");

        // 3. Test de Amigos (Añadir)
        System.out.print("Test Añadir Amigo: ");
        if (j1.anyadirAmigo(j2)) {
            System.out.println("OK");
            System.out.println("   Lista local de " + j1.getNombre() + ": " + j1.getAmigos().size() + " amigo(s)");
        } else {
            System.out.println("FALLO");
        }

        // 4. Test de Actualización en BD
        System.out.print("Test Actualizar Puntos: ");
        j1.setPuntos(500);
        if (j1.actualizarBD()) {
            System.out.println("OK (Nuevos puntos: 500)");
        } else {
            System.out.println("FALLO");
        }

        // 5. Test de Carga desde BD
        System.out.println("\nTest Cargar Amigos desde BD:");
        Jugador j1_recargado = new Jugador("pepe@gmail.com", "Pepe", "pass123", 500);
        j1_recargado.cargarAmigos();
        List<Jugador> listaAmigos = j1_recargado.getAmigos();
        
        if (!listaAmigos.isEmpty()) {
            System.out.println("   Amigos encontrados: " + listaAmigos.size());
            for (Jugador a : listaAmigos) {
                System.out.println("   - Amigo: " + a.getNombre());
            }
        } else {
            System.out.println("No se encontraron amigos en la BD.");
        }

        // 6. Test de Borrado
        System.out.print("\nTest Borrar Amigo: ");
        if (j1.borrarAmigo(j2)) {
            System.out.println("OK");
        } else {
            System.out.println("FALLO");
        }

        System.out.println("\n=== TESTS FINALIZADOS ===");
    }
}