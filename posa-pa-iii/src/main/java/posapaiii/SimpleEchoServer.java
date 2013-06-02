package posapaiii;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;

public class SimpleEchoServer {

	// the port number here
	private final int port;

	// Set the port number here
	public SimpleEchoServer(int port) {
		this.port = port;
	}

	public void start() {

		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ServerPipelineFactory());

		// Bind and start to accept incoming connections.
		// The InetSocketAddress plays role of the wrapper facade in the Wrapper
		// Facade pattern
		// The bind method plays role of the acceptor in the Acceptor-Connector
		// pattern.
		bootstrap.bind(new InetSocketAddress(port));

	}

	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		new SimpleEchoServer(port).start();
	}

	/*
	 * Handler implementation for the echo server. This class plays the concrete
	 * event handler role in the Reactor pattern.
	 */
	class EchoServerHandler extends SimpleChannelUpstreamHandler {

		private final Logger logger = Logger.getLogger(EchoServerHandler.class
				.getName());

		
		/*
		 * This hook method is dispatched by the Reactor when data shows up from
		 * a client.
		 */
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

			// Cast to a String first.
			String request = (String) e.getMessage();

			// Generate and write a response.
			String response = null;
			
			boolean close = false;

			if ("bye".equals(request.toLowerCase())) {
				close = true;
			} else {
				response = request + "\n";
			}

			// We do not need to write a ChannelBuffer here.
			// We know the encoder inserted at ServerPipelineFactory will do the
			// conversion.
			// Send back the received message to the remote peer.

			ChannelFuture future = e.getChannel().write(response);

			// Close the connection after sending 'Have a good day!'
			// if the client has sent 'bye'.
			if (close) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			logger.log(Level.WARNING, "Unexpected exception from downstream.",
					e.getCause());
			e.getChannel().close();
		}

		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {
			if (e instanceof ChannelStateEvent) {
				logger.info(e.toString());
			}
			super.handleUpstream(ctx, e);
		}
	}

	class ServerPipelineFactory implements ChannelPipelineFactory {

		public ChannelPipeline getPipeline() throws Exception {
			// Create a default pipeline implementation.
			ChannelPipeline pipeline = pipeline();

			// Add the text line codec combination first,
			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
					Delimiters.lineDelimiter()));
			pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
			pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));

			// and then business logic.
			pipeline.addLast("handler", new EchoServerHandler());

			return pipeline;
		}
	}

}
