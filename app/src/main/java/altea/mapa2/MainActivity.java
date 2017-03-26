package altea.mapa2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static altea.mapa2.R.id.map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnCameraIdleListener, LocationListener {

    List<ParkingMesto> mesta;
    List<ParkingMesto> trenutnoVidljivaMesta;
    List<Marker> trenutnoVidljiviMarkeri;
    List<LatLng> pozicije;
    GoogleMap currentMap;
    TileOverlay heatmapOverlay;
    HeatmapTileProvider heatmapProvider;
    int initialZoomPaddingPercent = 20;
    GoogleApiClient mGoogleApiClient;
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 300;
    Circle pinRadiusCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        GoogleMapOptions options = new GoogleMapOptions();
        options.compassEnabled(false).rotateGesturesEnabled(false).tiltGesturesEnabled(false);
        final Context currentContext = this;
        mapFragment.getView().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if ((currentMap == null) || (mesta != null && mesta.size() > 0)) return;
                        List<String[]> pinovi = readCsv(currentContext);
                        mesta = new ArrayList<ParkingMesto>();
                        pozicije = new ArrayList<LatLng>();
                        List<WeightedLatLng> pozicijeExpert = new ArrayList<WeightedLatLng>();
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (String[] pin : pinovi) {
                            ParkingMesto mesto = new ParkingMesto(pin);
                            mesta.add(mesto);
                            pozicije.add(mesto.pozicija);
                            builder.include(mesto.pozicija);
//                            currentMap.addMarker(new MarkerOptions().position(mesto.pozicija).title(mesto.naziv));
//                            PolygonOptions rectOptions = new PolygonOptions().add(mesto.oblast.northeast,mesto.oblast.southwest);
//                            Polygon polygon = currentMap.addPolygon(rectOptions);
//                            polygon.setFillColor(Color.parseColor("#566ffb"));
//                            ----------
//                            CircleOptions circleOptions = new CircleOptions()
//                                    .center(mesto.pozicija);
                            Location ne = new Location("point A");
                            ne.setLatitude(mesto.oblast.northeast.latitude);
                            ne.setLongitude(mesto.oblast.northeast.longitude);
                            Location sw = new Location("point B");
                            sw.setLatitude(mesto.oblast.southwest.latitude);
                            sw.setLongitude(mesto.oblast.southwest.longitude);
                            pozicijeExpert.add(new WeightedLatLng(mesto.pozicija, ne.distanceTo(sw)));
//                            circleOptions.radius(ne.distanceTo(sw) / 2);
//                            Circle circle = currentMap.addCircle(circleOptions);
//                            circle.setFillColor(Color.argb(700, 86, 111, 251));
//                            circle.setStrokeWidth(0);
                        }
                        addHeatMap(pozicijeExpert);
                        LatLngBounds bounds = builder.build();
                        int mapPadding = (int) (mapFragment.getView().getWidth() * (initialZoomPaddingPercent / 100.0));
                        currentMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapPadding));
                    }
                });
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        trenutnoVidljiviMarkeri = new ArrayList<Marker>();
    }

    private void addHeatMap(List<WeightedLatLng> pozicijeExpert) {
        int[] colors = {
                Color.rgb(32, 172, 232),
                Color.rgb(54, 47, 247)
        };
        float[] startPoints = {
                0.2f, 1f
        };

        Gradient gradient = new Gradient(colors, startPoints);
        // Create a heat map tile provider, passing it the latlngs of the police stations.
        heatmapProvider = new HeatmapTileProvider.Builder().weightedData(pozicijeExpert).radius(20).gradient(gradient).build();
        // Add a tile overlay to the map, using the heat map tile provider.
        heatmapOverlay = currentMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapProvider));
    }


    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

//    public void onConnected(Bundle connectionHint) {
//        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                mGoogleApiClient);
//        if (mLastLocation != null) {
//            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
//            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
//        }
//    }


    public void onMapReady(GoogleMap googleMap) {
        currentMap = googleMap;
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setTiltGesturesEnabled(true);
        googleMap.setOnCameraIdleListener(this);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        currentMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    public List<String[]> readCsv(Context context) {
        List<String[]> questionList = new ArrayList<String[]>();
        AssetManager assetManager = context.getAssets();

        try {
            InputStream csvStream = assetManager.open("InPins");
            InputStreamReader csvStreamReader = new InputStreamReader(csvStream);
            CSVReader csvReader = new CSVReader(csvStreamReader);
            String[] line;

            // throw away the header
            csvReader.readNext();

            while ((line = csvReader.readNext()) != null) {
                questionList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return questionList;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //        if (mRequestingLocationUpdates) {
        LocationRequest gmsRequest = new LocationRequest();
        int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        gmsRequest.setPriority(priority);
//        gmsRequest.setFastestInterval(request.trackingRate);
//        gmsRequest.setInterval(request.trackingRate);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, gmsRequest, this);
        //        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        CircleOptions circleOptions = new CircleOptions().center(currentLatLng);
        circleOptions.radius(1000);
        if (pinRadiusCircle != null) pinRadiusCircle.remove();
        pinRadiusCircle = currentMap.addCircle(circleOptions);
        pinRadiusCircle.setFillColor(Color.argb(50, 86, 111, 251));
        pinRadiusCircle.setStrokeWidth(2);
        pinRadiusCircle.setStrokeColor(Color.argb(50, 26, 41, 240));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

    @Override
    public void onCameraIdle() {
        trenutnoVidljivaMesta = new ArrayList<ParkingMesto>();
        List<WeightedLatLng> pozicijeExpert = new ArrayList<WeightedLatLng>();
        LatLngBounds currentBounds = currentMap.getProjection().getVisibleRegion().latLngBounds;
        float zoomLvl = currentMap.getCameraPosition().zoom;
        for (ParkingMesto mesto : mesta) {
            if (currentBounds.contains(mesto.pozicija)) {
                trenutnoVidljivaMesta.add(mesto);
                if (zoomLvl >= 16) {
                    trenutnoVidljiviMarkeri.add(currentMap.addMarker(new MarkerOptions().position(mesto.pozicija).title(mesto.naziv)));
                } else {
                    Location ne = new Location("point A");
                    ne.setLatitude(mesto.oblast.northeast.latitude);
                    ne.setLongitude(mesto.oblast.northeast.longitude);
                    Location sw = new Location("point B");
                    sw.setLatitude(mesto.oblast.southwest.latitude);
                    sw.setLongitude(mesto.oblast.southwest.longitude);
                    pozicijeExpert.add(new WeightedLatLng(mesto.pozicija, ne.distanceTo(sw)));
                }
            }
        }
        if (zoomLvl >= 16) {
            heatmapOverlay.remove();
        } else {
            if (trenutnoVidljiviMarkeri.size() > 0) {
                heatmapOverlay.clearTileCache();
                addHeatMap(pozicijeExpert);
                for (Marker mark : trenutnoVidljiviMarkeri) {
                    mark.remove();
                }
                trenutnoVidljiviMarkeri.clear();
            } else {
                if (pozicijeExpert.size() == 0) return;
                heatmapProvider.setWeightedData(pozicijeExpert);
                heatmapOverlay.clearTileCache();
            }
        }
    }
}
