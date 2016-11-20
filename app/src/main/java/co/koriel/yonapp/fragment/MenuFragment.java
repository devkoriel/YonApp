package co.koriel.yonapp.fragment;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;

import co.koriel.yonapp.R;
import co.koriel.yonapp.util.NonLeakingWebView;
import dmax.dialog.SpotsDialog;

public class MenuFragment extends FragmentBase {

    private WebView webView;
    private AlertDialog pDialog;

    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_menu, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("주간식단");

        pDialog = new SpotsDialog(getContext(), R.style.CustomDialogLoad);
        pDialog.show();

        try {
            new Thread() {
                public void run() {

                    final RelativeLayout menuLayout = (RelativeLayout) view.findViewById(R.id.menu_layout);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView = new NonLeakingWebView(getActivity());
                            webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            if (Build.VERSION.SDK_INT >= 19) {
                                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                            }
                            else {
                                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                            }
                            menuLayout.addView(webView);
                            webView.setVisibility(View.INVISIBLE);
                            webView.getSettings().setJavaScriptEnabled(true);
                            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                            webView.setWebViewClient(new WebViewClient() {

                                @Override
                                public void onPageFinished(WebView view, String url) {
                                    super.onPageFinished(view, url);
                                    pDialog.dismiss();
                                    webView.setVisibility(View.VISIBLE);
                                }
                            });
                            webView.loadUrl("http://165.132.13.38/_custom/yonsei/m/menu.jsp");
                        }
                    });
                }
            }.start();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return view;
    }
}
