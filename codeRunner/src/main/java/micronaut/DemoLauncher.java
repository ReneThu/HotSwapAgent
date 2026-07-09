package micronaut;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;

/**
 * Builds the {@link CodeExecutionElement} used to launch the demo application.
 *
 * <p>The command runs the top-level {@code :runMain} Gradle task with a hardcoded JDK so the
 * presentation does not depend on the ambient environment. {@code --no-daemon} keeps the process
 * tree self-contained, which lets {@link CodeExecutionElement#stopTask()} tear everything down
 * deterministically.
 *
 * <p>Tests substitute this bean to launch a lightweight process instead of Gradle.
 */
@Singleton
public class DemoLauncher {

    private final String gradlew;
    private final String task;
    private final String javaHome;

    @Inject
    public DemoLauncher(
            @Value("${demo.gradlew}") String gradlew,
            @Value("${demo.task}") String task,
            @Value("${demo.java-home}") String javaHome) {
        this.gradlew = gradlew;
        this.task = task;
        this.javaHome = javaHome;
    }

    protected DemoLauncher() {
        this.gradlew = null;
        this.task = null;
        this.javaHome = null;
    }

    public CodeExecutionElement newExecution() {
        List<String> command = List.of(
                gradlew,
                task,
                "--no-daemon",
                "-Dorg.gradle.java.home=" + javaHome);
        Map<String, String> environment = Map.of("JAVA_HOME", javaHome);
        return new CodeExecutionElement(command, environment);
    }
}
