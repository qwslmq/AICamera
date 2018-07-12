package com.whu.aicamera.ocr;

import android.content.Context;

import com.baidu.aip.ocr.AipOcr;
import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.whu.aicamera.ocr.Util.ClientUtil;
import com.whu.aicamera.ocr.Util.TokenUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

/**
 * this class provide the static function to get the result of Ocr(text recognize)
 * @author Chen Wuqiao
 */
public class CharacterRecognition {
    public static AipOcr client;
    public static String recognize(byte[] bytes){
        if(null == client){
            ClientUtil.getClient();
        }
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("recognize_granularity", "big");
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("detect_language", "true");
        options.put("vertexes_location", "true");
        options.put("probability", "true");
        //调用接口
        return client.general(bytes,options).toString();
    }
}
