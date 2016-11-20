package co.koriel.yonapp.fragment;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.NonLeakingWebView;
import co.koriel.yonapp.util.WebViewAllCapture;

public class TimeTableFragment extends FragmentBase implements View.OnClickListener {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sync:
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
        ImageButton menuCapture = (ImageButton) getActivity().findViewById(R.id.action_capture);
        menuCapture.setOnClickListener(this);

        getTimetable(false);

        return view;
    }

    private void getTimetable(final boolean isRefresh) {
        new Thread() {
            public void run() {
                try {
                    if (!NetworkUtil.isNetworkConnected(getActivity())) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                            }
                        });
                        interrupt();
                        return;
                    }

                    if (!isInterrupted()) {
                        final String js = "javascript:try { document.getElementById('username').value='" + DataBase.userInfo.getStudentId() + "';" +
                                "document.getElementById('password').value='" + Crypto.decryptPbkdf2(DataBase.userInfo.getStudentPasswd()) + "';" +
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
                                            .setTicker("시간표 가져오는 중...")
                                            .setContentTitle("연세대학교 연앱")
                                            .setContentText("시간표 가져오는 중...")
                                            .setContentIntent(contentIntent);

                                    notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                                    builder.setFullScreenIntent(contentIntent, true);

                                    if (Build.VERSION.SDK_INT >= 19) {
                                        timeTableWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                                    } else {
                                        timeTableWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                                    }
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

                                            if (url.equals("http://yscec.yonsei.ac.kr/login/index.php")) {
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
                                            }
                                        }
                                    });

                                    timeTableWebView.loadUrl("http://yscec.yonsei.ac.kr/local/timetable/index.php");
                                }
                            }
                        });
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    class JavaScriptInterface {
        @JavascriptInterface
        public void webViewHandler() {
            builder.setSmallIcon(R.drawable.ic_done_white_36dp)
                    .setContentText("시간표 가져오기 완료")
                    .setTicker("시간표 가져오기 완료")
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setProgress(0, 0, false);
            notificationManager.notify(0, builder.build());

            try {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timeTableWebView.setVisibility(View.VISIBLE);
                    }
                });
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timeTableWebView.saveWebArchive(getContext().getExternalCacheDir() + "/yonapp_timetable.mht");
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isFilePresent(String fileName) {
        String path = getContext().getExternalCacheDir() + fileName;
        File file = new File(path);
        return file.exists();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.action_capture) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(getContext(), "외부 저장소 쓰기 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                }

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

            } else {
                addImageToGallery(timeTableWebView, getContext());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    addImageToGallery(timeTableWebView, getContext());
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static void addImageToGallery(final WebView webView, final Context context) {
        WebViewAllCapture webViewAllCapture = new WebViewAllCapture();
        File path = context.getExternalCacheDir();
        File file = new File(path, "/yonapp_timetable.png");
        webViewAllCapture.onWebViewAllCapture(webView, context.getExternalCacheDir() + "/", "yonapp_timetable.png");
        Toast.makeText(context, "갤러리에 스크린샷이 저장되었습니다", Toast.LENGTH_SHORT).show();

        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}