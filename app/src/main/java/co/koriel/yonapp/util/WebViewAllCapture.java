package co.koriel.yonapp.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.webkit.WebView;

import java.io.File;
import java.io.FileOutputStream;

public class WebViewAllCapture {
    public WebViewAllCapture() {
    }

    public boolean onWebViewAllCapture(WebView mWebView, String mFilePath, String mScreenShotName) {
        Picture mPicture = mWebView.capturePicture();
        Bitmap mBitmap = Bitmap.createBitmap(mPicture.getWidth(), mPicture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mBitmap);
        mPicture.draw(mCanvas);
        File mFolder = new File(mFilePath);
        if(!mFolder.exists()) {
            mFolder.mkdirs();
        }

        File mFile = new File(mFilePath + mScreenShotName);

        try {
            FileOutputStream e = new FileOutputStream(mFile);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, e);
            e.close();
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        mWebView.setDrawingCacheEnabled(false);
        return mFile.exists();
    }

    public boolean onWebViewAllCapture(WebView mWebView, String mFilePath, String mScreenShotName, Bitmap.CompressFormat mFormat) {
        Picture mPicture = mWebView.capturePicture();
        Bitmap mBitmap = Bitmap.createBitmap(mPicture.getWidth(), mPicture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mBitmap);
        mPicture.draw(mCanvas);
        File mFolder = new File(mFilePath);
        if(!mFolder.exists()) {
            mFolder.mkdirs();
        }

        File mFile = new File(mFilePath + mScreenShotName);
        if("".equals(mFormat) || "null".equals(mFormat)) {
            mFormat = Bitmap.CompressFormat.PNG;
        }

        try {
            FileOutputStream e = new FileOutputStream(mFile);
            mBitmap.compress(mFormat, 100, e);
            e.close();
        } catch (Exception var11) {
            var11.printStackTrace();
        }

        mWebView.setDrawingCacheEnabled(false);
        return mFile.exists();
    }
}
