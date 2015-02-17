package es.tor.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import es.tor.dao.DeviceDao;
import es.tor.entity.Device;

@Controller
@RequestMapping(value = "/device")
public class DeviceController extends GenericController {
    @Autowired
    private DeviceDao deviceDao;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public Long register(@ModelAttribute Device device) {
        Device deviceActual;
        if (device.getId() != null && device.getId() > 0) {
            deviceActual = deviceDao.findById(device.getId());
        } else {
            deviceActual = deviceDao.findByToken(device.getToken());
        }

        try {
            if (deviceActual != null) {
                deviceActual.setToken(device.getToken());
                device = deviceDao.save(deviceActual);
            }

        } catch (Exception e) {
            logger.error("Device was already registered", e);
        }

        return device.getId();
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public void desregistrar(@ModelAttribute Device device, HttpServletResponse response) {
        deviceDao.remove(device);
        response.setStatus(200);
        response.setContentType("text/plain");
        response.setContentLength(0);
    }
}