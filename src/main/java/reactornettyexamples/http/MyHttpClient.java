package reactornettyexamples.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

public class MyHttpClient {
  static Logger log = LoggerFactory.getLogger(MyHttpClient.class);

  public static void main(String[] args) {
    HttpClient httpClient =
        HttpClient.create()             // Prepares a HTTP client for configuration.
            .port(1551)
            // client should connect.
            .wiretap(true)            // Applies a wire logger configuration.
            .headers(h -> h.add("Content-Type", "text/plain")); // Adds headers to the HTTP request


    HttpClient.RequestSender reqSender = httpClient
        .post()              // Specifies that POST method will be used
        .uri("/test");       // Specifies the path

    HttpClient.ResponseReceiver rcver = reqSender.send(ByteBufFlux.fromString(Flux.just("Hello")));

    String response = rcver.responseContent()
        .aggregate()
        .asString()
        .log("http-client")
        .block();

    log.info("Response: {}", response);
  }
}
