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

import cx.ath.mancel01.utils.F.Function;

public class HttpRunner {
        
//    public static void main(String... args) throws Exception {
//        Http server = Http.createServer(new Action<Tuple<Request, Response>>() {
//            @Override
//            public void apply(Tuple<Request, Response> reqResp) {
//                reqResp._2.write("<html><body><h1>Hello World! " 
//                    + System.currentTimeMillis() 
//                    + "</h1></body></html>", "UTF-8").end();
//            }
//        }).listen(8080);
//        SimpleLogger.info("Hit enter to stop the server ...");
//        System.in.read();
//        server.stop();
//    }
    
//    public static void main(String... args) throws Exception {
//        Router router = new Router();
//        router.
//            get("/").perform(new Function<ActionContext, Result>() {
//                @Override
//                public Result apply(ActionContext ctx) {
//
//                    return new Result();
//                }
//            }).
//            post("/").perform(null);
//        Http server = Http.createServer(router).listen(9000);
//        SimpleLogger.info("Hit enter to stop the server ...");
//        System.in.read();
//        server.stop();
//    }
}
