package reactornettyexamples.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpServer;

import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.List;

public class MyTcpServer {
  private static Logger log = LoggerFactory.getLogger(MyTcpServer.class);

  public static void main(String[] args) throws CertificateException, InterruptedException {
    SelfSignedCertificate cert = new SelfSignedCertificate();
    SslContextBuilder sslContextBuilder =
        SslContextBuilder.forServer(cert.certificate(), cert.privateKey());

    TcpServer.create()   // Prepares a TCP server for configuration.
        .port(1551)    // Configures the port number
        .secure(spec -> spec.sslContext(sslContextBuilder)) // Use self singed certificate.
        //.wiretap()  // Applies a wire logger configuration.
        .doOnConnection(MyTcpServer::onConnect)
        .bindUntilJavaShutdown(Duration.ofSeconds(30), null); // Starts the server in a blocking fashion, and waits for it to finish initializing.
  }

  private static void onConnect(Connection conn) {
    conn.addHandler(new StringToIntegerDecoder()); // add a handler to the netty pipeline
    MyServerConnection myConn = new MyServerConnection(conn);
    log.info("New client connected: {}", conn);
    myConn.handle();
  }

  /**
   * Decoder that returns the number of received bytes. This is a core netty feature.
   */
  public static class StringToIntegerDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
      int n = in.readableBytes();
      if (in.readableBytes() > 0) {
        in.readBytes(n);
        out.add(n); // store the result of the decoder here
      }
    }
  }
}
