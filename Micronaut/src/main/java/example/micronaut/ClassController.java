package example.micronaut;

import example.micronaut.dtos.JavaClassDto;
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
        //TODO replace by real code again

        List<JavaClassDto> elemets = new ArrayList<>();
        elemets.add(new JavaClassDto("Test_1"));
        elemets.add(new JavaClassDto("Test_2"));
        elemets.add(new JavaClassDto("Test_3"));

        return Collections.singletonMap("classDtos", elemets);
    }

    @Get("/show/{className}")
    @Produces(MediaType.TEXT_PLAIN)
    public String showDecompiledCode(String className) throws IOException {
        Optional<String> decompiledCLass = Application.classStore.getDecompiledClassAsString(className.replace("-", "/"));
        return decompiledCLass.orElse("Class not found!");
    }

    @Put("/hot-swap/{className}")
    public String update(@PathVariable String className) throws UnmodifiableClassException, ClassNotFoundException {
       String code =  """
                package org.example;
                
                public class Main {
                    public static void main(String[] args) throws Exception {
                        while (true) {
                            Thread.sleep(1000);
                            printHelloWorld();
                        }
                    }
                
                    public static void printHelloWorld() {
                        System.out.println("Hello, World!asdasdsad");
                    }
                }
                """;

        Application.hotSwap.hotSwap(className.replace("-", "/"), code);
        return "Received: ";
    }
}
