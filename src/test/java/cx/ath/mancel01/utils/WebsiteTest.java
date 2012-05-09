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

import cx.ath.mancel01.utils.F.Either;
import cx.ath.mancel01.utils.F.Function;
import cx.ath.mancel01.utils.F.Option;
import org.junit.Assert;
import org.junit.Test;

public class WebsiteTest {

    @Test
    public void testSuccess() {
        Option<String> result = selectWebsite(1);
        if (result.isDefined()) {
            System.out.println(result.get());
        } else {
            Assert.fail("fail");
        }
    }

    @Test
    public void testAuthFailure() {
        Option<String> result = selectWebsiteUnauth(1);
        if (result.isDefined()) {
            System.out.println(result.get());
            Assert.assertEquals(result.get(), "error : Unauthorized");
        } else {
            Assert.fail("fail, Option is None");
        }
    }

    @Test
    public void test404() {
        Option<String> result = selectWebsiteNotfound(1);
        if (result.isDefined()) {
            System.out.println(result.get());
            Assert.assertEquals(result.get(), "error : NotFound");
        } else {
            Assert.fail("fail, Option is None");
        }
    }

    public Option<String> selectWebsite(long id) {
        Option<Website> website = Website.find(id);
        return website.toRight(NotFound)
            .right
                .map(new Function<Website, Either<String, WebsiteCase>>() {
                    public Either<String, WebsiteCase> apply(final Website ws) {
                        Option<User> maybeUser = User.connected();
                        return maybeUser.map(
                            new Function<User, WebsiteCase>() {
                                public WebsiteCase apply(User webuser) {
                                    return WebsiteCase.create(ws.id, webuser.id);
                                }
                            }
                        ).toRight(Unauthorized);
                    }
                }).fold(error, foldSuccess); 
    }

    public Option<String> selectWebsiteUnauth(long id) {
        Option<Website> website = Website.find(id);
        return website.toRight(NotFound)
            .right
                .map(
                    new Function<Website, Either<String, WebsiteCase>>() {
                        public Either<String, WebsiteCase> apply(final Website ws) {
                            Option<User> maybeUser = User.unknown();
                            return maybeUser.map(
                                new Function<User, WebsiteCase>() {
                                    public WebsiteCase apply(User webuser) {
                                        return WebsiteCase.create(ws.id, webuser.id);
                                    }
                                }
                            ).toRight(Unauthorized);
                        }
                    }
                ).fold(error, foldSuccess);
    }

    public Option<String> selectWebsiteNotfound(long id) {
        Option<Website> website = Website.unknown(id);
        return website.toRight(NotFound)
            .right
                .map(
                    new Function<Website, Either<String, WebsiteCase>>() {
                        public Either<String, WebsiteCase> apply(final Website ws) {
                            Option<User> maybeUser = User.connected();
                            return maybeUser.map(
                                new Function<User, WebsiteCase>() {
                                    public WebsiteCase apply(User webuser) {
                                        return WebsiteCase.create(ws.id, webuser.id);
                                    }
                                }
                            ).toRight(Unauthorized);
                        }
                    }
                ).fold(error, foldSuccess);
    }

    public Function<String, String> error = new Function<String, String>() {
        public String apply(String value) {
            return "error : " + value.toString();
        }
    };
    
    public Function<Either<String, WebsiteCase>, String> foldSuccess = new Function<Either<String, WebsiteCase>, String>() {
        public String apply(Either<String, WebsiteCase> t) {
            return t.fold(error, success).get();
        }
    };
    
    public Function<WebsiteCase, String> success = new Function<WebsiteCase, String>() {
        public String apply(WebsiteCase value) {
            return "success : " + value.toString();
        }
    };

    public static class WebsiteCase {
        public static WebsiteCase create(long wsid, long userid) {
            return new WebsiteCase();
        }
    }

    public static class Website {

        private long id = 1;

        public static Option<Website> find(long id) {
            return Option.some(new Website());
        }
        public static Option<Website> unknown(long id) {
            return Option.none();
        }
    }

    public static class User {

        private long id = 1;

        public static Option<User> connected() {
            return Option.some(new User());
        }
        public static Option<User> unknown() {
            return Option.none();
        }
    }

    public static String NotFound = "NotFound";

    public static String Unauthorized = "Unauthorized";

}
