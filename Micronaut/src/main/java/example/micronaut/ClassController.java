package example.micronaut;

import example.micronaut.dtos.JavaClassDto;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.views.View;

import java.io.IOException;
import java.lang.instrument.UnmodifiableClassException;
import java.util.List;
import java.util.Optional;

@Controller("/class")
public class ClassController {

    @Get("/list")
//    @View("classView")
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<JavaClassDto> list() {
        return Application.classStore.getAllCollectedClasses().stream().map(JavaClassDto::new).toList();
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
