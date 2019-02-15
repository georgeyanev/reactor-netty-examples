package reactornettyexamples.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.time.Duration;

public class MyServerConnection {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  final Connection conn;

  public MyServerConnection(Connection conn) {
    this.conn = conn;
  }

  public void handle() {
    conn.inbound().receiveObject() // receive the object received as a result from our StringToIntegerDecoder (an Integer in this case)
        .log("MyServerConnection")
        .delayElements(Duration.ofSeconds(1)) // delay processing of the next element by 1 second
        .doOnNext(s ->
            log.info("Current received and decoded element: " + s))
        .take(5) // cancels (in facts disconnects the client) after five received elements
        .flatMap(s ->
            conn.outbound().sendString(
                Mono.just(String.format("byte count: %d", (Integer) s))
            ).then()
        )
        .subscribe(conn.disposeSubscriber()); // we have to use this core subscriber on order for the connection to get disconnected after take(5)
  }
}
