package example.micronaut;

import com.agent.ClassHotSwapInterface;
import com.agent.ClassStoreInterface;
import com.agent.MicronautApplicationInterface;
import io.micronaut.runtime.Micronaut;

public class Application implements MicronautApplicationInterface {

    //TODO move this to the bean logic of micronaut
    public static ClassStoreInterface classStore;
    public static ClassHotSwapInterface hotSwap;

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @Override
    public void start(ClassStoreInterface classStoreInterface, ClassHotSwapInterface classHotSwaper) {
        hotSwap = classHotSwaper;
        classStore = classStoreInterface;
        main(new String[]{});
    }
}