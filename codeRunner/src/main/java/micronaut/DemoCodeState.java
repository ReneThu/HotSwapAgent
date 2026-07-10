package micronaut;

import jakarta.inject.Singleton;

/**
 * Holds the demo app's currently edited code-editor contents so they can be mirrored live across
 * every browser window watching slide 3 (presenter + projected audience in Slidev presenter mode).
 *
 * <p>The demo app (served on :8080) is cross-origin to the deck, so a slide window cannot read or
 * write what another window has typed into the decompiled-code textarea. Instead each code page
 * publishes its edits here and polls for the others'; the window actually being typed in is never
 * overwritten. The value is reset whenever a run starts or stops, and whenever the demo navigates to
 * a different page, so a stale edit buffer is never replayed onto a freshly loaded page.
 */
@Singleton
public class DemoCodeState {

    private volatile String code = "";

    public String get() {
        return code;
    }

    public void set(String code) {
        this.code = code == null ? "" : code;
    }

    public void clear() {
        this.code = "";
    }
}
