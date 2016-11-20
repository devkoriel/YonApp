package co.koriel.yonapp.fragment;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.TextView;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.flyco.dialog.listener.OnOperItemClickL;
import com.flyco.dialog.widget.ActionSheetDialog;
import com.kakao.kakaolink.AppActionBuilder;
import com.kakao.kakaolink.KakaoLink;
import com.kakao.kakaolink.KakaoTalkLinkMessageBuilder;
import com.kakao.util.KakaoParameterException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NonLeakingWebView;
import dmax.dialog.SpotsDialog;

public class NoticeContentFragment extends FragmentBase {

    private String js;
    private final String jsLogin = "javascript: document.getElementById('username').value='" + DataBase.userInfo.getStudentId() + "';" +
            "document.getElementById('password').value='" + Crypto.decryptPbkdf2(DataBase.userInfo.getStudentPasswd()) + "';" +
            "(function(){document.getElementById('loginbtn').click();})()";

    private final String jsGetContent =  "javascript: window.HTMLOUT.showHTML(document.getElementById('page-content').innerHTML);";

    private Menu menu;

    private NonLeakingWebView noticeContentWebView;

    private TextView titleView;
    private TextView dateView;
    private TextView contentView;
    private TextView attachmentView;

    private String contentUrl;

    private Elements lectureElement;
    private Elements titleElement;
    private Elements dateElement;
    private Elements contentElement;
    private Elements attachmentElement;

    private String[] attachmentsLink;

    private AlertDialog pDialog;

    public NoticeContentFragment() {
        // Required empty public constructor
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
        this.menu = menu;
        this.menu.findItem(R.id.action_share).setVisible(true);
        this.menu.findItem(R.id.action_sync).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                final String[] stringItems = {"카카오톡", "페이스북"};
                final ActionSheetDialog dialog = new ActionSheetDialog(getContext(), stringItems, null);
                dialog.title("공유하고 싶은 서비스를 선택해주세요")//
                        .titleTextSize_SP(14.5f)//
                        .cancelText("취소")
                        .show();

                dialog.setOnOperItemClickL(new OnOperItemClickL() {
                    @Override
                    public void onOperItemClick(AdapterView<?> parent, View view, int position, long id) {
                        dialog.dismiss();

                        switch (position) {
                            case 0:
                                new Thread() {
                                    public void run() {
                                        try {
                                            final KakaoLink kakaoLink = KakaoLink.getKakaoLink(getActivity().getApplicationContext());
                                            final KakaoTalkLinkMessageBuilder kakaoTalkLinkMessageBuilder = kakaoLink.createKakaoTalkLinkMessageBuilder();
                                            String text = titleView.getText().toString() + "\n\n" + dateView.getText().toString() + "\n\n" + contentView.getText().toString() + "\n" + attachmentView.getText().toString();
                                            kakaoTalkLinkMessageBuilder.addText(text)
                                                    .addAppButton("앱으로 이동", new AppActionBuilder().setUrl(contentUrl).build());
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        kakaoLink.sendMessage(kakaoTalkLinkMessageBuilder, getContext());
                                                    } catch (KakaoParameterException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                        } catch (KakaoParameterException | NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                                break;
                            case 1:
                                new Thread() {
                                    public void run() {
                                        ShareLinkContent content = new ShareLinkContent.Builder()
                                                .setContentTitle(titleView.getText().toString())
                                                .setContentDescription(dateView.getText().toString())
                                                .setContentUrl(Uri.parse(contentUrl))
                                                .setImageUrl(Uri.parse("https://koriel.co/img/personal/yonapp/yonapp.png"))
                                                .build();
                                        ShareDialog.show(getActivity(), content);
                                    }
                                }.start();
                                break;
                            default:
                                break;
                        }
                    }
                });
                break;
            default:
                break;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        pDialog = new SpotsDialog(getContext(), R.style.CustomDialogLoad);
        pDialog.show();

        noticeContentWebView = new NonLeakingWebView(getActivity());
        noticeContentWebView.setVisibility(View.INVISIBLE);
        if (Build.VERSION.SDK_INT >= 19) {
            noticeContentWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            noticeContentWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        noticeContentWebView.getSettings().setJavaScriptEnabled(true);
        noticeContentWebView.getSettings().setLoadsImagesAutomatically(false);
        noticeContentWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        noticeContentWebView.addJavascriptInterface(new NoticeContentFragment.JavaScriptInterface(), "HTMLOUT");
        noticeContentWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.equals(contentUrl)) js = jsGetContent;
                else if (url.equals("http://yscec.yonsei.ac.kr/login/index.php")) js = jsLogin;
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
        });

        if(getArguments() != null) {
            contentUrl = getArguments().getString("link");
        }

        return inflater.inflate(R.layout.fragment_notice_content, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new Thread() {
            public void run() {
                try {
                titleView = (TextView) view.findViewById(R.id.notice_content_title);
                dateView = (TextView) view.findViewById(R.id.notice_content_date);
                contentView = (TextView) view.findViewById(R.id.notice_content);
                attachmentView = (TextView) view.findViewById(R.id.notice_content_attachment);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noticeContentWebView.loadUrl(contentUrl);
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void updateContent(final String html) {
        new Thread() {
            public void run() {
                Document document = Jsoup.parse(html.replace("</p>", "$$$"));

                lectureElement = document.getElementsByClass("page-title");
                titleElement = document.getElementsByClass("detail-title");
                dateElement = document.getElementsByClass("detail-date");
                contentElement = document.getElementsByClass("detail-contents");
                attachmentElement = document.select("ul.detail-attachment > li");
                attachmentsLink = new String[attachmentElement.size()];

                int i = 0;
                for(Element element : attachmentElement) {
                    attachmentsLink[i] = element.select("a").first().attr("href");
                    i++;
                }
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            titleView.setText("[" + lectureElement.text() + "]\n" + titleElement.text());
                            dateView.setText(dateElement.text());
                            contentView.setText(contentElement.text().replace("$$$", "\n\n"));

                            if (!attachmentElement.html().equals("")) {
                                String string = new String();
                                for(String str : attachmentsLink) {
                                    string += str + "\n";
                                }

                                attachmentView.setText(String.format(getContext().getString(R.string.fragment_notice_content_attachment), string));
                            }
                            pDialog.dismiss();

                            noticeContentWebView.destroy();
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /* An instance of this class will be registered as a JavaScript interface */
    class JavaScriptInterface
    {
        @JavascriptInterface
        public void showHTML(final String html)
        {
            updateContent(html);
        }
    }
}
