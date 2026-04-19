package gestor;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Envío de emails via Gmail SMTP con JavaMail.
 * Usado para recuperación de contraseña: genera una nueva contraseña aleatoria
 * y la envía al correo del jugador.
 */
public class GestorEmail {

    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final int    SMTP_PORT      = 587;
    private static final String EMAIL_CUENTA   = "ironTaisen@gmail.com";
    // App password de Gmail (sin espacios)
    private static final String EMAIL_PASSWORD = "kksod fpjsfuznidy";
    private static final String NOMBRE_REMIT   = "Iron Taisen";

    private static final String CHARS_CONTRASENA =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** Genera una contraseña aleatoria de 10 caracteres alfanuméricos. */
    public static String generarContrasennaAleatoria() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        // Garantizar al menos 1 mayúscula, 1 minúscula, 1 dígito
        sb.append(CHARS_CONTRASENA.charAt(rng.nextInt(26)));                  // mayúscula
        sb.append(CHARS_CONTRASENA.charAt(26 + rng.nextInt(26)));             // minúscula
        sb.append(CHARS_CONTRASENA.charAt(52 + rng.nextInt(10)));             // dígito
        for (int i = 3; i < 10; i++) {
            sb.append(CHARS_CONTRASENA.charAt(rng.nextInt(CHARS_CONTRASENA.length())));
        }
        // Mezclar para que los 3 primeros no sean siempre mayúscula/minúscula/dígito
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }

    /**
     * Envía al destinatario un email con su nueva contraseña temporal.
     * @param destinatario  Correo del jugador
     * @param nuevaContrasena  Contraseña en texto plano (ya generada)
     * @param nombreJugador  Nombre de usuario para personalizar el mensaje
     */
    public static void enviarContrasennaReset(
            String destinatario, String nuevaContrasena, String nombreJugador)
            throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.host",              SMTP_HOST);
        props.put("mail.smtp.port",              String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",         SMTP_HOST);
        // Timeouts para evitar bloqueos indefinidos (ms)
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout",           "8000");
        props.put("mail.smtp.writetimeout",      "8000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_CUENTA, EMAIL_PASSWORD);
            }
        });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(EMAIL_CUENTA, NOMBRE_REMIT, "UTF-8"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            msg.setSubject("Recuperación de contraseña – Onitama");

            String cuerpo =
                "Hola, " + nombreJugador + ".\n\n" +
                "Tu usuario de Onitama es:  " + nombreJugador + "\n" +
                "Tu nueva contraseña es:    " + nuevaContrasena + "\n\n" +
                "Un saludo,\n" +
                NOMBRE_REMIT;

            msg.setText(cuerpo, "UTF-8");
            Transport.send(msg);

        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Error de codificación al construir el email", e);
        }
    }
}
