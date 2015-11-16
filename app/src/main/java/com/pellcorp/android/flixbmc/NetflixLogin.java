package com.pellcorp.android.flixbmc;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetflixLogin {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/_BuildID_) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36";
    public static final String LOGIN_URL = "https://signup.netflix.com/Login";

    private DefaultHttpClient client;
    private final CookieStore cookieStore = new BasicCookieStore();
    private final HttpContext localContext = new BasicHttpContext();

    public NetflixLogin() {
        HttpParams params = new BasicHttpParams();
        params.setParameter(AllClientPNames.USER_AGENT, USER_AGENT);
        this.client = new DefaultHttpClient(params);

        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        this.client.setCookieStore(cookieStore);
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public LoginState login(String email, String password) {
        try {
            String authUrl = getAuthUrl();

            HttpPost post = new HttpPost(LOGIN_URL);

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            parameters.add(new BasicNameValuePair("authURL", authUrl));
            parameters.add(new BasicNameValuePair("email", email));
            parameters.add(new BasicNameValuePair("password", password));
            parameters.add(new BasicNameValuePair("RememberMe", "on"));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters);
            post.setEntity(entity);

            HttpResponse response = client.execute(post, localContext);
            if (response.getStatusLine().getStatusCode() == 302) {
                return new LoginState(true, null);
            } else {
                String html = EntityUtils.toString(response.getEntity());
                Document doc = Jsoup.parse(html, LOGIN_URL);
                Elements elements = doc.getElementsByAttributeValue("id", "page-LOGIN");
                if (elements.size() > 0) {
                    return new LoginState(false, "Login response page missing");
                } else {
                    return new LoginState(true, null);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to login", e);
            return new LoginState(false, e.getMessage());
        }
    }

    private String getAuthUrl() throws ParseException, IOException {
        HttpGet get = new HttpGet(LOGIN_URL);
        HttpResponse response = client.execute(get, localContext);
        String html = EntityUtils.toString(response.getEntity());
        
        Document doc = Jsoup.parse(html, LOGIN_URL);
        Elements elements = doc.getElementsByAttributeValue("name", "authURL");
        String authUrl = elements.first().attr("value");
        return authUrl;
    }
}

