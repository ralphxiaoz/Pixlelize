package com.example.pixelize;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;
import android.opengl.GLUtils;
import android.util.Log;

public class CustomRenderer implements GLSurfaceView.Renderer {
    private float pixelSize = 10.0f; // Default pixel size
    private int shaderProgram;
    private int pixelSizeLocation;
    private Bitmap cameraBitmap; // To hold the camera feed
    private boolean isPixelationEnabled = false;
    private int[] textureId = new int[1]; // To hold the texture ID

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Initialize shaders and OpenGL settings
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        shaderProgram = GLES20.glCreateProgram();             // Create empty OpenGL Program
        GLES20.glAttachShader(shaderProgram, vertexShader);   // Add the vertex shader to program
        GLES20.glAttachShader(shaderProgram, fragmentShader); // Add the fragment shader to program
        GLES20.glLinkProgram(shaderProgram);                   // Create OpenGL program executables

        // Check for linking errors
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("Shader", "Error linking program: " + GLES20.glGetProgramInfoLog(shaderProgram));
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = 0; // Set shader program to 0 to indicate failure
        }

        // Get the location of the pixelSize uniform variable
        pixelSizeLocation = GLES20.glGetUniformLocation(shaderProgram, "u_PixelSize");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // Adjust the viewport based on geometry changes
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        if (cameraBitmap != null) {
            // Generate and bind the texture
            GLES20.glGenTextures(1, textureId, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

            // Set texture parameters
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Load the bitmap into the texture
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, cameraBitmap, 0);

            // Use the shader program
            GLES20.glUseProgram(shaderProgram);
            
            // Draw the texture here (ensure you have the correct vertex data and attributes set up)
            // ...
        } else {
            Log.d("CustomRenderer", "Camera bitmap is null");
        }
    }

    private Bitmap applyPixelation(Bitmap bitmap, float pixelSize) {
        if (!isPixelationEnabled) {
            return bitmap; // Return the original bitmap if pixelation is disabled
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Create a scaled down version of the bitmap
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            Math.max(1, (int)(width / pixelSize)),
            Math.max(1, (int)(height / pixelSize)),
            false
        );
        
        // Scale it back up to the original size
        return Bitmap.createScaledBitmap(scaledBitmap, width, height, false);
    }

    private void renderBitmap(Bitmap bitmap) {
        // Create a texture and bind it
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Load the bitmap into the texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Draw the texture using your shader program
        GLES20.glUseProgram(shaderProgram);
        // Set up vertex data and draw
        // Ensure you bind the vertex buffer and set attributes
        // ...
    }

    public void setPixelSize(float size) {
        pixelSize = size;
    }

    // Method to update the camera feed
    public void updateCameraFeed(Bitmap bitmap) {
        this.cameraBitmap = bitmap; // Store the bitmap for rendering
        Log.d("CustomRenderer", "Camera bitmap updated: " + (bitmap != null ? "Valid" : "Null"));
    }

    // Method to load shaders
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type); // Create a vertex shader type
        GLES20.glShaderSource(shader, shaderCode); // Add the source code to the shader and compile it
        GLES20.glCompileShader(shader); // Compile the shader

        // Check for compilation errors
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader); // Delete the shader if it failed
            shader = 0; // Set shader to 0 to indicate failure
        }
        return shader; // Return the shader
    }

    // Vertex shader code
    private final String vertexShaderCode =
        "attribute vec4 a_Position;" +
        "attribute vec2 a_TexCoord;" +
        "varying vec2 v_TexCoord;" +
        "void main() {" +
        "    gl_Position = a_Position;" +
        "    v_TexCoord = a_TexCoord;" +
        "}";

    // Fragment shader code
    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform sampler2D u_Texture;" +
        "uniform float u_PixelSize;" +
        "varying vec2 v_TexCoord;" +
        "void main() {" +
        "    vec2 coord = v_TexCoord;" +
        "    coord = floor(coord / u_PixelSize) * u_PixelSize;" + // Pixelate effect
        "    gl_FragColor = texture2D(u_Texture, coord);" +
        "}";

    public void setPixelationEnabled(boolean enabled) {
        isPixelationEnabled = enabled;
    }
} 