package cn.whu.aicamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.face_recognition.FaceRecognition;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import cn.whu.aicamera.Utils.FileUtil;
import cn.whu.aicamera.Utils.PermissionUtil;
import cn.whu.object_recognition.ObjectRecognition;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraActivity";
    //拍照权限请求码
    private static final int REQUEST_PICTURE_PERMISSION = 1;
    //拍照权限
    private static final String[] PICTURE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int FACE_RECOGNITION = 0;
    private static final int CHARACTER_RECOGNITION = 1;
    private static final int OBJECT_RECOGNITION =3;
    //Sensor方向，大多数设备是90度
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    //Sensor方向，一些设备是270度
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    //sensor的方向为90度时，屏幕方向与Sensor方向的对应关系
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //sensor的方向为270度时，屏幕方向与Sensor方向的对应关系
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private SurfaceTexture mSurfaceTexture;
    private CameraView mCameraView;
    private ImageReader mImageReader;//用于接收拍照图片
    private TextView mTextView;
    private int mRecognitionOption = OBJECT_RECOGNITION;
    private String mRecognitionResult = null;

    private CaptureRequest.Builder mPreviewRequestBuilder;//预览请求的CaptureRequest.Builder对象
    private CameraDevice mCameraDevice;//代表摄像头的成员变量
    private CameraCaptureSession mCameraCaptureSession;//定义CameraCaptureSession成员变量
    private Size mPreviewSize;//预览大小
    private Integer mSensorOrientation;//Sensor方向
    private Semaphore mCameraLock = new Semaphore(1);//Camera互斥锁
    private String mCameraId;//摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private File mDir;//存放图片的父目录
    private File mFile;//图片的保存位置
    private Boolean mFlashSupported;//是否支持闪光灯

    private HandlerThread mCameraThread;//负责相机操作
    private Handler mCameraHandler;
    private HandlerThread mRecognitionThread;//负责图片识别
    private Handler mRecognitionHandler;


    Runnable handleRecognition = new Runnable() {
        @Override
        public void run() {
            takePicture();
            mRecognitionHandler.postDelayed(this, 2000);
        }
    };

    /**
     * 在这调用你们的接口
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            //获取捕获的照片数据
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();
            switch (mRecognitionOption){
                case FACE_RECOGNITION:
                    mRecognitionResult = FaceRecognition.recognize(bytes);
                    break;
                case CHARACTER_RECOGNITION:
                    mRecognitionResult = "CHARACTER_RECOGNITION";
                    //to do

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
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraLock.release();
            mCameraDevice = cameraDevice;
            startPreview();//开始预览
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            CameraActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
        }
        initView();
    }

    private void initView(){
        mTextView = (TextView)findViewById(R.id.show_result);

        mCameraView =  (CameraView) findViewById(R.id.camera_view);
        mCameraView.gerRenderer().setActivity(this);
        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView mNavigationView=(NavigationView)findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
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
                        drawerLayout.closeDrawers();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //开启后台线程
        startBackgroundThread();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        openCamera(mSurfaceTexture);
    }

    @Override
    protected void onPause() {
        //关闭摄像头
        closeCamera();
        //停止后台线程
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        /*switch (view.getId()) {
            case R.id.take_photo:
                takePicture();//拍照
                break;
        }*/
    }

    /**
     * 打开相机
     */
    @SuppressWarnings("MissingPermission")
    public void openCamera(SurfaceTexture _surfaceTexture) {
        if(_surfaceTexture == null) return;
        mSurfaceTexture = _surfaceTexture;
        PermissionUtil permissionUtil = new PermissionUtil(this);
        //若没有权限
        if (!permissionUtil.hasPermissionGranted(PICTURE_PERMISSIONS)) {
            //请求所需权限
            permissionUtil.requestRequiredPermissions(PICTURE_PERMISSIONS, R.string.need_permissions, REQUEST_PICTURE_PERMISSION);
            return;
        }

        if (this.isFinishing()) {
            return;
        }
        setCameraInfo();//设置Camera信息
        //获得Camera的系统服务管理器
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //若超过2500毫秒，Camera仍未打开
            if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("相机打开超时");
            }

            //打开Camera
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mCameraHandler);

        } catch (InterruptedException e) {
            throw new RuntimeException("打开相机时中断");
        } catch (CameraAccessException e) {
            throw new RuntimeException("无法访问相机");
        }
    }

    /**
     * 设置Camera信息
     */
    private void setCameraInfo() {
        //获得Camera的系统服务管理器
        CameraManager cameraManager =  (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //通过标识符返回当前连接的相机设备的列表，包括可能在使用其他相机客户端的相机
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                //获得指定CameraId相机设备的属性
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                //获得摄像头朝向
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                //使用后置摄像头
                if (facing != null && facing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }
                mCameraId = cameraId;
                //获得流配置
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                //获取摄像头支持的最大尺寸
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizeByArea());
                //创建一个ImageReader对象，用于获取摄像头的图像数据。设置图片大小为largest
                mImageReader = ImageReader.newInstance(largest.getWidth()/4, largest.getHeight()/4, ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mRecognitionHandler);

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), mCameraView.getWidth(), mCameraView.getHeight(), largest);

                int orientation = getResources().getConfiguration().orientation;
                //获得Sensor方向
                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                //是否有闪光灯
                Boolean available = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = (available == null ? false : available);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始预览
     */
    private void startPreview() {
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(mSurfaceTexture);
        try {
            //创建作为预览的CaptureRequest.Builder对象
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //将mTextureView的surface作为CaptureRequest.Builder的目标
            mPreviewRequestBuilder.addTarget(surface);

            //创建用于预览和拍照的CameraCaptureSession
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCameraCaptureSession = cameraCaptureSession;
                    updatePreview();//更新预览
                    mRecognitionHandler.postDelayed(handleRecognition, 2000);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新预览
     */
    private void updatePreview() {
        //设置CaptureRequest.Builder对象
        setBuilder(mPreviewRequestBuilder);

        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //预览
                    mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mCameraHandler);
                } catch (CameraAccessException e) {
                    throw new RuntimeException("无法访问相机");
                }
            }
        });
    }

    /**
     * 设置CaptureRequest.Builder对象
     * @param captureRequestBuilder  要设置的CaptureRequest.Builder对象
     */
    private void setBuilder(CaptureRequest.Builder captureRequestBuilder) {
        // 设置自动对焦模式
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //若支持闪光灯
        if (mFlashSupported) {
            //设置自动曝光模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * 选择最佳大小
     */
    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        //若有足够大的，选择最小的一个
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }


    /**
     * 开启后台线程
     */
    private void startBackgroundThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mRecognitionThread = new HandlerThread("RecognitionThread");
        mRecognitionThread.start();
        mRecognitionHandler = new Handler(mRecognitionThread.getLooper());
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        mCameraThread.quitSafely();
        mRecognitionThread.quitSafely();
        try {
            mCameraThread.join();
            mRecognitionThread.join();
            mCameraThread = null;
            mCameraHandler = null;
            mRecognitionThread = null;
            mRecognitionHandler = null;
        } catch (InterruptedException e) {
            throw new RuntimeException("停止后台线程时中断");
        }
    }

    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        try {
            mCameraLock.acquire();
            //关闭CameraCaptureSession
            closeCameraCaptureSession();
            //关闭CameraDevice
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            //关闭ImageReader
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("关闭相机时中断");
        } finally {
            mCameraLock.release();
        }
    }

    /**
     * 关闭CameraCaptureSession
     */
    private void closeCameraCaptureSession() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    /**
     * 拍照
     */
    private void takePicture() {
        //照片保存路径
        mFile = new File(mDir,  FileUtil.getFileName(true));

        if (mCameraDevice == null) {
            return;
        }

        try {
            //创建作为拍照的CaptureRequest.Builder
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //将mImageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());

            //设置AF、AE模式
            setBuilder(captureRequestBuilder);

            //获得屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            switch (mSensorOrientation) {
                //Sensor方向为90度时
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    //根据屏幕方向设置照片的方向
                    captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                //Sensor方向为270度时
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    //根据屏幕方向设置照片的方向
                    captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }

            //创建拍照的CameraCaptureSession.CaptureCallback对象
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                //在拍照完成时调用
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    ///Toast.makeText(CameraActivity.this, "保存到：" + mFile.toString(), Toast.LENGTH_SHORT).show();
                    updatePreview();//继续预览
                }
            };
            //停止连续取景
            mCameraCaptureSession.stopRepeating();
            //捕获静态图像
            mCameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * activity的onRequestPermissionsResult会被回调来通知结果（通过第三个参数）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //匹配请求码
        switch (requestCode) {
            case REQUEST_PICTURE_PERMISSION:
                if (grantResults.length == PICTURE_PERMISSIONS.length) {
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            stopApp(this);//若未被赋予相应权限，APP停止运行
                            break;
                        }
                    }
                    openCamera(mSurfaceTexture);
                } else {
                    stopApp(this);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 停止Activity：APP停止运行
     */
    private void stopApp(Activity activity) {
        Toast.makeText(activity, R.string.sorry, Toast.LENGTH_SHORT).show();
        activity.finish();
    }
}
