package com.owner.temp.util;

import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * http 网络请求工具类
 *
 * @author XiaoShuoKai
 */
public class HttpUtils {

    public static final String FORM = "application/x-www-form-urlencoded";
    public static final String JSON = "application/json";

    /**
     * 请求方式 - post
     */
    private static final String REQUEST_POST = "POST";
    /**
     * 请求方式 - get
     */
    private static final String REQUEST_GET = "GET";
    /**
     * 超时时间
     */
    private static final int TIME_OUT = 5 * 1000;

    /**
     * 发送post网络请求
     *
     * @param path   请求路径
     * @param params 请求参数(Map<String, String>)
     * @return 响应结果
     * @throws Exception
     */
    public static String sendPost(String path, Map<String, String> params) throws Exception {
        return sendPost(path, getChangeResult(params), null, JSON);
    }

    /**
     * 发送post网络请求
     *
     * @param path   请求路径
     * @param params 请求参数(JSONObject<String, Object>)
     * @return 响应结果
     * @throws Exception
     */
    public static String sendPost(String path, JSONObject params, JSONObject headerParam) throws Exception {
        return sendPost(path, params.toJSONString(), headerParam, JSON);
    }

    /**
     * 发送post网络请求
     *
     * @param path     请求路径
     * @param params   请求参数(Map<String, String>)
     * @param dataType 数据类型
     * @return
     * @throws Exception
     */
    public static String sendPost(String path, Map<String, String> params, String dataType) throws Exception {
        return sendPost(path, getChangeResult(params), null, dataType);
    }

    /**
     * 发送Post请求
     *
     * @param path        地址
     * @param postString  post参数
     * @param headerParam 头部参数
     * @param dataType    数据类型
     * @return String
     * @throws Exception 网络异常时抛出
     */
    private static String sendPost(String path, String postString, JSONObject headerParam, String dataType) throws Exception {
        return getStringFromResponse(send(path, postString, headerParam, dataType, REQUEST_POST));
    }

    /**
     * 发送get请求
     *
     * @param path       地址
     * @param postString 请求参数
     * @param dataType   数据类型
     * @return InputStream InputStream
     * @throws Exception 网络异常时抛出
     */
    private static InputStream sendGetReturnInputStream(String path, String postString, String dataType) throws Exception {
        return send(path, postString, null, dataType, REQUEST_GET);
    }

    /**
     * * 发送post 请求
     *
     * @param path        请求路径
     * @param postString  请求参数(Map<String, String>)
     * @param dataType    传输数据类型
     * @param headerParam 请求头部参数
     * @return String
     * @throws Exception
     */
    public static InputStream send(String path, String postString, JSONObject headerParam, String dataType, String requestMethod) throws Exception {
        // 得到实体的二进制数据，以便计算长度
        byte[] entityData = postString.getBytes("UTF-8");
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(requestMethod);
        conn.setConnectTimeout(TIME_OUT);
        // 如果通过post提交数据，必须设置允许对外输出数据
        conn.setDoOutput(true);
        // Content-Type: application/json
        // Content-Length: 38
        if (StringUtils.isEmpty(dataType)) {
            dataType = JSON;
        }
        conn.setRequestProperty("Content-Type", dataType);
        // 传输的数据长度
        conn.setRequestProperty("Content-Length", String.valueOf(entityData.length));
        //设置请求头参数
        setHeaderParam(conn, headerParam);
        OutputStream outStream = conn.getOutputStream();
        outStream.write(entityData);
        // 把内存中的数据刷新输送给对方
        outStream.flush();
        outStream.close();
        // 获取服务端的响应200代表成功
        String resultString = "";

        if (conn.getResponseCode() == 200) {
            return conn.getInputStream();
        } else {
            System.out.println("responseError request url:" + path + ", param is:" + postString + ", responseCode is:" + conn.getResponseCode());
        }
        return null;
    }

    private static String getStringFromResponse(InputStream inputStream) {
        if (inputStream != null) {
            return dealResponseResult(inputStream);
        }
        return null;
    }

    /**
     * 设置请求头参数
     *
     * @param conn        连接对象
     * @param headerParam 参数
     */
    private static void setHeaderParam(HttpURLConnection conn, JSONObject headerParam) {
        if (conn == null || headerParam == null) {
            return;
        }
        Iterator<String> iterator = headerParam.keySet().iterator();
        iterator.forEachRemaining(next -> {
            if (headerParam.containsKey(next)) {
                conn.setRequestProperty(next, headerParam.get(next).toString());
            }
        });
    }

    /**
     * 处理服务器的响应结果（将输入流转化成字符串）
     *
     * @param inputStream
     * @return
     */
    public static String dealResponseResult(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while ((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(byteArrayOutputStream.toByteArray());
    }

    /**
     * 将map转成String ,例如?title=dsfdsf&timelength=23&method=save
     *
     * @param params
     * @return
     */
    private static String getChangeResult(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 获取请求对象中的参数转换为JSON
     *
     * @param request
     * @return
     * @throws IOException
     */
    public static JSONObject getParameters(HttpServletRequest request) throws IOException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = streamReader.readLine()) != null) {
            responseStrBuilder.append(inputStr);
        }
        return JSONObject.parseObject(responseStrBuilder.toString());
    }

    /**
     * 发送get请求
     *
     * @param url url
     * @return String
     */
    public static String sendGet(String url){
        try {
            String content = null;
            URLConnection urlConnection = new URL(url).openConnection();
            HttpURLConnection connection = (HttpURLConnection) urlConnection;
            connection.setRequestMethod("GET");
            //连接
            connection.connect();
            //得到响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader
                        (connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder bs = new StringBuilder();
                String l;
                while ((l = bufferedReader.readLine()) != null) {
                    bs.append(l).append("\n");
                }
                content = bs.toString();
            }
            return content;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

//    /**
//     * 发送get请求
//     *
//     * @param url   url
//     * @param param param
//     * @return String
//     */
//    public static String sendGet(String url, Map<String, String> param) {
//        StringBuilder result = new StringBuilder();
//        BufferedReader in = null;
//        try {
//            URL realUrl = new URL(url);
//            // 打开和URL之间的连接
//            URLConnection connection = realUrl.openConnection();
//            // 设置通用的请求属性
//            connection.setRequestProperty("accept", "*/*");
//            connection.setRequestProperty("connection", "Keep-Alive");
//            connection.setRequestProperty("user-agent",
//                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
//            // 建立实际的连接
//            connection.connect();
//            // 遍历所有的响应头字段
//            if (param != null) {
//                for (String key : param.keySet()) {
//                    connection.addRequestProperty(key, param.get(key));
//                }
//            }
//            // 定义 BufferedReader输入流来读取URL的响应
//            in = new BufferedReader(new InputStreamReader(
//                    connection.getInputStream()));
//            String line;
//            while ((line = in.readLine()) != null) {
//                result.append(line);
//            }
//        } catch (Exception e) {
//            System.out.println("发送GET请求出现异常！" + e);
//            e.printStackTrace();
//        }
//        // 使用finally块来关闭输入流
//        finally {
//            try {
//                if (in != null) {
//                    in.close();
//                }
//            } catch (Exception e2) {
//                e2.printStackTrace();
//            }
//        }
//        return result.toString();
//    }
}
