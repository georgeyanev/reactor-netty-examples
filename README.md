## HTTP and TCP examples with reactor-netty

### HTTP
The http server delegates the incoming requests to a separate thread pool 
where some blocking tasks get executed (i.e. JDBC calls) as we do not want
to block netty I/O threads.  
The thread pool (actually a `java.util.concurrent.ThreadPoolExecutor` instance)
has an internal queue where the requests are queued before execution. In this way
we can deal with some short time request bursts.  
The queue can be bounded so if the queue capacity is reached the 
subsequent requests are rejected. This way we can protect our server from 
overloading.

### TCP
The server receives and processes five messages from the client and then
disconnects it. If the server is not available the client retries the 
connection (using `Mono.retryBackoff`).  
I'm still figuring out how to reconnect automatically on server disconnect
without creating a separate `TcpClient` just for that.
 
 
 
 
 
