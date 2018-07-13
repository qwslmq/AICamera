package cn.whu.aicamera.character_recognition.Util;

import java.util.Collections;
import java.util.List;

import cn.whu.aicamera.character_recognition.Bean.OcrGeneralPositionResult;
import cn.whu.aicamera.character_recognition.Bean.Words_result;

public class FliterUtil {
    /**
     * 对list按照Average置信度进行排序筛选，返回筛选后的对象list。
     * @param words_results 包含识别结果对象的List
     * @param MaxResult 最大返回字段数
     * @return 筛选后的对象list
     */
    public static List<Words_result> FliterByProbability(List<Words_result> words_results, int MaxResult){
        Collections.sort(words_results);
        int subEnd = words_results.size();
        if(subEnd > MaxResult ){
            subEnd = MaxResult;
        }
        return words_results.subList(0,subEnd);
    }
}
