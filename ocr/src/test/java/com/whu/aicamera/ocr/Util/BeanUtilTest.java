package com.whu.aicamera.ocr.Util;

import com.whu.aicamera.ocr.Bean.OcrGeneralPositionResult;

import org.junit.Test;

import static org.junit.Assert.*;

public class BeanUtilTest {

    @Test
    public void toGPResultObject() {
        String testJson = "{\n" +
                "\t\"log_id\": 676709620,\n" +
                "\t\"words_result\": [{\n" +
                "\t\t\"location\": {\n" +
                "\t\t\t\"height\": 20,\n" +
                "\t\t\t\"left\": 86,\n" +
                "\t\t\t\"top\": 387,\n" +
                "\t\t\t\"width\": 22\n" +
                "\t\t},\n" +
                "\t\t\"words\": \"N\"\n" +
                "\t}],\n" +
                "\t\"words_result_num\": 1\n" +
                "}";
        OcrGeneralPositionResult ocrGeneralPositionResult = BeanUtil.toGPResultObject(testJson);
        System.out.println(ocrGeneralPositionResult.getWords_result().get(0).getLocation().getHeight());
    }
}