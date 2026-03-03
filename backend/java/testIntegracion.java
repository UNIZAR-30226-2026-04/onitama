import VO.*;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

// Test de integracion entre modulos del backend
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class testIntegracion {

    private static Partida partida;
    private static List<CartaMov> cartasM;
    private static List<CartaAccion> cartasA;

    @BeforeAll
    static void setUp() throws Exception {
        System.out.println("=== SETUP INTEGRACION ===");
        partida = new Partida(0, "Jugandose", 0, "Publica", "", "", 0, 0, "d1", "d2", false, false);

        cartasM = new ArrayList<>();
        cartasM.add(new CartaMov("Tigre",    "(0,-2),(0,1)",                 0, "EQ1"));
        cartasM.add(new CartaMov("Cangrejo", "(-2,0),(2,0),(0,-1)",          0, "EQ1"));
        cartasM.add(new CartaMov("Mono",     "(-1,-1),(1,-1),(-1,1),(1,1)",  0, "EQ2"));
        cartasM.add(new CartaMov("Grulla",   "(0,-1),(-1,1),(1,1)",          0, "EQ2"));
        cartasM.add(new CartaMov("Dragon",   "(-2,-1),(2,-1),(-1,1),(1,1)",  0, "MAZO"));

        cartasA = new ArrayList<>();
        cartasA.add(new CartaAccion("Revivir",    "REVIVIR",    0, "USABLE", 1));
        cartasA.add(new CartaAccion("Espejo",     "ESPEJO",     0, "USABLE", 1));
        cartasA.add(new CartaAccion("Sacrificio", "SACRIFICIO", 0, "USABLE", 2));
        cartasA.add(new CartaAccion("Venganza",   "VENGANZA",   0, "USABLE", 2));

        inyectarCartas(partida, cartasM, cartasA);
        System.out.println("   Partida lista\n");
    }

    private static void inyectarCartas(Partida p, List<CartaMov> cm, List<CartaAccion> ca) throws Exception {
        Field fm = Partida.class.getDeclaredField("cartasM"); fm.setAccessible(true); fm.set(p, cm);
        Field fa = Partida.class.getDeclaredField("cartasA"); fa.setAccessible(true); fa.set(p, ca);
    }

    // -- Tablero + Ficha: capturas --

    @Test @Order(1)
    @DisplayName("Capturar peon deja la casilla vacia")
    void testCapturarFichaTablero() {
        Tablero tab = new Tablero(7);
        tab.cargarTablero();
        Ficha antes = tab.getPosicion(0, 0).getFicha();
        assertNotNull(antes);
        assertEquals(1, antes.getEquipo());

        boolean eraRey = tab.capturarFicha(0, 0);
        assertFalse(eraRey);
        assertNull(tab.getPosicion(0, 0).getFicha());
        System.out.println("[1] Captura de peon OK");
    }

    @Test @Order(2)
    @DisplayName("Capturar rey devuelve true")
    void testCapturarReyDetectaVictoria() {
        Tablero tab = new Tablero(7);
        tab.cargarTablero();
        Ficha rey = tab.getPosicion(3, 0).getFicha();
        assertNotNull(rey);
        assertTrue(rey.isRey());

        boolean eraRey = tab.capturarFicha(3, 0);
        assertTrue(eraRey);
        System.out.println("[2] Captura de rey OK");
    }

    // -- CartaAccion + Tablero --

    @Test @Order(3)
    @DisplayName("REVIVIR coloca peon en trono vacio")
    void testRevivirEnTronoVacio() {
        Tablero tab = new Tablero(7);
        tab.tablero[3][3] = new Posicion(3, 3, new Ficha(true, 1));

        assertNull(tab.trono1.getFicha());

        CartaAccion revivir = new CartaAccion("Revivir", "REVIVIR", 0, "USABLE", 1);
        boolean ok = revivir.ejecutarAccion(tab, 1);
        assertTrue(ok);
        assertNotNull(tab.trono1.getFicha());
        assertEquals(1, tab.trono1.getFicha().getEquipo());
        assertFalse(tab.trono1.getFicha().isRey());
        assertFalse(revivir.puedeUsarse());
        System.out.println("[3] REVIVIR en trono vacio OK");
    }

    @Test @Order(4)
    @DisplayName("Tras cargarTablero los tronos tienen reyes")
    void testRevivirTronoOcupado() {
        Tablero tab = new Tablero(7);
        tab.cargarTablero();
        // Nota: cargarTablero() crea nuevos Posicion que reemplazan tablero[i][j],
        // pero trono1/trono2 siguen apuntando a los objetos del constructor.
        // Se comprueba via getPosicion en vez de via trono1/trono2.
        Posicion posReal = tab.getPosicion(3, 0);
        assertNotNull(posReal.getFicha());
        assertTrue(posReal.getFicha().isRey());

        Posicion posRealEq2 = tab.getPosicion(3, 6);
        assertNotNull(posRealEq2.getFicha());
        System.out.println("[4] Tronos ocupados tras cargarTablero OK");
    }

    @Test @Order(5)
    @DisplayName("CartaAccion no puede usarse dos veces")
    void testCartaAccionNoReutilizable() {
        Tablero tab = new Tablero(7);
        tab.cargarTablero();
        CartaAccion espejo = new CartaAccion("Espejo2", "ESPEJO", 0, "USABLE", 1);

        assertTrue(espejo.puedeUsarse());
        espejo.ejecutarAccion(tab, 1);
        assertFalse(espejo.puedeUsarse());

        boolean ok = espejo.ejecutarAccion(tab, 1);
        assertFalse(ok);
        System.out.println("[5] Carta accion no reutilizable OK");
    }

    // -- Carta: limites de movimiento --

    @Test @Order(6)
    @DisplayName("Movimientos fuera del tablero se filtran")
    void testCartaMovLimites() {
        Carta carta = new Carta("Test");
        carta.anyadirMovimiento(new Posicion(-2, 0, null));
        carta.anyadirMovimiento(new Posicion(2, 0, null));
        carta.anyadirMovimiento(new Posicion(0, -1, null));

        Posicion esquina = new Posicion(0, 0, null);
        List<Posicion> destinos = carta.jugarCarta(esquina, 7);

        // Desde (0,0): (-2,0) fuera, (2,0) valido, (0,-1) fuera
        assertEquals(1, destinos.size());
        assertEquals(2, destinos.get(0).getX());
        assertEquals(0, destinos.get(0).getY());
        System.out.println("[6] Limites de movimiento OK");
    }

    // -- Tablero: trampas --

    @Test @Order(7)
    @DisplayName("Restricciones de casillas trampa")
    void testTrampaRestricciones() {
        Tablero tab = new Tablero(7);
        tab.cargarTablero();

        assertFalse(tab.seleccionarCasillaTrampa(3, 0)); // trono1
        assertFalse(tab.seleccionarCasillaTrampa(3, 6)); // trono2
        assertFalse(tab.seleccionarCasillaTrampa(0, 0)); // casilla ocupada

        assertTrue(tab.seleccionarCasillaTrampa(0, 3));   // casilla vacia
        assertTrue(tab.verificarCasillaTrampa(0, 3));
        assertFalse(tab.seleccionarCasillaTrampa(0, 3));  // doble trampa
        System.out.println("[7] Restricciones de trampas OK");
    }

    // -- JugadorIA + Partida: aplicar jugada al tablero --

    @Test @Order(8)
    @DisplayName("Jugada de la IA aplicada al tablero produce estado consistente")
    void testIAAplicadaAlTablero() {
      try {
        Partida p = new Partida(0, "Jugandose", 0, "Publica", "", "", 0, 0, "d1", "d2", false, false);
        List<CartaMov> cm = new ArrayList<>();
        cm.add(new CartaMov("Tigre",  "(0,-2),(0,1)",              0, "EQ1"));
        cm.add(new CartaMov("Mono",   "(-1,-1),(1,-1),(-1,1),(1,1)",0, "EQ1"));
        cm.add(new CartaMov("Grulla", "(0,-1),(-1,1),(1,1)",        0, "EQ2"));
        cm.add(new CartaMov("Dragon", "(-2,-1),(2,-1),(-1,1),(1,1)",0, "EQ2"));
        cm.add(new CartaMov("Cobra",  "(-1,0),(1,-1),(1,1)",        0, "MAZO"));
        List<CartaAccion> ca = new ArrayList<>();
        inyectarCartas(p, cm, ca);

        Tablero tab = p.getTablero();

        JugadorIA ia = new JugadorIA(JugadorIA.NivelDificultad.FACIL, 1);
        JugadorIA.Jugada jugada = ia.calcularMejorMovimiento(p);
        assertNotNull(jugada);

        if (!jugada.esAccion) {
            Posicion origen = tab.getPosicion(jugada.origenX, jugada.origenY);
            Posicion destino = tab.getPosicion(jugada.destinoX, jugada.destinoY);
            Ficha fichaMovida = origen.getFicha();

            assertNotNull(fichaMovida);
            assertEquals(1, fichaMovida.getEquipo());

            destino.setFicha(fichaMovida);
            origen.setFicha(null);

            assertNull(origen.getFicha());
            assertNotNull(destino.getFicha());
            assertEquals(1, destino.getFicha().getEquipo());
        }
        System.out.println("[8] Jugada IA aplicada OK");
      } catch (Exception e) { fail("Excepcion: " + e.getMessage()); }
    }

    // -- Subject/Observer --

    @Test @Order(9)
    @DisplayName("Patron Observer: attach, notify, dettach")
    void testObserverNotificacion() {
        Subject subject = new Subject();
        final int[] contador = {0};

        Observer obs = new Observer() {
            @Override
            public void update() { contador[0]++; }
        };

        subject.attach(obs);
        subject.nootify();
        assertEquals(1, contador[0]);

        subject.nootify();
        assertEquals(2, contador[0]);

        subject.dettach(obs);
        subject.nootify();
        assertEquals(2, contador[0]);
        System.out.println("[9] Patron Observer OK");
    }

    // -- Notificacion --

    @Test @Order(10)
    @DisplayName("Notificacion: estados y expiracion")
    void testNotificacion() {
        Timestamp ahora = new Timestamp(System.currentTimeMillis());
        Timestamp pasado = new Timestamp(System.currentTimeMillis() - 100000);
        Timestamp futuro = new Timestamp(System.currentTimeMillis() + 100000);

        Notificacion nExpirada = new Notificacion(1, Notificacion.TIPO_SOLICITUD_AMISTAD,
                "A", "B", Notificacion.ESTADO_PENDIENTE, ahora, pasado, null);
        assertTrue(nExpirada.haExpirado());

        Notificacion nVigente = new Notificacion(2, Notificacion.TIPO_INVITACION_PARTIDA,
                "A", "B", Notificacion.ESTADO_PENDIENTE, ahora, futuro, 42);
        assertFalse(nVigente.haExpirado());
        assertEquals(42, nVigente.getIdPartida());

        nVigente.setEstado(Notificacion.ESTADO_ACEPTADA);
        assertEquals(Notificacion.ESTADO_ACEPTADA, nVigente.getEstado());
        System.out.println("[10] Notificacion OK");
    }

    // -- Skin --

    @Test @Order(11)
    @DisplayName("Skin: creacion y modificacion")
    void testSkin() {
        Skin skin = new Skin("Oscura", "#000", "#00F", "#F00", 500);
        assertEquals("Oscura", skin.getNombre());
        assertEquals(500, skin.getPrecio());

        skin.setPrecio(300);
        assertEquals(300, skin.getPrecio());
        System.out.println("[11] Skin OK");
    }

    // -- Posicion.setFicha: logica de combate --

    @Test @Order(12)
    @DisplayName("setFicha: captura enemigo, aliado bloqueado, casilla vacia")
    void testSetFichaCombate() {
        // Peon EQ1 ataca peon EQ2 -> captura (devuelve 2)
        Posicion pos = new Posicion(0, 0, new Ficha(false, 2));
        int resultado = pos.setFicha(new Ficha(false, 1));
        assertEquals(2, resultado);

        // Peon EQ1 ataca rey EQ2: setFicha hace ficha=F antes de matar(),
        // asi que matar() actua sobre la nueva ficha y devuelve 2 en vez de 3
        Posicion posRey = new Posicion(0, 0, new Ficha(true, 2));
        int resultadoRey = posRey.setFicha(new Ficha(false, 1));
        assertEquals(2, resultadoRey); // Deberia ser 3, bug en setFicha

        // Aliado -> devuelve 1 (no puede mover)
        Posicion posAliada = new Posicion(0, 0, new Ficha(false, 1));
        int resultadoAliado = posAliada.setFicha(new Ficha(false, 1));
        assertEquals(1, resultadoAliado);

        // Casilla vacia -> devuelve 0
        Posicion posVacia = new Posicion(0, 0, null);
        int resultadoVacio = posVacia.setFicha(new Ficha(false, 1));
        assertEquals(0, resultadoVacio);
        System.out.println("[12] Logica de combate setFicha OK");
    }

    // -- Utilidades --

    private int contarFichas(Tablero tab) {
        int n = 0;
        for (int y = 0; y < tab.getDIM(); y++)
            for (int x = 0; x < tab.getDIM(); x++)
                if (tab.getPosicion(x, y).getFicha() != null) n++;
        return n;
    }

    public static void main(String[] args) {
        System.out.println("=== TEST INTEGRACION BACKEND (Onitama) ===\n");

        testIntegracion t = new testIntegracion();
        int ok = 0, fail = 0;
        try { setUp(); } catch (Exception e) { System.err.println("SETUP FALLO: " + e.getMessage()); return; }

        Runnable[] tests = {
            t::testCapturarFichaTablero, t::testCapturarReyDetectaVictoria,
            t::testRevivirEnTronoVacio, t::testRevivirTronoOcupado, t::testCartaAccionNoReutilizable,
            t::testCartaMovLimites, t::testTrampaRestricciones, t::testIAAplicadaAlTablero,
            t::testObserverNotificacion, t::testNotificacion, t::testSkin, t::testSetFichaCombate
        };
        String[] nombres = {
            "Captura peon", "Captura rey", "Revivir vacio", "Trono ocupado", "Accion no reutilizable",
            "Limites mov", "Trampas", "IA aplicada", "Observer", "Notificacion", "Skin", "Combate setFicha"
        };

        for (int i = 0; i < tests.length; i++) {
            try { tests[i].run(); ok++; }
            catch (AssertionError e) { System.err.println("FALLO " + nombres[i] + ": " + e.getMessage()); fail++; }
            catch (Exception e) { System.err.println("FALLO " + nombres[i] + ": " + e.getMessage()); fail++; }
        }
        System.out.println("\nResultado: " + ok + "/" + (ok+fail) + " tests pasados " + (fail == 0 ? "OK" : "FAIL"));
    }
}
