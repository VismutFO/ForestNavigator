package com.vismut_fo.forestnavigator;

import static android.graphics.Bitmap.Config.ARGB_8888;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.maps.CaptureScreenShotListener;
import com.microsoft.maps.CustomTileMapLayer;
import com.microsoft.maps.MapCameraChangedEventArgs;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapTileBitmapRequestedEventArgs;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnBitmapRequestedListener;
import com.microsoft.maps.OnMapCameraChangedListener;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;

    private Module module;

    private volatile Bitmap source, result;
    private Drawable bitmapDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.RASTER);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);

        source = null;

        result = Bitmap.createBitmap(512, 512, ARGB_8888);
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                result.setPixel(i, j, Color.TRANSPARENT);
            }
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        Log.d("<Layers>", Integer.toString(width) + " " + Integer.toString(height));

        ((FrameLayout)findViewById(R.id.map_view)).addView(mMapView);
        mMapView.setMapStyleSheet(MapStyleSheets.aerial());

        TextView latitude = (TextView)findViewById(R.id.XCoordinate), longtitude = (TextView)findViewById(R.id.YCoordinate);
        mMapView.addOnMapCameraChangedListener(new OnMapCameraChangedListener() {
            @Override
            public boolean onMapCameraChanged(@NonNull MapCameraChangedEventArgs mapCameraChangedEventArgs) {
                Log.d("<Layers>", "onMapCameraChanged");
                latitude.setText(String.format(Locale.getDefault(), "%f", mMapView.getCenter().getPosition().getLatitude()));
                longtitude.setText(String.format(Locale.getDefault(), "%f", mMapView.getCenter().getPosition().getLongitude()));

                latitude.invalidate();
                longtitude.invalidate();

                mMapView.beginCaptureImage(new CaptureScreenShotListener() {
                    @Override
                    public void onCaptureScreenShotCompleted(Bitmap bitmap) {
                        Log.d("<Layers>", "onCaptureScreenShotCompleted");
                        source = bitmap;
                        Log.d("<Layers>", "BitmapSize: " + Integer.toString(bitmap.getWidth()) + " " + Integer.toString(bitmap.getHeight()));
                    }
                });
                return false;
            }
        });
        mMapView.onCreate(savedInstanceState);

        mMapView.getCenter().getPosition();

        try {
            module = LiteModuleLoader.load(getModuleFilePath(this, "model3.ptl"));
        } catch (IOException e) {
            Log.d("<Pytorch>", this.getFilesDir().getAbsolutePath(), e);
            finish();
            throw new RuntimeException(this.getFilesDir().getAbsolutePath());
        }

        bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
        ImageView networkResult = findViewById(R.id.networkResult);
        networkResult.setImageDrawable(bitmapDrawable);

        Button runButton = (Button) findViewById(R.id.runNetworkButton);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the runButton");
                Log.d("<Layers>", "onModuleStart");
                new Thread(new Runnable() {
                    final Bitmap bitmapTemp = source;
                    public void run() {

                        Log.d("<Layers>", "InRun");
                        Log.d("<Layers>", "InLock");
                        Log.d("<Layers>", "InResult");
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                                bitmapTemp, 512, 512, false);
                        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
                        Log.d("<Layers>", "onNetworkStart");
                        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                        Log.d("<Layers>", "onNetworkEnd");
                        float[] scores = outputTensor.getDataAsFloatArray(); // ?
                        Log.d("<Layers>", "turnedTensorToArray");
                        for (int i = 0; i < 512; i++) {
                            for (int j = 0; j < 512; j++) {
                                if (scores[i * 512 + j] >= scores[262144 + i * 512 + j]) {
                                    result.setPixel(i, j, 0xA000AA00);
                                } else {
                                    result.setPixel(i, j, Color.TRANSPARENT);
                                }

                            }
                        }
                        Log.d("<Layers>", "filledNewResult");
                        bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
                        networkResult.setImageDrawable(bitmapDrawable);
                        networkResult.invalidate();

                        Log.d("<Layers>", "onModuleEnd");

                    }
                }).start();

            }
        });

        Button clearButton = (Button) findViewById(R.id.clearLayerButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the clearButton");
                for (int i = 0; i < result.getWidth(); i++) {
                    for (int j = 0; j < result.getWidth(); j++) {
                        result.setPixel(i, j, Color.TRANSPARENT);
                    }
                }
                bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
                networkResult.setImageDrawable(bitmapDrawable);
                networkResult.invalidate();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    public static String getModuleFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}