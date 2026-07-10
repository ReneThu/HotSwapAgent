package micronaut;

import jakarta.inject.Singleton;

/**
 * Holds the demo app's currently displayed page path so it can be mirrored across every browser
 * window watching slide 3 (presenter + projected audience in Slidev presenter mode).
 *
 * <p>The demo app (served on :8080) is cross-origin to the deck, so a slide window cannot read what
 * page its iframe has navigated to. Instead each demo page reports its own path here; the other
 * windows poll it and follow. The value is reset whenever a demo run starts or stops so a stale path
 * from a previous run is never replayed into a freshly booted app.
 */
@Singleton
public class DemoNavState {

    private volatile String path = "";

    public String get() {
        return path;
    }

    public void set(String path) {
        this.path = path == null ? "" : path.trim();
    }

    public void clear() {
        this.path = "";
    }
}
