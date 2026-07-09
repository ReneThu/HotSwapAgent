package micronaut;

import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnError;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;

/**
 * Bridges a browser xterm.js terminal to the PTY-wrapped gdb session.
 *
 * <p>Opening the socket spawns gdb against the slowdebug JVM (this is how the demo is "started" from
 * the page); PTY output is streamed back as binary frames so escape sequences survive intact;
 * incoming text frames are the presenter's keystrokes and are written straight to the PTY. Closing
 * the socket (the page's Stop button) kills gdb and the debuggee JVM.
 */
@ServerWebSocket("/gdb/ws")
public class GdbWebSocket {

    private final GdbSessionManager manager;

    public GdbWebSocket(GdbSessionManager manager) {
        this.manager = manager;
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
        manager.start(bytes -> {
            if (session.isOpen()) {
                session.sendAsync(bytes);
            }
        });
    }

    @OnMessage
    public void onMessage(String message) {
        manager.write(message);
    }

    @OnClose
    public void onClose() {
        manager.stop();
    }

    @OnError
    public void onError() {
        manager.stop();
    }
}
