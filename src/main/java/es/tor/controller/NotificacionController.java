package es.tor.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import es.tor.dao.DeviceDao;
import es.tor.dao.NotificationDao;
import es.tor.entity.Device;
import es.tor.entity.Notification;
import es.tor.push.AndroidPush;
import es.tor.push.ApplePush;

@Controller
@RequestMapping(value = "/notification")
public class NotificacionController extends GenericController {
    @Autowired
    protected NotificationDao notificationDao;

    @Autowired
    protected DeviceDao deviceDao;

    @RequestMapping(value = "/testAndroid", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String testAndroid(HttpServletRequest request) {
        Device iphone = new Device();
        iphone.setId(1000L);
        iphone.setType(Device.ANDROID);
        iphone.setToken("");

        List<Device> devices = new ArrayList<Device>();
        devices.add(iphone);

        Notification notification = new Notification();
        notification.setTitle("Android Test");
        notification.setMessage("Testing Android push notifications");
        
        AndroidPush androidPush = new AndroidPush(deviceDao);
        if (androidPush.send(notification, devices) == 1) {
            return "It works!!";
        } else {
            return "ERROR";
        }
    }
    
    @RequestMapping(value = "/testApple", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String testApple(HttpServletRequest request) {
        Device iphone = new Device();
        iphone.setId(1000L);
        iphone.setType(Device.APPLE);
        iphone.setToken("");

        List<Device> devices = new ArrayList<Device>();
        devices.add(iphone);

        Notification notification = new Notification();
        notification.setTitle("Apple Test");
        notification.setMessage("Testing Apple push notifications");
        
        ApplePush applePush = new ApplePush(deviceDao);
        if (applePush.send(notification, devices, false) == 1) {
            return "It works!!";
        } else {
            return "ERROR";
        }
    }
}