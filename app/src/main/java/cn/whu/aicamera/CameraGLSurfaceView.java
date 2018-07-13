package cn.whu.aicamera;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;

public class CameraGLSurfaceView extends GLSurfaceView{
    private static final String TAG = CameraGLSurfaceView.class.getSimpleName();

    private CameraViewRenderer mCameraViewRenderer;
    private CameraHandler mCameraHandler;
    private Activity context;

    private AttributeSet mAttributes;
    private Size previewSize = null;
    private ImageReader.OnImageAvailableListener onImageAvailableListener;
    private boolean isFront = true;

    public void setCameraFacing(){
        isFront = ! isFront;
        mCameraViewRenderer.setCameraFacing(isFront);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attributes) {
        super ( context, attributes);
        mAttributes = attributes;
    }

    public void init(Activity activity, ImageReader.OnImageAvailableListener _onImageAvailableListener){
        context = activity;
        onImageAvailableListener = _onImageAvailableListener;
        TypedArray attrs = context.obtainStyledAttributes(mAttributes, R.styleable.CameraGLSurfaceView);
        Integer desiredWidth = attrs.getInt(R.styleable.CameraGLSurfaceView_desiredWidth, 640);
        Integer desiredHeight = attrs.getInt(R.styleable.CameraGLSurfaceView_desiredHeight, 480);
        attrs.recycle();
        previewSize = new Size(desiredWidth, desiredHeight);
        mCameraHandler = new CameraHandler(onImageAvailableListener, previewSize, context);
        mCameraViewRenderer = new CameraViewRenderer(this, mCameraHandler);
        setEGLContextClientVersion ( 2 );
        setRenderer (mCameraViewRenderer);
        setRenderMode ( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraViewRenderer.onResume();
    }

    @Override
    public void onPause() {
        mCameraViewRenderer.onPause();
        super.onPause();
    }


    /**
     * Options for scaling the bounds of an image to the bounds of this view.
     */
    public enum ScaleType {
        /**
         * Scale the image using {@link Matrix.ScaleToFit#FILL}.
         */
        FIT_XY      (1),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#START}.
         */
        FIT_START   (2),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#CENTER}.
         * From XML, use this syntax:
         */
        FIT_CENTER  (3),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#END}.
         */
        FIT_END     (4),
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so
         * that both dimensions (width and height) of the image will be equal
         * to or larger than the corresponding dimension of the view
         * (minus padding). The image is then centered in the view.
         */
        CENTER_CROP (6);
        ScaleType(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }
}
