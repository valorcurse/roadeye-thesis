package com.cgi.roadeye.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by valorcurse on 14/04/14.
 */
public class ExtendedVideoView extends VideoView {
    public Bitmap mFrameBitmap;

    public ExtendedVideoView(Context context) {
        super(context);
    }

    public ExtendedVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
}
