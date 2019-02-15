package reactornettyexamples.http;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyHttpServer {
  static Logger log = LoggerFactory.getLogger(MyHttpServer.class);

  public static void main(String[] args) throws InterruptedException {

/*
     Use this thread pool to execute blocking tasks.
     Requests are queued in an internal queue which can hold requests after a burst.
     The queue can be bounded so after reaching some length the subsequent requests are rejected
     thus protecting our server from overload
*/
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, // one thread in the pool
        1,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1)); // one request allowed in the queue
    tpe.allowCoreThreadTimeOut(true);

    HttpServer.create()   // Prepares a TCP server for configuration.
        .port(1551)    // Configures the port number
        .wiretap(true)
        .route(routes ->
            // The server will respond only on POST requests
            // where the path starts with /test and then there is path parameter
            routes.post("/test", (req, res) -> {
              log.info("Request received...");

              return req.receive().aggregate().asString() // get
                  .publishOn(Schedulers.fromExecutorService(tpe)) // delegate processing to our thread pool
                  .map(MyHttpServer::process) // execute the long running blocking task

                  .doOnSuccess(s -> {
                    log.info("Operation successful: {}", s);
                    res.status(HttpResponseStatus.OK)
                       .addHeader(HttpHeaderNames.CONTENT_LENGTH, "" + s.length())
                       .sendString(Mono.just(s))
                       .then()
                       .subscribe();
                  })
                  .doOnError(e -> { // we presume the error comes from our thread pool complaining the queue is full
                    log.error("Error occurred: ", e);
                    res.status(HttpResponseStatus.TOO_MANY_REQUESTS)
                       .sendString(Mono.just("TOO_MANY_REQUESTS"))
                       .then()
                       .subscribe();
                  })
                  .onErrorReturn("ignored value") // don not propagate the error to the subscriber
                  .log("http-server-receive")
                  .then();
            }))

        .bindUntilJavaShutdown(Duration.ofSeconds(30), null); // Starts the server in a blocking fashion, and waits for it to finish initializing.
  }

  private static String process(String string) {
    log.info("Processing " + string + " will take 5 seconds...");
    try {
      Thread.sleep(5000);
      //throw new IllegalArgumentException();
    } catch (InterruptedException e) {
      log.error("", e);
    }
    return string + " processed!";
  }
}
