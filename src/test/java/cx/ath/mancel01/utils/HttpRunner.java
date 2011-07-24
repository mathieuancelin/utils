package cx.ath.mancel01.utils;

import cx.ath.mancel01.utils.F.Tuple;
import cx.ath.mancel01.utils.Http.HttpRequest;
import cx.ath.mancel01.utils.Http.HttpResponse;

public class HttpRunner {
        
    public static void main(String... args) throws Exception {
        Http server = Http.createServer(new Http.HttpCallback() {
            @Override
            public void apply(Tuple<HttpRequest, HttpResponse> reqResp) {
                reqResp._2.write("<html><body><h1>Hello World!</h1></body></html>", "UTF-8").end();
            }
        }).listen(8080);
        SimpleLogger.info("hit enter to stop the server ...");
        System.in.read();
        server.stop();
    }
}
