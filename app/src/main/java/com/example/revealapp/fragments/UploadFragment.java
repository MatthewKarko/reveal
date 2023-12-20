package com.example.revealapp.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.revealapp.NavigationActivity;
import com.example.revealapp.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.Calendar;

public class UploadFragment extends Fragment {

    private LinearLayout uploadFormLayout;
    private TextView todaysThemeTextView;
    private TextView themeTitleTextView;
    private ImageView postImageImageView;
    private EditText postTitleEditText;
    private EditText postDescriptionEditText;
    private Button uploadPostButton;
    private Button cancelPostButton;
    private TextureView previewImageTextureView;
    private Button capturePictureButton;

    private CameraManager cameraManager;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private Handler cameraCaptureHandler;
    private byte[] imageBytes;

    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private DocumentReference userDocumentRef;
    private DocumentReference themeDocumentRef;

    public UploadFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        uploadFormLayout = view.findViewById(R.id.uploadFormlayout);
        todaysThemeTextView = view.findViewById(R.id.todaysThemeTextView);
        themeTitleTextView = view.findViewById(R.id.themeTitleTextView);
        postImageImageView = view.findViewById(R.id.postImageImageView);
        postTitleEditText = view.findViewById(R.id.postTitleEditText);
        postDescriptionEditText = view.findViewById(R.id.postDescriptionEditText);
        uploadPostButton = view.findViewById(R.id.uploadPostButton);
        cancelPostButton = view.findViewById(R.id.cancelPostButton);
        previewImageTextureView = view.findViewById(R.id.imagePreviewTextureView);
        capturePictureButton = view.findViewById(R.id.capturePictureButton);

        uploadFormLayout.setVisibility(View.GONE);
        postImageImageView.setVisibility(View.GONE);

        uploadPostButton.setOnClickListener(x -> uploadPost());

        cancelPostButton.setOnClickListener(x -> {
            uploadFormLayout.setVisibility(View.GONE);
            postImageImageView.setVisibility(View.GONE);
            previewImageTextureView.setVisibility(View.VISIBLE);
            capturePictureButton.setVisibility(View.VISIBLE);
            todaysThemeTextView.setVisibility(View.VISIBLE);
            themeTitleTextView.setVisibility(View.VISIBLE);
        });

        firebaseStorage = FirebaseStorage.getInstance("gs://revealapp-3dfcf.appspot.com");
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        String userId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        userDocumentRef = firebaseFirestore.collection("Users").document(userId);

        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        Log.d("HomeFragment", "Start of day: " + startOfDay.toString());
        Log.d("HomeFragment", "End of day: " + endOfDay.toString());

        firebaseFirestore.collection("Themes")
                .whereGreaterThanOrEqualTo("Date", startOfDay)
                .whereLessThanOrEqualTo("Date", endOfDay)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().getDocuments().isEmpty()) {
                        DocumentSnapshot themeDocument = task.getResult().getDocuments().get(0);
                        String themeId = themeDocument.getId();
                        themeTitleTextView.setText(themeDocument.getString("Title"));
                        themeDocumentRef = firebaseFirestore.collection("Themes").document(themeId);
                        Log.d("HomeFragment", "Found theme ID for today: " + themeId);

                        firebaseFirestore.collection("Posts")
                                .whereEqualTo("Theme", themeDocumentRef)
                                .whereEqualTo("User", userDocumentRef)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful() && !task2.getResult().getDocuments().isEmpty()) {
                                        showAlreadyPostedDialog();
                                    }
                                });
                    } else {
                        if (task.isSuccessful()) {
                            Log.w("HomeFragment", "No theme found for today");
                        } else {
                            Log.e("HomeFragment", "Error fetching theme for today", task.getException());
                        }
                    }
                });

        cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1);

        cameraCaptureHandler = setupCameraCaptureHandler();
        setupOnImageCaptured();
        setupOnSurfaceTextureListener();

        capturePictureButton.setOnClickListener(view1 -> {
            capturePicture();
        });

        return view;
    }

    private void showAlreadyPostedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.CustomAlertDialogTheme));
        builder.setTitle("Already Posted");
        builder.setMessage("You can only post once per challenge. Come back tomorrow!");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(getActivity(), NavigationActivity.class);
                startActivity(intent);
            }
        });

        builder.setOnDismissListener(dialogInterface -> {
            Intent intent = new Intent(getActivity(), NavigationActivity.class);
            startActivity(intent);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void capturePicture() {
        if (cameraDevice == null || cameraCaptureSession == null) {
            throw new RuntimeException("Setup camera before taking picture");
        }

        capturePictureButton.setVisibility(View.GONE);

        try {
            CaptureRequest.Builder stillCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            stillCaptureRequestBuilder.addTarget(imageReader.getSurface());
            cameraCaptureSession.capture(stillCaptureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupOnImageCaptured() {
        imageReader.setOnImageAvailableListener(p0 -> {
            System.out.println("On image available");

            Image image = p0.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();

            imageBytes = bytes;
            uploadFormLayout.setVisibility(View.VISIBLE);
            postImageImageView.setVisibility(View.VISIBLE);
            previewImageTextureView.setVisibility(View.GONE);
            todaysThemeTextView.setVisibility(View.GONE);
            themeTitleTextView.setVisibility(View.GONE);

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            postImageImageView.setImageBitmap(rotatedBitmap);
        }, null);
    }

    private void uploadPost() {
        String imageName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageReference = firebaseStorage.getReference().child(imageName);

        UploadTask uploadImageTask = imageReference.putBytes(imageBytes);
        uploadImageTask.addOnFailureListener(exception -> System.out.println("Failed to upload"));
        uploadImageTask.addOnSuccessListener(taskSnapshot -> {
            System.out.println("Successful upload to cloud");

            HashMap<String, Object> post = new HashMap<>();
            post.put("Title", postTitleEditText.getText().toString().trim());
            post.put("Description", postDescriptionEditText.getText().toString().trim());
            post.put("NumLikes", 0);
            post.put("NumDislikes", 0);
            post.put("Picture", imageName);
            post.put("Theme", themeDocumentRef);
            post.put("User", userDocumentRef);

            Task addPostTask = firebaseFirestore.collection("Posts").add(post);
            addPostTask.addOnCompleteListener(task -> {
                System.out.println("Finished saving post!");
                Intent intent = new Intent(getActivity(), NavigationActivity.class);
                startActivity(intent);
            });
        });
    }

    private Handler setupCameraCaptureHandler() {
        HandlerThread handlerThread = new HandlerThread("cameraThread");
        handlerThread.start();
        return new Handler(handlerThread.getLooper());
    }

    private void setupOnSurfaceTextureListener() {
        previewImageTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                try {
                    setupCamera();
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void setupCamera() throws CameraAccessException {
        cameraManager.openCamera(cameraManager.getCameraIdList()[0], new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                UploadFragment.this.cameraDevice = cameraDevice;

                try {
                    CaptureRequest.Builder previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    Surface previewSurface = new Surface(previewImageTextureView.getSurfaceTexture());
                    previewCaptureRequestBuilder.addTarget(previewSurface);

                    cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface(), previewSurface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            UploadFragment.this.cameraCaptureSession = cameraCaptureSession;
                            try {
                                cameraCaptureSession.setRepeatingRequest(previewCaptureRequestBuilder.build(), null, cameraCaptureHandler);
                            } catch (CameraAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, cameraCaptureHandler);
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice p0) {
            }

            @Override
            public void onError(@NonNull CameraDevice p0, int p1) {
            }
        }, cameraCaptureHandler);
    }
}