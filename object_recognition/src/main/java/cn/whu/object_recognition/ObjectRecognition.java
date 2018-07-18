package cn.whu.object_recognition;

import com.baidu.aip.imageclassify.AipImageClassify;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ObjectRecognition {
    private static final String APP_ID = "11535230";
    private static final String API_KEY = "CkKeeheRmK1lRLtEg1qRITIB";
    private static final String SECRET_KEY = "QZpCPGxpfwMWzbEV7fdoUQPBwg4qMQlW";
    private static AipImageClassify client = new AipImageClassify(APP_ID, API_KEY, SECRET_KEY);

    public static String recognize(byte[] bytes) {
        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        HashMap<String, String> options = new HashMap<>();
        JSONObject res = client.advancedGeneral(bytes, options);
        Gson gson = new Gson();
        String results = "";
        try {
            Response response = gson.fromJson(res.toString(), Response.class);
            if (response.getResult() == null) results = res.getString("error_msg");
            else {
                StringBuilder sb = new StringBuilder();
                for (Response.Result result : response.getResult()) {
                    if (result.getScore() > 0.2)
                        sb.append(result.getRoot() + " " + result.getKeyword() + " " + result.getScore() + "\n");
                }
                results = sb.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }
}
