package org.acme.websockets;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class ChatMessage {

    private String id;
    private String username;
    private String supportname;
    private String message;
    private Date timestamp;

    public ChatMessage(String username, String supportname, String message) {
        this.username = username;
        this.supportname = supportname;
        this.message = message;
        this.id = username + "-" + supportname;
        this.timestamp = Date.from(Instant.now());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSupportname() {
        return supportname;
    }

    public void setSupportname(String supportname) {
        this.supportname = supportname;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(username, that.username) && Objects.equals(supportname, that.supportname) && Objects.equals(message, that.message) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, supportname, message, timestamp);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "username='" + username + '\'' +
                ", supportname='" + supportname + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
