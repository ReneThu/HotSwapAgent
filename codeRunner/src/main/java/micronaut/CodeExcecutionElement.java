package micronaut;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CodeExcecutionElement {

    private final String[] command;
    private Process process;
    private BufferedReader reader;
    private Thread outputThread;

    private final LinkedList<String> ringBuffer = new LinkedList<>();
    private final int bufferSize = 10;
    private final Lock lock = new ReentrantLock();

    public CodeExcecutionElement(String... command) {
        this.command = command;
    }

    public void run() {
       try {
           ProcessBuilder processBuilder = new ProcessBuilder(command);
           processBuilder.redirectErrorStream(true);

           this.process = processBuilder.start();

           // Initialize the reader
           reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

           // Start a thread to read the output
           this.outputThread = new Thread(this::readOutput);
           this.outputThread.start();

       } catch (Exception e) {
           throw new RuntimeException(e);
       }
   }

    private void readOutput() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lock.lock();
                try {
                    if (ringBuffer.size() == bufferSize) {
                        ringBuffer.removeFirst();
                    }
                    ringBuffer.addLast(line);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getRingBufferContents() {
        lock.lock();
        try {
            return new ArrayList<>(ringBuffer);
        } finally {
            lock.unlock();
        }
    }

    public void stopTask() {
        try {
            if (process != null) {
                process.destroy();
            }
            if (outputThread != null) {
                outputThread.interrupt();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
