package com.kraydel.thermalcameraopencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.opencv.core.CvType.CV_8UC1;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "KraydelThermalVision";
    private ThermalVision vision;
    private VisionStream vs;
    private Handler handler = new Handler();
   // private ThermalCameraManager thermalcameramanager;

    public class VisionStream implements Runnable {
        Context context;

        public VisionStream(Context c) {
            this.context = c;
        }

        @Override
        public void run() {
            vision = new ThermalVision(context);

            double[][][] data = readThermalImage("thermal.txt");

            //int data[] = thermalcameramanager.getTemperatureMatrix(0);
            if (data == null) {
                Log.e(TAG, "Could not read from the camera\n");
            } else {
                Log.i(TAG, "Data received " + data.length);
                int color = 0x000000;
                int index = 0;
                int left = vision.getWidth() + 165;
                int top = vision.getHeight() + 165;

                Canvas canvas = new Canvas();
                Paint paint[] = new Paint[1024];
                Rect rect[] = new Rect[1024];

                for (int i = 0; i < 1024; i++)
                    paint[i] = new Paint();

                for (int i = 0; i < 1024; i++)
                    rect[i] = new Rect();

                for (int i = 0; i < 32; i++) {
                    for (int j = 0; j < 32; j++) {
                        index = i * 32 + j;
//                        color = getColorFromKelvin(data[y * 32 + x]);
                        paint[index].setARGB(0xFF, (int)Math.round(data[i][j][0]), (int)Math.round(data[i][j][1]), (int)Math.round(data[i][j][2]));
                        paint[index].setStyle(Paint.Style.FILL);
                        rect[index].set(left + j * 10, top + i * 10, left + 10 + 10 * j, top + 10 + 10 * i);
                    }
                }
                vision.SetPaint(paint);
                vision.SetRectangle(rect);
                setContentView(vision);
                vision.draw(canvas);
            }
            handler.postDelayed(vs, 50);
        }
    }

    public class ThermalVision extends View {
        Paint[] paint;
        Rect[] rectangle;

        public ThermalVision(Context context) {
            super(context);
            init();
        }

        public ThermalVision(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public ThermalVision(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            init();
        }

        private void init() {
            paint = new Paint[1024];
            rectangle = new Rect[1024];
        }

        public void SetPaint(Paint p[]) {
            this.paint = p;
        }

        public void SetRectangle(Rect r[]) {
            this.rectangle = r;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i < 32; i++) {
                for (int j = 0; j < 32; j++) {
                    canvas.drawRect(rectangle[i * 32 + j], paint[i * 32 + j]);
                }
            }
        }
    }

    //Reads a thermal image from a txt file of temperatures and converts it to a grayscale Mat with exaggerated colours
    private double[][][] readThermalImage(String fileName){
        double[][] vals = new double[32][32];

        try {
            InputStream in = getResources().getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            double val,range;
            for(int i = 0; i < 32; i++){
                String[] temps = reader.readLine().split("\\t");
                for(int j = 0; j < temps.length; j++) {
                    val = Double.parseDouble(temps[j] = temps[j].replace("+", ""));
                    vals[i][j] = val;
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading Temperature File", e);
        }

        //Mat thermalImage = convertToGrayscaleMat(vals);
        double[][][] thermalImage = convertToRGBThermalMat(vals);

        return thermalImage;
    }

    private double[][][] convertToRGBThermalMat(double[][] temps){
        double[][][] rgbArray = new double[32][32][3];

        for(int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                double temp = temps[i][j];
                double red = 0, green = 0, blue =0;
                //temperatures below 256K (-17*C) are always black
                if ( temp < -17) {
                    //keep all rgb values at 0
                } else if (temp >= 55) {
                    //return white color for values above 328K (55*C)
                    red = 0xFF;
                    green = 0xFF;
                    blue = 0xFF;
                } else {
                    if (temp >= -17 && temp < 15) {
                        blue = ((temp + 17) / 2) * 0x11;
                    } else if (temp >= 15 && temp < 23) {
                        blue = 0xFF;
                        green = (temp - 15) * 0x22;
                    } else if (temp >= 23 && temp < 31) {
                        blue = 0;
                        green = 0xFF;
                        red = (temp - 23) * 0x22;
                    } else if (temp >= 31 && temp < 39) {
                        blue = 0;
                        green =0xFF - ((temp - 31) * 0x22);
                        red = 0xFF;
                    } else if (temp >= 39 && temp < 55) {
                        blue = ((temp - 39) * 0x11);
                        green = ((temp - 39) * 0x11);
                        red = 0xFF;
                    }
                }
                rgbArray[i][j][0] = red;
                rgbArray[i][j][1] = green;
                rgbArray[i][j][2] = blue;
            }
        }
        return rgbArray;
    }

    protected int getColorFromKelvin(int pixVal) {
        int color = 0x000000;
        int calcRed = 0x00;
        int calcGreen = 0x00;
        int calcBlue = 0x00;

        //temperatures below 256K (-17*C) are always black
        if (pixVal < 256) {
            return 0x000000;
        } else if (pixVal >= 328) {
            //return white color for values above 328K (55*C)
            return 0xFFFFFF;
        } else {
            if (pixVal >= 256 && pixVal < 288) {
                calcBlue = ((pixVal - 256) / 2) * 0x11;
            } else if (pixVal >= 288 && pixVal < 296) {
                calcBlue = 0xFF;
                calcGreen = (pixVal - 288) * 0x22;
            } else if (pixVal >= 296 && pixVal < 304) {
                calcGreen = 0xFF;
                calcRed = (pixVal - 296) * 0x22;
            } else if (pixVal >= 304 && pixVal < 312) {
                calcGreen = 0xFF - ((pixVal - 304) * 0x22);
                calcRed = 0xFF;
            } else if (pixVal >= 312 && pixVal < 328) {
                calcBlue = ((pixVal - 312) * 0x11);
                calcGreen = ((pixVal - 312) * 0x11);
                calcRed = 0xFF;
            }

            color |= (calcRed << 16) | (calcGreen << 8) | calcBlue;
        }

        return color;
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_thermal);
//
//        thermalcameramanager = (ThermalCameraManager) getSystemService(this.THERMAL_CAMERA_SERVICE);
//        if (thermalcameramanager == null) {
//            Log.e(TAG, "cannot get Thermal Camera Manager");
//            return;
//        } else {
//            Log.e(TAG, "success getting Thermal Camera Manager");
//        }
//
//        handler = new Handler();
//        vs = new VisionStream(this);
//        //Initialize readouts from EEPROM
//        thermalcameramanager.getTemperatureMatrix(1);
//
//        handler.post(vs);
//    }









//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
////        if(event.getAction() == MotionEvent.ACTION_DOWN ){
////            try {
////                Bitmap bm = Bitmap.createBitmap(getResources().getAssets().open("grace-face-1.jpg"));
////            }catch (IOException e){
////                Log.e("MainActivity","Error reading file from assets",e);
////            }
////                    //.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
////            Canvas canvas = new Canvas(bm);
////            canvas.drawBitmap(bm, new Rect(0,0, bm.getWidth(),bm.getHeight()),
////                new Rect(0,0, bm.getWidth(),bm.getHeight()), null);
////
////        }
//
//
//
////        if(event.getAction() == MotionEvent.ACTION_DOWN ){
////        //TODO:Test mat
////        double[][] vals = new double[32][32];
////        Mat thermalImage = new Mat(32,32,CV_8UC1);
////
////        try {
////
////            String tempFile = "thermal.txt";
////            InputStream in = getResources().getAssets().open(tempFile);
////            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
////
////            double val;
////            for(int i = 0; i < 32; i++){
////                String[] temps = reader.readLine().split("\\t");
////                for(int j = 0; j < temps.length; j++) {
////                    if ((val = Double.parseDouble(temps[j] = temps[j].replace("+",""))) < 0) {
////                        vals[i][j] = 0;
////                    }else{
////                        vals[i][j] = val*2;
////                    }
////                }
////            }
////            reader.close();
////        } catch (Exception e) {
////            Log.e("OpenCVActivity", "Error loading Temperature File", e);
////        }
////
////        //adds the values from the int array of doubled temps to the mat
////        for(int i = 0; i < 32; i++)
////            for(int j = 0; j < 32; j++)
////                thermalImage.put(i,j,vals[i][j]);
////
////        if(thermalImage.empty()){
////            Log.e("OpenCVActivity", "Error filling Mat with values");
////        }
////
////        Bitmap bm = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
////        Utils.matToBitmap(thermalImage,bm);
////        Canvas canvas = new Canvas(bm);
////        canvas.drawBitmap(bm, new Rect(0,0, bm.getWidth(),bm.getHeight()),
////                new Rect(0,0, bm.getWidth(),bm.getHeight()), null);
////
////
//////
//////        try {
//////            File file = new File("assets", "MatImage.jpg");
//////            OutputStream fOut = new FileOutputStream(file);
//////            bm.compress(Bitmap.CompressFormat.JPEG,0,fOut);
//////            fOut.close();
//////        } catch(IOException e){
//////            e.printStackTrace();
//////        }
////            if(imageSelect < 5) {
////                imageSelect++;
////            }else{
////                imageSelect = 0;
////            }
////        }
////
////        //Switches the image being shown when screen is tapped by incrementing imageSelect in the onTouchEvent method
//////        String file;
//////        switch (imageSelect){
//////            case 0: file = "FaceOn(23p5).txt"; break;
//////            case 1: file = "Sitting(23p5).txt"; break;
//////            case 2: file = "Room(23p5).txt"; break;
//////            case 3: file = "Window(23p5).txt"; break;
//////            case 4: file = "Celling(23p5).txt"; break;
//////            case 5: file = "PC(23p5).txt"; break;
//////            default: file = "thermal.txt"; break;
//////        }
//////
//////        //hijacks the aInputFrame Mat and replaces it with a stretched version of the thermal Mat
//////        Size fullScreen = new Size (aInputFrame.size().width,aInputFrame.size().height);
//////        Imgproc.resize(readThermalImage(file),aInputFrame,fullScreen);
//////
//////        calculateAmbientTemp(aInputFrame,file);
////
//
//        return super.onTouchEvent(event);
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
//    }
}
