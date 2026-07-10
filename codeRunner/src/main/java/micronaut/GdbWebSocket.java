package micronaut;

import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnError;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Bridges a browser xterm.js terminal to the shared PTY-wrapped gdb session.
 *
 * <p>Every open socket <em>attaches</em> to the single gdb session (the first attach spawns gdb
 * against the slowdebug JVM; this is how the demo is "started" from the page). PTY output is streamed
 * back as binary frames so escape sequences survive intact, and because the session is shared every
 * attached window — e.g. the presenter and the projected audience view in Slidev presenter mode —
 * receives the same output. Incoming text frames are keystrokes (typed or pasted) and are written
 * straight to the shared PTY. Closing the socket detaches; when the last window detaches the session
 * is torn down (gdb and the debuggee JVM are killed), so no orphans survive the slide.
 */
@ServerWebSocket("/gdb/ws")
public class GdbWebSocket {

    private final GdbSessionManager manager;
    private final Map<String, Consumer<byte[]>> consumers = new ConcurrentHashMap<>();

    public GdbWebSocket(GdbSessionManager manager) {
        this.manager = manager;
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
        Consumer<byte[]> consumer = bytes -> {
            if (session.isOpen()) {
                session.sendAsync(bytes);
            }
        };
        consumers.put(session.getId(), consumer);
        manager.attach(consumer);
    }

    @OnMessage
    public void onMessage(String message) {
        manager.write(message);
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        detach(session);
    }

    @OnError
    public void onError(WebSocketSession session) {
        detach(session);
    }

    private void detach(WebSocketSession session) {
        Consumer<byte[]> consumer = consumers.remove(session.getId());
        if (consumer != null) {
            manager.detach(consumer);
        }
    }
}
