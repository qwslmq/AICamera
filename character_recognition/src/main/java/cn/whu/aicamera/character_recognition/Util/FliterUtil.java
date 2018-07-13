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

    /**
     * 删除置信度(Average)低于最小置信度的结果
     * @param words_results 要筛选的对象list
     * @param MinProbability 最小的置信度
     * @return 筛选后的list
     */
    public static List<Words_result> FliterByMinProbability(List<Words_result> words_results, int MinProbability){
        for(Words_result words_result:words_results){
            if(words_result.getProbability().getAverage()<MinProbability){
                words_results.remove(words_result);
            }
        }
        return words_results;
    }

}
