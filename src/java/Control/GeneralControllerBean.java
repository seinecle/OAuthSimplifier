/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Control;

import Model.AccessTokenPlus;
import Model.Record;
import Private.APIkeys;
import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/*
 Copyright 2008-2013 Clement Levallois
 Authors : Clement Levallois <clementlevallois@gmail.com>
 Website : http://www.clementlevallois.net


 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright 2013 Clement Levallois. All rights reserved.

 The contents of this file are subject to the terms of either the GNU
 General Public License Version 3 only ("GPL") or the Common
 Development and Distribution License("CDDL") (collectively, the
 "License"). You may not use this file except in compliance with the
 License. You can obtain a copy of the License at
 http://gephi.org/about/legal/license-notice/
 or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
 specific language governing permissions and limitations under the
 License.  When distributing the software, include this License Header
 Notice in each file and include the License files at
 /cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
 License Header, with the fields enclosed by brackets [] replaced by
 your own identifying information:
 "Portions Copyrighted [year] [name of copyright owner]"

 If you wish your version of this file to be governed by only the CDDL
 or only the GPL Version 3, indicate your decision by adding
 "[Contributor] elects to include this software in this distribution
 under the [CDDL or GPL Version 3] license." If you do not indicate a
 single choice of license, a recipient has the option to distribute
 your version of this file under either the CDDL, the GPL Version 3 or
 to extend the choice of license to its licensees as provided above.
 However, if you add GPL Version 3 code and therefore, elected the GPL
 Version 3 license, then the option applies only if the new code is
 made subject to such option by the copyright holder.

 Contributor(s): Clement Levallois

 */
@ManagedBean
@ViewScoped
public class GeneralControllerBean {

    Mongo m;
    Morphia morphia;
    String dummy;
    boolean messageRendered = false;
    String uri = "";
    Twitter twitter;
    RequestToken requestToken;
    AccessToken accessToken;
    static boolean debug = false;
    Record r = new Record();

    public GeneralControllerBean() {
    }

    public static void main(String args[]) {
        debug = true;
        try {
            new GeneralControllerBean().init();
        } catch (TwitterException ex) {
            Logger.getLogger(GeneralControllerBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PostConstruct
    public String init() throws TwitterException {
        try {
            MongoClient mongoClient = new MongoClient("localhost", 27017);
            morphia = new Morphia();
            morphia.map(Record.class);
            morphia.map(AccessTokenPlus.class);

            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(APIkeys.getTwitterAPIKey());
            builder.setOAuthConsumerSecret(APIkeys.getTwitterAPISecret());
            Configuration configuration = builder.build();
            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();
//            twitter = TwitterFactory.getSingleton();
//            twitter.setOAuthConsumer(APIkeys.getTwitterAPIKey(), APIkeys.getTwitterAPISecret());

            Datastore dsOAuth = morphia.createDatastore(mongoClient, "oAuth");

            Map<String, List<String>> urlParams;
            if (!debug) {
                FacesContext ctx = FacesContext.getCurrentInstance();
                HttpServletRequest servletRequest = (HttpServletRequest) ctx.getExternalContext().getRequest();
                uri = servletRequest.getRequestURI();
                if (servletRequest.getQueryString() != null) {
                    uri = uri.concat("?").concat(servletRequest.getQueryString());
                }
                urlParams = getUrlParameters(uri);
            } else {
                urlParams = new HashMap();
                List<String> values = new ArrayList();
                values.add("this is an id");
                values.add("token");
                values.add("token verifier");
                urlParams.put("id", values);
                urlParams.put("oauth_token", values);
                urlParams.put("oauth_verifier", values);
            }

            for (String key : urlParams.keySet()) {
                if ("id".equals(key)) {
                    r.setId(urlParams.get(key).get(0));
                }
                if ("oauth_token".equals(key)) {
                    if (debug) {
                        r.setoAuth_token(urlParams.get(key).get(1));
                    } else {
                        r.setoAuth_token(urlParams.get(key).get(0));
                    }
                }
                if ("oauth_verifier".equals(key)) {
                    if (debug) {
                        r.setoAuth_verifier(urlParams.get(key).get(2));
                    } else {
                        r.setoAuth_verifier(urlParams.get(key).get(0));
                    }
                }
            }

            if (r.getIdGephi() != null & r.getoAuth_token() != null & r.getoAuth_verifier() != null) {
                //the access token is saved temporarily for the purpose of first identification
                dsOAuth.save(r);

                //the access token is saved permanently
//                requestToken = twitter.getOAuthRequestToken(r.getoAuth_token());
//                accessToken = twitter.getOAuthAccessToken(requestToken,r.getoAuth_verifier());
                messageRendered = true;

            }

        } catch (UnknownHostException | UnsupportedEncodingException ex) {
            Logger.getLogger(GeneralControllerBean.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        FacesContext.getCurrentInstance()
                .getPartialViewContext().getRenderIds().add("messageOK");
        FacesContext.getCurrentInstance()
                .getPartialViewContext().getRenderIds().add("messageFAIL");

        return null;
    }

    public String getDummy() {
        return dummy;
    }

    public void setDummy(String dummy) {
        this.dummy = dummy;
    }

    public static Map<String, List<String>> getUrlParameters(String url)
            throws UnsupportedEncodingException {
        Map<String, List<String>> params = new HashMap<>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 1) {
            String query = urlParts[1];
            for (String param : query.split("&")) {
                String pair[] = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }
                List<String> values = params.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }
        return params;
    }

    public boolean isMessageRendered() {
        return messageRendered;
    }

    public void setMessageRendered(boolean messageRendered) {
        this.messageRendered = messageRendered;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
