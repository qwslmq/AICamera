package cn.whu.aicamera;

import android.Manifest;
import android.content.Intent;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.ardemo.StartActivity;
import com.example.face_recognition.FaceRecognition;

import java.nio.ByteBuffer;
import java.util.Arrays;

import cn.whu.aicamera.Utils.PermissionsUtil;
import cn.whu.aicamera.character_recognition.CharacterRecognition;
import cn.whu.object_recognition.ObjectRecognition;

public class CameraActivity extends FragmentActivity implements PermissionsUtil.PermissionsListener {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private CameraGLSurfaceView mCameraGLSurfaceView;
    private PermissionsUtil mPermissionsUtil;
    private boolean mPermissionsSatisfied = false;
    private int mRecognitionOption = OBJECT_RECOGNITION;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private static final int FACE_RECOGNITION = 0;
    private static final int CHARACTER_RECOGNITION = 1;
    private static final int OBJECT_RECOGNITION = 3;
    private String mRecognitionResult = null;
    private TextView mTextView;
    private ImageButton switchCamera;
    byte[] bytes;

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //获取捕获的照片数据
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch (mRecognitionOption) {
                        case FACE_RECOGNITION:
                            mRecognitionResult = FaceRecognition.recognize(bytes);
                            break;
                        case CHARACTER_RECOGNITION:
                            mRecognitionResult = CharacterRecognition.recognize(bytes);
                            break;
                        case OBJECT_RECOGNITION:
                            mRecognitionResult = ObjectRecognition.recognize(bytes);
                            break;
                        default:
                            break;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(mRecognitionResult);
                        }
                    });
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("qws", "activity");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        if (PermissionsUtil.isMorHigher())
            setupPermissions();

        initView();
    }

    private void initView() {
        mCameraGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.cameraGLSurfaceView);
        mCameraGLSurfaceView.init(this, mOnImageAvailableListener);
        switchCamera = findViewById(R.id.switch_camera);
        mTextView = (TextView) findViewById(R.id.show_result);
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraGLSurfaceView.setCameraFacing();
            }
        });
        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.face_recognition:
                        Toast.makeText(CameraActivity.this, R.string.face_recognition, Toast.LENGTH_SHORT).show();
                        mRecognitionOption = FACE_RECOGNITION;
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.character_recognition:
                        Toast.makeText(CameraActivity.this, R.string.character_recognition, Toast.LENGTH_SHORT).show();
                        mRecognitionOption = CHARACTER_RECOGNITION;
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.object_recognition:
                        Toast.makeText(CameraActivity.this, R.string.object_recognition, Toast.LENGTH_SHORT).show();
                        mRecognitionOption = OBJECT_RECOGNITION;
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.ar_camera:
                        Toast.makeText(CameraActivity.this, R.string.ar_camera, Toast.LENGTH_SHORT).show();
                        startAr();
                        drawerLayout.closeDrawers();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    //跳转至ar界面
    private void startAr() {
        Intent intent = new Intent(CameraActivity.this, StartActivity.class);
        startActivity(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        int ui = getWindow().getDecorView().getSystemUiVisibility();
        ui = ui | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(ui);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mCameraGLSurfaceView.onResume();
        startBackgroundThread();

        if (PermissionsUtil.isMorHigher() && !mPermissionsSatisfied) {
            if (!mPermissionsUtil.checkPermissions())
                return;
            else
                mPermissionsSatisfied = true; //extra helper as callback sometimes isnt quick enough for future results
        }
    }

    @Override
    protected void onPause() {
        mCameraGLSurfaceView.onPause();
        stopBackgroundThread();
        bytes = null;
        super.onPause();
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("MainBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quit();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e("mr", "stopBackgroundThread");
        }
    }

    private void setupPermissions() {
        mPermissionsUtil = PermissionsUtil.attach(this);
        mPermissionsUtil.setRequestedPermissions(
                Manifest.permission.CAMERA
        );
    }

    @Override
    public void onPermissionsSatisfied() {
        Log.d(TAG, "onPermissionsSatisfied()");
        mPermissionsSatisfied = true;

    }

    @Override
    public void onPermissionsFailed(String[] failedPermissions) {
        Log.e(TAG, "onPermissionsFailed()" + Arrays.toString(failedPermissions));
        mPermissionsSatisfied = false;
        Toast.makeText(this, "shadercam needs all permissions to function, please try again.", Toast.LENGTH_LONG).show();
        this.finish();
    }
}
