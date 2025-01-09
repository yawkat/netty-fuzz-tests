package io.netty.handler;

import com.code_intelligence.jazzer.api.BugDetectors;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.SilentCloseable;
import io.micronaut.fuzzing.Dict;
import io.micronaut.fuzzing.FuzzTarget;
import io.micronaut.fuzzing.runner.LocalJazzerRunner;
import io.micronaut.fuzzing.util.ByteSplitter;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;
import io.netty.incubator.channel.uring.IOUringSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

@Dict(HandlerFuzzerBase.SEPARATOR)
@FuzzTarget
public class EchoHandlerFuzzer {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) throws Exception {
        TransportType transport = data.pickValue(TransportType.values());

        EventLoopGroup serverGroup = transport.group.apply(1);
        EventLoopGroup clientGroup = transport.group.apply(1);
        try {
            ServerChannel serverChannel = (ServerChannel) new ServerBootstrap()
                    .group(serverGroup)
                    .channel(transport.serverChannel)
                    .childHandler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            ctx.writeAndFlush(msg, ctx.voidPromise());
                        }
                    })
                    .bind("127.0.0.1", 0).sync().channel();

            try (SilentCloseable ignored = BugDetectors.allowNetworkConnections((host, port) -> host.equals("localhost") && port == ((InetSocketAddress) serverChannel.localAddress()).getPort())) {
                byte[] allBytes = data.consumeRemainingAsBytes();
                CompositeByteBuf receivedBack = serverChannel.alloc().compositeBuffer();

                Channel clientChannel = new Bootstrap()
                        .group(clientGroup)
                        .channel(transport.clientChannel)
                        .handler(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                receivedBack.addComponent(true, (ByteBuf) msg);
                            }

                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                if (evt instanceof ChannelInputShutdownEvent) {
                                    ctx.close(ctx.voidPromise());
                                }
                            }
                        })
                        .connect(serverChannel.localAddress())
                        .sync().channel();

                ByteBuf expected = clientChannel.alloc().buffer();
                ByteSplitter.ChunkIterator itr = HandlerFuzzerBase.SPLITTER.splitIterator(allBytes);
                while (itr.hasNext() && clientChannel.isOpen()) {
                    itr.proceed();
                    ByteBuf buffer = clientChannel.alloc().buffer(itr.length());
                    buffer.writeBytes(allBytes, itr.start(), itr.length());
                    expected.writeBytes(allBytes, itr.start(), itr.length());

                    clientChannel.writeAndFlush(buffer).sync();
                }
                ((SocketChannel) clientChannel).shutdownOutput(clientChannel.voidPromise());
                clientChannel.closeFuture().await(1, TimeUnit.MINUTES);

                if (!receivedBack.equals(expected)) {
                    throw new AssertionError(receivedBack.toString(StandardCharsets.UTF_8));
                }
                receivedBack.release();
            }
        } finally {
            serverGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        LocalJazzerRunner.create(EchoHandlerFuzzer.class).reproduce("fooSEPbar".getBytes(StandardCharsets.UTF_8));
    }

    private enum TransportType {
        NIO(NioEventLoopGroup::new, NioServerSocketChannel.class, NioSocketChannel.class),
        EPOLL(EpollEventLoopGroup::new, EpollServerSocketChannel.class, EpollSocketChannel.class),
        IO_URING(IOUringEventLoopGroup::new, IOUringServerSocketChannel.class, IOUringSocketChannel.class),
        @SuppressWarnings("deprecation")
        OIO(n -> new OioEventLoopGroup(), OioServerSocketChannel.class, OioSocketChannel.class);

        final IntFunction<? extends EventLoopGroup> group;
        final Class<? extends ServerSocketChannel> serverChannel;
        final Class<? extends SocketChannel> clientChannel;

        TransportType(IntFunction<? extends EventLoopGroup> group, Class<? extends ServerSocketChannel> serverChannel, Class<? extends SocketChannel> clientChannel) {
            this.group = group;
            this.serverChannel = serverChannel;
            this.clientChannel = clientChannel;
        }
    }
}
