package cn.whu.aicamera.character_recognition.Util;


import com.alibaba.fastjson.*;

import cn.whu.aicamera.character_recognition.Bean.OcrGeneralPositionResult;

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
}