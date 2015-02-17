package es.tor.push;

import java.util.List;

import org.apache.log4j.Logger;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.PayloadBuilder;

import es.tor.dao.DeviceDao;
import es.tor.entity.Device;
import es.tor.entity.Notification;

public class ApplePush {
    private final static String APPLE_CERT = "ApplePush.p12";
    private final static String APPLE_CERT_DEV = "ApplePushDev.p12";
    private final static String PASSWORD_APPLE_CERT = "";

    protected static Logger logger = Logger.getLogger(ApplePush.class);

    protected DeviceDao deviceDao;

    public ApplePush(DeviceDao deviceDao) {
        this.deviceDao = deviceDao;
    }

    public long send(Notification notification, List<Device> devices, boolean isProduction) {
        long correctos = 0;

        ApnsService service;
        try {
            if (isProduction) {
                service = APNS.newService()
                                .withCert(getClass().getResourceAsStream("/" + APPLE_CERT), PASSWORD_APPLE_CERT)
                                .withProductionDestination().build();
            } else {
                service = APNS.newService()
                                .withCert(getClass().getResourceAsStream("/" + APPLE_CERT_DEV), PASSWORD_APPLE_CERT)
                                .withSandboxDestination().build();
            }
        } catch (Exception e) {
            logger.error("Error reading iOS push certificate", e);
            return 0;
        }

        service.start();

        PayloadBuilder payloadBuilder = APNS.newPayload()
                        .alertBody(notification.getTitle() + ": " + notification.getMessage())
                        .customField("title", notification.getTitle())
                        .customField("message", notification.getMessage());
        String payload = payloadBuilder.build();

        for (Device device : devices) {
            if (device.getType().equals(Device.ANDROID))
                continue;

            try {
                ApnsNotification apnsNotification = service.push(device.getToken(), payload);
                if (apnsNotification != null && apnsNotification.getIdentifier() > 0) {
                    correctos++;
                } else {
                    logger.error("Error sending to token: " + device.getToken());
                }
            } catch (Exception e) {
                logger.error("Unexpected error sending iOS push", e);
            }
        }

        try {
            service.stop();
        } catch (Exception e) {
            logger.error("Unexpected error closing iOS push service", e);
        }

        return correctos;
    }
}
