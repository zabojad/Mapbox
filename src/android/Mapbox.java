package com.telerik.plugins.mapbox;

//import com.telerik.plugins.mapbox.R;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.support.design.widget.FloatingActionButton;
import android.graphics.Color;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.mapbox.mapboxsdk.annotations.*;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngZoom;
import com.mapbox.mapboxsdk.views.MapView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.drawable.BitmapDrawable;

import android.content.Context;
import android.content.res.AssetManager;

// TODO for screen rotation, see https://www.mapbox.com/mapbox-android-sdk/#screen-rotation
// TODO fox Xwalk compat, see nativepagetransitions plugin
// TODO look at demo app: https://github.com/mapbox/mapbox-gl-native/blob/master/android/java/MapboxGLAndroidSDKTestApp/src/main/java/com/mapbox/mapboxgl/testapp/MainActivity.java
public class Mapbox extends CordovaPlugin {

  public static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
  public static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
  public static final int LOCATION_REQ_CODE = 0;

  public static final int PERMISSION_DENIED_ERROR = 20;

  private static final String MAPBOX_ACCESSTOKEN_RESOURCE_KEY = "mapbox_accesstoken";

  private static final String ACTION_SHOW = "show";
  private static final String ACTION_HIDE = "hide";
  private static final String ACTION_ADD_BACKBUTTON_CALLBACK = "addBackButtonCallback";
  private static final String ACTION_ADD_MARKERS = "addMarkers";
  private static final String ACTION_ADD_MARKER = "addMarker";
  private static final String ACTION_REMOVE_MARKER = "removeMarker";
  private static final String ACTION_UPDATE_MARKER = "updateMarker";
  private static final String SHOW_MARKER_ANNOTATION = "showMarkerAnnotation";
  private static final String ACTION_REMOVE_ALL_MARKERS = "removeAllMarkers";
  private static final String ACTION_ADD_MARKER_CALLBACK = "addMarkerCallback";
  // TODO:
  // private static final String ACTION_REMOVE_MARKER_CALLBACK = "removeMarkerCallback";
  private static final String ACTION_ADD_POLYGON = "addPolygon";
  private static final String ACTION_ADD_GEOJSON = "addGeoJSON";
  private static final String ACTION_GET_ZOOMLEVEL = "getZoomLevel";
  private static final String ACTION_SET_ZOOMLEVEL = "setZoomLevel";
  private static final String ACTION_GET_CENTER = "getCenter";
  private static final String ACTION_SET_CENTER = "setCenter";
  private static final String ACTION_GET_TILT = "getTilt";
  private static final String ACTION_SET_TILT = "setTilt";
  private static final String ACTION_ANIMATE_CAMERA = "animateCamera";
  private static final String ACTION_ON_REGION_WILL_CHANGE = "onRegionWillChange";
  private static final String ACTION_ON_REGION_IS_CHANGING = "onRegionIsChanging";
  private static final String ACTION_ON_REGION_DID_CHANGE = "onRegionDidChange";
  private static final String ACTION_ON_SET_MY_LOCATION = "onSetMyLocation";


  // TODO:
  // private static final String ACTION_OFF_REGION_WILL_CHANGE = "offRegionWillChange";
  // private static final String ACTION_OFF_REGION_IS_CHANGING = "offRegionIsChanging";
  // private static final String ACTION_OFF_REGION_DID_CHANGE = "offRegionDidChange";

  public static MapView mapView;
  private static float retinaFactor;
  private String accessToken;
  private CallbackContext callback;
  private CallbackContext markerCallbackContext;
  private CallbackContext backbuttonCallbackContext;
  private boolean showUserLocation;
  private boolean setMyLocation;
  private Button my_location_button;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    DisplayMetrics metrics = new DisplayMetrics();
    cordova.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    retinaFactor = metrics.density;

    try {
      int mapboxAccesstokenResourceId = cordova.getActivity().getResources().getIdentifier(MAPBOX_ACCESSTOKEN_RESOURCE_KEY, "string", cordova.getActivity().getPackageName());
      accessToken = cordova.getActivity().getString(mapboxAccesstokenResourceId);
    } catch (Resources.NotFoundException e) {
      // we'll deal with this when the accessToken property is read, but for now let's dump the error:
      e.printStackTrace();
    }
  }

  @Override
  public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
    this.callback = callbackContext;
    try {
      if (ACTION_SHOW.equals(action)) {
        final JSONObject options = args.getJSONObject(0);
        final String style = getStyle(options.optString("style"));

        final JSONObject margins = options.isNull("margins") ? null : options.getJSONObject("margins");
        final int left = applyRetinaFactor(margins == null || margins.isNull("left") ? 0 : margins.getInt("left"));
        final int right = applyRetinaFactor(margins == null || margins.isNull("right") ? 0 : margins.getInt("right"));
        final int top = applyRetinaFactor(margins == null || margins.isNull("top") ? 0 : margins.getInt("top"));
        final int bottom = applyRetinaFactor(margins == null || margins.isNull("bottom") ? 0 : margins.getInt("bottom"));

        final JSONObject center = options.isNull("center") ? null : options.getJSONObject("center");
        this.showUserLocation = !options.isNull("showUserLocation") && options.getBoolean("showUserLocation");
        this.setMyLocation = options.isNull("hideSetMyLocation") || options.getBoolean("hideSetMyLocation") == false;

        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (accessToken == null) {
              callbackContext.error(MAPBOX_ACCESSTOKEN_RESOURCE_KEY + " not set in strings.xml");
              return;
            }
            mapView = new MapView(webView.getContext(), accessToken);

            // need to do this to register a receiver which onPause later needs
            mapView.onResume();
            mapView.onCreate(null);

            try {
              mapView.setCompassEnabled(options.isNull("hideCompass") || !options.getBoolean("hideCompass"));
              mapView.setRotateEnabled(options.isNull("disableRotation") || !options.getBoolean("disableRotation"));
              mapView.setScrollEnabled(options.isNull("disableScroll") || !options.getBoolean("disableScroll"));
              mapView.setZoomEnabled(options.isNull("disableZoom") || !options.getBoolean("disableZoom"));
              mapView.setTiltEnabled(options.isNull("disableTilt") || !options.getBoolean("disableTilt"));

              // placing these offscreen in case the user wants to hide them
              if (!options.isNull("hideAttribution") && options.getBoolean("hideAttribution")) {
                mapView.setAttributionMargins(-300, 0, 0, 0);
              }
              if (!options.isNull("hideLogo") && options.getBoolean("hideLogo")) {
                mapView.setLogoMargins(-300, 0, 0, 0);
              }

              if (showUserLocation) {
                showUserLocation();
              }

              Double zoom = options.isNull("zoomLevel") ? 10 : options.getDouble("zoomLevel");
              float zoomLevel = zoom.floatValue();
              if (center != null) {
                final double lat = center.getDouble("lat");
                final double lng = center.getDouble("lng");
                mapView.setLatLng(new LatLngZoom(lat, lng, zoomLevel));
              } else {
                if (zoomLevel > 18.0) {
                  zoomLevel = 18.0f;
                }
                mapView.setZoom(zoomLevel);
              }

              if (options.has("markers")) {
                addMarkers(options.getJSONArray("markers"));
              }
            } catch (Throwable e) {
              callbackContext.error(e.toString());
              return;
            }

            mapView.setStyleUrl(style);

            // position the mapView overlay
            int webViewWidth = webView.getView().getWidth();
            int webViewHeight = webView.getView().getHeight();
            final FrameLayout layout = (FrameLayout) webView.getView().getParent();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(webViewWidth - left - right, webViewHeight - top - bottom);
            params.setMargins(left, top, right, bottom);
            mapView.setLayoutParams(params);
            layout.addView(mapView);

            if (setMyLocation) {
              FrameLayout.LayoutParams lparams = new FrameLayout.LayoutParams(200, 200);
              lparams.setMargins(webViewWidth - left - right - 220, webViewHeight - top - bottom - 220, 0, 0);
              my_location_button = new Button(webView.getContext());
              try {
                Context ctx = cordova.getActivity().getApplicationContext();
                InputStream istream = ctx.getResources().getAssets().open("www/img/loc.svg");
                my_location_button.setBackgroundColor(Color.TRANSPARENT);
                my_location_button.setForeground(
                        createSVG(SVG.getFromInputStream(istream), 200, 200));
              } catch (Throwable e) {
                callbackContext.error(e.toString());
              };
              my_location_button.setLayoutParams(lparams);
              mapView.addView(my_location_button);
            }

            callbackContext.success();
          }
        });

      } else if (ACTION_HIDE.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              ViewGroup vg = (ViewGroup) mapView.getParent();
              if (vg != null) {
                vg.removeView(mapView);
              }
              callbackContext.success();
            }
          });
        }

      } else if (ACTION_GET_ZOOMLEVEL.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              final double zoomLevel = mapView.getZoom();
              callbackContext.success("" + zoomLevel);
            }
          });
        }

      } else if (ACTION_SET_ZOOMLEVEL.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                final JSONObject options = args.getJSONObject(0);
                final double zoom = options.getDouble("level");
                if (zoom >= 0 && zoom <= 20) {
                  final boolean animated = !options.isNull("animated") && options.getBoolean("animated");
                  mapView.setZoom(zoom, animated);
                  callbackContext.success();
                } else {
                  callbackContext.error("invalid zoomlevel, use any double value from 0 to 20 (like 8.3)");
                }
              } catch (JSONException e) {
                callbackContext.error(e.getMessage());
              }
            }
          });
        }

      } else if (ACTION_GET_CENTER.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              final LatLng center = mapView.getLatLng();
              Map<String, Double> result = new HashMap<String, Double>();
              result.put("lat", center.getLatitude());
              result.put("lng", center.getLongitude());
              callbackContext.success(new JSONObject(result));
            }
          });
        }

      } else if (ACTION_SET_CENTER.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                final JSONObject options = args.getJSONObject(0);
                final boolean animated = !options.isNull("animated") && options.getBoolean("animated");
                final double lat = options.getDouble("lat");
                final double lng = options.getDouble("lng");
                mapView.setLatLng(new LatLng(lat, lng), animated);
                callbackContext.success();
              } catch (JSONException e) {
                callbackContext.error(e.getMessage());
              }
            }
          });
        }

      } else if (ACTION_GET_TILT.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              final double tilt = mapView.getTilt();
              callbackContext.success("" + tilt);
            }
          });
        }

      } else if (ACTION_SET_TILT.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                final JSONObject options = args.getJSONObject(0);
                mapView.setTilt(
                        options.optDouble("pitch", 20),      // default 20
                        options.optLong("duration", 5000)); // default 5s
                callbackContext.success();
              } catch (JSONException e) {
                callbackContext.error(e.getMessage());
              }
            }
          });
        }

      } else if (ACTION_ANIMATE_CAMERA.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              try {
                // TODO check mandatory elements
                final JSONObject options = args.getJSONObject(0);

                final JSONObject target = options.getJSONObject("target");
                final double lat = target.getDouble("lat");
                final double lng = target.getDouble("lng");

                final CameraPosition.Builder builder =
                        new CameraPosition.Builder()
                                .target(new LatLng(lat, lng));

                if (options.has("bearing")) {
                  builder.bearing(((Double) options.getDouble("bearing")).floatValue());
                }
                if (options.has("tilt")) {
                  builder.tilt(((Double) options.getDouble("tilt")).floatValue());
                }
                if (options.has("zoomLevel")) {
                  builder.zoom(((Double) options.getDouble("zoomLevel")).floatValue());
                }

                mapView.animateCamera(
                        CameraUpdateFactory.newCameraPosition(builder.build()),
                        (options.optInt("duration", 15)) * 1000, // default 15 seconds
                        null);

                callbackContext.success();
              } catch (JSONException e) {
                callbackContext.error(e.getMessage());
              }
            }
          });
        }

      } else if (ACTION_ADD_POLYGON.equals(action)) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              final PolygonOptions polygon = new PolygonOptions();
              final JSONObject options = args.getJSONObject(0);
              final JSONArray points = options.getJSONArray("points");
              for (int i = 0; i < points.length(); i++) {
                final JSONObject marker = points.getJSONObject(i);
                final double lat = marker.getDouble("lat");
                final double lng = marker.getDouble("lng");
                polygon.add(new LatLng(lat, lng));
              }
              mapView.addPolygon(polygon);

              callbackContext.success();
            } catch (JSONException e) {
              callbackContext.error(e.getMessage());
            }
          }
        });

      } else if (ACTION_ADD_GEOJSON.equals(action)) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            // TODO implement
            callbackContext.success();
          }
        });

      } else if (ACTION_ADD_MARKERS.equals(action)) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              addMarkers(args.getJSONArray(0));
              callbackContext.success(args.getJSONArray(0));
            } catch (Throwable e) {
              callbackContext.error(e.toString());
            }
          }
        });
      } else if (ACTION_ADD_MARKER.equals(action)) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              addMarkers(args.getJSONArray(0));
              callbackContext.success();
            } catch (Throwable e) {
              callbackContext.error(e.toString());
            }
          }
        });
      } else if (SHOW_MARKER_ANNOTATION.equals(action)) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              JSONObject marker = args.getJSONObject(0);
              List<Annotation> markers = mapView.getAllAnnotations();

              for (int i = 0; i < markers.size(); i++) {
                Marker sel = (Marker) markers.get(i);
                if (sel.getTitle().equals(marker.getString("title"))){
                  mapView.removeMarker(sel);
                }
              }
              final MarkerOptions mo = new MarkerOptions();
              mo.title(marker.isNull("title") ? null : marker.getString("title"));
              mo.snippet(marker.isNull("subtitle") ? null : marker.getString("subtitle"));
              mo.position(new LatLng(marker.getDouble("lat"), marker.getDouble("lng")));
              if (marker.has("image")) {
                mo.icon(createIcon(marker));
              }
              Marker m = mapView.addMarker(mo);
              mapView.selectMarker(m);
              callbackContext.success();
            } catch (Throwable e) {
              callbackContext.error(e.toString());
            }
          }
        });
      } else if (ACTION_UPDATE_MARKER.equals(action)) {
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              JSONObject marker = args.getJSONObject(0);
              List<Annotation> markers = mapView.getAllAnnotations();
              for (int i = 0; i < markers.size(); i++) {
                Marker sel = (Marker) markers.get(i);
                if (sel.getTitle().equals(marker.getString("title"))){
                  mapView.removeMarker(sel);
                }
              }
              final MarkerOptions mo = new MarkerOptions();
              mo.title(marker.isNull("title") ? null : marker.getString("title"));
              mo.snippet(marker.isNull("subtitle") ? null : marker.getString("subtitle"));
              mo.position(new LatLng(marker.getDouble("lat"), marker.getDouble("lng")));
              if (marker.has("image")) {
                mo.icon(createIcon(marker));
              }
              mapView.addMarker(mo);
              callbackContext.success();
            } catch (Throwable e) {
              callbackContext.error(e.toString());
            }
          }
        });
      } else if (ACTION_REMOVE_ALL_MARKERS.equals(action)) {
        if (mapView != null) {
          cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              mapView.removeAllAnnotations();
              callbackContext.success();
            }
          });
        }

      } else if (ACTION_ADD_MARKER_CALLBACK.equals(action)) {
        this.markerCallbackContext = callbackContext;
        mapView.setOnInfoWindowClickListener(new com.telerik.plugins.mapbox.Mapbox.MarkerClickListener());

      } else if(ACTION_ADD_BACKBUTTON_CALLBACK.equals(action)){
        this.backbuttonCallbackContext = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if(mapView!=null)
              if(backbuttonCallbackContext!=null)
                //@anothar registering backbutton handler
                mapView.setOnKeyListener(new View.OnKeyListener() {
                  @Override
                  public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                            && keyCode == KeyEvent.KEYCODE_BACK
                            && event.getRepeatCount() == 0) {

                      if(event.getAction()!=KeyEvent.ACTION_DOWN)
                      {
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                        pluginResult.setKeepCallback(true);
                        backbuttonCallbackContext.sendPluginResult(pluginResult);
                      }
                      return true;
                    }
                    return false;
                  }
                });
              else
                mapView.setOnKeyListener(null);
          }});
      } else if (ACTION_ON_REGION_WILL_CHANGE.equals(action)) {
        if (mapView != null) {
          mapView.addOnMapChangedListener(new com.telerik.plugins.mapbox.Mapbox.RegionWillChangeListener(callbackContext));
        }

      } else if (ACTION_ON_REGION_IS_CHANGING.equals(action)) {
        if (mapView != null) {
          mapView.addOnMapChangedListener(new com.telerik.plugins.mapbox.Mapbox.RegionIsChangingListener(callbackContext));
        }

      } else if (ACTION_ON_REGION_DID_CHANGE.equals(action)) {
        if (mapView != null) {
          mapView.addOnMapChangedListener(new com.telerik.plugins.mapbox.Mapbox.RegionDidChangeListener(callbackContext));
        }

      } else if (ACTION_ON_SET_MY_LOCATION.equals(action)) {
        my_location_button.setOnClickListener(new com.telerik.plugins.mapbox.Mapbox.SetMyLocationListener(callbackContext));
      } else {
        return false;
      }
    } catch (Throwable t) {
      t.printStackTrace();
      callbackContext.error(t.getMessage());
    }
    return true;
  }

  private BitmapDrawable createSVG(SVG svg, int width, int height) throws SVGParseException {
    if (width == 0)
      width = applyRetinaFactor((int) Math.ceil(svg.getDocumentWidth()));
    if (height == 0)
      height = applyRetinaFactor((int) Math.ceil(svg.getDocumentHeight()));
    Bitmap newBM = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas bmcanvas = new Canvas(newBM);
    svg.renderToCanvas(bmcanvas);
    return new BitmapDrawable(cordova.getActivity().getApplicationContext().getResources(), newBM);
  }

  private Icon createIcon(JSONObject marker) throws JSONException, IOException, SVGParseException {
    InputStream istream = null;
    BitmapDrawable bitmap;
    Icon icon;
    Context ctx = cordova.getActivity().getApplicationContext();
    AssetManager am = ctx.getResources().getAssets();
    IconFactory iconFactory = IconFactory.getInstance(cordova.getActivity());
    final JSONObject imageSettings = marker.optJSONObject("image");
    try {
      if (imageSettings != null) {
        if (imageSettings.has("url")) {
          String filePath = imageSettings.getString("url");
          istream = am.open(filePath);
          if (filePath.endsWith(".svg")) {
            bitmap = createSVG(SVG.getFromInputStream(istream), imageSettings.has("width") ? applyRetinaFactor(imageSettings.getInt("width")) : 0,
                    imageSettings.has("height") ? applyRetinaFactor(imageSettings.getInt("height")) : 0);
          } else {
            bitmap = new BitmapDrawable(ctx.getResources(), istream);
          }
        } else if (imageSettings.has("data")) {
          byte[] decodedBytes = Base64.decode(imageSettings.getString("data"), 0);
          bitmap = new BitmapDrawable(ctx.getResources(), BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));

        } else if (imageSettings.has("svg")) {
          bitmap = createSVG(SVG.getFromString(imageSettings.getString("svg")), imageSettings.has("width") ? applyRetinaFactor(imageSettings.getInt("width")) : 0,
                  imageSettings.has("height") ? applyRetinaFactor(imageSettings.getInt("height")) : 0);
        } else {
          throw new JSONException("Not found image data");
        }
        if (imageSettings.has("width") && imageSettings.has("height")) {
          icon = iconFactory.fromDrawable(bitmap, applyRetinaFactor(imageSettings.getInt("width")),
                  applyRetinaFactor(imageSettings.getInt("height")));
        } else {
          icon = iconFactory.fromDrawable(bitmap);
        }

      } else {
        String filePath = marker.getString("image");
        istream = am.open(filePath);
        if (filePath.endsWith(".svg"))
          bitmap = createSVG(SVG.getFromInputStream(istream), 0, 0);
        else
          bitmap = new BitmapDrawable(ctx.getResources(), istream);
        icon = iconFactory.fromDrawable(bitmap);
      }
    } finally {
      if (istream != null)
        istream.close();
    }
    return icon;
  }

  private void addMarkers(JSONArray markers) throws JSONException,
          java.lang.NullPointerException,
          java.io.IOException, SVGParseException {

    for (int i = 0; i < markers.length(); i++) {
      final JSONObject marker = markers.getJSONObject(i);
      final MarkerOptions mo = new MarkerOptions();
      mo.title(marker.isNull("title") ? null : marker.getString("title"));
      mo.snippet(marker.isNull("subtitle") ? null : marker.getString("subtitle"));
      mo.position(new LatLng(marker.getDouble("lat"), marker.getDouble("lng")));
      if (marker.has("image")) {
        mo.icon(createIcon(marker));
      }
      mapView.addMarker(mo);
    }
  }

  private class RegionWillChangeListener implements MapView.OnMapChangedListener {
    private CallbackContext callback;

    public RegionWillChangeListener(CallbackContext providedCallback) {
      this.callback = providedCallback;
    }

    @Override
    public void onMapChanged(int change) {
      if ( change == MapView.REGION_WILL_CHANGE_ANIMATED ) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
      }
    }
  }

  private class RegionIsChangingListener implements MapView.OnMapChangedListener {
    private CallbackContext callback;

    public RegionIsChangingListener(CallbackContext providedCallback) {
      this.callback = providedCallback;
    }

    @Override
    public void onMapChanged(int change) {
      if ( change == MapView.REGION_IS_CHANGING ) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
      }
    }
  }

  private class RegionDidChangeListener implements MapView.OnMapChangedListener {
    private CallbackContext callback;

    public RegionDidChangeListener(CallbackContext providedCallback) {
      this.callback = providedCallback;
    }

    @Override
    public void onMapChanged(int change) {
      if ( change == MapView.REGION_DID_CHANGE_ANIMATED ) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        callback.sendPluginResult(pluginResult);
      }
    }
  }

  private class SetMyLocationListener implements View.OnClickListener {

    private CallbackContext callback;

    public SetMyLocationListener(CallbackContext providedCallback) {
      this.callback = providedCallback;
    }

    @Override
    public void onClick(View v) {
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
      pluginResult.setKeepCallback(true);
      callback.sendPluginResult(pluginResult);
    }
  }

  private class MarkerClickListener implements MapView.OnInfoWindowClickListener {

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
      // callback
      if (markerCallbackContext != null) {
        final JSONObject json = new JSONObject();
        try {
          json.put("title", marker.getTitle());
          json.put("subtitle", marker.getSnippet());
          json.put("lat", marker.getPosition().getLatitude());
          json.put("lng", marker.getPosition().getLongitude());
        } catch (JSONException e) {
          PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR,
                  "Error in callback of " + ACTION_ADD_MARKER_CALLBACK + ": " + e.getMessage());
          pluginResult.setKeepCallback(true);
          markerCallbackContext.sendPluginResult(pluginResult);
        }
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, json);
        pluginResult.setKeepCallback(true);
        markerCallbackContext.sendPluginResult(pluginResult);
        return true;
      }
      return false;
    }
  }

  private static int applyRetinaFactor(int i) {
    return (int) (i * retinaFactor);
  }

  private static String getStyle(final String requested) {
    if ("light".equalsIgnoreCase(requested)) {
      return Style.LIGHT;
    } else if ("dark".equalsIgnoreCase(requested)) {
      return Style.DARK;
    } else if ("emerald".equalsIgnoreCase(requested)) {
      return Style.EMERALD;
    } else if ("satellite".equalsIgnoreCase(requested)) {
      return Style.SATELLITE;
      // TODO not currently supported on Android
//    } else if ("hybrid".equalsIgnoreCase(requested)) {
//      return Style.HYBRID;
    } else if ("streets".equalsIgnoreCase(requested)) {
      return Style.MAPBOX_STREETS;
    } else {
      return requested;
    }
  }

  private boolean permissionGranted(String... types) {
    if (Build.VERSION.SDK_INT < 23) {
      return true;
    }
    for (final String type : types) {
      if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this.cordova.getActivity(), type)) {
        return false;
      }
    }
    return true;
  }

  protected void showUserLocation() {
    if (permissionGranted(COARSE_LOCATION, FINE_LOCATION)) {
      //noinspection MissingPermission
      mapView.setMyLocationEnabled(showUserLocation);
    } else {
      requestPermission(COARSE_LOCATION, FINE_LOCATION);
    }
  }


  private void requestPermission(String... types) {
    ActivityCompat.requestPermissions(
            this.cordova.getActivity(),
            types,
            LOCATION_REQ_CODE);
  }

  // TODO
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        this.callback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
        return;
      }
    }
    switch (requestCode) {
      case LOCATION_REQ_CODE:
        showUserLocation();
        break;
    }
  }

  public void onPause(boolean multitasking) {
    if (mapView != null) {
      mapView.onPause();
    }
  }

  public void onResume(boolean multitasking) {
    if (mapView != null) {
      mapView.onResume();
    }
  }

  public void onDestroy() {
    if (mapView != null) {
      mapView.onDestroy();
    }
  }
}
