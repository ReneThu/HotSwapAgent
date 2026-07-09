package micronaut;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

@Controller()
public class ClassController {

    private final DemoProcessManager demo;

    public ClassController(DemoProcessManager demo) {
        this.demo = demo;
    }

    @Get("/start")
    public HttpResponse<?> startCodeExecution() {
        boolean started = demo.start();
        return started ? HttpResponse.ok() : HttpResponse.status(HttpStatus.CONFLICT);
    }

    @Get("/stop")
    public HttpResponse<?> stopCodeExecution() {
        demo.stop();
        return HttpResponse.ok();
    }

    @Get("/output")
    @Produces(MediaType.TEXT_PLAIN)
    public String getOutput() {
        return demo.output();
    }
}
