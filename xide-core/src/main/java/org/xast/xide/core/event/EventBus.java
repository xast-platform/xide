package org.xast.xide.core.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private static EventBus instance;
    
    private final Map<Class<?>, List<Consumer<Object>>> subscribers;
    
    private EventBus() {
        this.subscribers = new HashMap<>();
    }
    
    public static EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribers
            .computeIfAbsent(eventType, k -> new ArrayList<>())
            .add((Consumer<Object>) handler);
    }
    
    public <T> void publish(T event) {
        Class<?> eventType = event.getClass();
        List<Consumer<Object>> handlers = subscribers.get(eventType);
        
        if (handlers != null) {
            for (Consumer<Object> handler : handlers) {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    System.err.println("Error handling event " + eventType.getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer<Object>> handlers = subscribers.get(eventType);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }
    
    public void clear() {
        subscribers.clear();
    }
}
