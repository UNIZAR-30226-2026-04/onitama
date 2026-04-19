import VO.*;
import JDBC.*;
import gestor.GestorNotificaciones;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

/**
 * Test integral del sistema de notificaciones y partidas privadas.
 * Requiere: BD con crear.sql y construir.sql ejecutados (Docker).
 */
public class testNotificaciones {
    private static int ok = 0, fail = 0;

    public static void main(String[] args) {
        System.out.println("=== TEST NOTIFICACIONES Y PARTIDAS PRIVADAS ===\n");

        try {
            // 1. Crear jugadores de prueba
            ejecutar("Paso 1: Registrar jugadores", () -> paso1Jugadores());

            // 2. Solicitud de amistad: enviar, aceptar, rechazar
            ejecutar("Paso 2: Solicitudes de amistad", () -> paso2Amistad());

            // 3. Invitación partida privada: enviar, aceptar, rechazar
            ejecutar("Paso 3: Invitaciones a partida privada", () -> paso3InvitacionPartida());

            // 4. Expiración de notificaciones
            ejecutar("Paso 4: Expiración de notificaciones", () -> paso4Expiracion());

            // 5. Solicitud pausa y reanudar (sobre partida en curso)
            ejecutar("Paso 5: Pausa y reanudar partida", () -> paso5PausaReanudar());

            // 6. Casos límite y validaciones
            ejecutar("Paso 6: Casos límite", () -> paso6CasosLimite());

            // 7. VO Jugador y métodos nuevos
            ejecutar("Paso 7: Jugador (cargarNotificaciones, aceptar, rechazar)", () -> paso7Jugador());

        } catch (Exception e) {
            System.err.println("ERROR FATAL: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== RESUMEN ===");
        System.out.println("   OK: " + ok + "   FALLOS: " + fail);
        System.out.println(fail == 0 ? "   RESULTADO: TODOS LOS TESTS PASARON" : "   RESULTADO: HAY FALLOS");
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private static void ejecutar(String titulo, ThrowingRunnable r) {
        System.out.println("\n--- " + titulo + " ---");
        try {
            r.run();
        } catch (Exception e) {
            fallo("Excepción: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void paso1Jugadores() throws SQLException {
        JugadorJDBC jdbc = new JugadorJDBC();
        if (jdbc.buscarJugador("NotifA") != null) {
            System.out.println("   [INFO] Jugadores ya existen, limpiando estado previo para test...");
            GestorNotificaciones g = new GestorNotificaciones();
            for (String u : new String[]{"NotifA", "NotifB", "NotifC"}) {
                for (Notificacion n : g.obtenerPendientes(u))
                    g.rechazarNotificacion(n.getIdNotificacion(), u);
            }
            if (jdbc.sonAmigos("NotifA","NotifB")) jdbc.borrarAmigo("NotifA","NotifB");
            if (jdbc.sonAmigos("NotifA","NotifC")) jdbc.borrarAmigo("NotifA","NotifC");
            if (jdbc.sonAmigos("NotifB","NotifC")) jdbc.borrarAmigo("NotifB","NotifC");
            PartidaJDBC pJdbc = new PartidaJDBC();
            for (Partida pa : pJdbc.buscarPartidasJugadorPrivadas("NotifA","NotifB"))
                if ("Jugandose".equals(pa.getEstado()) || "Pausada".equals(pa.getEstado()))
                    pJdbc.updateEstado(pa.getIDPartida(), "Finalizada");
            for (Partida pa : pJdbc.buscarPartidasJugadorPrivadas("NotifA","NotifC"))
                if ("Jugandose".equals(pa.getEstado()) || "Pausada".equals(pa.getEstado()))
                    pJdbc.updateEstado(pa.getIDPartida(), "Finalizada");
            for (Partida pa : pJdbc.buscarPartidasJugadorPrivadas("NotifB","NotifC"))
                if ("Jugandose".equals(pa.getEstado()) || "Pausada".equals(pa.getEstado()))
                    pJdbc.updateEstado(pa.getIDPartida(), "Finalizada");
            acierto("Estado de test reseteado (amistades, notificaciones y partidas)");
            return;
        }
        Jugador a = new Jugador("notifa@test.com", "NotifA", "pass1");
        Jugador b = new Jugador("notifb@test.com", "NotifB", "pass2");
        Jugador c = new Jugador("notifc@test.com", "NotifC", "pass3");
        a.registrarse();
        b.registrarse();
        c.registrarse();
        acierto("Jugadores NotifA, NotifB, NotifC registrados");
    }

    private static void paso2Amistad() throws SQLException {
        GestorNotificaciones g = new GestorNotificaciones();

        // 2.1 NotifA solicita amistad a NotifB
        int id1 = g.enviarSolicitudAmistad("NotifA", "NotifB");
        assertOk(id1 > 0, "Enviar solicitud amistad A->B");

        // 2.2 NotifB acepta
        boolean acep = g.aceptarNotificacion(id1, "NotifB");
        assertOk(acep, "NotifB acepta solicitud amistad");

        JugadorJDBC jdbc = new JugadorJDBC();
        assertOk(jdbc.sonAmigos("NotifA", "NotifB"), "Son amigos tras aceptar");

        // 2.3 NotifA solicita amistad a NotifC; NotifC rechaza
        int id2 = g.enviarSolicitudAmistad("NotifA", "NotifC");
        assertOk(id2 > 0, "Enviar solicitud amistad A->C");
        boolean rech = g.rechazarNotificacion(id2, "NotifC");
        assertOk(rech, "NotifC rechaza solicitud amistad");
        assertOk(!jdbc.sonAmigos("NotifA", "NotifC"), "No son amigos tras rechazar");

        // 2.4 Caso: enviar a uno mismo
        int id3 = g.enviarSolicitudAmistad("NotifA", "NotifA");
        assertOk(id3 == -1, "Solicitud a uno mismo debe fallar");

        // 2.5 Caso: ya amigos
        int id4 = g.enviarSolicitudAmistad("NotifA", "NotifB");
        assertOk(id4 == -1, "Solicitud a ya amigo debe fallar");

        // 2.6 Caso: solicitud duplicada pendiente
        int id5 = g.enviarSolicitudAmistad("NotifB", "NotifC");
        assertOk(id5 > 0, "B solicita a C");
        int id6 = g.enviarSolicitudAmistad("NotifB", "NotifC");
        assertOk(id6 == -1, "Solicitud duplicada pendiente debe fallar");
    }

    private static void paso3InvitacionPartida() throws SQLException {
        GestorNotificaciones g = new GestorNotificaciones();

        // 3.1 NotifA invita a NotifB (amigos) - expiración 60 segundos para test
        int idInv = g.enviarInvitacionPartidaConExpiracion("NotifA", "NotifB", 60);
        assertOk(idInv > 0, "Invitación partida A->B (amigos)");

        // 3.2 NotifB acepta -> crea partida privada
        boolean acep = g.aceptarNotificacion(idInv, "NotifB");
        assertOk(acep, "NotifB acepta invitación partida");

        PartidaJDBC pJdbc = new PartidaJDBC();
        List<Partida> partidas = pJdbc.buscarPartidasJugadorPrivadas("NotifA", "NotifB");
        assertOk(partidas != null && !partidas.isEmpty(), "Partida privada creada");
        if (!partidas.isEmpty()) {
            Partida p = partidas.stream().max(Comparator.comparingInt(Partida::getIDPartida)).orElse(partidas.get(0));
            assertOk("Privado".equals(p.getTipo()), "Tipo Privado");
            assertOk("Jugandose".equals(p.getEstado()) || "Pausada".equals(p.getEstado()), "Estado Jugandose/Pausada tras iniciar");
        }

        // 3.3 Nueva invitación: A invita a B de nuevo (ahora hay partida); B rechaza
        int idInv2 = g.enviarInvitacionPartidaConExpiracion("NotifA", "NotifB", 60);
        assertOk(idInv2 > 0, "Segunda invitación A->B");
        boolean rech = g.rechazarNotificacion(idInv2, "NotifB");
        assertOk(rech, "NotifB rechaza segunda invitación");

        // 3.4 Caso: invitar a no amigo
        int idInv3 = g.enviarInvitacionPartida("NotifA", "NotifC");
        assertOk(idInv3 == -1, "Invitación a no amigo debe fallar");
    }

    private static void paso4Expiracion() throws SQLException {
        GestorNotificaciones g = new GestorNotificaciones();

        // Rechazar la solicitud B->C pendiente para poder reenviar
        List<Notificacion> pend = g.obtenerPendientes("NotifC");
        for (Notificacion n : pend) {
            if ("NotifB".equals(n.getRemitente()))
                g.rechazarNotificacion(n.getIdNotificacion(), "NotifC");
        }

        // B hace amigo a C para poder invitar
        int idAmistad = g.enviarSolicitudAmistad("NotifB", "NotifC");
        if (idAmistad > 0) g.aceptarNotificacion(idAmistad, "NotifC");

        // Invitación que expira en 2 segundos
        int idExp = g.enviarInvitacionPartidaConExpiracion("NotifB", "NotifC", 2);
        assertOk(idExp > 0, "Invitación con expiración 2 segundos");

        System.out.println("   Esperando 3 segundos para que expire...");
        try { Thread.sleep(3500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        boolean acepExp = g.aceptarNotificacion(idExp, "NotifC");
        assertOk(!acepExp, "Aceptar notificación expirada debe fallar");

        NotificacionJDBC nJdbc = new NotificacionJDBC();
        Notificacion nExp = nJdbc.obtenerPorId(idExp);
        assertOk(nExp != null && Notificacion.ESTADO_RECHAZADA.equals(nExp.getEstado()),
                "Notificación expirada pasa a RECHAZADA automáticamente");
    }

    private static void paso5PausaReanudar() throws SQLException {
        GestorNotificaciones g = new GestorNotificaciones();
        PartidaJDBC pJdbc = new PartidaJDBC();

        List<Partida> partidas = pJdbc.buscarPartidasJugadorPrivadas("NotifA", "NotifB");
        if (partidas.isEmpty()) {
            fallo("No hay partida en curso para probar pausa");
            return;
        }
        Partida partidaActiva = partidas.stream().filter(pp -> "Jugandose".equals(pp.getEstado()) || "Pausada".equals(pp.getEstado())).max(Comparator.comparingInt(Partida::getIDPartida)).orElse(partidas.get(partidas.size()-1));
        int idPartida = partidaActiva.getIDPartida();

        // J1 (NotifA) solicita pausa a J2 (NotifB)
        int idPausa = g.enviarSolicitudPausaConExpiracion("NotifA", "NotifB", idPartida, 60);
        assertOk(idPausa > 0, "Solicitud pausa enviada");

        boolean acepPausa = g.aceptarNotificacion(idPausa, "NotifB");
        assertOk(acepPausa, "NotifB acepta pausa");

        Partida p = pJdbc.buscarPorId(idPartida);
        assertOk(p != null && "Pausada".equals(p.getEstado()), "Partida en estado Pausada");

        // NotifA solicita reanudar
        int idReanudar = g.enviarSolicitudReanudarConExpiracion("NotifA", "NotifB", idPartida, 60);
        assertOk(idReanudar > 0, "Solicitud reanudar enviada");

        boolean acepReanudar = g.aceptarNotificacion(idReanudar, "NotifB");
        assertOk(acepReanudar, "NotifB acepta reanudar");

        p = pJdbc.buscarPorId(idPartida);
        assertOk(p != null && "Jugandose".equals(p.getEstado()), "Partida reanudada (Jugandose)");
    }

    private static void paso6CasosLimite() throws SQLException {
        GestorNotificaciones g = new GestorNotificaciones();
        NotificacionJDBC nJdbc = new NotificacionJDBC();

        // Aceptar con usuario incorrecto
        int idFake = g.enviarSolicitudAmistad("NotifC", "NotifA");
        if (idFake > 0) {
            boolean acepFake = g.aceptarNotificacion(idFake, "NotifB");
            assertOk(!acepFake, "Aceptar con usuario que no es destinatario debe fallar");
            g.rechazarNotificacion(idFake, "NotifA");
        }

        // Rechazar dos veces la misma
        int idRech = g.enviarSolicitudAmistad("NotifC", "NotifB");
        if (idRech > 0) {
            g.rechazarNotificacion(idRech, "NotifB");
            boolean rech2 = g.rechazarNotificacion(idRech, "NotifB");
            assertOk(!rech2, "Rechazar ya rechazada debe fallar");
        }

        // obtenerPorId
        List<Notificacion> todas = g.obtenerPendientes("NotifA");
        if (!todas.isEmpty()) {
            Notificacion primera = nJdbc.obtenerPorId(todas.get(0).getIdNotificacion());
            assertOk(primera != null, "obtenerPorId devuelve notificación");
        }
        Notificacion inexistente = nJdbc.obtenerPorId(999999);
        assertOk(inexistente == null, "obtenerPorId con ID inexistente devuelve null");
    }

    private static void paso7Jugador() throws SQLException {
        Jugador j = Jugador.iniciarSesion("NotifA", "pass1");
        assertOk(j != null, "Login NotifA");

        j.cargarAmigos();
        assertOk(j.getAmigos() != null && !j.getAmigos().isEmpty(), "Jugador tiene amigos cargados");

        j.cargarNotificaciones();
        assertOk(j.getNotificacionesPendientes() != null, "getNotificacionesPendientes");

        // Solicitar amistad usando Jugador
        boolean solic = j.solicitarAmistad("NotifC");
        assertOk(solic, "Jugador.solicitarAmistad");

        Jugador jC = Jugador.iniciarSesion("NotifC", "pass3");
        jC.cargarNotificaciones();
        List<Notificacion> notifsC = jC.getNotificacionesPendientes();
        assertOk(notifsC != null && !notifsC.isEmpty(), "NotifC recibe notificación");

        if (!notifsC.isEmpty()) {
            int idNotif = notifsC.get(0).getIdNotificacion();
            boolean acepJ = jC.aceptarNotificacion(idNotif);
            assertOk(acepJ, "Jugador.aceptarNotificacion");

            jC.cargarAmigos();
            assertOk(jC.getAmigos().stream().anyMatch(a -> "NotifA".equals(a.getNombre())), "NotifC tiene a NotifA como amigo");
        }

        // Rechazar usando Jugador (invitación partida: A invita a B)
        j.enviarInvitacionPartida("NotifB");
        Jugador jB = Jugador.iniciarSesion("NotifB", "pass2");
        jB.cargarNotificaciones();
        for (Notificacion n : jB.getNotificacionesPendientes()) {
            if ("NotifA".equals(n.getRemitente()) && Notificacion.TIPO_INVITACION_PARTIDA.equals(n.getTipo())) {
                boolean rechJ = jB.rechazarNotificacion(n.getIdNotificacion());
                assertOk(rechJ, "Jugador.rechazarNotificacion (invitación partida)");
                break;
            }
        }

        // borrarAmigo
        j.cargarAmigos();
        Jugador amigoC = j.getAmigos().stream().filter(a -> "NotifC".equals(a.getNombre())).findFirst().orElse(null);
        if (amigoC != null) {
            boolean borr = j.borrarAmigo(amigoC);
            assertOk(borr, "Jugador.borrarAmigo");
            JugadorJDBC jdbc = new JugadorJDBC();
            assertOk(!jdbc.sonAmigos("NotifA", "NotifC"), "Ya no son amigos tras borrar");
        }
    }

    private static void assertOk(boolean condicion, String desc) {
        if (condicion) {
            acierto(desc);
        } else {
            fallo(desc);
        }
    }

    private static void acierto(String msg) {
        ok++;
        System.out.println("   [OK] " + msg);
    }

    private static void fallo(String msg) {
        fail++;
        System.out.println("   [FALLO] " + msg);
    }
}
