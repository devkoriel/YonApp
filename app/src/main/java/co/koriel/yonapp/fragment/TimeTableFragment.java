package co.koriel.yonapp.fragment;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.models.nosql.UserInfoDO;
import co.koriel.yonapp.R;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.NonLeakingWebView;
import co.koriel.yonapp.util.WebViewAllCapture;

public class TimeTableFragment extends FragmentBase {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private SharedPreferences sharedPreferences;

    private NonLeakingWebView timeTableWebView;
    private RelativeLayout relativeLayout;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;

    public TimeTableFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_capture:
                // Here, thisActivity is the current activity
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        // Show an expanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        Toast.makeText(getContext(), R.string.need_external_storage_write_permission, Toast.LENGTH_SHORT).show();
                    }

                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                } else {
                    addImageToGallery(timeTableWebView, getContext());
                }
                break;
            case R.id.action_sync:
                sharedPreferences.edit().putBoolean("enable_sync_yscec_timetable", true).apply();
                getTimetable(true);
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_time_table, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.app_name);

        relativeLayout = (RelativeLayout) view.findViewById(R.id.timetable_layout);

        if (sharedPreferences.getBoolean("enable_sync_yscec_timetable", true)) getTimetable(false);

        return view;
    }

    private void getTimetable(final boolean isRefresh) {
        try {
            if (!NetworkUtil.isNetworkConnected(getActivity())) {
                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        new Thread() {
            public void run() {
                try {
                    UserInfoDO userInfo = new UserInfoDO();
                    userInfo.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

                    userInfo = AWSMobileClient.defaultMobileClient().getDynamoDBMapper().load(UserInfoDO.class, AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

                    final String js = "javascript:try { document.getElementById('username').value='" + userInfo.getStudentId() + "';" +
                            "document.getElementById('password').value='" + Crypto.decryptPbkdf2(userInfo.getStudentPasswd()) + "';" +
                            "(function(){document.getElementById('loginbtn').click();})() } catch (exception) {}";

                    final String js2 = "javascript:try { document.getElementById('mobile-header').remove();" +
                            "$('#page-content').css('top', '0px'); " +
                            "document.getElementById('page-content-title').remove();" +
                            "document.getElementsByClassName('fc-toolbar')[0].remove();" +
                            "$('#content-region').css('padding', '0px 0px 0px');" +
                            "$('#content-region').css('width', '100%');" +
                            "document.getElementsByClassName('fc-day-header fc-widget-header fc-mon')[0].innerHTML = '월';" +
                            "document.getElementsByClassName('fc-day-header fc-widget-header fc-tue')[0].innerHTML = '화';" +
                            "document.getElementsByClassName('fc-day-header fc-widget-header fc-wed')[0].innerHTML = '수';" +
                            "document.getElementsByClassName('fc-day-header fc-widget-header fc-thu')[0].innerHTML = '목';" +
                            "document.getElementsByClassName('fc-day-header fc-widget-header fc-fri')[0].innerHTML = '금';" +
                            "$('div[role=\"main\"]').css('width', '100%');" +
                            "$('div[role=\"main\"]').css('margin', '0px');" + "" +
                            "window.invoke.webViewHandler()} catch (exception) {}";

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeTableWebView = new NonLeakingWebView(getActivity());
                            if (Build.VERSION.SDK_INT >= 21) {
                                timeTableWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                                timeTableWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                            } else if (Build.VERSION.SDK_INT >= 19) {
                                timeTableWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                            }
                            else {
                                timeTableWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                            }
                            timeTableWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            timeTableWebView.setVisibility(View.INVISIBLE);
                            timeTableWebView.getSettings().setJavaScriptEnabled(true);
                            timeTableWebView.getSettings().setDomStorageEnabled(true);
                            timeTableWebView.setDrawingCacheEnabled(true);
                            timeTableWebView.addJavascriptInterface(new JavaScriptInterface(), "invoke");
                            relativeLayout.addView(timeTableWebView);

                            if (isFilePresent("/yonapp_timetable.mht") && !isRefresh) {
                                timeTableWebView.loadUrl("file://" + getActivity().getExternalCacheDir() + "/yonapp_timetable.mht");
                                timeTableWebView.setVisibility(View.VISIBLE);
                            } else {
                                Intent push = new Intent();
                                push.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                final PendingIntent contentIntent = PendingIntent.getActivity(getContext(), 0, push, PendingIntent.FLAG_CANCEL_CURRENT);

                                builder = new NotificationCompat.Builder(getContext())
                                        .setPriority(NotificationCompat.PRIORITY_MAX)
                                        .setSmallIcon(R.drawable.ic_refresh_white_36dp)
                                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                                        .setOngoing(true)
                                        .setAutoCancel(false)
                                        .setTicker(getResources().getString(R.string.syncing_timetable))
                                        .setContentTitle(getResources().getString(R.string.app_name))
                                        .setContentText(getResources().getString(R.string.syncing_timetable))
                                        .setContentIntent(contentIntent);

                                notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                                builder.setFullScreenIntent(contentIntent, true);

                                timeTableWebView.getSettings().setLoadsImagesAutomatically(false);
                                timeTableWebView.setWebChromeClient(new WebChromeClient() {
                                    @Override
                                    public void onProgressChanged(WebView view, int progress) {
                                        builder.setProgress(100, progress, false);
                                        notificationManager.notify(0, builder.build());
                                    }
                                });
                                timeTableWebView.setWebViewClient(new WebViewClient() {
                                    @Override
                                    public void onPageFinished(WebView view, String url) {
                                        super.onPageFinished(view, url);

                                        if (url.equals("https://yscec.yonsei.ac.kr/login/index.php")) {
                                            if (Build.VERSION.SDK_INT >= 19) {
                                                timeTableWebView.evaluateJavascript(js, new ValueCallback<String>() {
                                                    @Override
                                                    public void onReceiveValue(String s) {
                                                    }
                                                });
                                            } else {
                                                timeTableWebView.loadUrl(js);
                                            }
                                        } else if (url.equals("http://yscec.yonsei.ac.kr/local/timetable/index.php")) {
                                            if (Build.VERSION.SDK_INT >= 19) {
                                                timeTableWebView.evaluateJavascript(js2, new ValueCallback<String>() {
                                                    @Override
                                                    public void onReceiveValue(String s) {
                                                    }
                                                });
                                            } else {
                                                timeTableWebView.loadUrl(js2);
                                            }
                                        } else {
                                            sharedPreferences.edit().putBoolean("enable_sync_yscec_timetable", false).apply();

                                            builder.setSmallIcon(R.drawable.ic_error_outline_white_36dp)
                                                    .setContentText(getResources().getString(R.string.sync_timetable_fail))
                                                    .setTicker(getResources().getString(R.string.sync_timetable_fail))
                                                    .setOngoing(false)
                                                    .setAutoCancel(true)
                                                    .setProgress(0, 0, false);
                                            notificationManager.notify(0, builder.build());
                                        }
                                    }
                                });

                                timeTableWebView.loadUrl("http://yscec.yonsei.ac.kr/local/timetable/index.php");
                            }
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    class JavaScriptInterface {
        @JavascriptInterface
        public void webViewHandler() {
            sharedPreferences.edit().putBoolean("enable_sync_yscec_timetable", true).apply();

            builder.setSmallIcon(R.drawable.ic_done_white_36dp)
                    .setContentText(getResources().getString(R.string.sync_timetable_success))
                    .setTicker(getResources().getString(R.string.sync_timetable_success))
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setProgress(0, 0, false);
            notificationManager.notify(0, builder.build());

            try {
                timeTableWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        timeTableWebView.setVisibility(View.VISIBLE);
                        timeTableWebView.saveWebArchive(getContext().getExternalCacheDir() + "/yonapp_timetable.mht");
                    }
                });
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isFilePresent(String fileName) {
        String path = getContext().getExternalCacheDir() + fileName;
        File file = new File(path);
        return file.exists();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    addImageToGallery(timeTableWebView, getContext());
                } else {
                    new MaterialDialog.Builder(getContext())
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

    public static void addImageToGallery(final WebView webView, final Context context) {
        WebViewAllCapture webViewAllCapture = new WebViewAllCapture();
        File path = context.getExternalCacheDir();
        File file = new File(path, "/yonapp_timetable.png");
        webViewAllCapture.onWebViewAllCapture(webView, context.getExternalCacheDir() + "/", "yonapp_timetable.png");

        Toast.makeText(context, R.string.screenshot_saved_into_gallery, Toast.LENGTH_SHORT).show();

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (timeTableWebView != null) {
            timeTableWebView.destroy();
        }
    }
}