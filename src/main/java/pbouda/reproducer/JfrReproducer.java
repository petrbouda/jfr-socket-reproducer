package pbouda.reproducer;

import jdk.jfr.Configuration;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingStream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JfrReproducer {

    private static final String CONFIGURATION =
            "<configuration version=\"2.0\">\n" +
            "    <event name=\"jdk.NativeMethodSample\">\n" +
            "        <setting name=\"enabled\">true</setting>\n" +
//            "        <setting name=\"enabled\">false</setting>\n" +
            "        <setting name=\"period\">20 ms</setting>\n" +
            "    </event>\n" +
            "    <event name=\"jdk.SocketRead\">\n" +
            "        <setting name=\"enabled\">true</setting>\n" +
            "        <setting name=\"stackTrace\">true</setting>\n" +
            "        <setting name=\"threshold\">0 s</setting>\n" +
            "    </event>\n" +
//            "   <event name=\"jdk.ThreadSleep\">\n" +
//            "        <setting name=\"enabled\">true</setting>\n" +
//            "        <setting name=\"stackTrace\">true</setting>\n" +
//            "        <setting name=\"threshold\">10 ms</setting>\n" +
//            "    </event>" +
            "</configuration>";

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        Path configuration = Files.createTempFile(null, ".xml");
        Files.write(configuration, CONFIGURATION.getBytes());

        Configuration config = Configuration.create(configuration);
        config.getSettings().forEach((key, value) -> System.out.println(key + ": " + value));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (EventStream es = new RecordingStream(config)) {
                // es.onEvent("jdk.ThreadSleep", System.out::println);
                es.onEvent("jdk.SocketRead", e -> {
                    // ClassCastException -> 'jdk.jfr.consumer.RecordedObject' instead of 'jdk.jfr.consumer.RecordedMethod'
                    // System.out.println(e);

                    // => jdk.NativeMethodSample ENABLED
                    // class jdk.jfr.consumer.RecordedObject {
                    //     type = java.net.Socket$SocketInputStream (classLoader = bootstrap)
                    //     name = "read"
                    //     descriptor = "([BII)I"
                    //     modifiers = 1
                    //     hidden = false
                    // }

                    // => jdk.NativeMethodSample DISABLED
                    // class jdk.jfr.consumer.RecordedMethod {
                    //     type = java.net.Socket$SocketInputStream (classLoader = bootstrap)
                    //     name = "read"
                    //     descriptor = "([BII)I"
                    //     modifiers = 1
                    //     hidden = false
                    // }
                    RecordedFrame frame = e.getStackTrace().getFrames().get(0);
                    Object method = frame.getValue("method");
                    System.out.println(method.getClass());
                });
                // es.onEvent("jdk.SocketWrite", System.out::println);
                es.start();
            }
        });

        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try (ServerSocket ss = new ServerSocket(5000)) {
                while (true) {
                    Socket socket = ss.accept();
                    System.out.println("A new client is connected : " + socket);
                    new ClientHandler(socket).start();
                }
            }
        });

        Thread.sleep(2000);

        try (var soc = new Socket("localhost", 5000);
             var dis = new DataInputStream(soc.getInputStream())) {

            while (true) {
                String received = dis.readUTF();
                System.out.println(received);
            }
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); socket) {
                while (true) {
                    Thread.sleep(1000);
                    dos.writeUTF("my-message");
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
