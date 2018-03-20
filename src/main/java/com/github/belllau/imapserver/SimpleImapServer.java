package com.github.belllau.imapserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.imap.ImapCommandDecoder;
import io.netty.handler.codec.imap.ImapResponse;
import io.netty.handler.codec.imap.ImapResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class SimpleImapServer {

	class Init extends ChannelInitializer<SocketChannel> {

		@Override
		public void initChannel(SocketChannel ch) throws Exception {

			ChannelPipeline pipeline = ch.pipeline();

			// pipeline.addLast(new SslHandler(engine));
			pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
			pipeline.addLast(new ImapCommandDecoder());
			pipeline.addLast(new ImapResponseEncoder());
			pipeline.addLast(new ImapCommandHandler());
			ch.write(new ImapResponse.Ok(null, null, "IMAP4rev1 server ready"));
		}
	}

	private int port;

	public SimpleImapServer(int port) {
		this.port = port;
	}

	public void start() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO)).childHandler(new Init());

			b.bind(port).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new SimpleImapServer(143).start();
	}
}
