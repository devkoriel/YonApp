package co.koriel.yonapp.fragment.viewer;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import co.koriel.yonapp.R;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PictureViewerActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private ImageView imageView;
    private PhotoViewAttacher photoViewAttacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_viewer);

        imageView = (ImageView) findViewById(R.id.imageView);
        final ImageButton imageButton = (ImageButton) findViewById(R.id.button_save);

        final String url = (String) getIntent().getExtras().get("url");
        Glide.with(PictureViewerActivity.this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        photoViewAttacher = new PhotoViewAttacher(imageView);

                        imageButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(PictureViewerActivity.this,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {

                                    // Should we show an explanation?
                                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                                        // Show an expanation to the user *asynchronously* -- don't block
                                        // this thread waiting for the user's response! After the user
                                        // sees the explanation, try again to request the permission.
                                        Toast.makeText(PictureViewerActivity.this, R.string.need_external_storage_write_permission, Toast.LENGTH_SHORT).show();
                                    }

                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                                } else {
                                    savePictureToFile(((BitmapDrawable) imageView.getDrawable()).getBitmap());
                                }
                            }
                        });

                        return false;
                    }
                })
                .into(imageView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    savePictureToFile(((BitmapDrawable) imageView.getDrawable()).getBitmap());
                } else {
                    new MaterialDialog.Builder(PictureViewerActivity.this)
                            .iconRes(R.drawable.ic_error_outline_black_48dp)
                            .limitIconToDefaultSize()
                            .title(R.string.screenshot_permission_failure_title_text)
                            .content(R.string.screenshot_permission_failure_text)
                            .positiveText(R.string.dialog_ok)
                            .show();
                }
                break;

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private boolean savePictureToFile(Bitmap bitmap) {
        final String filePath = getExternalCacheDir() + "/";

        TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
        dateFormatter.setTimeZone(tz);
        Date today = new Date();
        final String fileName = "yonapp_oneline_" + dateFormatter.format(today);

        File mFolder = new File(filePath);
        if(!mFolder.exists()) {
            mFolder.mkdirs();
        }

        File mFile = new File(filePath + fileName);

        try {
            FileOutputStream e = new FileOutputStream(mFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, e);
            e.close();

            Toast.makeText(PictureViewerActivity.this, R.string.screenshot_saved_into_gallery, Toast.LENGTH_SHORT).show();

            ContentValues values = new ContentValues();

            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.MediaColumns.DATA, mFile.getAbsolutePath());

            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return mFile.exists();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        photoViewAttacher.cleanup();
    }
}
