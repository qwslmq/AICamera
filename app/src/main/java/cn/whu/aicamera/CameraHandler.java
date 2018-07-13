package cn.whu.aicamera;

import android.app.Activity;
import android.content.Context;
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
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import cn.whu.aicamera.Utils.FileUtil;

public class CameraHandler {
    private static final String TAG = CameraHandler.class.getSimpleName();

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    //Sensor方向，大多数设备是90度
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    //Sensor方向，一些设备是270度
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

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
    private Integer mSensorOrientation;//Sensor方向
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private String mCameraID;
    private Size mDesiredPreviewSize = null;
    private Size mPreviewSize = null;
    private Activity context;
    private boolean isFront;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private HandlerThread mRecognitionThread;
    private Handler mRecognitionHandler;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private ImageReader mCaptureReader;

    private int mFacing;

    protected SurfaceTexture mSurfaceTexture;

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener;

    public CameraHandler(ImageReader.OnImageAvailableListener onImageAvailableListener, Size desiredPreviewSize, Activity activity) {
        mOnImageAvailableListener = onImageAvailableListener;
        mDesiredPreviewSize = desiredPreviewSize;
        context = activity;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }

    Runnable handleRecognition = new Runnable() {
        @Override
        public void run() {
            takePicture();
            mRecognitionHandler.postDelayed(this, 2000);
        }
    };

    public int getFacing() {
        return mFacing;
    }

    public Size getPreviewSize() {
        if (mPreviewSize == null) {
            throw new RuntimeException();
        }
        return mPreviewSize;
    }

    double distanceSq (Size one, Size two) {
        double dY = one.getHeight() - two.getHeight();
        double dX = one.getWidth() - two.getWidth();
        return dY * dY + dX * dX;
    }

    void calcPreviewSize(Context context, boolean is_front) {
        isFront = is_front;
        int lens_facing = CameraCharacteristics.LENS_FACING_FRONT;
        if(!isFront) lens_facing = CameraCharacteristics.LENS_FACING_BACK;
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        double distSq = Double.MAX_VALUE;
        try {
            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) != lens_facing)
                    continue;

                mFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                mCameraID = cameraID;
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                for ( Size psize : map.getOutputSizes(SurfaceTexture.class)) {

                    double tmpDistSq = distanceSq(psize, mDesiredPreviewSize);
                    Log.i("mr", String.format("size %d x %d   %f", psize.getWidth(), psize.getHeight(), tmpDistSq));

                    if ( tmpDistSq < distSq ) {
                        mPreviewSize = psize;
                        distSq = tmpDistSq;
                        Log.i("mr", String.format("mPreviewSize %d x %d", psize.getWidth(), psize.getHeight()));
                    }
                }
                mCaptureReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
                mCaptureReader.setOnImageAvailableListener(mOnImageAvailableListener,mRecognitionHandler);
                break;
            }
        } catch ( CameraAccessException e ) {
            Log.e("mr", "calcPreviewSize - Camera Access Exception");
        } catch ( IllegalArgumentException e ) {
            Log.e("mr", "calcPreviewSize - Illegal Argument Exception");
        } catch ( SecurityException e ) {
            Log.e("mr", "calcPreviewSize - Security Exception");
        }
        Log.i("mr", "camera size had to be chosen");
    }

    void openCamera(Context contexst) {
        CameraManager manager = (CameraManager)contexst.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraID);
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraID,mStateCallback,mBackgroundHandler);
        } catch ( CameraAccessException e ) {
            Log.e("mr", "OpenCamera - Camera Access Exception");
        } catch ( IllegalArgumentException e ) {
            Log.e("mr", "OpenCamera - Illegal Argument Exception");
        } catch ( SecurityException e ) {
            Log.e("mr", "OpenCamera - Security Exception");
        } catch ( InterruptedException e ) {
            Log.e("mr", "OpenCamera - Interrupted Exception");
        }
    }

    protected void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    protected final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

    };

    protected void createCameraPreviewSession() {
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            final Surface surface = new Surface(mSurfaceTexture);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mCaptureReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice)
                                return;

                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                                //mRecognitionHandler.postDelayed(handleRecognition, 2000);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "createCaptureSession");
                            }
                        }
                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "onConfigureFailed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreviewSession");
        }
    }

    /**
     * 拍照
     */
    private void takePicture() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCameraDevice == null) {
                return;
            }
            mCameraOpenCloseLock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            //创建作为拍照的CaptureRequest.Builder
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //将mImageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mCaptureReader.getSurface());

            //设置AF、AE模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            //获得屏幕方向
            int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
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
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };
            //停止连续取景
            mCaptureSession.stopRepeating();
            //捕获静态图像
            mCaptureSession.capture(captureRequestBuilder.build(), captureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mRecognitionThread = new HandlerThread("RecognitionBackground");
        mRecognitionThread.start();
        mRecognitionHandler = new Handler(mRecognitionThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        mRecognitionThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
            mRecognitionThread.join();
            mRecognitionThread = null;
            mRecognitionHandler = null;
        } catch (InterruptedException e) {
            Log.e("mr", "stopBackgroundThread");
        }
    }


}
