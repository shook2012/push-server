package es.tor.entity;

import java.io.Serializable;

public class Device implements Serializable {
    protected static final long serialVersionUID = 1L;

    public static final Integer APPLE = 1;
    public static final Integer ANDROID = 2;

    protected Long idDevice;
    protected Integer type;
    protected String token;

    public Long getIdDevice() {
        return idDevice;
    }

    public void setIdDevice(Long idDevice) {
        this.idDevice = idDevice;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        if (type == null) {
            this.type = Device.ANDROID;
        } else {
            this.type = type;
        }
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
