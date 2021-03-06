package org.alking.tecent.ai.impl;

import org.alking.tecent.ai.HttpClient;
import org.alking.tecent.ai.domain.Image;
import org.alking.tecent.ai.util.JsonUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class BaseClient {

    public static final String SIGN_FIELD_APP_ID = "app_id";

    public static final String SIGN_FIELD_TIME_STAMP = "time_stamp";

    public static final String SIGN_FIELD_NONCE_STR = "nonce_str";

    public static final String SIGN_FIELD_APP_KEY = "app_key";

    public static final String SIGN_FIELD_SIGN = "sign";

    public static final String SIGN_FIELD_IMAGE = "image";

    public static final String SIGN_FIELD_IMAGE_URL = "image_url";

    private final String appId;

    private final String appKey;

    private final HttpClient httpClient;

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public BaseClient(String appId, String appKey, HttpClient httpClient) {
        if(StringUtils.isEmpty(appId) || StringUtils.isEmpty(appKey)){
            throw new IllegalArgumentException("appId or appKey is empty");
        }
        if(httpClient == null){
            throw new IllegalArgumentException("httpClient is null");
        }
        this.appId = appId;
        this.appKey = appKey;
        this.httpClient = httpClient;
    }

    protected  <T> T normalReq(final String url, final TreeMap<String, String> map, Class<T> clazz) throws IOException {
        String sign = this.calcSign(map);
        map.put(SIGN_FIELD_SIGN,sign);
        final String json = getHttpClient().doPostFormString(url, map);
        return JsonUtil.fromJson(json, clazz);
    }

    protected String parseBase64(final Image image) throws IOException {

        if(image == null){
            throw new IOException("image is null");
        }

        if(Image.RES_TYPE_BASE64 == image.getType()){
            return image.getUri();
        }

        if(Image.RES_TYPE_LOCAL == image.getType()){
            String path = image.getUri();
            byte[] bytes = FileUtils.readFileToByteArray(new File(path));
            return Base64.getEncoder().encodeToString(bytes);
        }

        if(Image.RES_TYPE_HTTP == image.getType()){
            String uri = image.getUri();
            byte[] bytes = this.httpClient.doGetBytes(uri);
            return Base64.getEncoder().encodeToString(bytes);
        }

        throw new IOException("image is invalid");
    }

    /**
     * add timestamp and nonce_str automatic
     */
    private String calcSign(TreeMap<String, String> map) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        map.put(SIGN_FIELD_APP_ID,this.appId);
        String timestamp = String.valueOf(System.currentTimeMillis()/1000);
        map.put(SIGN_FIELD_TIME_STAMP,timestamp);
        map.put(SIGN_FIELD_NONCE_STR,timestamp);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (SIGN_FIELD_APP_KEY.equals(key)) {
                continue;
            }
            if(SIGN_FIELD_SIGN.equals(key)){
                continue;
            }
            String urlEncode = URLEncoder.encode(value, "UTF-8");
            sb.append(String.format("%s=%s&", key, urlEncode));
        }
        sb.append(String.format("%s=%s", SIGN_FIELD_APP_KEY, this.appKey));
        return DigestUtils.md5Hex(sb.toString()).toUpperCase();
    }
}
