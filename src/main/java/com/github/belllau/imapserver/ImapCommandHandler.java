package com.github.belllau.imapserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.bellau.rockyproto.CollectionsRequest;
import com.github.bellau.rockyproto.CollectionsResponse;
import com.github.bellau.rockyproto.MessageStoreGrpc.MessageStoreStub;

import io.grpc.stub.StreamObserver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.imap.AtomParameter;
import io.netty.handler.codec.imap.CloseListParameter;
import io.netty.handler.codec.imap.CommandParameter;
import io.netty.handler.codec.imap.ImapCommand;
import io.netty.handler.codec.imap.ImapResponse;
import io.netty.handler.codec.imap.ImapResponse.ResponseCode;
import io.netty.handler.codec.imap.OpenListParameter;
import io.netty.handler.codec.imap.QuotedStringParameter;

public class ImapCommandHandler extends SimpleChannelInboundHandler<ImapCommand> {

	private MessageStoreStub client;

	public ImapCommandHandler(MessageStoreStub messageStoreStub) {
		this.client = messageStoreStub;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ImapCommand cmd) throws Exception {
		if (cmd.getCommand().equalsIgnoreCase("capability")) {
			ctx.write(new ImapResponse.ServerResponse("CAPABILITY", Arrays.asList(new AtomParameter("LOGINDISABLED"))));
			ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "CAPABILITY completed"));
		} else if (cmd.getCommand().equalsIgnoreCase("login")) {
			ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "LOGIN completed"));
		} else if (cmd.getCommand().equalsIgnoreCase("list")) {
			long time = System.nanoTime();
			StreamObserver<CollectionsResponse> responseObserver = new StreamObserver<CollectionsResponse>() {

				@Override
				public void onCompleted() {
					// TODO Auto-generated method stub

				}

				@Override
				public void onError(Throwable arg0) {
					System.err.println("error ");
					arg0.printStackTrace();
				}

				@Override
				public void onNext(CollectionsResponse resp) {
					System.err.println("res in " + ((System.nanoTime() - time) / 1000000) + " ms");
					ctx.write(new ImapResponse.ServerResponse("LIST",
							Arrays.asList(new OpenListParameter(), new AtomParameter("\\HasNoChildren"),
									new CloseListParameter(), new QuotedStringParameter("/"),
									new QuotedStringParameter("INBOX")

							)));
					ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "LIST completed"));
				}

			};
			client.collections(CollectionsRequest.newBuilder().build(), responseObserver);

		} else if (cmd.getCommand().equalsIgnoreCase("LSUB")) {
			ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "LSUB completed"));
		} else if (cmd.getCommand().equalsIgnoreCase("SELECT")) {
			ctx.write(new ImapResponse.ServerResponse("FLAGS",
					Arrays.asList(new OpenListParameter(), new AtomParameter("\\Answered"),
							new AtomParameter("\\Flagged"), new AtomParameter("\\Deleted"), new AtomParameter("\\Seen"),
							new AtomParameter("\\Draft"), new CloseListParameter())));
			ctx.write(new ImapResponse.Ok(null, new ResponseCode("PERMANENTFLAGS",
					Arrays.asList(new OpenListParameter(), new AtomParameter("\\Answered"),
							new AtomParameter("\\Flagged"), new AtomParameter("\\Deleted"), new AtomParameter("\\Seen"),
							new AtomParameter("\\Draft"), new AtomParameter("\\*"), new CloseListParameter())),
					"Flags permitted."));
			ctx.write(new ImapResponse.MessageStatusResponse(0, "EXISTS", Collections.emptyList()));
			ctx.write(new ImapResponse.MessageStatusResponse(0, "RECENT", Collections.emptyList()));
			ctx.write(new ImapResponse.Ok(null,
					new ResponseCode("UIDVALIDITY", Arrays.asList(new AtomParameter("1462977609"))), "UIDs valid"));
			ctx.write(new ImapResponse.Ok(null, new ResponseCode("UIDNEXT", Arrays.asList(new AtomParameter("2"))),
					"Predicted next UID"));
			ctx.write(new ImapResponse.Ok(null,
					new ResponseCode("HIGHESTMODSEQ", Arrays.asList(new AtomParameter("2"))), "Highest"));

			ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), new ResponseCode("READ-WRITE", Collections.emptyList()),
					"Select completed"));

		} else if (cmd.getCommand().equalsIgnoreCase("noop")) {
			ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "NOOP completed"));
		} else if (cmd.getCommand().equalsIgnoreCase("status")) {
			List<CommandParameter> l = new ArrayList<>();
			l.add(cmd.getParameters().get(0));
			l.add(new OpenListParameter());
			for (CommandParameter p : cmd.getParameters()) {
				if (p instanceof AtomParameter) {
					AtomParameter at = (AtomParameter) p;
					if (at.toString().equals("MESSAGES")) {
						l.add(new AtomParameter("MESSAGES"));
						l.add(new AtomParameter("0"));
					} else if (at.toString().contentEquals("UNSEEN")) {
						l.add(new AtomParameter("UNSEEN"));
						l.add(new AtomParameter("0"));
					} else if (at.toString().contentEquals("RECENT")) {
						l.add(new AtomParameter("RECENT"));
						l.add(new AtomParameter("0"));
					} else if (at.toString().contentEquals("UIDNEXT")) {
						l.add(new AtomParameter("UIDNEXT"));
						l.add(new AtomParameter("2"));
					} else if (at.toString().contentEquals("UIDVALIDITY")) {
						l.add(new AtomParameter("UIDVALIDITY"));
						l.add(new AtomParameter("1462977609"));
					}
				}
			}
			l.add(new CloseListParameter());
			ctx.write(new ImapResponse.ServerResponse("STATUS", l));
			ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "STATUS completed"));
		} else if ("uid".equalsIgnoreCase(cmd.getCommand())) {
			if ("fetch".equalsIgnoreCase(cmd.getParameters().get(0).toString())) {
				ctx.writeAndFlush(new ImapResponse.Ok(cmd.getTag(), null, "UID FETCH completed"));
			}
		}
	}
}
