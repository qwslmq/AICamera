package cn.whu.aicamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.whu.aicamera.Utils.BufferUtil;
import cn.whu.aicamera.Utils.ShaderUtils;

/**
 * Created by Administrator on 2017-07-13.
 */

public class CameraView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener{
    private CameraRenderer cameraRenderer;
    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        cameraRenderer = new CameraRenderer(context);
        setRenderer(cameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
    public CameraRenderer gerRenderer(){
        return  cameraRenderer;
    }
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public class CameraRenderer implements Renderer {

        private Context mContext;

        private int mProgram;

        private SurfaceTexture mCameraTexture;
        private CameraActivity activity;

        private int uPosHandle;
        private int aTexHandle;
        private int mMVPMatrixHandle;

        private float[] mProjectMatrix = new float[16];
        private float[] mCameraMatrix  = new float[16];
        private float[] mMVPMatrix     = new float[16];
        private float[] mTempMatrix     = new float[16];

        private float[] mPosCoordinate = {-1, -1, -1, 1, 1, -1, 1, 1};

        //private float[] mTexCoordinate = {0, 1, 1, 1, 0, 0, 1, 0}; //前置摄像头

        private float[] mTexCoordinate = {1, 1, 0, 1, 1, 0, 0, 0}; //后置摄像头

        private FloatBuffer mPosBuffer;
        private FloatBuffer mTexBuffer;

        public CameraRenderer(Context context) {
            this.mContext = context;

            Matrix.setIdentityM(mProjectMatrix, 0);
            Matrix.setIdentityM(mCameraMatrix, 0);
            Matrix.setIdentityM(mMVPMatrix, 0);
            Matrix.setIdentityM(mTempMatrix, 0);
        }
        public void setActivity(CameraActivity _activity){
            activity = _activity;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

            mProgram = ShaderUtils.createProgram(mContext, "vertex_texture.glsl", "fragment_texture.glsl");
            GLES20.glUseProgram(mProgram);

            createAndBindVideoTexture();

            activity.openCamera(mCameraTexture);

            uPosHandle           = GLES20.glGetAttribLocation (mProgram, "position");
            aTexHandle           = GLES20.glGetAttribLocation (mProgram, "inputTextureCoordinate");
            mMVPMatrixHandle    = GLES20.glGetUniformLocation(mProgram, "textureTransform");

            mPosBuffer = BufferUtil.convertToFloatBuffer(mPosCoordinate);
            mTexBuffer = BufferUtil.convertToFloatBuffer(mTexCoordinate);

            GLES20.glVertexAttribPointer(uPosHandle, 2, GLES20.GL_FLOAT, false, 0, mPosBuffer);
            GLES20.glVertexAttribPointer(aTexHandle, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

            GLES20.glEnableVertexAttribArray(uPosHandle);
            GLES20.glEnableVertexAttribArray(aTexHandle);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            float ratio = (float)width/height;
            Matrix.orthoM(mProjectMatrix,0,-1,1,-ratio,ratio,1,7);// 3和7代表远近视点与眼睛的距离，非坐标点
            Matrix.setLookAtM(mCameraMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);// 3代表眼睛的坐标点
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mCameraMatrix, 0);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
//            mCameraTexture.getTransformMatrix(mTempMatrix);
//            Matrix.multiplyMM(mTempMatrix, 0, mTempMatrix, 0, mMVPMatrix, 0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            mCameraTexture.updateTexImage();
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mPosCoordinate.length / 2);
        }

        private void createAndBindVideoTexture() {
            int[] texture = new int[1];
            GLES20.glGenTextures(1, texture, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

            mCameraTexture = new SurfaceTexture(texture[0]);
            mCameraTexture.setOnFrameAvailableListener(CameraView.this);
        }
    }

}
