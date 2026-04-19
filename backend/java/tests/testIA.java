import VO.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test simplificado de JugadorIA usando JUnit 5.
 * Verifica las funcionalidades generales de la clase: creacion, evaluacion y calculo de jugadas.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class testIA {

    private static Partida partida;

    // Setup: crea una partida con tablero y cartas (sin necesidad de BD)
    @BeforeAll
    static void setUp() throws Exception {
        System.out.println("=== SETUP ===");
        partida = new Partida(0, "Jugandose", 0, "Publica", "", "", 0, 0, "d1", "d2", false, false);

        // Cartas de movimiento con movimientos reales de Onitama
        List<CartaMov> cartasM = new ArrayList<>();
        cartasM.add(new CartaMov("Tigre",    "(0,-2),(0,1)",                0, "EQ1"));
        cartasM.add(new CartaMov("Cangrejo", "(-2,0),(2,0),(0,-1)",        0, "EQ1"));
        cartasM.add(new CartaMov("Mono",     "(-1,-1),(1,-1),(-1,1),(1,1)",0, "EQ2"));
        cartasM.add(new CartaMov("Grulla",   "(0,-1),(-1,1),(1,1)",        0, "EQ2"));
        cartasM.add(new CartaMov("Dragon",   "(-2,-1),(2,-1),(-1,1),(1,1)",0, "MAZO"));

        // Cartas de accion
        List<CartaAccion> cartasA = new ArrayList<>();
        cartasA.add(new CartaAccion("Espejo",     "ESPEJO",     0, "USABLE", 1));
        cartasA.add(new CartaAccion("Sacrificio", "SACRIFICIO", 0, "USABLE", 2));

        // Inyectar cartas en la partida con reflexion
        Field fm = Partida.class.getDeclaredField("cartasM"); fm.setAccessible(true); fm.set(partida, cartasM);
        Field fa = Partida.class.getDeclaredField("cartasA"); fa.setAccessible(true); fa.set(partida, cartasA);
        System.out.println("   Partida lista (7x7, 5 cartas mov, 2 cartas accion)\n");
    }

    @Test @Order(1)
    @DisplayName("Crear JugadorIA con distintos niveles")
    void testCreacion() {
        JugadorIA ia = new JugadorIA(JugadorIA.NivelDificultad.FACIL, 1);
        assertEquals(JugadorIA.NivelDificultad.FACIL, ia.getNivel());
        assertEquals(1, ia.getEquipo());
        assertEquals("minimax_alfabeta", ia.getAlgoritmo());

        ia.setNivel(JugadorIA.NivelDificultad.DIFICIL);
        assertEquals(JugadorIA.NivelDificultad.DIFICIL, ia.getNivel());
        System.out.println("[1] Creacion y configuracion OK ✅");
    }

    @Test @Order(2)
    @DisplayName("evaluarTablero() devuelve valores coherentes")
    void testEvaluar() {
        int evalEq1 = new JugadorIA(JugadorIA.NivelDificultad.FACIL, 1).evaluarTablero(partida);
        int evalEq2 = new JugadorIA(JugadorIA.NivelDificultad.FACIL, 2).evaluarTablero(partida);
        assertNotEquals(0, evalEq1);
        assertNotEquals(0, evalEq2);
        System.out.println("[2] Evaluacion EQ1=" + evalEq1 + " EQ2=" + evalEq2 + " ✅");
    }

    @Test @Order(3)
    @DisplayName("calcularMejorMovimiento() devuelve jugada valida")
    void testMejorMovimiento() {
        JugadorIA ia = new JugadorIA(JugadorIA.NivelDificultad.FACIL, 1);
        JugadorIA.Jugada j = ia.calcularMejorMovimiento(partida);

        assertNotNull(j, "Debe devolver una jugada");
        if (!j.esAccion) {
            assertTrue(j.origenX >= 0 && j.origenX < 7 && j.destinoX >= 0 && j.destinoX < 7);
            assertNotNull(j.cartaMovimiento);
        }
        System.out.println("[3] Mejor jugada EQ1: " + j + " ✅");
    }

    @Test @Order(4)
    @DisplayName("seleccionarCarta() devuelve carta del equipo correcto")
    void testSeleccionarCarta() {
        JugadorIA ia = new JugadorIA(JugadorIA.NivelDificultad.FACIL, 1);
        CartaMov carta = ia.seleccionarCarta(partida);
        if (carta != null) {
            assertEquals("EQ1", carta.getEstado(), "La carta debe ser del EQ1");
        }
        System.out.println("[4] seleccionarCarta(): " + (carta != null ? carta.getNombre() : "accion") + " ✅");
    }

    @Test @Order(5)
    @DisplayName("La IA no modifica el tablero real")
    void testNoModificaEstado() {
        Tablero tab = partida.getTablero();
        int fichasAntes = contarFichas(tab);
        int numCartas = partida.getCartasMovimiento().size();

        new JugadorIA(JugadorIA.NivelDificultad.MEDIO, 1).calcularMejorMovimiento(partida);

        assertEquals(fichasAntes, contarFichas(tab), "Fichas no deben cambiar");
        assertEquals(numCartas, partida.getCartasMovimiento().size(), "Cartas no deben cambiar");
        System.out.println("[5] Estado no modificado ✅");
    }

    private int contarFichas(Tablero tab) {
        int n = 0;
        for (int y = 0; y < tab.getDIM(); y++)
            for (int x = 0; x < tab.getDIM(); x++)
                if (tab.getPosicion(x, y).getFicha() != null) n++;
        return n;
    }

    // Main para ejecutar sin JUnit runner
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   TEST DE JugadorIA (Onitama)         ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        testIA t = new testIA();
        int ok = 0, fail = 0;
        try { setUp(); } catch (Exception e) { System.err.println("SETUP FALLO: " + e.getMessage()); return; }

        Runnable[] tests = { t::testCreacion, t::testEvaluar, t::testMejorMovimiento, t::testSeleccionarCarta, t::testNoModificaEstado };
        String[] nombres = { "Creacion", "Evaluacion", "MejorMovimiento", "SeleccionarCarta", "NoModificaEstado" };

        for (int i = 0; i < tests.length; i++) {
            try { tests[i].run(); ok++; }
            catch (AssertionError e) { System.err.println("✗ " + nombres[i] + ": " + e.getMessage()); fail++; }
            catch (Exception e) { System.err.println("✗ " + nombres[i] + ": " + e.getMessage()); fail++; }
        }

        System.out.println("\n══ Resultado: " + ok + "/" + (ok+fail) + " tests pasados " + (fail == 0 ? "🎉" : "⚠️") + " ══");
    }
}
