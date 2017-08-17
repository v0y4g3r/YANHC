
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;

public class HttpClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private HttpResponseListener listener;
    private boolean hasBody = false;
    public HttpClientHandlerInitializer(HttpResponseListener listener) {
        this.listener = listener;
    }

    public HttpClientHandlerInitializer() {
        this.listener = null;
    }

    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpClientCodec());
        // Remove the following line if you don't want automatic content decompression.
        p.addLast(new HttpContentDecompressor());
        // Uncomment the following line if you don't want to handle HttpContents.
        //p.addLast(new HttpObjectAggregator(1048576));
        p.addLast(new HttpClientHandler());

        if (this.listener != null) {
            p.addLast(new SomeInboundHandler(this.listener));
        }
    }
}


class SomeInboundHandler extends SimpleChannelInboundHandler<String> {
    private HttpResponseListener listener;

    public SomeInboundHandler(HttpResponseListener listener) {
        this.listener = listener;
    }

    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        listener.cb(null, msg);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        listener.cb(cause, null);
    }
}
