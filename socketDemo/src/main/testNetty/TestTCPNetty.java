package testNetty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import io.netty.channel.ChannelHandler.Sharable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestTCPNetty {
    public static void main(String[] args) {
        //服务启动器
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        //设置线程池
        //Boss线程池
        EventLoopGroup bossLoopGrooup = new NioEventLoopGroup(1);
        //Work线程池
        ThreadFactory threadFactory = new DefaultThreadFactory("work thread pool");
        //cup个数
        int processorsNumber = Runtime.getRuntime().availableProcessors();
        EventLoopGroup workLoogGroup = new NioEventLoopGroup(processorsNumber);
        //指定Netty的Boss线程和Work线程
        serverBootstrap.group(bossLoopGrooup,workLoogGroup);
        //如果是以下的声明方式，说明Boss线程和Work线程共享一个线程池
        //实际一般情况环境下，这种共享线程池的方式已经够了
//        serverBootstrap.group(workLoogGroup);

        //下面设置服务通道类型
        //只能是实现了ServerChannel接口的 “服务器”通道类
        serverBootstrap.channel(NioServerSocketChannel.class);
        //当然也可以这样创建（那个SelectorProvider是不是感觉很熟悉？）
        //serverBootstrap.channelFactory(new ChannelFactory<NioServerSocketChannel>() {
        //  @Override
        //  public NioServerSocketChannel newChannel() {
        //      return new NioServerSocketChannel(SelectorProvider.provider());
        //  }
        //});

        //设置处理器
        //为了演示，这里我们设置了一组简单的ByteArrayDecoder和ByteArrayEncoder
        //Netty的特色就在这一连串“通道水管”中的“处理器”
        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ByteArrayEncoder());
                ch.pipeline().addLast(new TCPServerHandler());
                ch.pipeline().addLast(new ByteArrayDecoder());
            }
        });
        //设置Netty服务器绑定ip和端口
        serverBootstrap.option(ChannelOption.SO_BACKLOG,128);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE,true);
        serverBootstrap.bind(new InetSocketAddress("0.0.0.0",83));

    }

    @Sharable
    static class TCPServerHandler extends ChannelInboundHandlerAdapter {
        /**
         * 日志
         */
        private Log LOGGER = LogFactory.getLog(TCPServerHandler.class);
        /**
         * 每一个channel，都有独立的handler、ChannelHandlerContext、ChannelPipeline、Attribute
         * 所以不需要担心多个channel中的这些对象相互影响。<br>
         * 这里我们使用content这个key，记录这个handler中已经接收到的客户端信息。
         */
        private static AttributeKey<StringBuffer> content = AttributeKey.valueOf("content");

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.channelRegistered(ctx)");
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.channelUnregistered(ctx)");
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.channelActive(ctx) = " + ctx.toString());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.channelInactive(ctx)");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            LOGGER.info("channelRead(ChannelHandlerContext ctx, Object msg)");
            /*
             * 我们使用IDE工具模拟长连接中的数据缓慢提交。
             * 由read方法负责接收数据，但只是进行数据累加，不进行任何处理
             * */
            ByteBuf byteBuf = (ByteBuf) msg;
            try{
                StringBuffer contextBuffer = new StringBuffer();
                while (byteBuf.isReadable()){
                    contextBuffer.append((char)byteBuf.readByte());
                }
                //加入临时区域
                StringBuffer content = ctx.attr(TCPServerHandler.content).get();
                if (content==null){
                    content = new StringBuffer();
                    ctx.attr(TCPServerHandler.content).set(content);
                }
                content.append(contextBuffer);
            }catch (Exception e){
                throw e;
            }finally {
                byteBuf.release();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            /*
             * 由readComplete方法负责检查数据是否接收完了。
             * 和之前的文章一样，我们检查整个内容中是否有“over”关键字
             * */
            StringBuffer content = ctx.attr(TCPServerHandler.content).get();
            //如果条件成立说明还没有接收到完整客户端信息
            if(content.indexOf("over") == -1) {
                return;
            }
            //当接收到信息后，首先要做的的是清空原来的历史信息
            ctx.attr(TCPServerHandler.content).set(new StringBuffer());

            //准备向客户端发送响应
            ByteBuf byteBuf = ctx.alloc().buffer(1024);
            byteBuf.writeBytes("回发响应信息！".getBytes());
            ctx.writeAndFlush(byteBuf);


            /*
             * 关闭，正常终止这个通道上下文，就可以关闭通道了
             * （如果不关闭，这个通道的回话将一直存在，只要网络是稳定的，服务器就可以随时通过这个回话向客户端发送信息）。
             * 关闭通道意味着TCP将正常断开，其中所有的
             * handler、ChannelHandlerContext、ChannelPipeline、Attribute等信息都将注销
             * */
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            LOGGER.info("super.userEventTriggered(ctx, evt)");
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.channelWritabilityChanged(ctx)");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            LOGGER.info("super.exceptionCaught(ctx, cause)");
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.handlerAdded(ctx)");
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("super.handlerRemoved(ctx)");
        }
    }

}

