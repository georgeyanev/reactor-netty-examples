package reactornettyexamples.tcp;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class MyTcpClient {
  private static Logger log = LoggerFactory.getLogger(MyTcpClient.class);

  public static void main(String[] args) throws InterruptedException {

    CountDownLatch latch = new CountDownLatch(1);
    StringBuilder toSend = new StringBuilder("a");

    TcpClient.create() // Prepares a TCP client for configuration.
        .port(1551) // server port
        // client should connect.
        // Configures SSL providing an already configured SslContext.
        .secure(spec -> spec
            .sslContext(SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)))
        //.wiretap()           // Applies a wire logger configuration.
        .doOnConnected(con -> {
          log.info("Client connected successfully!");

          // the next sequence of operators resulting in a Publisher<Void> never completes on its own
          // so the client stays permanently connected
          con.outbound().sendString(Mono.just(toSend.toString()))
              .then(con.inbound()
                  .receive()
                  .asString()
                  .log("tcp-connection")
                  .doOnNext(s -> log.info("Server returned: " + s))
                  .flatMap(s -> con.outbound()
                      .sendString(Mono.just(toSend.append("a").toString()))
                      .then()
                  )
              )
              .then()
              .subscribe();
        })
        .doOnDisconnected(con -> {
          log.info("Server disconnected!");
          latch.countDown();
        })
        .connect()
        .log("tcp-client")
        .doOnError(e -> log.error("Error connecting to server ... " + e.getMessage()))
        .retryBackoff(Long.MAX_VALUE, Duration.ofSeconds(3), Duration.ofSeconds(10)) // retry on server missing (which causes error)
        .block();

    latch.await(); // the client is running until server disconnects the client
  }
}
