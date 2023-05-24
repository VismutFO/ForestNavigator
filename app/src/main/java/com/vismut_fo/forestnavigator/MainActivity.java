package com.vismut_fo.forestnavigator;

import static android.graphics.Bitmap.Config.ARGB_8888;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;



    private BingMap bingMap;

    private Network network;

    private volatile Bitmap source, result;
    private volatile Drawable bitmapDrawable;
    int width, height;

    private boolean needToDisplayUser = false;

    private TextView latitude, longitude;
    private Button runButton, clearButton;
    private ImageView networkResult;

    private ProgressBar spinner;

    private boolean[][] usedPixels;

    boolean getNeedToDisplayUser() {
        return needToDisplayUser;
    }

    void OnMapCameraChanged(double x, double y, Bitmap source) {
        Log.d("<Layers>", "onMapCameraChanged");
        latitude.setText(String.format(Locale.getDefault(), "%f", x));
        longitude.setText(String.format(Locale.getDefault(), "%f", y));

        latitude.invalidate();
        longitude.invalidate();

        for (int i = 0; i < result.getWidth(); i++) {
            for (int j = 0; j < result.getHeight(); j++) {
                result.setPixel(i, j, Color.TRANSPARENT);
            }
        }
        if (width < height) {
            bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, width, false));
        }
        else {
            bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, height, height, false));

        }
        networkResult.setImageDrawable(bitmapDrawable);
        networkResult.invalidate();

        this.source = source;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("<Layers>", "OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        needToDisplayUser = false;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]
                    {android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST_LOCATION);
        }
        else {
            needToDisplayUser = true;
        }

        bingMap = new BingMap(this, savedInstanceState);
        ((FrameLayout)findViewById(R.id.map_view)).addView(bingMap.getmMapView());

        network = new Network(this);
        initAllForNetwork();
        initAllGraphics();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bingMap.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bingMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        bingMap.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        bingMap.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bingMap.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bingMap.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        bingMap.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                needToDisplayUser = true;
            } else {
                Log.d("<Layers>", "Something wrong with permission");
            }
        }
    }

    private void initAllForNetwork() {
        usedPixels = new boolean[512][512];
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        width = size.x;
        height = size.y;
        Log.d("<Layers>", Integer.toString(width) + " " + Integer.toString(height));

        source = null;
        result = Bitmap.createBitmap(512, 512, ARGB_8888);
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                result.setPixel(i, j, Color.TRANSPARENT);
            }
        }


    }

    private void initAllGraphics() {
        spinner = (ProgressBar)findViewById(R.id.progressSpinner);
        spinner.setVisibility(View.GONE);

        latitude = (TextView)findViewById(R.id.XCoordinate);
        longitude = (TextView)findViewById(R.id.YCoordinate);

        if (width < height) {
            bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, width, false));
        }
        else {
            bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, height, height, false));

        }
        networkResult = findViewById(R.id.networkResult);
        networkResult.setImageDrawable(bitmapDrawable);

        runButton = (Button) findViewById(R.id.runNetworkButton);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the runButton");
                Log.d("<Layers>", "onModuleStart");
                spinner.setVisibility(View.VISIBLE);
                network.run(source);

            }
        });

        clearButton = (Button) findViewById(R.id.clearLayerButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the clearButton");
                for (int i = 0; i < result.getWidth(); i++) {
                    for (int j = 0; j < result.getWidth(); j++) {
                        result.setPixel(i, j, Color.TRANSPARENT);
                    }
                }
                if (width < height) {
                    bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, width, false));
                }
                else {
                    bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, height, height, false));

                }
                networkResult.setImageDrawable(bitmapDrawable);
                networkResult.invalidate();
            }
        });
    }

    private int countNonEmpty(int i, int j) {
        usedPixels[i][j] = true;
        Log.d("<NonEmpty>", Integer.toString(i) + " " + Integer.toString(j));
        int count = 1;
        if (i - 1 >= 0 && !usedPixels[i - 1][j] && result.getPixel(i - 1, j) != Color.TRANSPARENT) {
            count += countNonEmpty(i - 1, j);
        }
        if (j - 1 >= 0 && !usedPixels[i][j - 1] && result.getPixel(i, j - 1) != Color.TRANSPARENT) {
            count += countNonEmpty(i, j - 1);
        }
        if (i + 1 < 512 && !usedPixels[i + 1][j] && result.getPixel(i + 1, j) != Color.TRANSPARENT) {
            count += countNonEmpty(i + 1, j);
        }
        if (j + 1 < 512 && !usedPixels[i][j + 1] && result.getPixel(i, j + 1) != Color.TRANSPARENT) {
            count += countNonEmpty(i, j + 1);
        }
        return count;
    }

    private void clearNonEmpty(int i, int j) {
        result.setPixel(i, j, Color.TRANSPARENT);
        if (i - 1 >= 0 && result.getPixel(i - 1, j) != Color.TRANSPARENT) {
            clearNonEmpty(i - 1, j);
        }
        if (j - 1 >= 0 && result.getPixel(i, j - 1) != Color.TRANSPARENT) {
            clearNonEmpty(i, j - 1);
        }
        if (i + 1 < 512 && result.getPixel(i + 1, j) != Color.TRANSPARENT) {
            clearNonEmpty(i + 1, j);
        }
        if (j + 1 < 512 && result.getPixel(i, j + 1) != Color.TRANSPARENT) {
            clearNonEmpty(i, j + 1);
        }
    }

    void onNetworkEnd(float[] scores) {
        Log.d("<Layers>", "scores size: " + scores.length);
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                usedPixels[i][j] = false;
                if (scores[i * 512 + j] < scores[i * 512 + j + 262144]) {
                    result.setPixel(j, i, 0xA00000AA);
                } else {
                    result.setPixel(j, i, Color.TRANSPARENT);
                }
            }
        }
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                if (result.getPixel(i, j) != Color.TRANSPARENT && !usedPixels[i][j]) {
                    int count = countNonEmpty(i, j);
                    if (count < 680) {
                        clearNonEmpty(i, j);
                    }
                }
            }
        }
        Log.d("<Layers>", "filledNewResult");
        spinner.setVisibility(View.INVISIBLE);
        if (width < height) {
            bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, width, false));
        }
        else {
            bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, height, height, false));
        }
        networkResult.setImageDrawable(bitmapDrawable);
        Log.d("<Layers>", "addedNewResult");
        networkResult.invalidate();
    }
}