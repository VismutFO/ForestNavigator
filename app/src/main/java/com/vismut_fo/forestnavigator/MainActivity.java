package com.vismut_fo.forestnavigator;

import static android.graphics.Bitmap.Config.ARGB_8888;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microsoft.maps.CaptureScreenShotListener;
import com.microsoft.maps.GPSMapLocationProvider;
import com.microsoft.maps.MapCameraChangedEventArgs;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapUserLocation;
import com.microsoft.maps.MapUserLocationTrackingMode;
import com.microsoft.maps.MapUserLocationTrackingState;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapCameraChangedListener;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final float[] NO_MEAN_RGB = new float[] {0.5f, 0.5f, 0.5f};
    private static final float[] NO_STD_RGB = new float[] {0.5f, 0.5f, 0.5f};

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private MapView mMapView;

    private Module module;

    private volatile Bitmap source, result;
    private Drawable bitmapDrawable;
    private int width, height;

    private TextView latitude, longitude;
    private Button runButton, clearButton;
    private ImageView networkResult;

    private ProgressBar spinner;

    private boolean needToDisplayUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initMap(savedInstanceState);
        initAllForNetwork();
        initAllGraphics();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    needToDisplayUser = true;
                }
                else {
                    Log.d("<Layers>", "Something wrong with permission");
                }
            }

        }
    }

    private void initMap(Bundle savedInstanceState) {
        mMapView = new MapView(this, MapRenderMode.RASTER);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);

        ((FrameLayout)findViewById(R.id.map_view)).addView(mMapView);
        mMapView.setMapStyleSheet(MapStyleSheets.aerial());

        mMapView.addOnMapCameraChangedListener(new OnMapCameraChangedListener() {
            @Override
            public boolean onMapCameraChanged(@NonNull MapCameraChangedEventArgs mapCameraChangedEventArgs) {
                Log.d("<Layers>", "onMapCameraChanged");
                latitude.setText(String.format(Locale.getDefault(), "%f", mMapView.getCenter().getPosition().getLatitude()));
                longitude.setText(String.format(Locale.getDefault(), "%f", mMapView.getCenter().getPosition().getLongitude()));

                latitude.invalidate();
                longitude.invalidate();

                mMapView.beginCaptureImage(new CaptureScreenShotListener() {
                    @Override
                    synchronized public void onCaptureScreenShotCompleted(Bitmap bitmap) {
                        Log.d("<Layers>", "onCaptureScreenShotCompleted");
                        for (int i = 0; i < result.getWidth(); i++) {
                            for (int j = 0; j < result.getHeight(); j++) {
                                result.setPixel(i, j, Color.TRANSPARENT);
                            }
                        }
                        bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
                        networkResult.setImageDrawable(bitmapDrawable);
                        networkResult.invalidate();

                        //source = bitmap.extractAlpha();
                        source = bitmap;
                        Log.d("<Layers>", source.getConfig().toString());

                        File file = new File("", "file.jpg");
                        try (FileOutputStream out = new FileOutputStream("file.jpg")) {

                            //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            //MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Log.d("<Layers>", "BitmapSize: " + Integer.toString(bitmap.getWidth()) + " " + Integer.toString(bitmap.getHeight()));
                    }
                });
                return false;
            }
        });
        needToDisplayUser = false;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]
                    {android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST_LOCATION);
        }
        else {
            needToDisplayUser = true;
        }
        if (needToDisplayUser) {
            Log.d("<Layers>", "getPermission");
            MapUserLocation userLocation = mMapView.getUserLocation();

            MapUserLocationTrackingState userLocationTrackingState =
                    userLocation.startTracking(new GPSMapLocationProvider.Builder(getApplicationContext()).build());
            if (userLocationTrackingState == MapUserLocationTrackingState.PERMISSION_DENIED) {
                Log.d("<Layers>", "Get to user location without permission");
            } else if (userLocationTrackingState == MapUserLocationTrackingState.READY) {
                userLocation.setTrackingMode(MapUserLocationTrackingMode.CENTERED_ON_USER);
            } else if (userLocationTrackingState == MapUserLocationTrackingState.DISABLED) {
                Log.d("<Layers>", "Need to check on line 191");
            }
        }
        mMapView.onCreate(savedInstanceState);
    }

    private void initAllForNetwork() {
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

        try {
            module = LiteModuleLoader.load(getModuleFilePath(this, "model.ptl"));
        } catch (IOException e) {
            Log.d("<Pytorch>", this.getFilesDir().getAbsolutePath(), e);
            finish();
            throw new RuntimeException(this.getFilesDir().getAbsolutePath());
        }
    }

    private void initAllGraphics() {
        spinner = (ProgressBar)findViewById(R.id.progressSpinner);
        spinner.setVisibility(View.GONE);

        latitude = (TextView)findViewById(R.id.XCoordinate);
        longitude = (TextView)findViewById(R.id.YCoordinate);

        bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
        networkResult = findViewById(R.id.networkResult);
        networkResult.setImageDrawable(bitmapDrawable);

        runButton = (Button) findViewById(R.id.runNetworkButton);
        runButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the runButton");
                Log.d("<Layers>", "onModuleStart");
                spinner.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    final Bitmap bitmapTemp = source;
                    synchronized public void run() {

                        Log.d("<Layers>", "InRun");
                        Log.d("<Layers>", "InLock");
                        Log.d("<Layers>", "InResult");
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                                bitmapTemp, 512, 512, false);
                        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                                NO_MEAN_RGB, NO_STD_RGB);

                        Log.d("<Layers>", "onNetworkStart");
                        //Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                        Map<String, IValue> outTensors = module.forward(IValue.from(inputTensor)).toDictStringKey();
                        final Tensor outputTensor = Objects.requireNonNull(outTensors.get("out")).toTensor();

                        Log.d("<Layers>", "onNetworkEnd");
                        float[] scores = outputTensor.getDataAsFloatArray(); // ?
                        Log.d("<Layers>", "scores length: " + Integer.toString(scores.length));
                        Log.d("<Layers>", "turnedTensorToArray");
                        for (int i = 0; i < 512; i++) {
                            for (int j = 0; j < 512; j++) {
                                if (scores[i * 512 + j] < scores[262144 + i * 512 + j]) {
                                    result.setPixel(i, j, 0xA000AA00);
                                } else {
                                    result.setPixel(i, j, Color.TRANSPARENT);
                                }

                            }
                        }
                        Log.d("<Layers>", "filledNewResult");
                        spinner.setVisibility(View.INVISIBLE);
                        bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
                        networkResult.setImageDrawable(bitmapDrawable);
                        networkResult.invalidate();

                        Log.d("<Layers>", "onModuleEnd");

                    }
                }).start();

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
                bitmapDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(result, width, height, false));
                networkResult.setImageDrawable(bitmapDrawable);
                networkResult.invalidate();
            }
        });
    }

    private static String getModuleFilePath(Context context, String assetName) throws IOException {
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