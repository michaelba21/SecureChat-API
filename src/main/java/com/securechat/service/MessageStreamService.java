package com.securechat.service;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MessageStreamService {

    // Connection timeout: 30 minutes (allows long-polling for real-time updates)
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L; 

    // Thread-safe storage: room ID -> list of active SSE connections for that room
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();
    
    // Factory pattern for creating emitters (allows easier testing/mocking)
    private final EmitterFactory emitterFactory = new EmitterFactory();

    /**
     * Subscribe a client to receive real-time updates for a specific chat room.
     * Creates an SSE connection that will receive events when messages are published.
     */
    public SseEmitter subscribe(UUID roomId) {
        // Create new SSE emitter with 30-minute timeout
        SseEmitter emitter = emitterFactory.createEmitter(DEFAULT_TIMEOUT);
        
        // Get or create the list of emitters for this room (thread-safe)
        CopyOnWriteArrayList<SseEmitter> emitters = roomEmitters.computeIfAbsent(roomId, 
            id -> new CopyOnWriteArrayList<>());
        
        // Add new emitter to the room's subscriber list
        emitters.add(emitter);

        // Cleanup callbacks: automatically remove emitter when connection ends
        emitter.onCompletion(() -> emitters.remove(emitter));     // Client closes connection
        emitter.onTimeout(() -> emitters.remove(emitter));      
        emitter.onError(e -> emitters.remove(emitter));           // Error occurs

        return emitter;  // Return emitter to client (Spring handles HTTP streaming)
    }

    /**
     * Broadcast an event to all clients subscribed to a specific chat room.
     * Used to notify clients of new messages, updates, or deletions.
     */
    public void publish(UUID roomId, String eventName, Object payload) {
        // Get all active emitters (SSE connections) for this room
        List<SseEmitter> emitters = roomEmitters.get(roomId);
        
        // Early exit if no subscribers for this room
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        // Collect failed emitters to remove after iteration
        // Avoids ConcurrentModificationException by not removing during iteration
        List<SseEmitter> failedEmitters = new ArrayList<>();
        
        // Send event to each connected client
        for (SseEmitter emitter : emitters) {
            try {
                // Send SSE event with name and JSON payload
                emitter.send(SseEmitter.event()
                    .name(eventName)           // Event type (e.g., "new-message")
                    .data(payload, MediaType.APPLICATION_JSON));  // JSON data
            } catch (IOException ex) {
                // Client disconnected or network error
                emitter.complete();            // Cleanly close the emitter
                failedEmitters.add(emitter);  
            }
        }
        
        // Remove failed emitters after iteration (thread-safe operation)
        for (SseEmitter failedEmitter : failedEmitters) {
            emitters.remove(failedEmitter);
        }
    }

    // Package-private getter for testing (allows mocking emitter factory)
    EmitterFactory getEmitterFactory() {
        return emitterFactory;
    }

    /**
     * Inner factory class for creating SseEmitter instances.
     * Enables testability by allowing mock emitters in unit tests.
     */
    static class EmitterFactory {
        // Creates a new SseEmitter with the specified timeout
        SseEmitter createEmitter(long timeout) {
            return new SseEmitter(timeout);
        }
    }
} 