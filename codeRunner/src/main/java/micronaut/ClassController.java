package micronaut;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller()
public class ClassController {
    private CodeExcecutionElement codeExcecutionElement = null;

    @Get("/start")
    public HttpResponse<?> startCodeExecution() {
        if (codeExcecutionElement != null) {
            return HttpResponse.status(HttpStatus.CONFLICT);
        }

        this.codeExcecutionElement = new CodeExcecutionElement("/home/marco/Documents/Development/techEvangelistGeneric/HotSwapAgentV2/gradlew", ":runMain"); //TODO this is hardcoded.
        this.codeExcecutionElement.run();
        return HttpResponse.ok();
    }

    @Get("/stop")
    public HttpResponse<?> stopCodeExecution() {
        this.codeExcecutionElement.stopTask();

        return HttpResponse.ok();
    }

    @Get("/output")
    @Produces(MediaType.TEXT_PLAIN)
    public String getOutput() {
        List<String> output =  this.codeExcecutionElement.getRingBufferContents();

        return output.stream().collect(Collectors.joining("\n"));
    }
}
