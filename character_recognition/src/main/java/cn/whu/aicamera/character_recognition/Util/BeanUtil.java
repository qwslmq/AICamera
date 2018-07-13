package cn.whu.aicamera.character_recognition.Util;


import com.alibaba.fastjson.*;

import java.util.List;

import cn.whu.aicamera.character_recognition.Bean.OcrGeneralPositionResult;
import cn.whu.aicamera.character_recognition.Bean.Words_result;

/**
 * 提供json转化为bean的static方法
 */
public class BeanUtil {
    /**
     * 用于将含位置信息的通用文字识别返回结果（字符串类型）转换为实体对象
     * @param resultInJsonString json字符串
     * @return OcrGeneralPositonResult对象
     */
    public static OcrGeneralPositionResult toGPResultObject(String resultInJsonString){
        return JSONObject.parseObject(resultInJsonString,OcrGeneralPositionResult.class);
    }

    /**
     * 从list中提取字段，换行返回。
     * @param words_results 识别结果对象list
     * @return 从list中提取出的字段
     */
    public static String getWords(List<Words_result> words_results){
        StringBuilder stringBuilder = new StringBuilder();
        for(Words_result words_result : words_results){
            stringBuilder.append(words_result.getWords());
            stringBuilder.append("\r\n"); //换行
        }
        return stringBuilder.toString();
    }
    public static String getWordsWithProbability(List<Words_result> words_results){
        StringBuilder stringBuilder = new StringBuilder();
        for(Words_result words_result : words_results){
            stringBuilder.append(words_result.getWords());
            stringBuilder.append("\t");
            stringBuilder.append(words_result.getProbability().getAverage());
            stringBuilder.append("\r\n"); //换行
        }
        return stringBuilder.toString();
    }
}
