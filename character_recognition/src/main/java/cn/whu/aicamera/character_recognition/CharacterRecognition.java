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
    /**
     * 静态client对象，实现单例模式
     */
    public static AipOcr client;

    /**
     * @param bytes 图片的byte数组
     * @return 识别生成的文字
     */
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
//        options.put("vertexes_location", "true");
        options.put("probability", "true");
        //调用接口
        return getProbabilityFliterResult(client.general(bytes,options).toString(),3);
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
        return BeanUtil.getWordsWithProbability(FliterUtil.FliterByProbability(words_results,maxResult));
    }
}
