package co.koriel.yonapp.fragment;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.fragment.adapter.NoticeAdapter;
import co.koriel.yonapp.fragment.adapter.NoticeItem;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.NonLeakingWebView;
import co.koriel.yonapp.util.SerializeObject;
import co.koriel.yonapp.util.YscecHelper;

public class NoticeFragment extends FragmentBase {
    private SwipeRefreshLayout swipeRefreshLayout;
    private SwipeRefreshLayout swipeRefreshLayoutEmpty;

    private NonLeakingWebView webView;
    private ListView noticeListView;
    private NoticeAdapter noticeAdapter;
    private ArrayList<NoticeItem> noticeScanList;

    public NoticeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notice, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.app_name);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);
        swipeRefreshLayoutEmpty = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_empty);
        swipeRefreshLayoutEmpty.setOnRefreshListener(OnRefreshLayout);

        noticeScanList = new ArrayList<>();
        noticeAdapter = new NoticeAdapter();
        noticeListView = (ListView) view.findViewById(R.id.notice_list);
        noticeListView.setEmptyView(swipeRefreshLayoutEmpty);
        noticeListView.setOnItemClickListener(OnClickListItem);
        noticeListView.setAdapter(noticeAdapter);
        noticeListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        noticeListView.setOnScrollListener(OnScrollChange);

        String sNoticeScanList = SerializeObject.ReadSettings(getContext(), "notice_scan_list.dat");
        if (!sNoticeScanList.equalsIgnoreCase("")) {
            Object objNsl = SerializeObject.stringToObject(sNoticeScanList);
            if (objNsl instanceof ArrayList) {
                noticeScanList.clear();
                noticeScanList.addAll((ArrayList<NoticeItem>) objNsl);
            }

            noticeAdapter.clearItems();
            noticeAdapter.addAllItems(noticeScanList);
            noticeAdapter.notifyDataSetChanged();
        } else {
            swipeRefreshLayoutEmpty.setRefreshing(true);
            getNotice();
        }

        return view;
    }

    private AdapterView.OnItemClickListener OnClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            new Thread() {
                public void run() {
                    final Bundle bundle = new Bundle();
                    bundle.putString("link", noticeScanList.get(position).getLink());

                    NoticeContentFragment noticeContentFragment = new NoticeContentFragment();
                    noticeContentFragment.setArguments(bundle);
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.notice_root_container, noticeContentFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }.start();
        }
    };

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getNotice();
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(noticeListView != null && noticeListView.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = noticeListView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = noticeListView.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    private void getNotice() {
        try {
            if (!NetworkUtil.isNetworkConnected(getActivity())) {
                swipeRefreshLayout.setRefreshing(false);
                swipeRefreshLayoutEmpty.setRefreshing(false);
                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String js = "javascript:try { document.getElementById('username').value='" + DataBase.userInfo.getStudentId() + "';" +
                            "document.getElementById('password').value='" + Crypto.decryptPbkdf2(DataBase.userInfo.getStudentPasswd()) + "';" +
                            "(function(){document.getElementById('loginbtn').click();})()} catch (exception) {}" +
                            "finally {window.HTMLOUT.showHTML(document.getElementsByClassName('board-list-area')[0].innerHTML);}";

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView = new NonLeakingWebView(getActivity());
                            if (Build.VERSION.SDK_INT >= 21) {
                                webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                            } else if (Build.VERSION.SDK_INT >= 19) {
                                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                            }
                            else {
                                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                            }
                            webView.setVisibility(View.INVISIBLE);
                            webView.getSettings().setJavaScriptEnabled(true);
                            webView.getSettings().setLoadsImagesAutomatically(false);
                            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                            webView.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");
                            webView.setWebViewClient(new WebViewClient() {

                                @Override
                                public void onPageFinished(WebView view, String url) {
                                    super.onPageFinished(view, url);

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
                            webView.loadUrl(YscecHelper.BaseUrl + YscecHelper.BoardUrl);
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayoutEmpty.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
    }



    private void updateList(final String html) {
        new Thread() {
            public void run() {
                try {
                    Document document = Jsoup.parse(html);
                    Elements noticeElements = document.getElementsByClass("post-title");
                    Elements linkElements = document.select("a:has(.board-lecture-title)");
                    Elements dateElements = document.getElementsByClass("post-date");

                    noticeScanList.clear();
                    for(int i = 0; i < noticeElements.size(); i++) {
                        String notice = noticeElements.get(i).text();
                        String date = dateElements.get(i).text();

                        NoticeItem noticeItem = new NoticeItem();
                        noticeItem.setLink(linkElements.get(i).attr("href"));
                        DateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일-hh:mm");

                        Date noticeDate;
                        try {
                            noticeDate = dateFormat.parse(date.split(",")[0] + "-" + date.split(",")[2].split(" ")[1]);
                            noticeItem.setTimestamp(noticeDate.getTime()/1000);
                        } catch (ParseException e) {
                            e.printStackTrace();

                            if (i == 0) {
                                noticeItem.setTimestamp(0);
                            } else {
                                noticeItem.setTimestamp(noticeScanList.get(i - 1).getTimestamp());
                            }
                        }

                        StringBuilder stringBuilder = new StringBuilder(notice);
                        stringBuilder.replace(notice.lastIndexOf("]"), notice.lastIndexOf("]") + 1, "]\n");
                        noticeItem.setTitle(stringBuilder.toString());
                        noticeItem.setDate(date);

                        noticeScanList.add(noticeItem);
                    }

                    Collections.sort(noticeScanList, new Comparator<NoticeItem>() {
                        @Override
                        public int compare(NoticeItem lhs, NoticeItem rhs) {
                            return lhs.getTimestamp() > rhs.getTimestamp() ? -1 : lhs.getTimestamp() < rhs.getTimestamp() ? 1:0;
                        }
                    });

                    String sNoticeScanList = SerializeObject.objectToString(noticeScanList);
                    if (sNoticeScanList != null && !sNoticeScanList.equalsIgnoreCase("")) {
                        SerializeObject.WriteSettings(getContext(), sNoticeScanList, "notice_scan_list.dat");
                    } else {
                        SerializeObject.WriteSettings(getContext(), "", "notice_scan_list.dat");
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                            swipeRefreshLayoutEmpty.setRefreshing(false);
                            noticeAdapter.clearItems();
                            noticeAdapter.addAllItems(noticeScanList);
                            noticeAdapter.notifyDataSetChanged();
                            webView.destroy();
                        }
                    });
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    class JavaScriptInterface
    {
        @JavascriptInterface
        public void showHTML(final String html)
        {
            updateList(html);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().deleteFile("notice_scan_list.dat");
    }
}

