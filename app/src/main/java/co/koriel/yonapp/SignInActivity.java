package co.koriel.yonapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.mobile.push.GCMTokenHelper;
import co.amazonaws.mobile.user.IdentityManager;
import co.amazonaws.mobile.user.IdentityProvider;
import co.amazonaws.mobile.user.signin.FacebookSignInProvider;
import co.amazonaws.mobile.user.signin.GoogleSignInProvider;
import co.amazonaws.mobile.user.signin.SignInManager;
import co.amazonaws.models.nosql.BlackListDO;
import co.amazonaws.models.nosql.UserInfoDO;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.NonLeakingWebView;
import co.koriel.yonapp.util.YscecHelper;

public class SignInActivity extends Activity implements View.OnClickListener{
    private final static String LOG_TAG = SignInActivity.class.getSimpleName();
    private SignInManager signInManager;

    /** Permission Request Code (Must be < 256). */
    private static final int GET_ACCOUNTS_PERMISSION_REQUEST_CODE = 93;

    /** The Google OnClick listener, since we must override it to get permissions on Marshmallow and above. */
    private View.OnClickListener googleOnClickListener;

    private NonLeakingWebView signinWebView;

    private MaterialDialog pDialog;

    public EditText studentIdEditText;
    public EditText studentPasswdEditText;

    private DynamoDBMapper dynamoDBMapper;
    private UserInfoDO userInfo;

    private int sLength;

    /**
     * SignInResultsHandler handles the final result from sign in. Making it static is a best
     * practice since it may outlive the SplashActivity's life span.
     */
    private class SignInResultsHandler implements IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result and starts the main activity.
         * @param provider the identity provider used for sign-in.
         */
        @Override
        public void onSuccess(final IdentityProvider provider) {

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose();

            new Thread() {
                public void run() {
                    BlackListDO blackList = dynamoDBMapper.load(BlackListDO.class, userInfo.getStudentId());

                    if (blackList != null) {
                        AWSMobileClient.defaultMobileClient().getIdentityManager().signOut();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignInActivity.this, R.string.blocked_user, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        userInfo.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
                        userInfo.setGcmToken("activated");
                        userInfo.setGcmTokenKey(getSharedPreferences(GCMTokenHelper.class.getName(), MODE_PRIVATE).getString("deviceToken", ""));
                        dynamoDBMapper.save(userInfo);

                        // Load user name and image.
                        AWSMobileClient.defaultMobileClient()
                                .getIdentityManager().loadUserInfoAndImage(provider, new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(SignInActivity.this, MainActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                                // finish should always be called on the main thread.
                                finish();
                            }
                        });
                    }
                }
            }.start();
        }

        /**
         * Recieves the sign-in result indicating the user canceled and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        @Override
        public void onCancel(final IdentityProvider provider) {
            Log.d(LOG_TAG, String.format("User sign-in with %s canceled.",
                provider.getDisplayName()));

            Toast.makeText(SignInActivity.this, R.string.login_cancel, Toast.LENGTH_LONG).show();
        }

        /**
         * Receives the sign-in result that an error occurred signing in and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         * @param ex the exception that occurred.
         */
        @Override
        public void onError(final IdentityProvider provider, final Exception ex) {
            Log.e(LOG_TAG, String.format("User Sign-in failed for %s : %s",
                provider.getDisplayName(), ex.getMessage()), ex);

            final AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(SignInActivity.this);
            errorDialogBuilder.setTitle(R.string.login_error);
            errorDialogBuilder.setMessage(
                String.format(getResources().getString(R.string.login_fail) + "\n%s", ex.getMessage()));
            errorDialogBuilder.setNeutralButton(R.string.dialog_ok, null);
            errorDialogBuilder.show();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        userInfo = new UserInfoDO();

        Button yscecAuthButton = (Button) findViewById(R.id.yscec_auth_button);
        studentIdEditText = (EditText) findViewById(R.id.student_id_edittext);
        studentPasswdEditText = (EditText) findViewById(R.id.student_passwd_edittext);
        studentPasswdEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    yscecAuthenticate();
                    return true;
                }
                return false;
            }
        });

        TextView privacyText = (TextView) findViewById(R.id.privacy_text_view);

        yscecAuthButton.setOnClickListener(this);
        studentIdEditText.setOnClickListener(this);
        studentPasswdEditText.setOnClickListener(this);
        privacyText.setOnClickListener(this);

        YscecHelper.isAuthenticated = false;

        signInManager = SignInManager.getInstance(this);

        signInManager.setResultsHandler(this, new SignInResultsHandler());

        // Initialize sign-in buttons.
        signInManager.initializeSignInButton(FacebookSignInProvider.class,
            this.findViewById(R.id.fb_login_button));

        googleOnClickListener =
            signInManager.initializeSignInButton(GoogleSignInProvider.class, findViewById(R.id.g_login_button));

        if (googleOnClickListener != null) {
            // if the onClick listener was null, initializeSignInButton will have removed the view.
            this.findViewById(R.id.g_login_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Activity thisActivity = SignInActivity.this;
                    if (ContextCompat.checkSelfPermission(thisActivity,
                        Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(SignInActivity.this,
                            new String[]{Manifest.permission.GET_ACCOUNTS},
                            GET_ACCOUNTS_PERMISSION_REQUEST_CODE);
                        return;
                    }

                    // call the Google onClick listener.
                    googleOnClickListener.onClick(view);
                }
            });
        }

        signinWebView = new NonLeakingWebView(SignInActivity.this);

        if (Build.VERSION.SDK_INT >= 21) {
            signinWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            signinWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= 19) {
            signinWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            signinWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        signinWebView.getSettings().setJavaScriptEnabled(true);
        signinWebView.getSettings().setLoadsImagesAutomatically(false);
        signinWebView.getSettings().setDomStorageEnabled(true);
        signinWebView.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String permissions[], final int[] grantResults) {
        if (requestCode == GET_ACCOUNTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.findViewById(R.id.g_login_button).callOnClick();
            } else {
                Log.i(LOG_TAG, "Permissions not granted for Google sign-in. :(");
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        signInManager.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Crypto.androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // pause/resume Mobile Analytics collection
        AWSMobileClient.defaultMobileClient().handleOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // pause/resume Mobile Analytics collection
        AWSMobileClient.defaultMobileClient().handleOnPause();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.yscec_auth_button:
                yscecAuthenticate();
                break;

            case R.id.student_id_edittext:
                studentIdEditText.setText("");
                break;

            case R.id.student_passwd_edittext:
                studentPasswdEditText.setText("");
                break;

            case R.id.privacy_text_view:
                String url ="https://koriel.co/blog/%EC%97%B0%EC%95%B1-%EA%B0%9C%EC%9D%B8%EC%A0%95%EB%B3%B4-%EC%B7%A8%EA%B8%89%EB%B0%A9%EC%B9%A8";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);

            default:
                break;
        }
    }

    public boolean yscecAuthenticate() {
        if (!NetworkUtil.isNetworkConnected(this)) {
            Toast.makeText(SignInActivity.this, R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
            return false;
        } else if (studentIdEditText.getText().toString().matches("") && studentPasswdEditText.getText().toString().matches("")) {
            Toast.makeText(SignInActivity.this, R.string.please_enter_student_id_passwd, Toast.LENGTH_SHORT).show();
            return false;
        } else if (studentIdEditText.getText().toString().matches("")) {
            Toast.makeText(SignInActivity.this, R.string.please_enter_student_id, Toast.LENGTH_SHORT).show();
            return false;
        } else if (studentPasswdEditText.getText().toString().matches("")) {
            Toast.makeText(SignInActivity.this, R.string.please_enter_student_passwd, Toast.LENGTH_SHORT).show();
            return false;
        }

        pDialog = new MaterialDialog.Builder(this)
                .title(R.string.signin_auth)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();

        sLength = 0;

        final String js = "javascript:try{window.HTMLOUT.showErr(document.getElementsByClassName('loginerrors')[0].innerHTML);} catch (exception) {" +
                "document.getElementById('username').value='" + studentIdEditText.getText() + "';" +
                "document.getElementById('password').value='" + studentPasswdEditText.getText() + "';" +
                "(function(){document.getElementById('loginbtn').click();})() }";

        signinWebView.clearCache(true);
        signinWebView.clearHistory();
        signinWebView.clearCookies();
        signinWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(final WebView view, String url) {
                super.onPageFinished(view, url);

                if(url.equals(YscecHelper.BaseUrlSecure + YscecHelper.LoginUrl)) {
                    if (sLength == 0) {
                        if (Build.VERSION.SDK_INT >= 19) {
                            view.evaluateJavascript(js, new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {

                                }
                            });
                        } else {
                            view.loadUrl(js);
                        }
                    }
                } else if (url.equals(YscecHelper.BaseUrl + YscecHelper.AuthUrl)) {
                    new Thread() {
                        public void run() {
                            YscecHelper.isAuthenticated = true;
                            String encryptedPasswd = Crypto.encrypt(studentPasswdEditText.getText().toString());

                            userInfo.setStudentId(studentIdEditText.getText().toString());
                            userInfo.setStudentPasswd(encryptedPasswd);

                            try {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();

                                        final MaterialDialog dialog = new MaterialDialog.Builder(SignInActivity.this)
                                                .iconRes(R.drawable.ic_done_black_48dp)
                                                .limitIconToDefaultSize()
                                                .title(R.string.auth_success)
                                                .content(R.string.auth_continue)
                                                .positiveText(R.string.dialog_ok)
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        dialog.dismiss();

                                                        final Activity thisActivity = SignInActivity.this;
                                                        if (ContextCompat.checkSelfPermission(thisActivity,
                                                                Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                                                            ActivityCompat.requestPermissions(SignInActivity.this,
                                                                    new String[]{Manifest.permission.GET_ACCOUNTS},
                                                                    GET_ACCOUNTS_PERMISSION_REQUEST_CODE);
                                                        } else {
                                                            googleOnClickListener.onClick(view);
                                                        }
                                                    }
                                                })
                                                .show();
                                    }
                                });
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        });

        signinWebView.loadUrl(YscecHelper.BaseUrl + YscecHelper.AuthUrl);

        return YscecHelper.isAuthenticated;
    }

    /* An instance of this class will be registered as a JavaScript interface */
    class JavaScriptInterface {
        @JavascriptInterface
        public void showErr(String html) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pDialog.dismiss();

                    final MaterialDialog dialog = new MaterialDialog.Builder(SignInActivity.this)
                            .iconRes(R.drawable.ic_error_outline_black_48dp)
                            .limitIconToDefaultSize()
                            .title(R.string.auth_fail)
                            .content(R.string.auth_check)
                            .positiveText(R.string.dialog_ok)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            });

            YscecHelper.isAuthenticated = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        signinWebView.destroy();
    }
}
