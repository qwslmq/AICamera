package com.example.face_recognition;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.util.Base64Util;

import org.json.JSONException;

import java.util.HashMap;

public class FaceRecognition {
    private static final String APP_ID = "11476995";
    private static final String API_KEY = "hZuOSlM0o5470sHAClsVBgdN";
    private static final String SECRET_KEY = "AaPy6kVouqU0umrop91AofY8zORf3eab";
    private static AipFace client;
    private static void init(){
        client = new AipFace(APP_ID, API_KEY, SECRET_KEY);
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");
    }
    public static String recognize(byte[] from) {
        init();
        String imageType = "BASE64";
        HashMap<String, String> options = new HashMap<>();
        options.put("face_field", "age,beauty,expression,gender,glasses,race");
        options.put("max_face_num", "2");
        options.put("face_type", "LIVE");
        try{
            String result = client.detect(Base64Util.encode(from), imageType, options).toString(2);
            return result;
        }
        catch (JSONException e){
            e.printStackTrace();
            return "Error Occurred!";
        }
    }
}
