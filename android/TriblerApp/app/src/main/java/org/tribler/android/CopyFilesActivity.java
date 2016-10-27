package org.tribler.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.tribler.android.service.TriblerdService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.BindView;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

/**
 * TODO: SET exported="false" before public release!
 */
public class CopyFilesActivity extends BaseActivity {

    @BindView(R.id.copy_progress)
    View progressView;

    @BindView(R.id.copy_progress_status)
    TextView statusBar;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy_files);

        TriblerdService.stop(this);

        // Start coping files
        handleIntent(getIntent());
    }

    protected void showLoading(@Nullable CharSequence text) {
        if (text == null) {
            progressView.setVisibility(View.GONE);
        } else {
            statusBar.setText(text);
            progressView.setVisibility(View.VISIBLE);
        }
    }

    protected void showLoading(boolean show) {
        showLoading(show ? "" : null);
    }

    protected void showLoading(@StringRes int resId) {
        showLoading(getText(resId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleIntent(Intent intent) {
        // Get parameters
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        rxSubs.add(Observable.from(extras.keySet())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<String>() {

                    public void onNext(String key) {
                        try {
                            String value = String.valueOf(extras.get(key));

                            File from;
                            File to;

                            if (key.startsWith("/")) {
                                from = new File(key);
                            } else {
                                from = new File(getFilesDir(), key);
                            }

                            if (value == null) {
                                to = new File(getFilesDir(), key);
                            } else if (value.startsWith(".")) {
                                to = new File(getFilesDir(), value);
                            } else if (value.startsWith("/")) {
                                to = new File(value);
                            } else {
                                to = new File(getExternalCacheDir(), value);
                            }

                            copy(from.getCanonicalFile(), to.getCanonicalFile());

                        } catch (Exception ex) {
                            onError(ex);
                        }
                    }

                    public void onCompleted() {
                        CopyFilesActivity.this.finish();
                    }

                    public void onError(Throwable e) {
                        Log.e("CopyFile", "onError", e);
                    }
                }));
    }

    private void copy(File in, File out) throws IOException {
        runOnUiThread(() -> showLoading(in.getPath() + "\n\n" + out.getPath()));

        InputStream input = new FileInputStream(in);
        OutputStream output = new FileOutputStream(out);

        Log.i("CopyFileStart", in.getPath());

        MyUtils.copy(input, output);

        Log.i("CopyFileDone", out.getPath());
    }

}
