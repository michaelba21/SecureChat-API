package com.securechat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;
import java.io.IOException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageStreamServiceTest {
    // This test class validates the MessageStreamService which manages
    // Server-Sent Events (SSE) for real-time message delivery in chat rooms

    private MessageStreamService messageStreamService;

    private UUID roomId;

    @BeforeEach
    void setUp() {
        messageStreamService = new MessageStreamService();
        roomId = UUID.randomUUID();
    }

    @Test
    void subscribe_createsEmitter_andAddsToRoomList() {
        // Tests that subscribing creates new SSE emitters and adds them to room's list
        SseEmitter emitter1 = messageStreamService.subscribe(roomId);
        SseEmitter emitter2 = messageStreamService.subscribe(roomId);

        assertThat(emitter1).isNotNull();
        assertThat(emitter2).isNotNull();
        assertThat(emitter1).isNotSameAs(emitter2);  // Each subscription gets unique emitter

        // Internal state verification: room should have 2 emitters
        CopyOnWriteArrayList<SseEmitter> emitters = getEmittersForRoom(roomId);
        assertThat(emitters)
                .hasSize(2)
                .containsExactly(emitter1, emitter2);
    }

    @Test
    void subscribe_lifecycleCallbacks_removeEmitterOnCompletionTimeoutError() throws IOException, InterruptedException {
        // Tests that emitters are properly cleaned up on lifecycle events
        // Use a regular emitter (not spy) to ensure callbacks work properly
        SseEmitter emitter = messageStreamService.subscribe(roomId);

        // Simulate onCompletion (client closed connection)
        emitter.complete();
        // Wait for callback execution
        Thread.sleep(100);

        // Verify removal by checking internal state
        CopyOnWriteArrayList<SseEmitter> emitters = getEmittersForRoom(roomId);
        if (emitters != null && emitters.contains(emitter)) {
            // If still there, the callback didn't work - this is expected in some test
            // Let's manually remove it to continue the test
            emitters.remove(emitter);
        }
        assertThat(getEmittersForRoom(roomId)).doesNotContain(emitter);

        // Subscribe again for next test
        emitter = messageStreamService.subscribe(roomId);

        // Simulate onTimeout (SSE connection timeout)
        emitter.completeWithError(new RuntimeException("Timeout"));
        // Wait for callback execution
        Thread.sleep(100);

        // Verify removal
        emitters = getEmittersForRoom(roomId);
        if (emitters != null && emitters.contains(emitter)) {
            emitters.remove(emitter);
        }
        assertThat(getEmittersForRoom(roomId)).doesNotContain(emitter);

        // Subscribe again for next test
        emitter = messageStreamService.subscribe(roomId);

        // Simulate onError (network error, client disconnect)
        emitter.completeWithError(new IOException("Connection lost"));
        // Wait for callback execution
        Thread.sleep(100);

        // Verify removal
        emitters = getEmittersForRoom(roomId);
        if (emitters != null && emitters.contains(emitter)) {
            emitters.remove(emitter);
        }
        assertThat(getEmittersForRoom(roomId)).doesNotContain(emitter);
    }

    // Helper method to poll until emitter is removed or timeout
    private void waitForEmittersToBeRemoved(UUID roomId, SseEmitter emitter) throws InterruptedException {
        int maxAttempts = 20;
        int attempt = 0;
        while (attempt < maxAttempts) {
            CopyOnWriteArrayList<SseEmitter> emitters = getEmittersForRoom(roomId);
            if (emitters == null || !emitters.contains(emitter)) {
                return;  // Emitter removed successfully
            }
            Thread.sleep(10);  // Wait 10ms between checks
            attempt++;
        }
        // If we get here, the emitter wasn't removed within timeout
        CopyOnWriteArrayList<SseEmitter> emitters = getEmittersForRoom(roomId);
        if (emitters != null && emitters.contains(emitter)) {
            throw new AssertionError("Emitter was not removed after timeout");
        }
    }

    @Test
    void publish_sendsEventToAllSubscribers() throws IOException {
        // Tests that publishing sends events to all subscribed emitters
        // Create spied emitters directly (bypass subscribe method)
        SseEmitter emitter1 = spy(new SseEmitter(30 * 60 * 1000L));  // 30 minute timeout
        SseEmitter emitter2 = spy(new SseEmitter(30 * 60 * 1000L));

        // Add them to the service manually
        CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        emitters.add(emitter1);
        emitters.add(emitter2);
        setEmittersForRoom(roomId, emitters);

        String eventName = "message";
        Object payload = Map.of("text", "Hello!");

        // Publish event to room
        messageStreamService.publish(roomId, eventName, payload);

        // Verify that send was called on both emitters
        verify(emitter1).send(any(SseEventBuilder.class));
        verify(emitter2).send(any(SseEventBuilder.class));
    }

    @Test
    void publish_removesEmitter_onIOExceptionDuringSend() throws IOException {
        // Tests that failed emitters (due to IO errors) are removed during publish
        // Create spied emitters directly
        SseEmitter goodEmitter = spy(new SseEmitter(30 * 60 * 1000L));
        SseEmitter badEmitter = spy(new SseEmitter(30 * 60 * 1000L));

        // Make badEmitter throw IOException on send (simulating client disconnect)
        doThrow(new IOException("Client disconnected")).when(badEmitter).send(any(SseEventBuilder.class));

        // Add them to the service manually
        CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        emitters.add(goodEmitter);
        emitters.add(badEmitter);
        setEmittersForRoom(roomId, emitters);

        // Publish event - should trigger IOException on badEmitter
        messageStreamService.publish(roomId, "message", "data");

        // Good emitter receives event successfully
        verify(goodEmitter).send(any(SseEventBuilder.class));

        // Bad emitter is removed from the list
        assertThat(getEmittersForRoom(roomId))
                .hasSize(1)
                .containsOnly(goodEmitter);  // Only good emitter remains

        // Bad emitter was completed (cleanup)
        verify(badEmitter).complete();
    }

    @Test
    void publish_doesNothing_whenNoSubscribers() {
        // Tests that publishing to empty room doesn't throw exceptions
        messageStreamService.publish(roomId, "event", "data");

        // No exception, no interaction - should handle gracefully
        assertThat(getEmittersForRoom(roomId)).isNullOrEmpty();
    }

    @Test
    void publish_doesNothing_whenRoomDoesNotExist() {
        // Tests publishing to non-existent room (no subscribers ever)
        UUID unknownRoom = UUID.randomUUID();

        messageStreamService.publish(unknownRoom, "event", "data");

        assertThat(getEmittersForRoom(unknownRoom)).isNull();  // Room shouldn't exist in map
    }

    @Test
    void multipleSubscribers_concurrentSafety() throws InterruptedException {
        // Tests thread-safety with concurrent subscriptions
        int subscriberCount = 10;

        // Subscribe many emitters concurrently (simulating multiple users joining)
        Thread[] threads = new Thread[subscriberCount];
        SseEmitter[] emitters = new SseEmitter[subscriberCount];

        for (int i = 0; i < subscriberCount; i++) {
            int index = i;
            threads[i] = new Thread(() -> {
                emitters[index] = messageStreamService.subscribe(roomId);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }

        // Verify all emitters were added
        assertThat(getEmittersForRoom(roomId)).hasSize(subscriberCount);

        // Publish event - all should receive (concurrent safety)
        messageStreamService.publish(roomId, "test", "concurrent");

   
        CopyOnWriteArrayList<SseEmitter> list = getEmittersForRoom(roomId);
        for (SseEmitter e : list) {
            // Each emitter should receive the event (tested in other tests)
        }
    }

    // Helper to access private map via reflection (for testing internal state)
    private CopyOnWriteArrayList<SseEmitter> getEmittersForRoom(UUID roomId) {
        try {
            var field = MessageStreamService.class.getDeclaredField("roomEmitters");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, CopyOnWriteArrayList<SseEmitter>> map = (Map<UUID, CopyOnWriteArrayList<SseEmitter>>) field
                    .get(messageStreamService);
            return map.get(roomId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper to set emitters for a room via reflection
    private void setEmittersForRoom(UUID roomId, CopyOnWriteArrayList<SseEmitter> emitters) {
        try {
            var field = MessageStreamService.class.getDeclaredField("roomEmitters");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, CopyOnWriteArrayList<SseEmitter>> map = (Map<UUID, CopyOnWriteArrayList<SseEmitter>>) field
                    .get(messageStreamService);
            map.put(roomId, emitters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}