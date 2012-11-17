/*
 *  Copyright 2011-2012 Mathieu ANCELIN
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

import cx.ath.mancel01.utils.Concurrent.Promise;
import cx.ath.mancel01.utils.F.Action;
import cx.ath.mancel01.utils.F.Tuple;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;


/**
 * Heavily inspired of Node.JS(https://github.com/joyent/node), 
 * and Node.x (https://github.com/purplefox/node.x).
 * 
 * @author mathieuancelin
 */
public class Http {

    public static Http createServer(HttpCallback handler) {
        return new Http(handler);
    }
    
    public static Http createServer(Action<Tuple<Request, Response>> handler) {
        return new Http(handler);
    }
    
    private static final ExecutorService backgroundPool = 
            Executors.newCachedThreadPool();
    private static final ExecutorService corePool = 
            Executors.newCachedThreadPool();
    private ServerBootstrap bootstrap;
    private Action<Tuple<Request, Response>> handler;
    
    private Map<Channel, HttpConnection> connectionMap =
            new ConcurrentHashMap<Channel, HttpConnection>();
    
    private Http(Action<Tuple<Request, Response>> handler) {
        init(handler);
    }
    
    private void init(Action<Tuple<Request, Response>> handler) {
        this.handler = handler;
        ChannelFactory factory =
                new NioServerSocketChannelFactory(backgroundPool, corePool);
        bootstrap = new ServerBootstrap(factory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("handler", new HttpRequestHandler());
                pipeline.addLast("compressor", new HttpContentCompressor());
                pipeline.addLast("decompressor", new HttpContentDecompressor());
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
            if (e.getMessage() instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) e.getMessage();
                Map<String, String> headers = new HashMap<String, String>();
                for (Map.Entry<String, String> h : request.getHeaders()) {
                    headers.put(h.getKey(), h.getValue());
                }
                Request req = new Request(request.getMethod().toString(), request.getUri(), headers);
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
        private Action<Tuple<Request, Response>> httpCallback;
        private volatile Request currentRequest;

        public void request(HttpCallback httpCallback) {
            this.httpCallback = httpCallback;
        }
        
        public void request(Action<Tuple<Request, Response>> httpCallback) {
            this.httpCallback = httpCallback;
        }

        void handleRequest(Request req) {
            try {
                this.currentRequest = req;
                if (httpCallback != null) {
                    httpCallback.apply(
                        new Tuple<Request, Response>(
                            req, 
                            new Response(channel, false)
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

    public static abstract class HttpCallback implements Action<Tuple<Request, Response>> {}

    public static class Request {

        public final String method;
        public final String uri;
        public final Map<String, String> headers;
        private Map<String, List<String>> params;

        protected Request(String method, String uri, Map<String, String> headers) {
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

    public static class Response {

        public final Map<String, String> headers = new HashMap<String, String>();
        public int statusCode;
        private Channel channel;
        private boolean headWritten;
        private final boolean keepAlive;
        private ChannelFuture writeFuture;

        Response(Channel channel, boolean keepAlive) {
            this.channel = channel;
            this.keepAlive = keepAlive;
        }
        
        public void async(Promise promise) {
            promise.onRedeem(new Action<Promise>() {
                @Override
                public void apply(Promise t) {
                    try {
                        Object o = t.get();
                        for (String message : M.caseClassOf(String.class, o)) {
                            writeString(message, "utf-8").end();
                        }
                        for (Buffer message : M.caseClassOf(Buffer.class, o)) {
                            writeBuffer(message).end();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        public Response writeBuffer(Buffer chunk) {
            return writeResponse(chunk.toChannelBuffer());
        }

        public Response writeString(String chunk, String enc) {
            return writeResponse(Buffer.fromString(chunk, enc).toChannelBuffer());
        }
        
        public Response header(String key, String value) {
            headers.put(key, value);
            return this;
        }
        
        public Response satus(int status) {
            this.statusCode = status;
            return this;
        }
        
        public void write(Buffer chunk) {
            Promise<Buffer> p = new Promise<Buffer>();
            async(p);
            p.apply(chunk);
        }

        public void write(String content) {
            Promise<String> p = new Promise<String>();
            async(p);
            p.apply(content);
        }

        public void end() {
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }

        private Response writeResponse(ChannelBuffer chunk) {
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
            ChannelBuffer cb = buff.toChannelBuffer();
            buffer.writeBytes(buff.toChannelBuffer());
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
            buffer.setBytes(pos, b.toChannelBuffer());
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

        public ChannelBuffer toChannelBuffer() {
            return buffer;
        }
    }
    
//    public static class Router implements Action<Tuple<Request, Response>> {
//        
//        public static enum Method {
//            ALL, GET, PUT, POST, DELETE, HEAD
//        }
//        
//        public static class Result {
//            public final String entity;
//            public final int status;
//            public final String contentType;
//
//            public Result(String entity, int status, String contentType) {
//                this.entity = entity;
//                this.status = status;
//                this.contentType = contentType;
//            }
//        }
        
//        public static class Route {
//            
//            private final String routePattern;
//            
//            private final Method method;
//            
//            private final Router router;
//            
//            public Route(Method method, String routePattern, Router router) {
//                this.routePattern = routePattern;
//                this.method = method;
//                this.router = router;
//            }
//            
//            public Router perform(Function<ActionContext, Result> context) {
//                return router;
//            }
//            
//            boolean pathMatches(String p) {
//                return true;
//            }
//        }
        
//        public static class Render {
//            public static Result redirect(String url) {
//                ResponseBuilder builder;
//                try {
//                    builder = Response.seeOther(new URI(url));
//                } catch (URISyntaxException ex) {
//                    throw new RuntimeException(ex);
//                }
//                return builder.build();
//            }
//
//            public static Result text(final String text) {
//                return Response.ok(text, MediaType.TEXT_PLAIN).build();
//            }
//
//            public static Result binary(String file) {
//                return binary(file, MediaType.APPLICATION_OCTET_STREAM);
//            }
//
//            public static Result binary(File file) {
//                return binary(file, MediaType.APPLICATION_OCTET_STREAM);
//            }
//
//            public static Result binary(String file, String type) {
//                return Response.ok(new File(file), type).build();
//            }
//
//            public static Result binary(File file, String type) {
//                return Response.ok(file, type).build();
//            }
//
//            public static Result json(Object json) {
//                return Response.ok(json, MediaType.APPLICATION_JSON).build();
//            }
//
//            public static Result xml(Object xml) {
//                return Response.ok(xml, MediaType.APPLICATION_XML).build();
//            }
//
//            public static Result notFound() {
//                return new Result(new PageDuo("Page not found").toString(), 404, "text/html");
//            }
//
//            public static Result badRequest() {
//                return Response.status(Response.Status.BAD_REQUEST)
//                    .type(MediaType.TEXT_HTML)
//                    .entity(new PageDuo("Bad request").toString()).build();
//            }
//
//            public static Result ok() {
//                return Response.ok().build();
//            }
//
//            public static Result error() {
//                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .type(MediaType.TEXT_HTML)
//                    .entity(new PageDuo("Error").toString()).build();
//            }
//
//            public static Result unavailable() {
//                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
//                    .type(MediaType.TEXT_HTML)
//                    .entity(new Page("Error", "<h1>Service unavailable</h1>").toString()).build();
//            }
//
//            public static Result accesDenied() {
//                return Response.status(Response.Status.FORBIDDEN)
//                    .type(MediaType.TEXT_HTML)
//                    .entity(new PageDuo("Acces denied").toString()).build();
//            }
//
//            public static Result todo() {
//                return Response.status(501)
//                    .type(MediaType.TEXT_HTML)
//                    .entity(new Page("TODO", "<h1>Page not yet implemented</h1>").toString()).build();
//            }

//            static class PageDuo extends Page {
//
//                public PageDuo(String title) {
//                    super(title, "<h1>" + title + "</h1>");
//                }
//            }
//
//            static class Page {
//                private final String title;
//                private final String boody;
//
//                public Page(String title, String boody) {
//                    this.title = title;
//                    this.boody = boody;
//                }
//
//                @Override
//                public String toString() {
//                    return "<html>"
//                            + "<head>"
//                                + "<title>" + title + "</title>"
//                            + "</head>"
//                            + "<body>"
//                                + boody
//                            + "</body>"
//                        + "</html>";
//                }
//            }
//        }
        
//        public static class ActionContext {
//            public String uri;
//            public String path;
//            public Method method;
//            
//            public String getParam(String param) {
//                return null;
//            }
//        }
//        
//        private final List<Route> routes = new ArrayList<Route>();
//
//        @Override
//        public void apply(Tuple<Request, Response> reqResp) {
//            String path = reqResp._1.uri;
//            if (path.contains("?")) {
//                path = path.split("\\?")[0];
//            }
//            Result r = null;
//            for (Route route : routes) {
//                if (route.method.equals(Method.ALL) || reqResp._1.method.equals(route.method.name())) {
//                    if (route.pathMatches(path)) {
//                        
//                    }
//                }
//            }
//            if (r == null) {
//                reqResp._2.statusCode = 404;
//                reqResp._2.write("\n" + reqResp._1.method + " " + path + "\n", "UTF-8").end();
//            }
//        }
//        
//        public Route get(String pattern) {
//            return new Route(Method.GET, pattern, this);
//        }
//        
//        public Route head(String pattern) {
//            return new Route(Method.HEAD, pattern, this);
//        }
//        
//        public Route post(String pattern) {
//            return new Route(Method.POST, pattern, this);
//        }
//        
//        public Route put(String pattern) {
//            return new Route(Method.PUT, pattern, this);
//        }
//        
//        public Route delete(String pattern) {
//            return new Route(Method.DELETE, pattern, this);
//        }
//        
//        public Route all(String pattern) {
//            return new Route(Method.ALL, pattern, this);
//        }
//    }
}
