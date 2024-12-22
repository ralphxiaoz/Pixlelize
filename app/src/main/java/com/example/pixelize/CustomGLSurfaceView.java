package com.example.pixelize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class CustomGLSurfaceView extends GLSurfaceView {
    private final CustomRenderer renderer;
    private boolean isPixelationEnabled = false;

    // Constructor for creating the view programmatically
    public CustomGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2); // Use OpenGL ES 2.0
        renderer = new CustomRenderer();
        setRenderer(renderer);
    }

    // Constructor for inflating from XML
    public CustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2); // Use OpenGL ES 2.0
        renderer = new CustomRenderer();
        setRenderer(renderer);
    }

    public void setPixelationEnabled(boolean enabled) {
        if (renderer != null) {
            renderer.setPixelationEnabled(enabled);
            requestRender();
        }
    }

    public void setPixelSize(float pixelSize) {
        if (renderer != null && isPixelationEnabled) {
            renderer.setPixelSize(pixelSize);
            requestRender();
        }
    }

    // Method to update the camera feed
    public void updateCameraFeed(Bitmap bitmap) {
        // Convert Bitmap to a texture
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Load the bitmap into the texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        
        // Now draw the texture in your onDrawFrame method
        // Make sure to use the correct shader program and draw calls
    }

    // Method to check if the surface is available for rendering
    public boolean isAvailable() {
        return getHolder().getSurface() != null && getHolder().getSurface().isValid();
    }

    // Method to capture the current frame as a Bitmap
    public Bitmap getBitmap() {
        // Create a bitmap with the same dimensions as the view
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas); // Draw the current view onto the canvas
        return bitmap;
    }

    public void loadStaticBitmap() {
        Bitmap staticBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample_image);
        updateCameraFeed(staticBitmap);
    }
} 