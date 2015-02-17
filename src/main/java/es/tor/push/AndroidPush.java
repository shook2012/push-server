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

    public long send(Notification notification, List<Device> devices) {
        long correctos = 0;

        List<String> tokens = new ArrayList<String>();
        List<Device> devicesSend = new ArrayList<Device>();
        for (Device device : devices) {
            if (device.getType().equals(Device.APPLE))
                continue;

            devicesSend.add(device);
            tokens.add(device.getToken());

            if (devicesSend.size() == 1000) {
                correctos += sendMultiMessage(notification, devicesSend, tokens);

                tokens = new ArrayList<String>();
                devicesSend = new ArrayList<Device>();
            }

        }

        if (devicesSend.size() > 0) {
            correctos += sendMultiMessage(notification, devicesSend, tokens);
        }
        
        return correctos;
    }

    private boolean checkResult(Result result, Device device) {
        if (result.getMessageId() != null) {
            // Update token if is different
            if (result.getCanonicalRegistrationId() != null) {
                device.setToken(result.getCanonicalRegistrationId());
                if (dao != null)
                    dao.save(device);
            }

            return true;

        } else {
            // Remove device if not registered
            if (result.getErrorCodeName().equals(Constants.ERROR_NOT_REGISTERED)) {
                if (dao != null)
                    dao.remove(device);
            } else {
                logger.error("Error sending push to device: " + result.getErrorCodeName());
            }

            return false;
        }
    }

    private int sendMultiMessage(Notification notification, List<Device> devices, List<String> tokens) {
        String collapseKey = getCollapseKey(notification);

        Message message = new Message.Builder()
                        .collapseKey(collapseKey)
                        .addData("title",
                                        notification.getTitle().length() > 50 ? notification.getTitle().substring(1,
                                                        50) : notification.getTitle()).addData("message", notification.getMessage())
                        .build();

        try {
            Sender sender = new Sender();
            MulticastResult results = sender.send(message, tokens, 5);
            int count = 0;
            int corrects = 0;
            for (Result result : results.getResults()) {
                if (checkResult(result, devices.get(count))) {
                    corrects++;
                }
                count++;
            }

            return corrects;

        } catch (Exception e) {
            logger.error("Unexpected error sending push to Android", e);
        }

        return 0;
    }

    private String getCollapseKey(Notification notification) {
        return String.valueOf(notification.getTitle());
    }
}
