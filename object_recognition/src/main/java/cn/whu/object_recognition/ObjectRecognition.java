package cn.whu.object_recognition;

import com.baidu.aip.imageclassify.AipImageClassify;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ObjectRecognition {
    public static final String APP_ID = "11476931";
    public static final String API_KEY = "TNV6j00ik0BaosonghYdcpGq";
    public static final String SECRET_KEY = "v7IjM7AVx26xOtYZ6qtGBhBNb1W0a2UZ";

    public static String recognize(byte[] bytes){
        AipImageClassify client = new AipImageClassify(APP_ID, API_KEY, SECRET_KEY);
        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        HashMap<String, String> options = new HashMap<>();
        options.put("top_num","3");
        JSONObject res = client.advancedGeneral(bytes, options);
        String result = null;
        try {
            result = res.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
