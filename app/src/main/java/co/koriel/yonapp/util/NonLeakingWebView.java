package co.koriel.yonapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * see http://stackoverflow.com/questions/3130654/memory-leak-in-webview and http://code.google.com/p/android/issues/detail?id=9375
 * Note that the bug does NOT appear to be fixed in android 2.2 as romain claims
 *
 * Also, you must call {@link #destroy()} from your activity's onDestroy method.
 */
public class NonLeakingWebView extends WebView {
    private static Field sConfigCallback;

    static {
        try {
            sConfigCallback = Class.forName("android.webkit.BrowserFrame").getDeclaredField("sConfigCallback");
            sConfigCallback.setAccessible(true);
        } catch (Exception e) {
            // ignored
        }

    }


    public NonLeakingWebView(Context context) {
        super(context.getApplicationContext());
        setWebViewClient( new MyWebViewClient((Activity)context) );
    }

    public NonLeakingWebView(Context context, AttributeSet attrs) {
        super(context.getApplicationContext(), attrs);
        setWebViewClient(new MyWebViewClient((Activity)context));
    }

    public NonLeakingWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context.getApplicationContext(), attrs, defStyle);
        setWebViewClient(new MyWebViewClient((Activity)context));
    }

    @Override
    public void destroy() {
        super.destroy();

        try {
            if( sConfigCallback!=null )
                sConfigCallback.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected static class MyWebViewClient extends WebViewClient {
        protected WeakReference<Activity> activityRef;

        public MyWebViewClient( Activity activity ) {
            this.activityRef = new WeakReference<Activity>(activity);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                final Activity activity = activityRef.get();
                if( activity!=null )
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }catch( RuntimeException ignored ) {
                // ignore any url parsing exceptions
            }
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    public void clearCookies()
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else
        {
            CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(this.getContext());
            cookieSyncMngr.startSync();
            CookieManager cookieManager=CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }
}