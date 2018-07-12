package com.whu.aicamera.ocr.Util;

import android.content.Context;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;

public class TokenUtil {

    /**
     * 用明文ak，sk初始化
     */
    public static void initAccessTokenWithAkSk(Context context) {
        OCR.getInstance(context).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken result) {
                String token = result.getAccessToken();
                System.out.println(token);
            }
            @Override
            public void onError(OCRError error) {
                error.printStackTrace();
            }
        }, context.getApplicationContext(),  "rbAmZcYh8QWn7ttzpGhdBiP9", "B4fYVHczVXBCsSgyMOtwyQ9RBpjDrj0O");
    }
}
