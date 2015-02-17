package es.tor.entity;

import java.io.Serializable;

public class Device implements Serializable {
    protected static final long serialVersionUID = 1L;

    public static final Integer APPLE = 1;
    public static final Integer ANDROID = 2;

    protected Long id;
    protected Integer type;
    protected String token;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
