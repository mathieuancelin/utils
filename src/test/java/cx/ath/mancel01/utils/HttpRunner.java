package cx.ath.mancel01.utils;

import static cx.ath.mancel01.utils.F.*;
import static cx.ath.mancel01.utils.Http.*;

public class HttpRunner {
        
    public static void main(String... args) throws Exception {
        Http server = Http.createServer(new HttpCallback() {
            @Override
            public void apply(Tuple<Request, Response> reqResp) {
                reqResp._2.write("<html><body><h1>Hello World! " 
                    + System.currentTimeMillis() 
                    + "</h1></body></html>", "UTF-8").end();
            }
        }).listen(8080);
        SimpleLogger.info("Hit enter to stop the server ...");
        System.in.read();
        server.stop();
    }
}
