package example.micronaut;

import com.agent.TransientCLassInterface;
import example.micronaut.dtos.CodeElementDto;
import example.micronaut.dtos.CodeUpdateDto;
import example.micronaut.dtos.JavaClassDto;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.views.View;

import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

@Controller("/class")
public class ClassController {

    @Get("/list")
    @View("classView")
    @Produces(MediaType.TEXT_HTML)
    public Map<String, List<JavaClassDto>> list() {
        List<JavaClassDto> elemets = Application.classStore.getAllCollectedClasses().stream().map(JavaClassDto::new).toList();
        return Collections.singletonMap("classDtos", elemets);
    }

    @Get("/show/{className}")
    @View("codeElementView")
    @Produces(MediaType.TEXT_HTML)
    public Map<String, CodeElementDto> showDecompiledCode(String className) throws IOException {
        Optional<String> code = Application.classStore.getDecompiledClassAsString(classNameToInternal(className));

        if (code.isEmpty()) {
            //TODO return 404 in this case
            throw new RuntimeException("CLass not found.");
        }

        CodeElementDto codeElementDto = new CodeElementDto(classNameToJavaFormate(className), code.get());
        return Collections.singletonMap("codeElement", codeElementDto);
    }

    @Put("/hot-swap/{className}")
    public HttpResponse<?> update(@PathVariable String className, @Body CodeUpdateDto code) throws UnmodifiableClassException, ClassNotFoundException, InterruptedException {
        TransientCLassInterface transiendtCLassInterface = Application.hotSwap.hotSwap(classNameToInternal(className), code.getCode());
        transiendtCLassInterface.waitUntilReloaded();
        return HttpResponse.ok();
    }

    private static String classNameToJavaFormate(String className) {
        return className.replace("-", ".").replace("/", ".");
    }

    private static String classNameToInternal(String className) {
        return className.replace("-", "/").replace(".", "/");
    }
}
