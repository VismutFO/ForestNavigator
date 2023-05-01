package com.vismut_fo.forestnavigator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.microsoft.maps.CaptureScreenShotListener;
import com.microsoft.maps.MapCameraChangedEventArgs;
import com.microsoft.maps.MapLayer;
import com.microsoft.maps.MapLoadingStatus;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapCameraChangedListener;
import com.microsoft.maps.OnMapLoadingStatusChangedListener;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class MainActivity extends AppCompatActivity {

    private class ImageCapturer implements CaptureScreenShotListener {

        @Override
        public void onCaptureScreenShotCompleted(Bitmap bitmap) {
            //CaptureScreenShotListener.super.onCaptureScreenShotCompleted(bitmap);
        }
    }

    private MapView mMapView;
    private Bitmap source;

    private Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);
        ((FrameLayout)findViewById(R.id.map_view)).addView(mMapView);
        mMapView.setMapStyleSheet(MapStyleSheets.aerialWithOverlay());

        /* Log.d("<Layers>", Integer.toString(mMapView.getLayers().size()));
        for (MapLayer q : mMapView.getLayers()) {
            Log.d("<Layers>", q.getClass().toString());

        };*/

        mMapView.addOnMapLoadingStatusChangedListener(new OnMapLoadingStatusChangedListener() {
            @Override
            public boolean onMapLoadingStatusChanged(MapLoadingStatus mapLoadingStatus) {
                Log.d("<Layers>", "onMapLoadingStatusChanged");
                return false;
            }
        });

        mMapView.addOnMapCameraChangedListener(new OnMapCameraChangedListener() {
            @Override
            public boolean onMapCameraChanged(MapCameraChangedEventArgs mapCameraChangedEventArgs) {
                Log.d("<Layers>", "onMapCameraChanged");

                mMapView.beginCaptureImage(new CaptureScreenShotListener() {
                    @Override
                    public void onCaptureScreenShotCompleted(Bitmap bitmap) {
                        Log.d("<Layers>", "onCaptureScreenShotCompleted");
                    }
                });
                return false;
            }
        });


                /* Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
                Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                byte[] scores = outputTensor.getDataAsByteArray();
                source = bitmap; */
  
        mMapView.onCreate(savedInstanceState);
        try {
            module = LiteModuleLoader.load(getModuleFilePath(this, "model3.ptl"));
        } catch (IOException e) {
            Log.d("<Pytorch>", this.getFilesDir().getAbsolutePath(), e);
            finish();
            throw new RuntimeException(this.getFilesDir().getAbsolutePath());
            //throw new RuntimeException(e);
        }

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