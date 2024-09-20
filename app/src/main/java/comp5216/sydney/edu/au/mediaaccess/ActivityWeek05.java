package comp5216.sydney.edu.au.mediaaccess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityWeek05 extends Activity {

    //request codes
    private static final int MY_PERMISSIONS_REQUEST_OPEN_CAMERA = 101;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHOTOS = 102;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_VIDEO = 103;
    private static final int MY_PERMISSIONS_REQUEST_READ_VIDEOS = 104;
    public final String APP_TAG = "MobileComputingTutorial";
    public String photoFileName = "photo.jpg";
    public String videoFileName = "video.mp4";
    MarshmallowPermission marshmallowPermission = new MarshmallowPermission(this);
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;
    private File file;

    public static void addLatLonToMetadata(String imagePath, double latitude, double longitude) throws IOException {
        ExifInterface exif = new ExifInterface(imagePath);

        // Set latitude and longitude
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertDecimalToDMS(latitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude >= 0 ? "N" : "S");
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertDecimalToDMS(longitude));
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude >= 0 ? "E" : "W");

        // Save the changes
        exif.saveAttributes();
    }

    private static String convertDecimalToDMS(double decimal) {
        int degrees = (int) decimal;
        int minutes = (int) ((decimal - degrees) * 60);
        double seconds = ((decimal - degrees) * 60 - minutes) * 60;

        String degreesString = String.format("%02d", degrees);
        String minutesString = String.format("%02d", minutes);
        String secondsString = String.format("%.4f", seconds);

        return degreesString + "/1," + minutesString + "/1," + secondsString + "/1";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week05);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        storage = FirebaseStorage.getInstance();

        // Create a storage reference from our app
        storageRef = storage.getReference();
        geocoder = new Geocoder(this.getApplicationContext(), Locale.getDefault());
    }

    public void onLoadPhotoClick(View view) {

        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Bring up gallery to select a photo
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_PHOTOS);

    }

    public void onLoadVideoClick(View view) {

        // Create intent for picking a video from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        // Bring up gallery to select a video
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_VIDEOS);

    }

    public void onTakePhotoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()) {
            marshmallowPermission.requestPermissionForCamera();
        } else if (!marshmallowPermission.checkPermissionForFineLocation()) {
            marshmallowPermission.requestPermissionForFineLocation();
        } else if (!marshmallowPermission.checkPermissionForCoarseLocation()) {
            marshmallowPermission.requestPermissionForCoarseLocation();
        } else {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            photoFileName = "IMG_" + timeStamp + ".jpg";

            // Create a photo file reference
            Uri file_uri = getFileUri(photoFileName, 0);

            // Add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

            // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
            // So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, MY_PERMISSIONS_REQUEST_OPEN_CAMERA);
            }
        }
    }

    public void onRecordVideoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to capture a video and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            videoFileName = "VIDEO_" + timeStamp + ".mp4";

            // Create a video file reference
            Uri file_uri = getFileUri(videoFileName, 1);

            // add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

            // Start the video record intent to capture video
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_RECORD_VIDEO);
        }
    }

    private void scanFile(String path) {

        MediaScannerConnection.scanFile(ActivityWeek05.this,
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    // Returns the Uri for a photo/media stored on disk given the fileName and type
    public Uri getFileUri(String fileName, int type) {
        Uri fileUri = null;
        try {
            String typestr = "images"; //default to images type
            if (type == 1) {
                typestr = "videos";
            } else if (type != 0) {
                typestr = "audios";
            }

            File mediaStorageDir = new File(getExternalMediaDirs()[0], APP_TAG);

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d(APP_TAG, "failed to create directory");
            }

            // Create the file target for the media based on filename
            file = new File(mediaStorageDir, fileName);

            // Wrap File object into a content provider, required for API >= 24
            // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
            if (Build.VERSION.SDK_INT >= 24) {
                fileUri = FileProvider.getUriForFile(this.getApplicationContext(), "comp5216.sydney.edu.au.mediaaccess.fileprovider", file);
            } else {
                fileUri = Uri.fromFile(mediaStorageDir);
            }
        } catch (Exception ex) {
            Log.e("getFileUri", ex.getStackTrace().toString());
        }
        return fileUri;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        final VideoView mVideoView = findViewById(R.id.videoview);
        ImageView ivPreview = findViewById(R.id.photopreview);

        mVideoView.setVisibility(View.GONE);
        ivPreview.setVisibility(View.GONE);

        // If the camera has been opened for a photo
        if (requestCode == MY_PERMISSIONS_REQUEST_OPEN_CAMERA) {
            if (resultCode == RESULT_OK) {
                Bitmap takenImage = BitmapFactory.decodeFile(file.getAbsolutePath());
                scanFile(file.getAbsolutePath());

                // Check permission for location
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                // Attempts to get the last location if still valid
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {

                            @Override
                            public void onSuccess(Location location) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }

                                fusedLocationClient.getLocationAvailability().addOnSuccessListener(new
                                        OnSuccessListener < LocationAvailability > () {
                                    @Override
                                    public void onSuccess(LocationAvailability locationAvailability)
                                    {
                                        // Checks if the location is still valid
                                        if (locationAvailability.isLocationAvailable())
                                        {
                                            // Logic to handle location object
                                            double latitude = location.getLatitude();
                                            double longitude = location.getLongitude();

                                            try {
                                                // Adds the location to the image metadata
                                                addLatLonToMetadata(file.getAbsolutePath(), latitude, longitude);
                                                Log.d("Location Added", String.valueOf(latitude));
                                                Toast.makeText(ActivityWeek05.this, "Location Added", Toast.LENGTH_SHORT).show();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                            String city = "Unknown";
                                            try {
                                                // Gets the city using geocoder
                                                city = getCityFromLatLong(latitude, longitude);
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            // Uploads the image to firebase
                                            uploadImage(Uri.fromFile(file), city);
                                        }
                                        else {
                                            // Uses location request to get the location
                                            Toast.makeText(ActivityWeek05.this, "Location not available", Toast.LENGTH_SHORT).show();
                                            LocationRequest locationRequest = LocationRequest.create()
                                                    .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                                                    .setInterval(10000) // Update interval in milliseconds
                                                    .setFastestInterval(5000); // Fastest update interval

                                            LocationCallback locationCallback = new LocationCallback() {
                                                @Override
                                                public void onLocationResult(LocationResult locationResult) {
                                                    if (locationResult == null) {
                                                        Toast.makeText(ActivityWeek05.this, "Location not available", Toast.LENGTH_SHORT).show();
                                                        return;
                                                    }
                                                    if (locationResult.getLocations().get(0) != null) {
                                                        Location location = locationResult.getLocations().get(0);
                                                        double latitude = location.getLatitude();
                                                        double longitude = location.getLongitude();
                                                        try {
                                                            // Adds the location to the image metadata
                                                            addLatLonToMetadata(file.getAbsolutePath(), latitude, longitude);
                                                            Log.d("Location Added", String.valueOf(latitude));
                                                            Toast.makeText(ActivityWeek05.this, "Location Added", Toast.LENGTH_SHORT).show();
                                                        } catch (IOException e) {
                                                            throw new RuntimeException(e);
                                                        }

                                                        String city = "Unknown";
                                                        try {
                                                            // Gets the city using geocoder
                                                            city = getCityFromLatLong(latitude, longitude);
                                                        } catch (IOException e) {
                                                            throw new RuntimeException(e);
                                                        }

                                                        // Uploads the image to firebase
                                                        uploadImage(Uri.fromFile(file), city);

                                                    }
                                                }
                                            };

                                            if (ActivityCompat.checkSelfPermission(ActivityWeek05.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                                    ActivityCompat.checkSelfPermission(ActivityWeek05.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                // TODO: Consider calling
                                                //    ActivityCompat#requestPermissions// here to request the missing permissions, and then overriding
                                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                //                                          int[] grantResults)
                                                // to handle the case where the user grants the permission. See the documentation
                                                // for ActivityCompat#requestPermissions for more details.
                                                return;
                                            }
                                            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                                        }
                                    }
                                });
                            }
                        });


                ivPreview.setImageBitmap(takenImage);
                ivPreview.setVisibility(View.VISIBLE);

            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHOTOS) {
            if (resultCode == RESULT_OK) {
                Uri photoUri = data.getData();
                // Do something with the photo based on Uri
                Bitmap selectedImage;
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);

                    // Load the selected image into a preview
                    ivPreview.setImageBitmap(selectedImage);
                    ivPreview.setVisibility(View.VISIBLE);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_VIDEOS) {
            if (resultCode == RESULT_OK) {
                Uri videoUri = data.getData();
                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.setVideoURI(videoUri);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new OnPreparedListener() {
                    // Close the progress bar and play the video
                    public void onPrepared(MediaPlayer mp) {
                        mVideoView.start();
                    }
                });
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_VIDEO) {
                Uri takenVideoUri = getFileUri(videoFileName, 1);
                scanFile(file.getAbsolutePath());

                // Checks if the app has permission to access the location
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                // Attempts to get the last location if still valid
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                fusedLocationClient.getLocationAvailability().addOnSuccessListener(new
                                   OnSuccessListener < LocationAvailability > () {
                                       @Override
                                       public void onSuccess(LocationAvailability locationAvailability)
                                       {
                                           // Checks if the location is still valid
                                           if (locationAvailability.isLocationAvailable())
                                           {
                                               // Logic to handle location object
                                               double latitude = location.getLatitude();
                                               double longitude = location.getLongitude();
                                               Log.d("Location Added", String.valueOf(latitude));
                                               Toast.makeText(ActivityWeek05.this, "Location Added", Toast.LENGTH_SHORT).show();

                                               // Save the location to the list and stores it in internal storage
                                               List<VideoLocation> videoLocations = loadVideoLocationsFromInternalStorage(ActivityWeek05.this);
                                               videoLocations.add(new VideoLocation(latitude, longitude, takenVideoUri.getPath()));
                                               saveVideoLocationsToInternalStorage(ActivityWeek05.this, videoLocations);

                                               String city = "Unknown";
                                               try {
                                                   // Gets the city using geocoder
                                                   city = getCityFromLatLong(latitude, longitude);
                                               } catch (IOException e) {
                                                   throw new RuntimeException(e);
                                               }
                                               // Uploads the image to firebase
                                               uploadImage(takenVideoUri, city);
                                           }
                                           else {
                                               // Logic to handle when no location is available
                                               // Uses location request to get the location
                                               Toast.makeText(ActivityWeek05.this, "Location not available", Toast.LENGTH_SHORT).show();
                                               LocationRequest locationRequest = LocationRequest.create()
                                                       .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                                                       .setInterval(10000) // Update interval in milliseconds
                                                       .setFastestInterval(5000); // Fastest update interval

                                               LocationCallback locationCallback = new LocationCallback() {
                                                   @Override
                                                   public void onLocationResult(LocationResult locationResult) {
                                                       if (locationResult == null) {
                                                           Toast.makeText(ActivityWeek05.this, "Location not available", Toast.LENGTH_SHORT).show();
                                                           return;
                                                       }
                                                       if (locationResult.getLocations().get(0) != null) {
                                                           Location location = locationResult.getLocations().get(0);
                                                           double latitude = location.getLatitude();
                                                           double longitude = location.getLongitude();

                                                           // Save the location to the list and stores it in internal storage
                                                           List<VideoLocation> videoLocations = loadVideoLocationsFromInternalStorage(ActivityWeek05.this);
                                                           videoLocations.add(new VideoLocation(latitude, longitude, takenVideoUri.getPath()));
                                                           saveVideoLocationsToInternalStorage(ActivityWeek05.this, videoLocations);

                                                           String city = "Unknown";
                                                           try {
                                                               // Gets the city using geocoder
                                                               city = getCityFromLatLong(latitude, longitude);
                                                           } catch (IOException e) {
                                                               throw new RuntimeException(e);
                                                           }
                                                           // Uploads the image to firebase
                                                           uploadImage(Uri.fromFile(file), city);

                                                       }
                                                   }
                                               };

                                               if (ActivityCompat.checkSelfPermission(ActivityWeek05.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                                       && ActivityCompat.checkSelfPermission(ActivityWeek05.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                                   // TODO: Consider calling
                                                   //    ActivityCompat#requestPermissions// here to request the missing permissions, and then overriding
                                                   //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                   //                                          int[] grantResults)
                                                   // to handle the case where the user grants the permission. See the documentation
                                                   // for ActivityCompat#requestPermissions for more details.
                                                   return;
                                               }
                                               fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                                           }
                                       }
                                   });
                            }
                        });

                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.setVideoURI(takenVideoUri);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new OnPreparedListener() {
                    // Close the progress bar and play the video
                    public void onPrepared(MediaPlayer mp) {
                        mVideoView.start();
                    }
                });
        }
    }

    /**
     * Uploads the image to firebase
     * @param file
     * @param location
     */
    private void uploadImage(Uri file, String location) {
        StorageReference riversRef = storageRef.child("images/" + location + "/" + file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.d("Upload Failed", "Upload Failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                Log.d("Upload Success", "Upload Success");
            }
        });

    }

    /**
     * Gets the city from the latitude and longitude
     * @param latitude
     * @param longitude
     * @return
     * @throws IOException
     */
    public String getCityFromLatLong(double latitude, double longitude) throws IOException {
        Log.d("Location sent", latitude + ", " + longitude);
        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
        Log.d("Location", String.valueOf(addressList));
        if (addressList != null && !addressList.isEmpty()) {
            Address address = addressList.get(0);
            String city = address.getLocality();
            if (city == null) {
                city = address.getAdminArea();
            }
            if (city == null) {
                city = address.getSubAdminArea();
            }
            if (city == null) {
                city = address.getSubLocality();
            }
            return city;
        }
        return "Unknown";

    }

    /**
     * Saves the video locations to internal storage
     * @param context
     * @param videoLocations
     */
    public void saveVideoLocationsToInternalStorage(Context context, List<VideoLocation> videoLocations) {
        try {
            FileOutputStream fos = context.openFileOutput("video_locations.dat", Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(videoLocations);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (VideoLocation videoLocation : loadVideoLocationsFromInternalStorage(ActivityWeek05.this)) {
            Log.d("VideoLocation", videoLocation.getVideoUriPath());
        }
    }

    /**
     * Loads the video locations from internal storage
     * @param context
     * @return
     */
    public List<VideoLocation> loadVideoLocationsFromInternalStorage(Context context) {
        try {
            FileInputStream fis = context.openFileInput("video_locations.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<VideoLocation> videoLocations = (List<VideoLocation>) ois.readObject();
            ois.close();
            return videoLocations;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
