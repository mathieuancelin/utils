/*
 *  Copyright 2011 Mathieu ANCELIN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package cx.ath.mancel01.utils;

import java.nio.charset.Charset;
import java.util.List;
import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.F.Tuple;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Http {

    public static Http createServer(HttpCallback handler) {
        return new Http(handler);
    }
    private static final ExecutorService backgroundPool = 
            Executors.newCachedThreadPool();
    private static final ExecutorService corePool = 
            Executors.newCachedThreadPool();
    private ServerBootstrap bootstrap;
    private HttpCallback handler;
    
    private Map<Channel, HttpConnection> connectionMap =
            new ConcurrentHashMap<Channel, HttpConnection>();

    private Http(HttpCallback handler) {
        this.handler = handler;
        ChannelFactory factory =
                new NioServerSocketChannelFactory(backgroundPool, corePool);
        bootstrap = new ServerBootstrap(factory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("handler", new HttpRequestHandler());
                return pipeline;
            }
        });
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
    }

    public Http listen(int port) {
        return listen(port, "0.0.0.0");
    }

    public Http listen(int port, String host) {
        try {
            bootstrap.bind(new InetSocketAddress(InetAddress.getByName(host), port));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void stop() {
        bootstrap.releaseExternalResources();
    }

    public class HttpRequestHandler extends SimpleChannelUpstreamHandler {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Channel ch = e.getChannel();
            HttpConnection conn = connectionMap.get(ch);
            if (e.getMessage() instanceof org.jboss.netty.handler.codec.http.HttpRequest) {
                org.jboss.netty.handler.codec.http.HttpRequest request = (org.jboss.netty.handler.codec.http.HttpRequest) e.getMessage();
                Map<String, String> headers = new HashMap<String, String>();
                for (Map.Entry<String, String> h : request.getHeaders()) {
                    headers.put(h.getKey(), h.getValue());
                }
                HttpRequest req = new HttpRequest(request.getMethod().toString(), request.getUri(), headers);
                
                conn.handleRequest(req);
                ChannelBuffer requestBody = request.getContent();
                if (requestBody.readable()) {
                    conn.handleChunk(new Buffer(requestBody));
                }
            } else if (e.getMessage() instanceof HttpChunk) {
                HttpChunk chunk = (HttpChunk) e.getMessage();
                Buffer buff = Buffer.fromChannelBuffer(chunk.getContent());
                conn.handleChunk(buff);
            } 
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            e.getCause().printStackTrace();
            e.getChannel().close();
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
            Channel ch = e.getChannel();
            HttpConnection conn = new HttpConnection(ch);
            conn.request(handler);
            connectionMap.put(ch, conn);
        }

        @Override
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
            Channel ch = e.getChannel();
            connectionMap.remove(ch);
        }
    }

    public static class HttpConnection {

        private final Channel channel;

        HttpConnection(Channel channel) {
            this.channel = channel;
        }
        private HttpCallback httpCallback;
        private volatile HttpRequest currentRequest;

        public void request(HttpCallback httpCallback) {
            this.httpCallback = httpCallback;
        }

        void handleRequest(HttpRequest req) {
            try {
                this.currentRequest = req;
                if (httpCallback != null) {
                    httpCallback.apply(
                        new Tuple<HttpRequest, HttpResponse>(
                            req, 
                            new HttpResponse(channel, false)
                        )
                    );
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        void handleChunk(Buffer chunk) {
            try {
                if (currentRequest != null) {
                    currentRequest.dataReceived(chunk);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        private void handleThrowable(Throwable t) {
            t.printStackTrace();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            }
        }
    }

    public static abstract class HttpCallback implements Action<Tuple<HttpRequest, HttpResponse>> {
    }

    public static class HttpRequest {

        public final String method;
        public final String uri;
        public final Map<String, String> headers;
        private Map<String, List<String>> params;

        protected HttpRequest(String method, String uri, Map<String, String> headers) {
            this.method = method;
            this.uri = uri;
            this.headers = headers;
        }
        private Action<Buffer> dataHandler;

        public void data(Action<Buffer> dataHandler) {
            this.dataHandler = dataHandler;
        }

        public String getParam(String param) {
            if (params == null) {
                QueryStringDecoder queryStringDecoder = 
                        new QueryStringDecoder(uri);
                params = queryStringDecoder.getParameters();
            }
            List<String> list = params.get(param);
            if (list != null) {
                return list.get(0);
            } else {
                return null;
            }
        }

        void dataReceived(Buffer data) {
            if (dataHandler != null) {
                dataHandler.apply(data);
            }
        }
    }

    public static class HttpResponse {

        public final Map<String, String> headers = new HashMap<String, String>();
        public int statusCode;
        private Channel channel;
        private boolean headWritten;
        private final boolean keepAlive;
        private ChannelFuture writeFuture;

        HttpResponse(Channel channel, boolean keepAlive) {
            this.channel = channel;
            this.keepAlive = keepAlive;
        }

        public HttpResponse write(Buffer chunk) {
            return write(chunk._toChannelBuffer());
        }

        public HttpResponse write(String chunk, String enc) {
            return write(Buffer.fromString(chunk, enc)._toChannelBuffer());
        }

        public void end() {
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }

        private HttpResponse write(ChannelBuffer chunk) {
            if (!headWritten) {
                org.jboss.netty.handler.codec.http.HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
                response.setContent(chunk);
                if (keepAlive) {
                    response.setHeader(CONTENT_LENGTH, response.getContent().readableBytes());
                }
                writeFuture = channel.write(response);
                headWritten = true;
            }
            return this;
        }
    }

    public static class Buffer {

        private ChannelBuffer buffer;

        public static Buffer newFixed(int size) {
            return new Buffer(ChannelBuffers.buffer(size));
        }

        public static Buffer newDynamic(int size) {
            return new Buffer(ChannelBuffers.dynamicBuffer(size));
        }

        public static Buffer fromChannelBuffer(ChannelBuffer buffer) {
            return new Buffer(buffer);
        }

        public static Buffer newWrapped(byte[] bytes) {
            return new Buffer(ChannelBuffers.wrappedBuffer(bytes));
        }

        public static Buffer fromString(String str, String enc) {
            return new Buffer(ChannelBuffers.copiedBuffer(str, Charset.forName(enc)));
        }

        public static Buffer fromString(String str) {
            return Buffer.fromString(str, "UTF-8");
        }

        public Buffer(ChannelBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public String toString() {
            return buffer.toString(Charset.forName("UTF-8"));
        }

        public String toString(String enc) {
            return buffer.toString(Charset.forName(enc));
        }

        public byte byteAt(int pos) {
            return buffer.getByte(pos);
        }

        public Buffer append(Buffer buff) {
            ChannelBuffer cb = buff._toChannelBuffer();
            buffer.writeBytes(buff._toChannelBuffer());
            cb.readerIndex(0);
            return this;
        }

        public Buffer append(byte[] bytes) {
            buffer.writeBytes(bytes);
            return this;
        }

        public Buffer append(byte b) {
            buffer.writeByte(b);
            return this;
        }

        public Buffer append(String str, String enc) {
            return append(str, Charset.forName(enc));
        }

        public Buffer append(String str) {
            return append(str, CharsetUtil.UTF_8);
        }

        private Buffer append(String str, Charset charset) {
            byte[] bytes = str.getBytes(charset);
            buffer.writeBytes(bytes);
            return this;
        }

        public Buffer setByte(int pos, byte b) {
            buffer.setByte(pos, b);
            return this;
        }

        public Buffer setBytes(int pos, Buffer b) {
            buffer.setBytes(pos, b._toChannelBuffer());
            return this;
        }

        public Buffer setBytes(int pos, byte[] b) {
            buffer.setBytes(pos, b);
            return this;
        }

        public Buffer setBytes(int pos, String str) {
            return setBytes(pos, str, CharsetUtil.UTF_8);
        }

        public Buffer setBytes(int pos, String str, String enc) {
            return setBytes(pos, str, Charset.forName(enc));
        }

        private Buffer setBytes(int pos, String str, Charset charset) {
            byte[] bytes = str.getBytes(charset);
            buffer.setBytes(pos, bytes);
            return this;
        }

        public int capacity() {
            return buffer.capacity();
        }

        public int length() {
            return buffer.writerIndex();
        }

        public Buffer slice(int start, int end) {
            return new Buffer(buffer.slice(start, end - start));
        }

        public Buffer copy(int start, int end) {
            return new Buffer(buffer.copy(start, end - start));
        }

        public Buffer duplicate() {
            return new Buffer(buffer.duplicate());
        }

        public ChannelBuffer _toChannelBuffer() {
            return buffer;
        }
    }
}
