package com.baidu.ardemo;


import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.baidu.ar.ARFragment;
import com.baidu.ar.bean.DuMixARConfig;
import com.baidu.ar.constants.ARConfigKey;
import com.baidu.ar.external.ARCallbackClient;
import com.baidu.ar.util.Res;

import org.json.JSONException;
import org.json.JSONObject;

public class ARActivity extends AppCompatActivity {

    private ARFragment mARFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        // 设置获取资源的上下文Context
        Res.addResource(this);
        // 设置App Id
        DuMixARConfig.setAppId("13437");
        // 设置API Key
        DuMixARConfig.setAPIKey("81253f86960db69794c710f58ab3c4ef");
        // 设置Secret Key
        //DuMixARConfig.setSecretKey("D1pGOGbYYxdyfgYlHZe33K30vxNG3FXG");


        if(findViewById(R.id.bdar_id_fragment_container)!=null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            //调起AR的参数
            String arKey = getIntent().getStringExtra(ARConfigKey.AR_KEY);
            int arType = getIntent().getIntExtra(ARConfigKey.AR_TYPE,0);

            Bundle data = new Bundle();
            JSONObject jsonObject = new JSONObject();
            try{
                jsonObject.put(ARConfigKey.AR_KEY,arKey);
                jsonObject.put(ARConfigKey.AR_TYPE,arType);
            }catch(JSONException e){
                e.printStackTrace();
            }
            data.putString(ARConfigKey.AR_VALUE, jsonObject.toString());
            mARFragment = new ARFragment();
            mARFragment.setArguments(data);
            mARFragment.setARCallbackClient(new ARCallbackClient() {
                @Override
                public void openUrl(String url) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri contentUrl = Uri.parse(url);
                    intent.setData(contentUrl);
                    startActivity(intent);
                }

                // AR黑名单回调接口：当手机不支持AR时，通过该接口传入退化H5页面的url
                @Override
                public void nonsupport(String url) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri contentUrl = Uri.parse(url);
                    intent.setData(contentUrl);
                    startActivity(intent);
                    ARActivity.this.finish();
                }

                @Override
                public void share(String title, String content, String shareUrl, String resUrl, int type) {
                    // type = 1 视频，type = 2 图片
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, content);
                    shareIntent.putExtra(Intent.EXTRA_TITLE, title);
                    shareIntent.setType("text/plain");
                    // 设置分享列表的标题，并且每次都显示分享列表
                    startActivity(Intent.createChooser(shareIntent, "分享到"));
                }
            });

            fragmentTransaction.replace(R.id.bdar_id_fragment_container, mARFragment);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        boolean backFlag = false;
        if (mARFragment != null) {
            backFlag = mARFragment.onFragmentBackPressed();
        }
        if (!backFlag) {
            super.onBackPressed();
        }
    }


}
