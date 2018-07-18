package cn.whu.aicamera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import cn.whu.aicamera.CameraGLSurfaceView.ScaleType;
import cn.whu.aicamera.Utils.BufferUtil;
import cn.whu.aicamera.Utils.ShaderUtils;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class CameraViewRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = CameraViewRenderer.class.getSimpleName();
    private int[] textureId;
    private FloatBuffer pVertex;
    private FloatBuffer pTexCoordFront;
    private FloatBuffer pTexCoordBack;
    private int programID;
    private boolean isFront = true;

    private boolean mGLInit = false;
    private boolean mUpdateSurfaceTexture = false;

    private float[] mTexRotateMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1};

    private CameraGLSurfaceView mSurfaceView;
    private CameraHandler mCameraHandler;
    private SurfaceTexture mSurfaceTexture;

    private WindowManager mWindowManager;
    private OrientationEventListener mOrientationListener;

    private Size mPreviewSize = null;

    CameraViewRenderer(CameraGLSurfaceView view, CameraHandler cameraHandler) {
        Log.d("qws", "render");
        mSurfaceView = view;
        mCameraHandler = cameraHandler;

        float[] vertex = {
                -1.0f, -1.0f,   // 0 bottom left   A
                1.0f, -1.0f,   // 1 bottom right  B
                -1.0f, 1.0f,   // 2 top left      C
                1.0f, 1.0f,   // 3 top right     D
        };

        float[] ttmp_front = {
                0.0f, 0.0f,     // 0 bottom left
                1.0f, 0.0f,     // 1 bottom right
                0.0f, 1.0f,     // 2 top left
                1.0f, 1.0f,      // 3 top right
        };

        float[] ttmp_back = {
                1.0f, 0.0f,     // 1 bottom right
                0.0f, 0.0f,     // 0 bottom left
                1.0f, 1.0f,      // 3 top right
                0.0f, 1.0f,     // 2 top left
        };

        pVertex = BufferUtil.convertToFloatBuffer(vertex);
        pTexCoordFront = BufferUtil.convertToFloatBuffer(ttmp_front);
        pTexCoordBack = BufferUtil.convertToFloatBuffer(ttmp_back);

        Context ctx = mSurfaceView.getContext();
        if (!view.isInEditMode()) {
            mCameraHandler.initCamera(ctx, isFront);
            mPreviewSize = mCameraHandler.getPreviewSize();
        }

        mWindowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        if (!view.isInEditMode()) {
            mOrientationListener = new OrientationListener(ctx);
        }
    }

    public void onResume() {
        mCameraHandler.startBackgroundThread();

        if (mOrientationListener.canDetectOrientation() == true) {
            Log.v(TAG, "Can detect orientation");
            mOrientationListener.enable();
        } else {
            Log.v(TAG, "Cannot detect orientation");
            mOrientationListener.disable();
        }
    }

    public void onPause() {
        mGLInit = false;
        mUpdateSurfaceTexture = false;
        mCameraHandler.closeCamera();
        mCameraHandler.stopBackgroundThread();
        mOrientationListener.disable();
    }

    public void setCameraFacing(boolean is_front) {
        isFront = is_front;
        mCameraHandler.closeCamera();
        Context ctx = mSurfaceView.getContext();
        if (!mSurfaceView.isInEditMode()) {
            mCameraHandler.initCamera(ctx, isFront);
            mPreviewSize = mCameraHandler.getPreviewSize();
        }
        mCameraHandler.openCamera();
    }

    @Override
    public void onSurfaceCreated(GL10 unused, javax.microedition.khronos.egl.EGLConfig eglConfig) {
        initTex();
        mSurfaceTexture = new SurfaceTexture(textureId[0]);
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mCameraHandler.setSurfaceTexture(mSurfaceTexture);

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Clear white
        checkGlError("glClearColor");

        programID = ShaderUtils.createProgram(mSurfaceView.getContext(), "vertex_texture.glsl", "fragment_texture.glsl");

        mCameraHandler.openCamera();

        mGLInit = true;

        updateViewport();
    }

    private void initTex() {
        textureId = new int[1];
        GLES20.glGenTextures(1, textureId, 0);
        checkGlError("glGenTextures");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);
        checkGlError("glBindTexture");

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameteri");

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameteri");

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        checkGlError("glTexParameteri");

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        checkGlError("glTexParameteri");
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (!mGLInit) return;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        checkGlError("glClear");

        synchronized (this) {
            if (mUpdateSurfaceTexture) {
                mSurfaceTexture.updateTexImage();
                mUpdateSurfaceTexture = false;

                updateViewport();
            }
        }

        GLES20.glUseProgram(programID);
        checkGlError("glUseProgram");

        int trmh = GLES20.glGetUniformLocation(programID, "uTexRotateMatrix");
        checkGlError("glGetUniformLocation");

        GLES20.glUniformMatrix4fv(trmh, 1, false, mTexRotateMatrix, 0);
        checkGlError("glUniformMatrix4fv");

        int vPositionLocation = GLES20.glGetAttribLocation(programID, "vPosition");
        checkGlError("glGetAttribLocation");

        int vTecCoordLocation = GLES20.glGetAttribLocation(programID, "vTexCoord");
        checkGlError("glGetAttribLocation");

        GLES20.glVertexAttribPointer(vPositionLocation, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex);
        checkGlError("glVertexAttribPointer");

        GLES20.glVertexAttribPointer(vTecCoordLocation, 2, GLES20.GL_FLOAT, false, 4 * 2, isFront ? pTexCoordFront : pTexCoordBack);
        checkGlError("glVertexAttribPointer");

        GLES20.glEnableVertexAttribArray(vPositionLocation);
        checkGlError("glEnableVertexAttribArray");

        GLES20.glEnableVertexAttribArray(vTecCoordLocation);
        checkGlError("glEnableVertexAttribArray");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        checkGlError("glActiveTexture");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId[0]);
        checkGlError("glBindTexture");

        GLES20.glUniform1i(GLES20.glGetUniformLocation(programID, "sTexture"), 0);
        checkGlError("glUniform1i");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");

        GLES20.glFlush();
        checkGlError("glFlush");
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        updateTextureRotationMatrix();
        updateViewport();
    }


    private void updateViewport() {
        updateViewport(ScaleType.CENTER_CROP);
    }

    private void updateViewport(CameraGLSurfaceView.ScaleType scaleType) {
        Matrix mMatrix = new Matrix();
        RectF mSurfaceRect = new RectF();
        RectF mLastImageRect = new RectF();
        RectF mImageRect = new RectF();
        Point mRealSize = new Point();
        boolean swap = mSurfaceView.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        int imageWidth = !swap ? mCameraHandler.getPreviewSize().getWidth() : mCameraHandler.getPreviewSize().getHeight();
        int imageHeight = !swap ? mCameraHandler.getPreviewSize().getHeight() : mCameraHandler.getPreviewSize().getWidth();

        mSurfaceView.getDisplay().getRealSize(mRealSize);

        mImageRect.set(0, 0, imageWidth, imageHeight);

//        mImageRect.set(0, 0, mRealSize.x, mRealSize.y);

        if (scaleType == CameraGLSurfaceView.ScaleType.CENTER_CROP) {
            float scaleImage = (float) imageWidth / imageHeight;
            float scaleSurface = (float) mRealSize.x / mRealSize.y;

            int newTextureWidth, newTextureHeight;
            int x, y;

            if (scaleImage < scaleSurface) {
                newTextureWidth = (int) mRealSize.x;
                newTextureHeight = (int) (mRealSize.x / scaleImage);
            } else {
                newTextureWidth = (int) (mRealSize.y * scaleImage);
                newTextureHeight = (int) mRealSize.y;
            }

            x = ((int) mRealSize.x - newTextureWidth) / 2;
            y = ((int) mRealSize.y - newTextureHeight) / 2;

            mImageRect.set(x, y, x + newTextureWidth, y + newTextureHeight);
        } else {
            Matrix.ScaleToFit scaleToFit;

            switch (scaleType) {
                case FIT_CENTER:
                    scaleToFit = Matrix.ScaleToFit.CENTER;
                    break;
                case FIT_END:
                    scaleToFit = Matrix.ScaleToFit.END;
                    break;
                case FIT_START:
                    scaleToFit = Matrix.ScaleToFit.START;
                    break;
                case FIT_XY:
                    scaleToFit = Matrix.ScaleToFit.FILL;
                    break;
                default:
                    throw new RuntimeException("Unknown ScaleType enum value.");
            }

            mSurfaceRect.set(0, 0, mRealSize.x, mRealSize.y);
            mMatrix.setRectToRect(mImageRect, mSurfaceRect, scaleToFit);
            mMatrix.mapRect(mImageRect);
        }

        if (mLastImageRect != mImageRect) {
            GLES20.glViewport((int) mImageRect.left, (int) mImageRect.top, (int) mImageRect.width(), (int) mImageRect.height());
            checkGlError("glViewport");
            mLastImageRect.set(mImageRect);
        }
    }

    public synchronized void onFrameAvailable(SurfaceTexture st) {
        mUpdateSurfaceTexture = true;
        mSurfaceView.requestRender();
    }

    private void updateTextureRotationMatrix() {

        Display display = mWindowManager.getDefaultDisplay();

        float offset = 0;
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_90:
                offset = 180;
                break;
        }

        Log.i(TAG, String.format("OFFSET: %f", offset));

        android.opengl.Matrix.setRotateM(mTexRotateMatrix, 0, offset, 0f, 0f, 1f);

        if (mSurfaceView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            android.opengl.Matrix.setRotateM(mTexRotateMatrix, 0, -90.0f + offset, 0f, 0f, 1f);
            Log.i(TAG, String.format("rotate: 0, %f x, 0.f, 0f, 1f", 90.0f + offset));
            //Matrix.scaleM(mTexRotateMatrix, 0, mTexRotateMatrix, 0, 1, -1, 1f);
        } else {
            // Matrix.setRotateM(mTexRotateMatrix, 0, offset, 0f, 0f, 1f);
            Log.i(TAG, String.format("rotate: 0, %f x, 0.f, 0f, 1f", offset));
        }
    }

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    class OrientationListener extends OrientationEventListener {
        OrientationListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_UI);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // Force orientation recheck for case when 180 screen rotatation doesn't fire onSurfaceChanged.
            updateTextureRotationMatrix();
        }
    }
}