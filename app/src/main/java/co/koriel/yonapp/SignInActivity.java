package co.koriel.yonapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.flyco.dialog.listener.OnBtnClickL;
import com.flyco.dialog.widget.NormalDialog;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.mobile.user.IdentityManager;
import co.amazonaws.mobile.user.IdentityProvider;
import co.amazonaws.mobile.user.signin.FacebookSignInProvider;
import co.amazonaws.mobile.user.signin.GoogleSignInProvider;
import co.amazonaws.mobile.user.signin.SignInManager;
import co.amazonaws.models.nosql.BlackListDO;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.NonLeakingWebView;
import co.koriel.yonapp.util.YscecHelper;
import dmax.dialog.SpotsDialog;

public class SignInActivity extends Activity implements View.OnClickListener{
    private final static String LOG_TAG = SignInActivity.class.getSimpleName();
    private SignInManager signInManager;

    /** Permission Request Code (Must be < 256). */
    private static final int GET_ACCOUNTS_PERMISSION_REQUEST_CODE = 93;

    /** The Google OnClick listener, since we must override it to get permissions on Marshmallow and above. */
    private View.OnClickListener googleOnClickListener;

    private NonLeakingWebView signinWebView;

    private AlertDialog pDialog;

    public EditText studentIdEditText;
    public EditText studentPasswdEditText;

    private DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    private PaginatedScanList<BlackListDO> blackList;
    private DynamoDBMapper dynamoDBMapper;

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

            if(checkBlackList(DataBase.userInfo.getStudentId())) {
                AWSMobileClient.defaultMobileClient().getIdentityManager().signOut();
                Toast.makeText(SignInActivity.this, "차단된 사용자입니다", Toast.LENGTH_LONG).show();
            } else {

                Log.d(LOG_TAG, String.format("User sign-in with %s succeeded",
                        provider.getDisplayName()));

                // The sign-in manager is no longer needed once signed in.
                SignInManager.dispose();

                Toast.makeText(SignInActivity.this, String.format("%s로 로그인에 성공했습니다.",
                        provider.getDisplayName()), Toast.LENGTH_LONG).show();

                // Load user name and image.
                AWSMobileClient.defaultMobileClient()
                        .getIdentityManager().loadUserInfoAndImage(provider, new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "Launching Main Activity...");
                        startActivity(new Intent(SignInActivity.this, MainActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        // finish should always be called on the main thread.
                        finish();
                    }
                });

                new Thread() {
                    public void run() {
                        Log.d(LOG_TAG, "Saving UserId to Database...");
                        DataBase.userInfo.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
                        dynamoDBMapper.save(DataBase.userInfo);
                    }
                }.start();
            }
        }

        /**
         * Recieves the sign-in result indicating the user canceled and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        @Override
        public void onCancel(final IdentityProvider provider) {
            Log.d(LOG_TAG, String.format("User sign-in with %s canceled.",
                provider.getDisplayName()));

            Toast.makeText(SignInActivity.this, String.format("%s로 로그인이 취소되었습니다.",
                provider.getDisplayName()), Toast.LENGTH_LONG).show();
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
            errorDialogBuilder.setTitle("Sign-In Error");
            errorDialogBuilder.setMessage(
                String.format("%s로 로그인에 실패했습니다.\n%s", provider.getDisplayName(), ex.getMessage()));
            errorDialogBuilder.setNeutralButton("Ok", null);
            errorDialogBuilder.show();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();

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
        if (Build.VERSION.SDK_INT >= 19) {
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
            Toast.makeText(SignInActivity.this, "인터넷에 연결해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (studentIdEditText.getText().toString().matches("") && studentPasswdEditText.getText().toString().matches("")) {
            Toast.makeText(SignInActivity.this, "학번과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        } else if (studentIdEditText.getText().toString().matches("")) {
            Toast.makeText(SignInActivity.this, "학번을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        } else if (studentPasswdEditText.getText().toString().matches("")) {
            Toast.makeText(SignInActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        pDialog = new SpotsDialog(SignInActivity.this, R.style.CustomDialogAuth);
        pDialog.show();

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

                if(url.equals(YscecHelper.BaseUrl + YscecHelper.LoginUrl)) {
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
                } else if (url.equals(YscecHelper.BaseUrl + YscecHelper.BoardUrl)) {
                    new Thread() {
                        public void run() {
                            YscecHelper.isAuthenticated = true;
                            String encryptedPasswd = Crypto.encrypt(studentPasswdEditText.getText().toString());

                            DataBase.userInfo.setStudentId(studentIdEditText.getText().toString());
                            DataBase.userInfo.setStudentPasswd(encryptedPasswd);

                            // Login with Google Account Recommended
                            final Activity thisActivity = SignInActivity.this;
                            if (ContextCompat.checkSelfPermission(thisActivity,
                                    Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(SignInActivity.this,
                                        new String[]{Manifest.permission.GET_ACCOUNTS},
                                        GET_ACCOUNTS_PERMISSION_REQUEST_CODE);
                                return;
                            }

                            try {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pDialog.dismiss();

                                        final NormalDialog dialog = new NormalDialog(SignInActivity.this);
                                        dialog.style(NormalDialog.STYLE_TWO)
                                                .bgColor(Color.parseColor("#383838"))//
                                                .cornerRadius(5)//
                                                .btnText("취소", "확인")
                                                .title("포탈 인증 성공!")
                                                .content("아래 확인 버튼을 눌러 계속 진행하세요.")//
                                                .contentGravity(Gravity.CENTER)//
                                                .contentTextColor(Color.parseColor("#ffffff"))//
                                                .dividerColor(Color.parseColor("#222222"))//
                                                .btnTextSize(15.5f, 15.5f)//
                                                .btnTextColor(Color.parseColor("#ffffff"), Color.parseColor("#ffffff"))//
                                                .btnPressColor(Color.parseColor("#2B2B2B"))//
                                                .widthScale(0.85f)//
                                                .show();

                                        dialog.setOnBtnClickL(
                                                new OnBtnClickL() {
                                                    @Override
                                                    public void onBtnClick() {
                                                        dialog.dismiss();
                                                    }
                                                },
                                                new OnBtnClickL() {
                                                    @Override
                                                    public void onBtnClick() {
                                                        dialog.dismiss();

                                                        googleOnClickListener.onClick(view);
                                                    }
                                                });
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

        signinWebView.loadUrl(YscecHelper.BaseUrl + YscecHelper.BoardUrl);

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

                    final NormalDialog dialog = new NormalDialog(SignInActivity.this);
                    dialog.style(NormalDialog.STYLE_TWO)
                            .bgColor(Color.parseColor("#383838"))//
                            .cornerRadius(5)//
                            .btnNum(1)
                            .btnText("확인")
                            .title("포탈 인증 실패 ㅠㅠ")
                            .content("학번과 비밀번호를 올바르게 적었는 지 확인해보세요.")//
                            .contentGravity(Gravity.CENTER)//
                            .contentTextColor(Color.parseColor("#ffffff"))//
                            .dividerColor(Color.parseColor("#222222"))//
                            .btnTextSize(15.5f)//
                            .btnTextColor(Color.parseColor("#ffffff"))//
                            .btnPressColor(Color.parseColor("#2B2B2B"))//
                            .widthScale(0.85f)//
                            .show();

                    dialog.setOnBtnClickL(
                            new OnBtnClickL() {
                                @Override
                                public void onBtnClick() {
                                    dialog.dismiss();
                                }
                            });
                }
            });

            YscecHelper.isAuthenticated = false;
        }
    }

    public boolean checkBlackList(String studentId) {

        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();

        Thread checkBlackListThread = new Thread() {
            public void run() {
                blackList = dynamoDBMapper.scan(BlackListDO.class, scanExpression);
            }
        };

        checkBlackListThread.start();
        try {
            checkBlackListThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < blackList.size(); i++) {
            if(studentId.equals(blackList.get(i).getStudentId())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        signinWebView.destroy();
    }
}
