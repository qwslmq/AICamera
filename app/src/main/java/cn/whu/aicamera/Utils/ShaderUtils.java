package cn.whu.aicamera.Utils;

import android.content.Context;
import android.opengl.GLES20;

/**
 * Created by Administrator on 2017-06-20.
 */

public class ShaderUtils {

    public static int createProgram(Context context, String fileName1, String fileName2) {
        String vertexSource = AssetsUtils.read(context, fileName1);
        String fragmentSource = AssetsUtils.read(context, fileName2);
        return createProgram(vertexSource, fragmentSource);
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        // 加载顶点着色器
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0)
        {
            return 0;
        }

        // 加载片元着色器
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0)
        {
            return 0;
        }

        // 创建着色器程序
        int programID = GLES20.glCreateProgram();
        // 若程序创建成功则向程序中加入顶点着色器与片元着色器
        if (programID != 0)
        {
            // 向程序中加入顶点着色器
            GLES20.glAttachShader(programID, vertexShader);
            // 向程序中加入片元着色器
            GLES20.glAttachShader(programID, pixelShader);
            // 链接程序
            GLES20.glLinkProgram(programID);
            // 存放链接成功program数量的数组
            int[] linkStatus = new int[1];
            // 获取program的链接情况
            GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, linkStatus, 0);
            // 若链接失败则报错并删除程序
            if (linkStatus[0] != GLES20.GL_TRUE)
            {
                GLES20.glDeleteProgram(programID);
                programID = 0;
            }
        }

        // 释放shader资源
        GLES20.glDeleteShader(vertexShader );
        GLES20.glDeleteShader(pixelShader);

        return programID;
    }

    private static int loadShader(int shaderType, String source) {
        int shaderID = GLES20.glCreateShader(shaderType);
        if (shaderID == 0) return 0;

        // 加载shader源代码
        GLES20.glShaderSource(shaderID, source);
        // 编译shader
        GLES20.glCompileShader(shaderID);
        // 存放编译成功shader数量的数组
        int[] compiled = new int[1];
        // 获取Shader的编译情况
        GLES20.glGetShaderiv(shaderID, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0)
        {
            //若编译失败则显示错误日志并删除此shader
            GLES20.glDeleteShader(shaderID);
            shaderID = 0;
        }
        return shaderID;
    }
}
