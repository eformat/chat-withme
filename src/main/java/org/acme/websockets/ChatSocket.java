package org.acme.websockets;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@ServerEndpoint("/chat/{username}")
@ApplicationScoped
public class ChatSocket {

    private final Logger log = LoggerFactory.getLogger(ChatSocket.class);

    @Inject
    @Channel("chats-emit-out")
    Emitter<ChatMessage> chats;

    Map<String, Session> userSessions = new ConcurrentHashMap<>();
    Map<String, Session> supportSessions = new ConcurrentHashMap<>();
    Map<String, String> user2support = new ConcurrentHashMap<>();
    Queue<String> idleSupport = new ConcurrentLinkedQueue<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        if (supportSessions.containsKey(username) || userSessions.containsKey(username)) {
            log.info(username + " is already connected.");
            debugState();
            return;
        }
        if (username.matches("support.*")) {
            supportSessions.put(username, session);
            if (!user2support.containsValue(username)) { // support not engaged
                if (user2support.containsValue("")) { // and we have users waiting
                    for (String key : user2support.keySet()) { // assign support to user
                        if ("".equals(user2support.get(key))) {
                            user2support.replace(key, "", username);
                            break;
                        }
                    }
                } else {
                    // more support than users
                    idleSupport.add(username);
                }
            }

        } else {
            userSessions.put(username, session);
            if (!user2support.containsValue(username) && idleSupport.isEmpty()) { // user not already added
                user2support.put(username, "");
            }
            if (!idleSupport.isEmpty()) {
                user2support.put(username, idleSupport.remove());
            }
        }
        debugState();
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        broadcast("User " + username + " left", username);
        if (username.matches("support.*")) {
            supportSessions.remove(username);
            if (user2support.containsValue(username)) {
                for (String key : user2support.keySet()) {
                    String value = user2support.get(key);
                    if (username.equals(value)) {
                        user2support.replace(key, value, ""); // unassign support to user
                    }
                }
            }
        } else {
            userSessions.remove(username);
            if (user2support.containsKey(username)) {
                user2support.remove(username); // remove user from support conversation
            }
        }
        if (idleSupport.contains(username)) {
            idleSupport.remove(username);
        }
        debugState();
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        log.error("onError", throwable);
        broadcast("User " + username + " left on error: " + throwable, username);
        if (username.matches("support.*")) {
            supportSessions.remove(username);
            if (user2support.containsValue(username)) {
                for (String key : user2support.keySet()) {
                    String value = user2support.get(key);
                    if (username.equals(value)) {
                        user2support.replace(key, value, ""); // unassign support to user
                    }
                }
            }
        } else {
            userSessions.remove(username);
            if (user2support.containsKey(username)) {
                user2support.remove(username); // remove user from support conversation
            }
        }
        if (idleSupport.contains(username)) {
            idleSupport.remove(username);
        }
        debugState();
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        if (message.equalsIgnoreCase("_ready_")) {
            broadcast("User " + username + " joined", username);
        } else {
            broadcast(">> " + username + ": " + message, username);
        }
    }

    private void broadcast(String message, String username) {
        // user -> support
        if (user2support.containsKey(username) && !user2support.get(username).equals("")) {
            supportSessions.get(user2support.get(username)).getAsyncRemote().sendObject(message, result -> {
                if (result.getException() != null) {
                    log.error("Support Unable to send message: " + result.getException());
                }
            });
            // we store messages to kafka only when user and support are connected
            ChatMessage chatMessage = new ChatMessage(username, user2support.get(username), message);
            chats.send(KafkaRecord.of(chatMessage.getId(), chatMessage));
        }
        // support -> user
        if (user2support.containsValue(username)) {
            for (String key : user2support.keySet()) {
                String value = user2support.get(key);
                if (username.equals(value)) {
                    userSessions.get(key).getAsyncRemote().sendObject(message, result -> {
                        if (result.getException() != null) {
                            log.error("User Unable to send message: " + result.getException());
                        }
                    });
                    // we store messages to kafka only when user and support are connected
                    ChatMessage chatMessage = new ChatMessage(key, username, message);
                    chats.send(KafkaRecord.of(chatMessage.getId(), chatMessage));
                }
            }
        }
        // send message to self
        if (userSessions.containsKey(username)) {
            userSessions.get(username).getAsyncRemote().sendObject(message, result -> {
                if (result.getException() != null) {
                    log.error("User Unable to send message: " + result.getException());
                }
            });
        }
        if (supportSessions.containsKey(username)) {
            supportSessions.get(username).getAsyncRemote().sendObject(message, result -> {
                if (result.getException() != null) {
                    log.error("User Unable to send message: " + result.getException());
                }
            });
        }
    }

    private void debugState() {
        for (String key : user2support.keySet()) {
            log.info(">>> [user2support] k: " + key + " v: " + user2support.get(key));
        }
        for (String key : idleSupport.stream().toList()) {
            log.info(">>> [idleSupport] k: " + key);
        }
    }

}
