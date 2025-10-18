package com.example.myapplication6;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import java.io.InputStream;

public class MyLocationLayerActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private boolean isEditMode = false;

    private ImageView overlayImage;
    private View overlayControls;
    private SupportMapFragment mapFragment;
    private Matrix imageMatrix = new Matrix();
    private float scaleFactor = 1f;
    private float rotationDegrees = 0f;
    private float translationX = 0f;
    private float translationY = 0f;

    private ScaleGestureDetector scaleGestureDetector;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MyLocationOverlay myOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean firstLocationUpdate = true;
    private boolean isFollowMode = false; // 追従モードのフラグ
    private final ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(this, "位置情報のパーミッションが必要です", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    //UIの作成
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_location);

        myOverlay = findViewById(R.id.myLocationOverlay);
        myOverlay.bringToFront();

        overlayImage = findViewById(R.id.overlay_image);
        overlayControls = findViewById(R.id.overlay_controls);
        Button editModeButton = findViewById(R.id.edit_mode_btn);
        SeekBar alphaSeekBar = findViewById(R.id.alpha_seekbar);
        TextView editModeText = findViewById(R.id.edit_mode_text);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        //編集モード
        editModeButton.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            if (isEditMode) {
                overlayImage.setOnTouchListener(new OverlayTouchListener());
                overlayControls.setVisibility(View.VISIBLE);
                if (mapFragment != null && mapFragment.getView() != null) {
                    mapFragment.getView().setVisibility(View.GONE);
                }
                editModeButton.setText("編集モード ON");
                editModeText.setVisibility(View.VISIBLE);
            } else {
                overlayImage.setOnTouchListener(null);
                overlayControls.setVisibility(View.GONE);
                if (mapFragment != null && mapFragment.getView() != null) {
                    mapFragment.getView().setVisibility(View.VISIBLE);
                }
                editModeButton.setText("編集モード OFF");
                editModeText.setVisibility(View.GONE);
            }
        });

        Button selectImageButton = findViewById(R.id.select_image_btn);
        selectImageButton.setOnClickListener(v -> openImagePicker());

        //ギャラリーなどから画像を選んで地図上に重ねる
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                overlayImage.setImageBitmap(BitmapFactory.decodeStream(inputStream));
                                inputStream.close();
                                overlayImage.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(this, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        //透過度調整スライダー
        alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                overlayImage.setAlpha(progress / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        overlayImage.setScaleType(ImageView.ScaleType.MATRIX);
        overlayImage.setImageMatrix(imageMatrix);

        //ピンチズームの検出
        scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scaleFactor *= detector.getScaleFactor();
                        scaleFactor = Math.max(0.3f, Math.min(scaleFactor, 5.0f));
                        applyTransformations();
                        return true;
                    }
                });

        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //現在地ボタン
        ImageButton myLocationBtn = findViewById(R.id.btnMyLocation);
        myLocationBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                isFollowMode = true; // 追従ON
                startLocationUpdates();
            } else {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        // Googleマップの青丸に同期するコールバック
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || googleMap == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    // Projectionで画面座標を取得して青丸更新
                    Point screenPoint = googleMap.getProjection().toScreenLocation(latLng);
                    myOverlay.setLocation(screenPoint.x, screenPoint.y);

                    // 追従モードONならカメラも移動
                    if (isFollowMode) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        isFollowMode = false;//追従モードのフラグを切る
                    }
                }
            }
        };


    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    @Override
    //マップが準備できたタイミングで呼ばれる
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // 現在地ボタンと位置情報表示ON
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true); // 標準青丸ON
        } else {
            // パーミッションをリクエストする
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Googleマップ標準のUI設定
        googleMap.getUiSettings().setMyLocationButtonEnabled(true); // ボタン表示ON
        googleMap.getUiSettings().setZoomControlsEnabled(true);      // ズームコントロールもONにしたい場合
    }


    @SuppressLint("MissingPermission")
    //位置情報を更新
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000);

        fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
    }

    @Override
    //画面を離れたら位置情報の取得を止める
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    //回転・拡大・移動をまとめて反映
    private void applyTransformations() {
        imageMatrix.reset();
        float px = overlayImage.getWidth() / 2f;
        float py = overlayImage.getHeight() / 2f;
        imageMatrix.postScale(scaleFactor, scaleFactor, px, py);
        imageMatrix.postRotate(rotationDegrees, px, py);
        imageMatrix.postTranslate(translationX, translationY);
        overlayImage.setImageMatrix(imageMatrix);
    }

    //画像を 指でドラッグ・ピンチ・回転 できるようにする
    private class OverlayTouchListener implements View.OnTouchListener {
        private float lastX, lastY;
        private float initialAngle = 0f;
        private boolean isRotating = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!isEditMode) return false;

            scaleGestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    isRotating = false;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 2) {
                        initialAngle = getAngle(event);
                        isRotating = true;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() == 1 && !isRotating) {
                        // 平行移動
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        translationX += dx;
                        translationY += dy;
                        applyTransformations();
                        lastX = event.getX();
                        lastY = event.getY();
                    } else if (event.getPointerCount() == 2 && isRotating) {
                        float currentAngle = getAngle(event);
                        float deltaAngle = currentAngle - initialAngle;
                        rotationDegrees += deltaAngle;
                        applyTransformations();
                        initialAngle = currentAngle;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerCount() == 2) {
                        isRotating = false;
                    }
                    break;
            }
            return true;
        }

        // 2点間の角度を取得
        private float getAngle(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.toDegrees(Math.atan2(dy, dx));
        }
    }

}
