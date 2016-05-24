package org.tribler.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureVideoListener implements View.OnClickListener {

    private Activity mActivity;
    private Uri mVideoCaptureUri;

    public CaptureVideoListener(Activity activity) {
        mActivity = activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        File file = getOutputVideoFile();
        if (file == null) {
            Log.e("Tribler", "failed to obtain output file");
            //TODO: advise user
        }
        mVideoCaptureUri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mVideoCaptureUri);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // 0: low 1: high
        mActivity.startActivityForResult(intent, MainActivity.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Log.d("Tribler", "Video saved to:\n" + data.getData());
            //TODO: advise user
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("Tribler", "User cancelled the video capture");
        } else {
            Log.e("Tribler", "failed to capture video");
            //TODO: advise user
        }
    }

    /**
     * @return The file created for saving a video
     */
    private File getOutputVideoFile() {
        File videoDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            videoDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES).getAbsolutePath());
        } else {
            videoDir = new File(mActivity.getFilesDir(), Environment.DIRECTORY_MOVIES);
        }
        // Create the storage directory if it does not exist
        if (!videoDir.exists() && !videoDir.mkdirs()) {
            Log.e(getClass().getName(), "Failed to create directory");
            return null;
        } else if (!videoDir.isDirectory()) {
            Log.e(getClass().getName(), "Not a directory");
            return null;
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(new Date());
        return new File(videoDir, "VID_" + timeStamp + ".mp4");
    }
}