package es.tor.push;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import es.tor.dao.DeviceDao;
import es.tor.entity.Device;
import es.tor.entity.Notification;
import es.tor.push.android.Constants;
import es.tor.push.android.Message;
import es.tor.push.android.MulticastResult;
import es.tor.push.android.Result;
import es.tor.push.android.Sender;

public class AndroidPush {
    protected static Logger logger = Logger.getLogger(AndroidPush.class);

    protected DeviceDao dao;

    public AndroidPush(DeviceDao dao) {
        this.dao = dao;
    }

    public long sendAndroid(Notification notificacion, List<Device> dispositivos) {
        long correctos = 0;

        List<String> tokens = new ArrayList<String>();
        List<Device> dispositivosSend = new ArrayList<Device>();
        for (Device dispositivo : dispositivos) {
            if (dispositivo.getType().equals(Device.APPLE))
                continue;

            dispositivosSend.add(dispositivo);
            tokens.add(dispositivo.getToken());

            if (dispositivosSend.size() == 1000) {
                correctos += sendMultiMessage(notificacion, dispositivosSend, tokens);

                tokens = new ArrayList<String>();
                dispositivosSend = new ArrayList<Device>();
            }

        }

        if (dispositivosSend.size() > 0) {
            correctos += sendMultiMessage(notificacion, dispositivosSend, tokens);
        }
        
        return correctos;
    }

    protected boolean checkResult(Result result, Device dispositivo) {
        if (result.getMessageId() != null) {
            // Si nos devuelve un nuevo Id, cambiamos el token
            if (result.getCanonicalRegistrationId() != null) {
                dispositivo.setToken(result.getCanonicalRegistrationId());
                if (dao != null)
                    dao.save(dispositivo);
            }

            return true;

        } else {
            // Si nos dice que no está registrado el dispositivo,
            // lo eliminamos de la bd
            if (result.getErrorCodeName().equals(Constants.ERROR_NOT_REGISTERED)) {
                if (dao != null)
                    dao.remove(dispositivo.getIdDispositivo());
            } else {
                logger.error("Error al enviar push al dispositivo: " + result.getErrorCodeName());
            }

            return false;
        }
    }

    protected int sendMultiMessage(Notification notificacion, List<Device> dispositivos, List<String> tokens) {
        String collapseKey = getCollapseKey(notificacion);

        String mensaje;
        // Todos los dispositivos de este grupo estarán con el mismo idioma
        if (dispositivos.get(0).getIdioma().equals(1)) {
            if (notificacion.getMensaje() != null && !notificacion.getMensaje().isEmpty()) {
                mensaje = notificacion.getMensaje();
            } else {
                mensaje = notificacion.getMensajeVa();
            }
        } else {
            if (notificacion.getMensajeVa() != null && !notificacion.getMensajeVa().isEmpty()) {
                mensaje = notificacion.getMensajeVa();
            } else {
                mensaje = notificacion.getMensaje();
            }
        }

        Message message = new Message.Builder()
                        .collapseKey(collapseKey)
                        .addData("cabecera",
                                        notificacion.getTitulo().length() > 50 ? notificacion.getTitulo().substring(1,
                                                        50) : notificacion.getTitulo()).addData("mensaje", mensaje)
                        .build();

        try {
            Sender sender = new Sender();
            MulticastResult results = sender.send(message, tokens, 5);
            int count = 0;
            int correctos = 0;
            for (Result result : results.getResults()) {
                if (checkResult(result, dispositivos.get(count))) {
                    correctos++;
                }
                count++;
            }

            return correctos;

        } catch (Exception e) {
            logger.error("Error inesperado al enviar push Android", e);
        }

        return 0;
    }

    private String getCollapseKey(Notification notificacion) {
        return String.valueOf(notificacion.getIdNotificacion());
    }
}
