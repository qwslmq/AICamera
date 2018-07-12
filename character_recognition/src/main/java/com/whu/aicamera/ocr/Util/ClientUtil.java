package com.whu.aicamera.ocr.Util;

import com.baidu.aip.ocr.AipOcr;
import com.whu.aicamera.ocr.CharacterRecognition;

import org.json.JSONObject;

import java.util.HashMap;

public class ClientUtil {
    public static final String APP_ID = "11528113";
    public static final String API_KEY = "Wt5gGEz7XImQpBqjk9aSsYKN";
    public static final String SECRET_KEY = "GuDBV7S1RtKAWqpVWtrKDLi1GNeWuw2c";
    public static void getClient(){
        // 初始化一个AipOcr
        CharacterRecognition.client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        CharacterRecognition.client.setConnectionTimeoutInMillis(2000);
        CharacterRecognition.client.setSocketTimeoutInMillis(60000);

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
        System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");
    }
}
