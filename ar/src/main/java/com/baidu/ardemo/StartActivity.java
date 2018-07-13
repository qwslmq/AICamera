package com.baidu.ardemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    private Button button1, button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1 = findViewById(R.id.btnSlam);
        button2 = findViewById(R.id.btnImu);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(StartActivity.this, ARActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("ar_key","10225382");
                bundle.putInt("ar_type",5);
                intent1.putExtras(bundle);
                startActivity(intent1);

            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(StartActivity.this, ARActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("ar_key", "10225387");
                bundle.putInt("ar_type",0);
                intent1.putExtras(bundle);
                startActivity(intent1);
            }
        });
    }
}
