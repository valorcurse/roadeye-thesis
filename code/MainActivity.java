package com.cgi.roadeye.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.MediaController;
import android.widget.Switch;
import android.widget.VideoView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener {

    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/RoadEye/";

    public static final String lang = "plate";
    private static final String TAG = "RoadEye: ";

    protected GridView platesGridView;
    protected VideoView videoView;
    protected Switch cameraVideoSwitch;

    private OpenCVCamera mOpenCvCameraView;
    private ArrayList<Mat> framesBuffer = new ArrayList<Mat>();
    private Vector<Plate> bandsBuffer = new Vector<Plate>();
    private ArrayList<Plate> gridPlatesBuffer = new ArrayList<Plate>();
    private Vector<Plate> platesBuffer = new Vector<Plate>();
    private ImageAdapter adapter;


    TessBaseAPI baseApi;

    double averageBand = 7;
    double clippingConstant = 0.1;

    private static int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();

    // Band queue
    ArrayBlockingQueue<Runnable> bandQueue = new ArrayBlockingQueue<Runnable>(1);
    RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.DiscardPolicy();
    ExecutorService bandThreadsExecutor = new ThreadPoolExecutor(
            NUMBER_OF_CORES / 2,
            NUMBER_OF_CORES / 2,
            0,
            TimeUnit.MILLISECONDS,
            bandQueue,
            rejectedExecutionHandler);

    // Plate queue
    LinkedBlockingQueue<Runnable> plateQueue = new LinkedBlockingQueue<Runnable>();
    ExecutorService plateThreadsExecutor = new ThreadPoolExecutor(
            NUMBER_OF_CORES / 2,
            NUMBER_OF_CORES / 2,
            0,
            TimeUnit.MILLISECONDS,
            plateQueue);

    boolean running = true;
    boolean useCamera = true;
    String videoName = "VIDEO0003.mp4";

    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mOpenCVCallBack);

        setContentView(R.layout.activity_main);

        cameraVideoSwitch = (Switch) findViewById(R.id.switch1);
        cameraVideoSwitch.setChecked(useCamera);
        cameraVideoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                useCamera = isChecked;

                switchCameraVideo(isChecked);
            }
        });

        platesGridView = (GridView) findViewById(R.id.platesGridView);
        adapter = new ImageAdapter(this, R.id.list_item_image, gridPlatesBuffer);
        platesGridView.setAdapter(adapter);

        videoView = (VideoView) findViewById(R.id.videoView);

        videoView.setVideoURI(Uri.parse(DATA_PATH + videoName));
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        mediaMetadataRetriever.setDataSource(DATA_PATH + videoName);

        mOpenCvCameraView = (OpenCVCamera) findViewById(R.id.OpenCvView);
        mOpenCvCameraView.setCvCameraViewListener(this);

        ViewGroup parent = (ViewGroup) findViewById(R.id.cameraVideoLayout);

        parent.removeView(mOpenCvCameraView);
        parent.removeView(videoView);
        switchCameraVideo(useCamera);

        baseApi = new TessBaseAPI();
        baseApi.init(DATA_PATH, lang);
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);

    }

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {

                    mOpenCvCameraView.enableView();

                    initRecognitionThread();

                }
                break;

                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        running = false;
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(final Mat inputFrame) {

        if (useCamera)
            if (framesBuffer.size() < 1)
                framesBuffer.add(inputFrame);

        return inputFrame;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    private void initRecognitionThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (running) {

//                    if (framesBuffer.size() > 0 || bandsBuffer.size() > 0 || platesBuffer.size() > 0)
//                        Log.i(TAG, "Frames: " + framesBuffer.size() + "\tBands: " + bandsBuffer.size() +
//                                "\tPlates: " + platesBuffer.size());

                    if (!useCamera) {
                        if (bandQueue.remainingCapacity() > 0) {
                            final Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(videoView.getCurrentPosition() * 1000); //unit in microsecond
                            Mat frameMat = new Mat();
                            Utils.bitmapToMat(bmFrame, frameMat);
                            framesBuffer.add(frameMat);
//                            Log.i(TAG, "Added frame to buffer.");
                        }
                    }

                    if (platesBuffer.size() > 0) {

                        final Plate plate = platesBuffer.remove(0);
//                        ArrayList<Mat> letters = plate.getLetters();

                        Mat plateImage = plate.getPlateImage();

                        if (plateImage != null && plateImage.size().area() > 0) {
//                            String recognizedText = "";
                            baseApi.setImage(Utilities.convertMatToBmp(plateImage));
                            String recognizedText = baseApi.getUTF8Text();
                            recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9\\-]+", "");
//
                            if (recognizedText.length() != 0)
                                plate.setInformation(recognizedText);

                            // Add plate to gridview
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (adapter.getCount() > 3) {
                                        adapter.clear();
                                    }

//                                    Log.i(TAG, "Adding plate.");
                                    adapter.add(plate);
                                }
                            });
                        }
                    }


                    if (framesBuffer.size() > 0) {
                        final Mat frame = framesBuffer.remove(0);
                        bandThreadsExecutor.execute(new FindBands(frame, bandsBuffer, averageBand, clippingConstant));
                    }

                    if (bandsBuffer.size() > 0) {
                        plateThreadsExecutor.execute(new FindPlates(bandsBuffer.remove(0), platesBuffer, averageBand));
                    }

                }
            }
        }).start();

    }

    private void switchCameraVideo(boolean useCamera) {
        ViewGroup parent = (ViewGroup) findViewById(R.id.cameraVideoLayout);
        if (parent != null) {

            // Enable camera
            if (useCamera) {

                parent.removeView(videoView);
                parent.addView(mOpenCvCameraView);

                mOpenCvCameraView.enableView();
                mOpenCvCameraView.setEnabled(true);
                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

            } else { // Enable video

                parent.removeView(mOpenCvCameraView);
                parent.addView(videoView);

                videoView.setVisibility(View.VISIBLE);
                videoView.setEnabled(true);
                videoView.start();
            }
        }
    }
}