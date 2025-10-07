package micronaut;

import io.micronaut.runtime.Micronaut;

public class Application {

    //http://localhost:8081/start
    //http://localhost:8081/stop
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}