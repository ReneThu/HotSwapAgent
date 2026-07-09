package micronaut;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class ClassControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    DemoProcessManager demo;

    /**
     * Replace the real Gradle launcher with a lightweight, fast process so the controller's
     * start/stop state machine can be tested without building or running the demo.
     */
    @MockBean(DemoLauncher.class)
    DemoLauncher lightweightLauncher() {
        return new DemoLauncher() {
            @Override
            public CodeExecutionElement newExecution() {
                return new CodeExecutionElement(List.of("sleep", "30"));
            }
        };
    }

    @AfterEach
    void tearDown() {
        demo.stop();
    }

    @Test
    void outputIsEmptyWhenNothingRunning() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/output").accept(MediaType.TEXT_PLAIN), String.class);

        assertEquals(HttpStatus.OK, response.status());
        assertEquals("", response.body() == null ? "" : response.body());
    }

    @Test
    void stopWhenIdleIsSafe() {
        HttpResponse<?> response = client.toBlocking().exchange(HttpRequest.GET("/stop"));

        assertEquals(HttpStatus.OK, response.status());
    }

    @Test
    void secondStartWhileRunningReturnsConflict() {
        assertEquals(HttpStatus.OK, client.toBlocking().exchange(HttpRequest.GET("/start")).status());

        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/start")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void startStopStartWorks() {
        assertEquals(HttpStatus.OK, client.toBlocking().exchange(HttpRequest.GET("/start")).status());
        assertEquals(HttpStatus.OK, client.toBlocking().exchange(HttpRequest.GET("/stop")).status());
        // After a stop the controller must accept a fresh start instead of returning 409 CONFLICT.
        assertEquals(HttpStatus.OK, client.toBlocking().exchange(HttpRequest.GET("/start")).status());
    }
}
