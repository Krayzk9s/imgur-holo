package com.krayzk9s.imgurholo;

import android.content.SharedPreferences;
import android.util.Log;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ImgUr3Api;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.util.HashMap;

/**
 * Created by info on 8/30/13.
 */
public class ApiCall {
    public static final String OAUTH_CALLBACK_SCHEME = "imgur-holo";
    public static final String OAUTH_CALLBACK_HOST = "authcallback";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
    public static final String MASHAPE_KEY = "CoV9d8oMmqhy8YdAbCAnB1MroW1xMJpP";
    public static final String MASHAPE_URL = "https://imgur-apiv3.p.mashape.com/";
    private static final String CLIENTID = "4cd3f96f162ac80";
    private static final String SECRETID = "9cd3c621a4e064422e60aba4ccf84d6b149b4463";
    public static final Token EMPTY_TOKEN = null;
    final OAuthService service = new ServiceBuilder().provider(ImgUr3Api.class).apiKey(CLIENTID).debug().callback(OAUTH_CALLBACK_URL).apiSecret(SECRETID).build();
    Token accessToken;
    Verifier verifier;
    boolean loggedin;
    SharedPreferences settings;

    public ApiCall(SharedPreferences _settings) {
        settings = _settings;
        if(settings.contains("AccessToken"))
            loggedin = true;
    }

    public JSONObject makeCall(String url, String method, HashMap<String, Object> args) {
        Log.d("Call", url);
        try {
            JSONObject data = null;
            String methodString = null;
            if (url.contains("?"))
                methodString = "&_method=" + method;
            else
                methodString = "?_method=" + method;
            if (loggedin) {
                Token accessKey = getAccessToken();
                Log.d("Making Call", accessKey.toString());
                HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + url + methodString)
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Bearer " + accessKey.getToken())
                        .fields(args)
                        .asJson();
                Log.d("Getting Code", String.valueOf(response.getCode()));
                int code = response.getCode();
                if (code == 403) {
                    accessKey = renewAccessToken();
                    response = Unirest.post(MASHAPE_URL + url + methodString)
                            .header("accept", "application/json")
                            .header("X-Mashape-Authorization", MASHAPE_KEY)
                            .header("Authorization", "Bearer " + accessKey.getToken())
                            .fields(args)
                            .asJson();
                }
                if (code == 200) {
                    data = response.getBody().getObject();
                    Log.d("Got data", data.toString());
                }
            } else {
                HttpResponse<JsonNode> response = Unirest.post(MASHAPE_URL + url + methodString)
                        .header("accept", "application/json")
                        .header("X-Mashape-Authorization", MASHAPE_KEY)
                        .header("Authorization", "Client-ID " + CLIENTID)
                        .fields(args)
                        .asJson();
                Log.d("Getting Code", String.valueOf(response.getCode()));
                int code = response.getCode();
                if (code == 403) {
                    response = Unirest.post(MASHAPE_URL + url + methodString)
                            .header("accept", "application/json")
                            .header("X-Mashape-Authorization", MASHAPE_KEY)
                            .header("Authorization", "Client-ID " + CLIENTID)
                            .fields(args)
                            .asJson();
                }
                if (code == 200) {
                    data = response.getBody().getObject();
                    Log.d("Got data", data.toString());
                }
            }
            return data;
        } catch (Exception e) {
            Log.e("Error getting data!", e.toString());
            return null;
        }
    }
    public Token renewAccessToken() {
        SharedPreferences.Editor editor = settings.edit();
        accessToken = service.refreshAccessToken(accessToken);
        Log.d("URI", accessToken.getRawResponse());
        editor.putString("AccessToken", accessToken.getToken());
        editor.commit();
        return accessToken;
    }

    public Token getAccessToken() {
        if (settings.contains("RefreshToken")) {
            accessToken = new Token(settings.getString("AccessToken", ""), settings.getString("RefreshToken", ""));
            loggedin = true;
            Log.d("URI", accessToken.toString());
        } else {
            loggedin = false;
        }
        return accessToken;
    }
}
