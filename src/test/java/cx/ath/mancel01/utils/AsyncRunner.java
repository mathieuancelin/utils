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
import cx.ath.mancel01.utils.Http.Request;
import cx.ath.mancel01.utils.Http.Response;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRunner {
    
    public static Promise<String> p = new Concurrent.Promise<String>(); 
                
    public static CountDownLatch l = new CountDownLatch(1);
    
    public static AtomicInteger i = new AtomicInteger(0);
    
    public static void main(String... args) throws Exception {
        Http server = Http.createServer(new Action<Tuple<Request, Response>>() {
            @Override
            public void apply(Tuple<Request, Response> reqResp) {
                boolean update = reqResp._1.uri.toLowerCase().trim().contains("update");
                boolean reset = reqResp._1.uri.toLowerCase().trim().contains("reset");
                boolean iframe = reqResp._1.uri.toLowerCase().trim().contains("iframe");
                if (update) {
                    p.apply(reqResp._1.getParam("value"));
                    reqResp._2.write("OK\n");
                } else if (reset) {
                    p.apply("-1");
                    p = new Promise<String>();
                    reqResp._2.write("OK\n");
                } else if(iframe) {
                    int v = i.incrementAndGet();
                    System.out.println(reqResp._1.uri + " entering async " + v);
                    reqResp._2.async(p);
                    System.out.println(reqResp._1.uri + " async done " + v);
                } else {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<html><body><h1>Hello World! ").append(System.currentTimeMillis()).append("</h1>");
                    int c = 0;
                    for (int i = 0; i < 100; i++) {
                        c++;
                        builder.append("<iframe src=\"/iframe/").append(System.nanoTime()).append("\" width=\"50\" height=\"50\"></iframe>");
                        try {
                            Thread.currentThread().sleep(1);
                        } catch (InterruptedException ex) {
                            
                        }
                        if (c == 10) {
                            builder.append("<br/>");
                            c = 0;
                        }
                    }
                    builder.append("</body></html>");
                    reqResp._2.write(builder.toString());
                }
            }
        }).listen(9000);
        SimpleLogger.info("Hit enter to stop the server ...");
        System.in.read();
        server.stop();
    }

}
