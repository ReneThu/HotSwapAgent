package micronaut;

import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Serves the xterm.js terminal page that drives the live gdb debugging demo.
 *
 * <p>The page is a full-screen terminal (no on-screen buttons) that connects back to
 * {@link GdbWebSocket} over a WebSocket. It is embedded in the presentation via an iframe so the
 * whole demo runs from within the deck. The HTML lives as a classpath resource to keep it out of
 * Java string literals.
 */
@Controller
public class GdbController {

    private final String terminalPage;

    public GdbController(ResourceResolver resourceResolver) {
        Optional<InputStream> resource = resourceResolver.getResourceAsStream("classpath:gdb-terminal.html");
        if (resource.isEmpty()) {
            throw new IllegalStateException("gdb-terminal.html not found on the classpath");
        }
        try (InputStream in = resource.get()) {
            this.terminalPage = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read gdb-terminal.html", e);
        }
    }

    @Get("/gdb")
    @Produces(MediaType.TEXT_HTML)
    public String terminalPage() {
        return terminalPage;
    }
}
