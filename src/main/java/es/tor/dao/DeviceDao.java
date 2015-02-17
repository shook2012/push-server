package es.tor.dao;

import es.tor.entity.Device;

public class DeviceDao {
    public Device findByToken(String token) {
        return null;
    }
    
    public Device findById(Long id) {
        return null;
    }
    
    public Device save(Device device) {
        if (device.getIdDevice() == null || device.getIdDevice().equals(0)) {
            device.setIdDevice(1L);
        }
        return device;
    }
    
    public void remove(Device device) {
        
    }
}