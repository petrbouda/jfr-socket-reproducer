# jfr-socket-reproducer

Reproducer for JFR Streaming in Java 14
- it looks like combination of `jdk.SocketRead` and `jdk.NativeMethodSample` does not work when I run the program from IntelliJ Idea
- if I run it locally `java ./src/main/java/pbouda/reproducer/JfrReproducer.java` everything is OK
- however, an execution from Intellij fails if the even `jdk.NativeMethodSample` is enabled

```
java.lang.ClassCastException: class jdk.jfr.consumer.RecordedObject cannot be cast to class jdk.jfr.consumer.RecordedMethod (jdk.jfr.consumer.RecordedObject and jdk.jfr.consumer.RecordedMethod are in module jdk.jfr of loader 'bootstrap')
	at jdk.jfr/jdk.jfr.consumer.RecordedFrame.getMethod(RecordedFrame.java:99)
	at jdk.jfr/jdk.jfr.internal.tool.PrettyWriter.printJavaFrame(PrettyWriter.java:445)
	at jdk.jfr/jdk.jfr.internal.tool.PrettyWriter.printValue(PrettyWriter.java:306)
	at jdk.jfr/jdk.jfr.internal.tool.PrettyWriter.printStackTrace(PrettyWriter.java:242)
	at jdk.jfr/jdk.jfr.internal.tool.PrettyWriter.print(PrettyWriter.java:221)
	at jdk.jfr/jdk.jfr.consumer.RecordedObject.toString(RecordedObject.java:975)
	at java.base/java.lang.String.valueOf(String.java:3388)
	at java.base/java.io.PrintStream.println(PrintStream.java:1047)
	at jdk.jfr/jdk.jfr.internal.consumer.Dispatcher$EventDispatcher.offer(Dispatcher.java:52)
	at jdk.jfr/jdk.jfr.internal.consumer.Dispatcher.dispatch(Dispatcher.java:165)
	at jdk.jfr/jdk.jfr.internal.consumer.EventDirectoryStream.processOrdered(EventDirectoryStream.java:211)
	at jdk.jfr/jdk.jfr.internal.consumer.EventDirectoryStream.processRecursionSafe(EventDirectoryStream.java:139)
	at jdk.jfr/jdk.jfr.internal.consumer.EventDirectoryStream.process(EventDirectoryStream.java:97)
	at jdk.jfr/jdk.jfr.internal.consumer.AbstractEventStream.execute(AbstractEventStream.java:243)
	at jdk.jfr/jdk.jfr.internal.consumer.AbstractEventStream$1.run(AbstractEventStream.java:265)
	at jdk.jfr/jdk.jfr.internal.consumer.AbstractEventStream$1.run(AbstractEventStream.java:262)
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:391)
	at jdk.jfr/jdk.jfr.internal.consumer.AbstractEventStream.run(AbstractEventStream.java:262)
	at jdk.jfr/jdk.jfr.internal.consumer.AbstractEventStream.start(AbstractEventStream.java:222)
	at jdk.jfr/jdk.jfr.consumer.RecordingStream.start(RecordingStream.java:344)
	at pbouda.reproducer.JfrReproducer.lambda$main$2(JfrReproducer.java:78)
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1130)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:630)
	at java.base/java.lang.Thread.run(Thread.java:832)
```

- if I disable emitting the event and run the Java Class using IntelliJ then everything is OK:

```
 <event name="jdk.NativeMethodSample">
     <setting name="enabled">false</setting>
     <setting name="period">20 ms</setting>
 </event>
```

```
jdk.SocketRead {
  startTime = 22:58:56.943
  duration = 0.0754 ms
  host = "localhost"
  address = "127.0.0.1"
  port = 5000
  timeout = 0 s
  bytesRead = 10 bytes
  endOfStream = false
  eventThread = "main" (javaThreadId = 1)
  stackTrace = [
    java.net.Socket$SocketInputStream.read(byte[], int, int) line: 68
    java.io.DataInputStream.readFully(byte[], int, int) line: 199
    java.io.DataInputStream.readUTF(DataInput) line: 613
    java.io.DataInputStream.readUTF() line: 568
    pbouda.reproducer.JfrReproducer.main(String[]) line: 99
    ...
  ]
}
```

- It looks like that the main problem is caused by printing (calling toString method) on RecordedEvent because an event 
is casted to `RecordedMethod` but it's emitted just as a `RecordedObject` class
- I tried to copy the command that is generated as the first line in a console and run it from my terminal, however, it
worked without any problem.
- It does not look like a problem with JDK

