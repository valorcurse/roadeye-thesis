package com.cgi.roadeye.android;

/**
 * Created by valorcurse on 13/06/14.
 */

import android.content.Context;
import android.hardware.Camera.Size;
import android.util.AttributeSet;

import org.opencv.android.NativeCameraView;

import java.util.List;

public class OpenCVCamera extends NativeCameraView {

    private static final String TAG = "OpenCVCamera:";

    public OpenCVCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected org.opencv.core.Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {

        return new org.opencv.core.Size(1360, 720);
    }

    public List<org.opencv.core.Size> getResolutionList() {
        return mCamera.getSupportedPreviewSizes();
    }

}

