/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Control;

import Model.Record;
import Persistence.MongoMorphia;
import com.github.jmkgreen.morphia.Morphia;
import com.mongodb.Mongo;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.RequestToken;

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
@SessionScoped
public class AuthentificationStart implements Serializable {

    Mongo m;
    Morphia morphia;
    String dummy;
    boolean messageRendered = false;
    String uri = "";
    Twitter twitter;
    RequestToken requestToken;
    static boolean debug = false;
    Record r;
    boolean local = false;

    public AuthentificationStart() {
    }

    public static void main(String args[]) throws ServletException {
        debug = true;
        try {
            new AuthentificationStart().execute();
        } catch (TwitterException ex) {
            Logger.getLogger(AuthentificationStart.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void execute() throws TwitterException, ServletException {
        try {
            MongoMorphia mm = new MongoMorphia();
            mm.initialize();

            Map<String, List<String>> urlParams = new HashMap();

            if (!debug) {
                try {
                    FacesContext ctx = FacesContext.getCurrentInstance();
                    HttpServletRequest servletRequest = (HttpServletRequest) ctx.getExternalContext().getRequest();
                    uri = servletRequest.getRequestURI();
                    if (servletRequest.getQueryString() != null) {
                        uri = uri.concat("?").concat(servletRequest.getQueryString());
                    }
                    urlParams = getUrlParameters(uri);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(AuthentificationStart.class.getName()).log(Level.SEVERE, null, ex);
                }
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

            String id = "test";

            for (String key : urlParams.keySet()) {
                if ("id".equals(key)) {
                    id = urlParams.get(key).get(0);
                }
            }

            String callback;
            if (local) {
                callback = "http://localhost:8080/OAuthSimplifier/faces/success.xhtml?id=" + id;
            } else {
                callback = "http://www.exploreyourdata.com:8090/OAuthSimplifier/faces/success.xhtml?id=" + id;

            }

            MyOwnTwitterFactory factory = new MyOwnTwitterFactory();
            twitter = factory.createOneTwitterInstance();

            requestToken = twitter.getOAuthRequestToken(callback);
            r = new Record();
            r.setId(id);
            r.setRequestToken(requestToken);

            String urlAuthorization = requestToken.getAuthorizationURL();

            ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
//            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
//            request.logout();
//            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
//            if (session != null) {
//                session.invalidate();
//            }

            ec.redirect(urlAuthorization);
//            FacesContext.getCurrentInstance().responseComplete();

//            FacesContext.getCurrentInstance()
//                    .getPartialViewContext().getRenderIds().add("messageOK");
//            FacesContext.getCurrentInstance()
//                    .getPartialViewContext().getRenderIds().add("messageFAIL");
        } catch (IOException ex) {
            Logger.getLogger(AuthentificationStart.class.getName()).log(Level.SEVERE, null, ex);
        }

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
                    values = new ArrayList<>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }
        return params;
    }

    public boolean isMessageRendered() throws TwitterException, ServletException {
        execute();
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
