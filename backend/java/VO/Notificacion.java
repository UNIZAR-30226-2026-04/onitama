package VO;

import java.sql.Timestamp;
import JDBC.NotificacionJDBC;
import java.sql.SQLException;
import DAO.NotificacionDAO;

/**
 * Value Object para notificaciones unificadas: amistad, invitación partida, pausa, reanudar.
 */
public class Notificacion {
    public static final String TIPO_SOLICITUD_AMISTAD = "SOLICITUD_AMISTAD";
    public static final String TIPO_INVITACION_PARTIDA = "INVITACION_PARTIDA";
    public static final String TIPO_SOLICITUD_PAUSA = "SOLICITUD_PAUSA";
    public static final String TIPO_REANUDAR_PARTIDA = "REANUDAR_PARTIDA";

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_ACEPTADA = "ACEPTADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";

    private int idNotificacion;
    private String tipo;
    private String remitente;
    private String destinatario;
    private String estado;
    private Timestamp fechaCreacion;
    private Timestamp fechaExpiracion;
    private Integer idPartida;
    private NotificacionDAO dao;

    public Notificacion(int idNotificacion, String tipo, String remitente, String destinatario,
                        String estado, Timestamp fechaCreacion, Timestamp fechaExpiracion, Integer idPartida) {
        this.idNotificacion = idNotificacion;
        this.tipo = tipo;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.idPartida = idPartida;
        dao = new NotificacionJDBC();
    }

    public boolean registrarNotificacion(){
        try {
            idNotificacion = dao.crear(this);
            return idNotificacion >= 0; 
        } catch (SQLException e) {
            return false;
        }
    }

    public int getIdNotificacion() {
        return idNotificacion;
    }

    public String getTipo() {
        return tipo;
    }

    public String getRemitente() {
        return remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getEstado() {
        return estado;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public Timestamp getFechaExpiracion() {
        return fechaExpiracion;
    }

    public Integer getIdPartida() {
        return idPartida;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /** Devuelve true si la notificación ha expirado (solo aplicable cuando fechaExpiracion no es null). */
    public boolean haExpirado() {
        if (fechaExpiracion == null) return false;
        return new Timestamp(System.currentTimeMillis()).after(fechaExpiracion);
    }
}
