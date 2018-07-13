package cn.whu.aicamera.character_recognition;

import com.baidu.aip.ocr.AipOcr;


import java.util.HashMap;
import java.util.List;

import cn.whu.aicamera.character_recognition.Bean.OcrGeneralPositionResult;
import cn.whu.aicamera.character_recognition.Bean.Words_result;
import cn.whu.aicamera.character_recognition.Util.BeanUtil;
import cn.whu.aicamera.character_recognition.Util.ClientUtil;
import cn.whu.aicamera.character_recognition.Util.FliterUtil;

/**
 * this class provide the static function to get the result of Ocr(text recognize)
 * @author Chen Wuqiao
 */
public class CharacterRecognition {
    public static final String GENERAL = "general";
    public static final String BASIC_GENERAL = "basicGeneral";
    public static final String BASIC_ACCURATE_GENERAL = "basicAccurateGeneral";
    public static final String ACCURATE_GENERAL = "accurateGeneral";
    /**
     * 静态client对象，实现单例模式
     */
    public static AipOcr client;
    public static int MaxResult = 5;
    /**
     * 文字识别默认接口，默认为通用文字识别，无位置信息，普通精度，根据置信度进行筛选
     * @param bytes 图片的byte数组
     * @return 识别生成的文字
     */
    public static String recognize(byte[] bytes){
        return basicGeneralRecognize(bytes);
    }

    /**
     * 文字识别带识别方式接口，可制定文字识别方式 ，默认根据置信度进行筛选，
     * @param bytes 图片的bytes数组
     * @param recognizeType 识别的方式，如general 通用文字识别（带位置版），默认为通用文字识别（不带位置信息）
     * @return 识别生成的文字
     */
    public static String recognize(byte[] bytes, String recognizeType){
        String result = "";
        switch (recognizeType){
            case BASIC_GENERAL:
                result = basicGeneralRecognize(bytes);
                break;
            case GENERAL:
                result = GeneralRecognize(bytes);
                break;
            case BASIC_ACCURATE_GENERAL:
                result = basicGeneralRecognize(bytes);
                break;
            case ACCURATE_GENERAL:
                result = GeneralRecognize(bytes);
                break;
            default:
                result = basicGeneralRecognize(bytes);
                break;
        }
        return result;
    }


    /**
     * 通用文字识别（不含位置信息，普通精度）
     * @param bytes 图片的byte数组
     * @return 识别生成的文字
     */
    public static String basicGeneralRecognize(byte[] bytes){
        ClientUtil.checkClient();
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("detect_language", "true");
        options.put("probability", "true");
        return getProbabilityFliterResult(client.basicGeneral(bytes,options).toString(),MaxResult);
    }

    /**
     * 通用文字识别含位置信息版
     * @param bytes 图片的byte数组
     * @return 识别生成的文字
     */
    public static String GeneralRecognize(byte[] bytes){
        ClientUtil.checkClient();
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("recognize_granularity", "big");
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("detect_language", "true");
        options.put("probability", "true");
        //调用接口
        return getProbabilityFliterResult(client.general(bytes,options).toString(),MaxResult);
    }
    /**
     * 通用文字识别高精度版，不含位置信息
     * @param bytes 图片的byte数组
     * @return 识别生成的文字
     */
    public static String basicAccurateGeneralRecognize(byte[] bytes){
        ClientUtil.checkClient();
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("detect_direction", "true");
        options.put("probability", "true");
        //调用接口
        return getProbabilityFliterResult(client.basicAccurateGeneral(bytes,options).toString(),MaxResult);
    }

    /**
     * 通用文字识别高精度版，含位置信息
     * @param bytes 图片的byte数组
     * @return 识别生成的文字
     */
    public static String AccurateGeneralRecognize(byte[] bytes){
        ClientUtil.checkClient();
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("recognize_granularity", "big");
        options.put("detect_direction", "true");
        options.put("probability", "true");
        //调用接口
        return getProbabilityFliterResult(client.accurateGeneral(bytes,options).toString(),MaxResult);
    }

    /**
     * 根据置信度对返回结果进行筛选，按Average置信度进行排序，选取不超过最大返回字段数的多个字段。
     * @param originResult 初始识别生成的结果字符串，json格式
     * @param maxResult 最大返回字段数
     * @return 筛选后的结果，仅包含文字
     */
    public static String getProbabilityFliterResult(String originResult, int maxResult){
        OcrGeneralPositionResult resultBean = BeanUtil.toGPResultObject(originResult);
        List<Words_result> words_results = resultBean.getWords_result();
        return BeanUtil.getWords(FliterUtil.FliterByProbability(words_results,maxResult));
    }
}
