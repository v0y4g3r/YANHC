
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.lang.annotation.ElementType;
import java.util.TreeMap;


public class HttpClientHandler extends SimpleChannelInboundHandler<HttpObject> {
    private ChannelHandlerContext ctx;
    private ByteBuf inBuf = null;
    private AttributeKey<Boolean> key = AttributeKey.valueOf("failed");



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {

        if (inBuf == null) {
            inBuf = ctx.alloc().buffer();
        }
        Boolean b = ctx.channel().attr(key).get();

        if (b == null || b == Boolean.FALSE) {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;
                if (response.status().code() != 200) {
                    System.out.println("HTTP Status code != 200");
                    ctx.channel().attr(key).set(Boolean.TRUE);
                    ctx.fireExceptionCaught(new Throwable(String.format("Request failed:[%d]", response.status().code())));
                }
                return;
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                inBuf.writeBytes(content.content());
                if (content instanceof LastHttpContent) {
                    String s = inBuf.toString(CharsetUtil.UTF_8);
                    inBuf.resetWriterIndex();
                    inBuf.release();
                    ctx.fireChannelRead(s);
                }
            }

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;//init context
    }
}
