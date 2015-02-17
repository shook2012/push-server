package es.tor.dao;

import java.util.ArrayList;
import java.util.List;

import es.tor.entity.Device;

public class DeviceDao {
    private static List<Device> devices = new ArrayList<Device>();
    private static long nextId = 0;
    
    public Device findByToken(String token) {
        for (Device device: devices) {
            if (device.getToken().equals(token)) {
                return device;
            }
        }
        return null;
    }
    
    public Device findById(Long id) {
        for (Device device: devices) {
            if (device.getId().equals(id)) {
                return device;
            }
        }
        return null;
    }
    
    public Device save(Device device) {
        devices.add(device);
        if (device.getId() == null || device.getId().equals(0)) {
            device.setId(++nextId);
        }
        return device;
    }
    
    public void remove(Device device) {
        devices.remove(device);
    }
}