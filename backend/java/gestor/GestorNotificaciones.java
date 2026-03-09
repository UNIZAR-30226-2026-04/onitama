package gestor;

import JDBC.JugadorJDBC;
import JDBC.NotificacionJDBC;
import JDBC.PartidaJDBC;
import VO.Notificacion;
import VO.Partida;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

/**
 * Coordina la lógica de aceptar y rechazar notificaciones según su tipo.
 */
public class GestorNotificaciones {

    private final NotificacionJDBC notifJdbc;
    private final JugadorJDBC jugadorJdbc;
    private final PartidaJDBC partidaJdbc;

    // Minutos de validez para invitaciones de partida.
    public static final int MINUTOS_EXPIRACION_PARTIDA = 1;

    public GestorNotificaciones() {
        this.notifJdbc = new NotificacionJDBC();
        this.jugadorJdbc = new JugadorJDBC();
        this.partidaJdbc = new PartidaJDBC();
    }

    //Obtiene las notificaciones pendientes de un jugador.
    public List<Notificacion> obtenerPendientes(String nombreUsuario) throws SQLException {
        return notifJdbc.obtenerPendientes(nombreUsuario);
    }

    // Acepta una notificación. La acción depende del tipo.
    public boolean aceptarNotificacion(int idNotificacion, String nombreQuienAcepta) throws SQLException {
        Notificacion n = notifJdbc.obtenerPorId(idNotificacion);
        if (n == null || !Notificacion.ESTADO_PENDIENTE.equals(n.getEstado())) {
            return false;
        }
        if (!n.getDestinatario().equals(nombreQuienAcepta)) {
            return false;
        }
        if (n.haExpirado()) {
            notifJdbc.actualizarEstado(idNotificacion, Notificacion.ESTADO_RECHAZADA);
            return false;
        }

        switch (n.getTipo()) {
            case Notificacion.TIPO_SOLICITUD_AMISTAD:
                jugadorJdbc.insertarAmistad(n.getRemitente(), n.getDestinatario());
                return notifJdbc.actualizarEstado(idNotificacion, Notificacion.ESTADO_ACEPTADA);

            case Notificacion.TIPO_INVITACION_PARTIDA:
                Partida partida = crearPartidaPrivada(n.getRemitente(), n.getDestinatario());
                if (partida != null && partida.registrarPartida() && partida.iniciarPartida()) {
                    return notifJdbc.actualizarEstado(idNotificacion, Notificacion.ESTADO_ACEPTADA);
                }
                return false;

            case Notificacion.TIPO_SOLICITUD_PAUSA:
                if (n.getIdPartida() != null) {
                    if (partidaJdbc.updateEstado(n.getIdPartida(), "Pausada")) {
                        return notifJdbc.actualizarEstado(idNotificacion, Notificacion.ESTADO_ACEPTADA);
                    }
                }
                return false;

            case Notificacion.TIPO_REANUDAR_PARTIDA:
                if (n.getIdPartida() != null) {
                    if (partidaJdbc.updateEstado(n.getIdPartida(), "Jugandose")) {
                        return notifJdbc.actualizarEstado(idNotificacion, Notificacion.ESTADO_ACEPTADA);
                    }
                }
                return false;

            default:
                return false;
        }
    }

    // Rechaza una notificación.
    public boolean rechazarNotificacion(int idNotificacion, String nombreQuienRechaza) throws SQLException {
        Notificacion n = notifJdbc.obtenerPorId(idNotificacion);
        if (n == null || !Notificacion.ESTADO_PENDIENTE.equals(n.getEstado())) {
            return false;
        }
        if (!n.getDestinatario().equals(nombreQuienRechaza)) {
            return false;
        }
        return notifJdbc.actualizarEstado(idNotificacion, Notificacion.ESTADO_RECHAZADA);
    }

    // Envía una solicitud de amistad (crea notificación).
    public int enviarSolicitudAmistad(String remitente, String destinatario) throws SQLException {
        if (remitente.equals(destinatario)) return -1;
        if (jugadorJdbc.sonAmigos(remitente, destinatario)) return -1;
        if (existeNotificacionPendiente(Notificacion.TIPO_SOLICITUD_AMISTAD, remitente, destinatario)) return -1;

        Notificacion n = new Notificacion(0, Notificacion.TIPO_SOLICITUD_AMISTAD, remitente, destinatario,
                Notificacion.ESTADO_PENDIENTE, null, null, null);
        return notifJdbc.crear(n);
    }

    // Envía invitación a partida privada.
    public int enviarInvitacionPartida(String remitente, String destinatario) throws SQLException {
        return enviarInvitacionPartidaConExpiracion(remitente, destinatario, MINUTOS_EXPIRACION_PARTIDA * 60);
    }

    /** Para tests: invitación con expiración en segundos. */
    public int enviarInvitacionPartidaConExpiracion(String remitente, String destinatario, int segundosExpiracion) throws SQLException {
        if (remitente.equals(destinatario)) return -1;
        if (!jugadorJdbc.sonAmigos(remitente, destinatario)) return -1;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, segundosExpiracion);
        Timestamp expiracion = new Timestamp(cal.getTimeInMillis());
        Notificacion n = new Notificacion(0, Notificacion.TIPO_INVITACION_PARTIDA, remitente, destinatario,
                Notificacion.ESTADO_PENDIENTE, null, expiracion, null);
        return notifJdbc.crear(n);
    }

    // Envía solicitud de pausa. Requiere ID de partida.
    public int enviarSolicitudPausa(String remitente, String destinatario, int idPartida) throws SQLException {
        return enviarSolicitudPausaConExpiracion(remitente, destinatario, idPartida, MINUTOS_EXPIRACION_PARTIDA * 60);
    }

    /** Para tests: pausa con expiración en segundos. */
    public int enviarSolicitudPausaConExpiracion(String remitente, String destinatario, int idPartida, int segundos) throws SQLException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, segundos);
        Timestamp expiracion = new Timestamp(cal.getTimeInMillis());
        Notificacion n = new Notificacion(0, Notificacion.TIPO_SOLICITUD_PAUSA, remitente, destinatario,
                Notificacion.ESTADO_PENDIENTE, null, expiracion, idPartida);
        return notifJdbc.crear(n);
    }

    // Envía solicitud de reanudar partida.
    public int enviarSolicitudReanudar(String remitente, String destinatario, int idPartida) throws SQLException {
        return enviarSolicitudReanudarConExpiracion(remitente, destinatario, idPartida, MINUTOS_EXPIRACION_PARTIDA * 60);
    }

    /** Para tests: reanudar con expiración en segundos. */
    public int enviarSolicitudReanudarConExpiracion(String remitente, String destinatario, int idPartida, int segundos) throws SQLException {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, segundos);
        Timestamp expiracion = new Timestamp(cal.getTimeInMillis());
        Notificacion n = new Notificacion(0, Notificacion.TIPO_REANUDAR_PARTIDA, remitente, destinatario,
                Notificacion.ESTADO_PENDIENTE, null, expiracion, idPartida);
        return notifJdbc.crear(n);
    }

    private Partida crearPartidaPrivada(String j1, String j2) {
        return new Partida(-1, "Esperando", 0, "Privado",
                "", "", 0, 0, j1, j2, false, false, 0);
    }

    private boolean existeNotificacionPendiente(String tipo, String remitente, String destinatario) throws SQLException {
        List<Notificacion> pend = notifJdbc.obtenerPendientes(destinatario);
        for (Notificacion n : pend) {
            if (tipo.equals(n.getTipo()) && remitente.equals(n.getRemitente())) {
                return true;
            }
        }
        return false;
    }
}
