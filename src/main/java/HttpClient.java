
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;

import java.net.URI;
import java.net.URISyntaxException;

@SuppressWarnings("ALL")
public class HttpClient {
    //TODO 处理HTTP异常状态码
    private EventLoopGroup nioEventLoop;
    private Bootstrap b;
    private Channel ch;

    public HttpClient(EventLoopGroup group) {
        if (group == null) return;
        b = new Bootstrap();
        nioEventLoop = group;
        b.group(nioEventLoop).channel(NioSocketChannel.class);
    }


    public Promise<String> get(String url) throws URISyntaxException {
        return sendRequest(url, HttpMethod.GET, null);
    }

    public Promise<String> post(String url) throws URISyntaxException {
        return sendRequest(url, HttpMethod.POST, null);
    }

    public Promise<String> post(String url, ByteBuf content) throws URISyntaxException {
        return sendRequest(url, HttpMethod.POST, content);
    }

    private ChannelFuture sendRequest(String url, HttpResponseListener listener, HttpMethod method, ByteBuf content) throws URISyntaxException {
        URI uri = new URI(url);
        if (uri.getScheme() == null) uri = new URI("http://" + uri.getRawPath());
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 80;

        try {
            b.handler(new HttpClientHandlerInitializer(listener));
            Channel ch = this.b.connect(host, port).sync().channel();

            //build URI
            String rawPath = uri.getRawPath();
            String query = uri.getQuery();
            if (query != null)
                rawPath += "?" + query;

            HttpRequest request;
            if (content != null) {
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, rawPath, content);
            } else {
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, rawPath);
            }

            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

            return ch.writeAndFlush(request);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Promise<String> sendRequest(String url, HttpMethod method, ByteBuf content) throws URISyntaxException {
        final Promise<String> promise = new DefaultPromise(nioEventLoop.next());
        try {
            sendRequest(url, (err, resp) -> {
                if (err != null) {
                    promise.setFailure(err);
                    return;
                }
                promise.setSuccess(resp);
            }, method, content);
        } catch (Exception e) {
            promise.setFailure(e);
        }
        return promise;
    }

    private Promise<JSONObject> sendRequestAndGetJson(String url, HttpMethod method, ByteBuf content) {
        final Promise<JSONObject> promise = new DefaultPromise<>(nioEventLoop.next());

        try {
            sendRequest(url, (err, resp) -> {
                if (err != null) {
                    promise.setFailure(err);
                    return;
                }
                try {
                    JSONObject json = (JSONObject) JSON.parse(resp);
                    promise.setSuccess(json);

                } catch (JSONException e) {
                    promise.setFailure(e);
                    return;
                }
            }, method, content);
        } catch (URISyntaxException e) {
            promise.setFailure(e);
        }
        return promise;
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        HttpClient httpClient = new HttpClient(new NioEventLoopGroup());
//        HttpPostRequestEncoder
        httpClient.sendRequestAndGetJson("http://219.223.222.4:5000/user/login", HttpMethod.POST, null).addListener(f -> {
            if (f.isSuccess()) {
                JSONObject json = (JSONObject) f.getNow();
                System.out.println(json.getString("msg"));
            } else
                System.out.println("failed");
        });
    }
}
