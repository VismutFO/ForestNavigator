package com.vismut_fo.forestnavigator;

import static android.graphics.Bitmap.Config.ARGB_8888;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.microsoft.maps.CaptureScreenShotListener;
import com.microsoft.maps.CustomTileMapLayer;
import com.microsoft.maps.MapCameraChangedEventArgs;
import com.microsoft.maps.MapLayer;
import com.microsoft.maps.MapLoadingStatus;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapTileBitmapRequestedEventArgs;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnBitmapRequestedListener;
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
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;

    private Module module;

    private volatile Bitmap result;

    private final Object lock = new Object();

    private int timeNotUsedModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timeNotUsedModule = (int) (System.currentTimeMillis()) - 20000;
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.RASTER);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);

        //CustomTileMapLayer networkResult = new CustomTileMapLayer();
        result = null;
        /*
        networkResult.addOnBitmapRequestedListener(new OnBitmapRequestedListener() {
            @Override
            public void onBitmapRequested(@NonNull MapTileBitmapRequestedEventArgs event) {
                Log.d("<Layers>", "catchBitmap");
                synchronized (lock) {
                    synchronized (result) {
                        if (result == null) {
                            Log.d("<Layers>", "FaildeToCatchBitmap");
                            return;
                        }
                        int size = result.getRowBytes() * result.getHeight();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                        result.copyPixelsToBuffer(byteBuffer);
                        event.getRequest().setPixelData(byteBuffer.array());
                        result = null;
                    }
                }
            }
        });
        */
        //mMapView.getLayers().add(networkResult);

        ((FrameLayout)findViewById(R.id.map_view)).addView(mMapView);
        mMapView.setMapStyleSheet(MapStyleSheets.aerialWithOverlay());

        /*
        mMapView.addOnMapLoadingStatusChangedListener(new OnMapLoadingStatusChangedListener() {
            @Override
            public boolean onMapLoadingStatusChanged(MapLoadingStatus mapLoadingStatus) {
                Log.d("<Layers>", "onMapLoadingStatusChanged");
                return false;
            }
        });
         */

        mMapView.addOnMapCameraChangedListener(new OnMapCameraChangedListener() {
            @Override
            public boolean onMapCameraChanged(@NonNull MapCameraChangedEventArgs mapCameraChangedEventArgs) {
                Log.d("<Layers>", "onMapCameraChanged");

                mMapView.beginCaptureImage(new CaptureScreenShotListener() {
                    @Override
                    public void onCaptureScreenShotCompleted(Bitmap bitmap) {
                        Log.d("<Layers>", "onCaptureScreenShotCompleted");
                        int temp = (int) (System.currentTimeMillis()) - timeNotUsedModule;
                        if (temp <= 20000) {
                            Log.d("<Layers>", Integer.toString(temp));
                            return;
                        }
                        Log.d("<Layers>", "onModuleStart");
                        timeNotUsedModule = (int) (System.currentTimeMillis());
                        new Thread(new Runnable() {
                            final Bitmap bitmapTemp = bitmap;
                            public void run() {
                                Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                                        bitmapTemp, 512, 512, false);
                                Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
                                Log.d("<Layers>", "onNetworkStart");
                                Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                                Log.d("<Layers>", "onNetworkEnd");
                                float[] scores = outputTensor.getDataAsFloatArray(); // ?
                                result = Bitmap.createBitmap(512, 512, ARGB_8888);
                                for (int index = 0; index < 512 * 512; index += 4) {
                                    byte[] temp = ByteBuffer.allocate(4).putInt(Float.floatToIntBits(scores[index / 4])).array();
                                    for (int j = 0; j < 4; j++) {
                                        result.setPixel((index + j) / 512, (index + j) % 512, temp[j]);
                                    }
                                }
                                // display result

                                Log.d("<Layers>", "onModuleEnd");
                            }
                            }).start();

                    }
                });
                return false;
            }
        });

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
        timeNotUsedModule++;
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

    public void runModule(Bitmap bitmap) {

    }
}