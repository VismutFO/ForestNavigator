package com.vismut_fo.forestnavigator;

import static com.microsoft.maps.platformabstraction.IO.getApplicationContext;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.microsoft.maps.CaptureScreenShotListener;
import com.microsoft.maps.GPSMapLocationProvider;
import com.microsoft.maps.MapCameraChangedEventArgs;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapUserLocation;
import com.microsoft.maps.MapUserLocationTrackingMode;
import com.microsoft.maps.MapUserLocationTrackingState;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapCameraChangedListener;

import java.util.Locale;

public class BingMap {
    private final MapView mMapView;

    private final MainActivity owner;

    public BingMap(MainActivity owner, Bundle savedInstanceState) {
        this.owner = owner;
        mMapView = new MapView((Context) owner, MapRenderMode.RASTER);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);


        mMapView.setMapStyleSheet(MapStyleSheets.aerial());

        mMapView.addOnMapCameraChangedListener(new OnMapCameraChangedListener() {
            @Override
            public boolean onMapCameraChanged(@NonNull MapCameraChangedEventArgs mapCameraChangedEventArgs) {
                Log.d("<Layers>", "onMapCameraChanged");


                mMapView.beginCaptureImage(new CaptureScreenShotListener() {
                    @Override
                    synchronized public void onCaptureScreenShotCompleted(Bitmap bitmap) {
                        owner.OnMapCameraChanged(mMapView.getCenter().getPosition().getLatitude(),
                                mMapView.getCenter().getPosition().getLongitude(), bitmap);

                        Log.d("<Layers>", "onCaptureScreenShotCompleted");

                    }
                });
                return false;
            }
        });



        if (owner.getNeedToDisplayUser()) {
            Log.d("<Layers>", "getPermission");
            MapUserLocation userLocation = mMapView.getUserLocation();

            MapUserLocationTrackingState userLocationTrackingState =
                    userLocation.startTracking(new GPSMapLocationProvider.Builder(owner.getApplicationContext()).build());
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

    MapView getmMapView() {
        return mMapView;
    }

    public void onStart() {
        mMapView.onStart();
    }

    protected void onResume() {
        mMapView.onResume();
    }

    protected void onPause() {
        mMapView.onPause();
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        mMapView.onSaveInstanceState(outState);
    }

    protected void onStop() {
        mMapView.onStop();
    }

    protected void onDestroy() {
        mMapView.onDestroy();
    }

    public void onLowMemory() {
        mMapView.onLowMemory();
    }
}
