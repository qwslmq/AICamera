package com.example.face_recognition;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.util.Base64Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class FaceRecognition {
    private static final String APP_ID = "11476995";
    private static final String API_KEY = "hZuOSlM0o5470sHAClsVBgdN";
    private static final String SECRET_KEY = "AaPy6kVouqU0umrop91AofY8zORf3eab";
    private static AipFace client = new AipFace(APP_ID, API_KEY, SECRET_KEY);
    static {
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");
    }
    /*private static void init(){
        client = new AipFace(APP_ID, API_KEY, SECRET_KEY);
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");
    }*/
    public static String recognize(byte[] from) {
        //init();
        String imageType = "BASE64";
        HashMap<String, String> options = new HashMap<>();
        options.put("face_field", "age,beauty,expression,gender,glasses,race");
        options.put("max_face_num", "2");
        options.put("face_type", "LIVE");
        try{
            JSONObject jsonObject = client.detect(Base64Util.encode(from), imageType, options);
            if(jsonObject.getInt("error_code") != 0){
                return "No face recognized.";
            }
            StringBuilder stringBuilder = new StringBuilder();
            JSONObject result = jsonObject.getJSONObject("result");
            int face_num = 0;
            face_num = result.getInt("face_num");
            stringBuilder.append("Number of people: " + face_num + "\n");
            JSONArray face_list = result.getJSONArray("face_list");
            for(int i = 0; i<face_list.length(); i++){
                JSONObject o = face_list.getJSONObject(i);
                System.out.println("age: " + o.getInt("age"));
                stringBuilder.append("Age: " + o.getInt("age") + "\n");
                System.out.println("beauty: " + o.getDouble("beauty"));
                stringBuilder.append("Beauty: " + o.getDouble("beauty") + "\n");
                System.out.println("expression: " + o.getJSONObject("expression").getString("type"));
                stringBuilder.append("Expression: " + o.getJSONObject("expression").getString("type") + "\n");
                System.out.println("gender: " + o.getJSONObject("gender").getString("type"));
                stringBuilder.append("Gender: " + o.getJSONObject("gender").getString("type") + "\n");
                System.out.println("glasses: " + o.getJSONObject("glasses").getString("type"));
                stringBuilder.append("Glasses: " + o.getJSONObject("glasses").getString("type") + "\n");
                System.out.println("race: " + o.getJSONObject("race").getString("type"));
                stringBuilder.append("Race: " + o.getJSONObject("race").getString("type") + "\n");
            }
            return stringBuilder.toString();
        }
        catch(JSONException e){
            e.printStackTrace();
            return "Error occurred.";
        }
    }
}
