package comp5216.sydney.edu.au.mediaaccess;

import java.io.Serializable; // Import the Serializable interface

/**
 * VideoLocation class represents a location with a video URI.
 */
public class VideoLocation implements Serializable {

    private double latitude;
    private double longitude;
    private String videoUriPath;

    public VideoLocation(double latitude, double longitude, String videoUriPath) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.videoUriPath = videoUriPath;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude =  longitude;
    }

    public String getVideoUriPath() {
        return videoUriPath;
    }

    public void setVideoUriPath(String videoUriPath) {
        this.videoUriPath = videoUriPath;
    }
}