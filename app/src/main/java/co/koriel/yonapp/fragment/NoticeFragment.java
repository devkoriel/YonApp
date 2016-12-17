package co.koriel.yonapp.fragment;

import android.graphics.Color;
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
import android.widget.SimpleAdapter;
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
import java.util.HashMap;

import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.util.Crypto;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.NonLeakingWebView;
import co.koriel.yonapp.util.SerializeObject;
import co.koriel.yonapp.util.YscecHelper;

public class NoticeFragment extends FragmentBase {
    private static final String NOTICE = "notice";
    private static final String DATE = "date";
    private static final String LINK = "link";
    private static final String TIMESTAMP = "timestamp";

    private SwipeRefreshLayout swipeRefreshLayout;

    private NonLeakingWebView webView;
    private ListView noticeListView;
    private SimpleAdapter simpleAdapter;
    private ArrayList<HashMap<String, String>> noticeScanList;
    private ArrayList<HashMap<String, String>> noticeArrangedList;
    private ArrayList<HashMap<String, String>> noticeList;

    private Elements noticeElements;
    private Elements linkElements;
    private Elements dateElements;

    private Date noticeDate;

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

        noticeScanList = new ArrayList<>();
        noticeArrangedList = new ArrayList<> ();
        noticeList = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(getContext(), noticeList, android.R.layout.simple_list_item_2, new String[]{"item1", "item2"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setTextSize(15);
                text2.setTextSize(12);
                text2.setTextColor(Color.GRAY);
                return view;
            }
        };
        noticeListView = (ListView) view.findViewById(R.id.notice_list);
        noticeListView.setOnItemClickListener(OnClickListItem);
        noticeListView.setAdapter(simpleAdapter);
        noticeListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        noticeListView.setOnScrollListener(OnScrollChange);

        String sNoticeScanList = SerializeObject.ReadSettings(getContext(), "nsl.dat");
        String sNoticeArrangedList = SerializeObject.ReadSettings(getContext(), "nal.dat");
        if (!sNoticeScanList.equalsIgnoreCase("") && !sNoticeArrangedList.equalsIgnoreCase("")) {
            Object objNsl = SerializeObject.stringToObject(sNoticeScanList);
            if (objNsl instanceof ArrayList) {
                noticeScanList.clear();
                noticeScanList.addAll((ArrayList<HashMap<String, String>>) objNsl);
            }

            Object objNal = SerializeObject.stringToObject(sNoticeArrangedList);
            if (objNal instanceof ArrayList) {
                noticeArrangedList.clear();
                noticeArrangedList.addAll((ArrayList<HashMap<String, String>>) objNal);
            }

            noticeList.clear();
            noticeList.addAll(noticeArrangedList);
            simpleAdapter.notifyDataSetChanged();
        } else {
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
                    bundle.putString(LINK, noticeScanList.get(position).get(LINK));

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
        swipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        new Thread() {
            public void run() {
                try {
                    if (!NetworkUtil.isNetworkConnected(getActivity())) {
                        swipeRefreshLayout.setRefreshing(false);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                            }
                        });
                        interrupt();
                        return;
                    }

                    if (!isInterrupted()) {
                        final String js = "javascript:try { document.getElementById('username').value='" + DataBase.userInfo.getStudentId() + "';" +
                                "document.getElementById('password').value='" + Crypto.decryptPbkdf2(DataBase.userInfo.getStudentPasswd()) + "';" +
                                "(function(){document.getElementById('loginbtn').click();})()} catch (exception) {}" +
                                "finally {window.HTMLOUT.showHTML(document.getElementsByClassName('board-list-area')[0].innerHTML);}";

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView = new NonLeakingWebView(getActivity());
                                if (Build.VERSION.SDK_INT >= 19) {
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
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void updateList(final String html) {
        new Thread() {
            public void run() {
                Document document = Jsoup.parse(html);
                noticeElements = document.getElementsByClass("post-title");
                linkElements = document.select("a:has(.board-lecture-title)");
                dateElements = document.getElementsByClass("post-date");

                noticeScanList.clear();
                for(int i = 0; i < noticeElements.size(); i++) {
                    HashMap<String, String> map = new HashMap<> ();
                    map.put(NOTICE, noticeElements.get(i).text());
                    map.put(LINK, linkElements.get(i).attr("href"));
                    map.put(DATE, dateElements.get(i).text());
                    DateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일-hh:mm");
                    try {
                        noticeDate = dateFormat.parse(dateElements.get(i).text().split(",")[0] + "-" + dateElements.get(i).text().split(",")[2].split(" ")[1]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    map.put(TIMESTAMP, String.valueOf(noticeDate.getTime()/1000));
                    noticeScanList.add(map);
                }

                Collections.sort(noticeScanList, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {
                        return Long.parseLong(lhs.get(TIMESTAMP)) > Long.parseLong(rhs.get(TIMESTAMP)) ? -1 : Long.parseLong(lhs.get(TIMESTAMP)) < Long.parseLong(rhs.get(TIMESTAMP)) ? 1:0;
                    }
                });

                noticeArrangedList.clear();
                for(int j = 0; j < noticeScanList.size(); j++) {
                    HashMap<String, String> map = new HashMap<> ();
                    StringBuilder stringBuilder = new StringBuilder(noticeScanList.get(j).get(NOTICE));
                    stringBuilder.replace(noticeScanList.get(j).get(NOTICE).lastIndexOf("]"), noticeScanList.get(j).get(NOTICE).lastIndexOf("]") + 1, "]\n");
                    map.put("item1", stringBuilder.toString());
                    map.put("item2", noticeScanList.get(j).get(DATE));
                    noticeArrangedList.add(map);
                }

                String sNoticeScanList = SerializeObject.objectToString(noticeScanList);
                if (sNoticeScanList != null && !sNoticeScanList.equalsIgnoreCase("")) {
                    SerializeObject.WriteSettings(getContext(), sNoticeScanList, "nsl.dat");
                } else {
                    SerializeObject.WriteSettings(getContext(), "", "nsl.dat");
                }

                String sNoticeArrangedList = SerializeObject.objectToString(noticeArrangedList);
                if (sNoticeArrangedList != null && !sNoticeArrangedList.equalsIgnoreCase("")) {
                    SerializeObject.WriteSettings(getContext(), sNoticeArrangedList, "nal.dat");
                } else {
                    SerializeObject.WriteSettings(getContext(), "", "nal.dat");
                }

                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noticeList.clear();
                            noticeList.addAll(noticeArrangedList);
                            swipeRefreshLayout.setRefreshing(false);
                            simpleAdapter.notifyDataSetChanged();
                            webView.destroy();
                        }
                    });
                } catch (NullPointerException e) {
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
        getContext().deleteFile("nsl.dat");
        getContext().deleteFile("nal.dat");
    }
}

