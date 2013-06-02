package posapaiv;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
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
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.CharsetUtil;

/*
 * Wrapper Facade :
 * ServerChannelFactory : Factory pattern to create a Channel which abstracts a universal asynchronous 
 * IO (commonality). It shields all underlying OS/Platform specific (java socket level) details. 
 * Specific  implementation (variability) can be Java NIO (TCP/IP) or OIO (TCP or UDP) based transport. 
 *   
 * Reactor Thread and Acceptor/Connector Thread.
 * NioServerChannelFactory does not create IO Thread by itself but it two takes thread pools as parameters for  
 * reactor and  acceptor execution/thread resources. 
 * One can pass in thread pool with a # of threads considering # of cores available on the server.
 * Reactor threads are separate from threads that actually handle client requests (in this case echo the message).
 *   
 * Message Handlers :
 * ServerBootstrap : This class separates the connection management (connecting/disconnecting
 * and connection state) from message handling. 
 * It takes a Pipeline Factory which specifies one or more message handlers.
 *   
 */

public class HSHAEchoServer {

	// the port number here
	private final int port;

	private static final int threadPoolSize = 8;

	/*
	 * Define a static ExecutionHandler like an instance of the predefined
	 * OrderedMemoryAwareThreadPoolExecutor class (the HS role in the pattern).
	 * Making the ExecutorHandle static ensures that there is only one thread
	 * pool and one queue, for purity of the HS/HA pattern implementation. The
	 * HA role in the pattern is embedded in the channel implementation
	 * interacting with the queue of the OrderedMemoryAwareThreadPoolExecutor.
	 */
	private static final ExecutionHandler hsExecutor = new ExecutionHandler(
			new OrderedMemoryAwareThreadPoolExecutor(threadPoolSize, 1000000,
					1000000));

	// Set the port number here
	public HSHAEchoServer(int port) {
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

		/*
		 * Bind and start to accept incoming connections. 
		 */
		bootstrap.bind(new InetSocketAddress(port));

	}

	public static void main(String[] args) throws Exception {
		int port;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		} else {
			port = 8080;
		}
		new HSHAEchoServer(port).start();
	}

	/*
	 * Handler implementation for the echo server. This class plays the concrete
	 * event handler role in the Reactor pattern. The HA role in the pattern is
	 * embedded in the channel * implementation interacting with the queue of
	 * the OrderedMemoryAwareThreadPoolExecutor
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

			/*
			 * We do not need to write a ChannelBuffer here. We know the encoder
			 * inserted at ServerPipelineFactory will do the conversion. Send
			 * back the received message to the remote peer.
			 */

			ChannelFuture future = e.getChannel().write(response);

			/*
			 * Close the connection after sending 'Have a good day!' if the
			 * client has sent 'bye'.
			 */
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
		public void channelConnected(ChannelHandlerContext ctx,
				ChannelStateEvent e) throws Exception {
			// Send greeting for a new connection.
			e.getChannel().write(
					"Welcome to " + InetAddress.getLocalHost().getHostName()
							+ "!\r\n");
			e.getChannel().write("It is " + new Date() + " now.\r\n");
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

			/*
			 * Insert OrderedMemoryAwareThreadPoolExecutor before your blocking
			 * handler When an event arrives at the Half Synch Executor, the
			 * ExecutionHandler places the event onto the queue of events
			 * managed by the OrderedMemoryAwareThreadPoolExecutor, and the
			 * thread pool processes those enqueued events, executing the
			 * EchoServerHandler that follows Half Synch Executor.
			 */
			pipeline.addLast("pipelineExecutor", hsExecutor);

			// EchoServerHandler contains code that blocks
			pipeline.addLast("handler", new EchoServerHandler());

			return pipeline;
		}
	}

}
