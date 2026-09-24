package ru.itmo.soa.vehicle.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.time.Instant;

@XmlRootElement(name = "Error")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Error", propOrder = {"timestamp", "status", "error", "message", "path"})
public class Error {

    @XmlElement
    private String timestamp;

    @XmlElement
    private Integer status;

    @XmlElement
    private String error;

    @XmlElement
    private String message;

    @XmlElement
    private String path;

    public Error() {
    }

    public Error(Integer status, String error, String message, String path) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
