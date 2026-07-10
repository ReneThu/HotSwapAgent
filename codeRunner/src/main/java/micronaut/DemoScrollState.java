package micronaut;

import jakarta.inject.Singleton;

/**
 * Holds the demo app's current scroll position (window + code textarea, JSON-encoded) so it can be
 * mirrored across every browser window watching slide 3 (presenter + projected audience in Slidev
 * presenter mode).
 *
 * <p>The demo app (served on :8080) is cross-origin to the deck, so a slide window cannot read or
 * scroll another window's iframe. Instead each demo page publishes its own scroll offsets here and
 * polls for the others'; the value is reset whenever a run starts or stops, or the demo navigates to
 * a different page, so a stale offset is never applied to an unrelated page.
 */
@Singleton
public class DemoScrollState {

    private volatile String scroll = "";

    public String get() {
        return scroll;
    }

    public void set(String scroll) {
        this.scroll = scroll == null ? "" : scroll;
    }

    public void clear() {
        this.scroll = "";
    }
}
