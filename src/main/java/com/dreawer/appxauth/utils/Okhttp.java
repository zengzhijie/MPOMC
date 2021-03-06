package com.dreawer.appxauth.utils;

import com.dreawer.appxauth.consts.ThirdParty;
import com.dreawer.appxauth.exception.WxAppException;
import com.dreawer.appxauth.manager.TokenManager;
import com.dreawer.responsecode.rcdt.ResponseCode;
import com.dreawer.responsecode.rcdt.Success;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <CODE>Okhttp</CODE>
 *
 * @author fenrir
 * @Date 18-7-3
 */
@Component
@Slf4j
public class Okhttp {

    private Logger logger = Logger.getLogger(Okhttp.class); // 日志记录器

    @Autowired
    private ThirdParty thirdParty;

    @Autowired
    private TokenManager tokenManager;


    public final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    /**
     * 同步的Get请求
     *
     * @param url url
     */
    public String getSync(String url) throws IOException {
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        return responseStr;
    }


    /**
     * 异步的Get请求
     *
     * @param url url
     */
    public void getAsyn(String url) {
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        // 请求加入调度
        call.enqueue(new Callback() {
            // 请求失败的回调
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("请求失败", e);
            }

            // 请求成功的回调
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 将response转化成String
                String responseStr = response.body().string();
            }
        });
    }

    /**
     * 同步的Post请求
     *
     * @param url    url
     * @param params params
     * @return responseStr
     * @throws IOException
     */
    public String postSync(String url, Map<String, String> params)
            throws IOException {
        // RequestBody
        RequestBody requestBody;
        if (params == null) {
            params = new HashMap<>();
        }
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        /**
         * 在这对添加的参数进行遍历
         */
        for (Map.Entry<String, String> map : params.entrySet()) {
            String key = map.getKey();
            String value;
            /**
             * 判断值是否是空的
             */
            if (map.getValue() == null) {
                value = "";
            } else {
                value = map.getValue();
            }
            /**
             * 把key和value添加到formBody中
             */
            builder.add(key, value);
        }
        requestBody = builder.build();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        return responseStr;
    }

    /**
     * 同步的Json格式POST
     *
     * @param url
     * @param RequestJsonbean
     * @return
     * @throws IOException
     */
    public String postSyncJson(String url, Object RequestJsonbean) throws IOException {

        Gson gson = new Gson();
        String json = gson.toJson(RequestJsonbean);
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);

        // 创建一个Request
        final Request request = new Request.Builder().url(url).post(body).build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        return responseStr;
    }

    /**
     * 同步的Json格式POST
     *
     * @param url
     * @return
     * @throws IOException
     */
    public String SimplepostSyncJson(String url, String json) throws IOException {

        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);

        // 创建一个Request
        final Request request = new Request.Builder().url(url).post(body).build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        return responseStr;

    }


    public ResponseCode testToken(String accessToken, String appid) throws IOException {
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(thirdParty.URL_CATEGORY_QUERY(accessToken))
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        // 将response转化成String
        String responseStr = response.body().string();
        JSONObject jsonObject = new JSONObject(responseStr);
        log.info("appid为" + appid + "测试Token结果" + responseStr);
        //如果微信返回报错信息
        String errcode;
        if (jsonObject.has("errcode")) {
            errcode = jsonObject.get("errcode") + "";
            //微信61007错误码为小程序在授权给该平台时之前已经授权给上一个平台
            //导致接调用失败,这种情况下所有开发接口均不可使用,需抛出异常给上层处理
            if (errcode.equals("61007")) {
                throw new WxAppException("61007", "该小程序已经授权给其他第三方平台");
            }
            //42001token过期 重新刷新
            if (errcode.equals("42001")) {
                String token = tokenManager.refreshToken(appid);
                return Success.SUCCESS(token);
            } else {
                return Success.SUCCESS(accessToken);
            }
        } else {
            return Success.SUCCESS(accessToken);
        }
    }

    /**
     * 获取QRcode
     *
     * @param url url
     */
    public String getQRcode(String url) throws IOException {
        // 创建OKHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        // 创建一个Request
        final Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        String codeUrl = null;
        if (response.isSuccessful()) {
            byte[] bytes = response.body().bytes();
            UploadImage(bytes);
        }
        return codeUrl;

    }

    public void UploadImage(byte[] file) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "2.jpg", RequestBody.create(MediaType.parse("image/png"), file))
                .build();
        Request request = new Request.Builder()
                .url("https://image.dreawer.com/uploadImage?appname=APPX&type=IMAGE")
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);
        // 返回值为response
        Response response = call.execute();
        logger.info(response.body().toString());
    }

}
