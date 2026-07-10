package micronaut;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

@Controller()
public class ClassController {

    private final DemoProcessManager demo;
    private final DemoNavState nav;
    private final DemoCodeState code;
    private final DemoScrollState scroll;

    public ClassController(DemoProcessManager demo, DemoNavState nav, DemoCodeState code, DemoScrollState scroll) {
        this.demo = demo;
        this.nav = nav;
        this.code = code;
        this.scroll = scroll;
    }

    @Get("/start")
    public HttpResponse<?> startCodeExecution() {
        nav.clear();
        code.clear();
        scroll.clear();
        boolean started = demo.start();
        return started ? HttpResponse.ok() : HttpResponse.status(HttpStatus.CONFLICT);
    }

    @Get("/stop")
    public HttpResponse<?> stopCodeExecution() {
        demo.stop();
        nav.clear();
        code.clear();
        scroll.clear();
        return HttpResponse.ok();
    }

    @Get("/output")
    @Produces(MediaType.TEXT_PLAIN)
    public String getOutput() {
        return demo.output();
    }

    @Get("/status")
    @Produces(MediaType.TEXT_PLAIN)
    public String getStatus() {
        return demo.isRunning() ? "running" : "stopped";
    }

    @Get("/demo/nav")
    @Produces(MediaType.TEXT_PLAIN)
    public String getNav() {
        return nav.get();
    }

    @Post("/demo/nav")
    @Consumes(MediaType.TEXT_PLAIN)
    public HttpResponse<?> setNav(@Body String path) {
        String normalized = path == null ? "" : path.trim();
        // A new page means a fresh code buffer: drop any edits carried over from the previous page so
        // the freshly loaded page starts from the server's decompiled code, not a stale edit.
        if (!normalized.equals(nav.get())) {
            code.clear();
            scroll.clear();
        }
        nav.set(path);
        return HttpResponse.ok();
    }

    @Get("/demo/code")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCode() {
        return code.get();
    }

    @Post("/demo/code")
    @Consumes(MediaType.TEXT_PLAIN)
    public HttpResponse<?> setCode(@Body String source) {
        code.set(source);
        return HttpResponse.ok();
    }

    @Get("/demo/scroll")
    @Produces(MediaType.TEXT_PLAIN)
    public String getScroll() {
        return scroll.get();
    }

    @Post("/demo/scroll")
    @Consumes(MediaType.TEXT_PLAIN)
    public HttpResponse<?> setScroll(@Body String offsets) {
        scroll.set(offsets);
        return HttpResponse.ok();
    }
}
