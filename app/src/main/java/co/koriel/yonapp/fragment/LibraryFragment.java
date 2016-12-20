package co.koriel.yonapp.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.koriel.yonapp.R;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.SeparatedListAdapter;

public class LibraryFragment extends FragmentBase {
    public final static String ITEM_TITLE = "title";
    public final static String ITEM_CAPTION = "caption";

    public final static String URL = "http://library.yonsei.ac.kr/seat/reserveStatus";

    private SwipeRefreshLayout swipeRefreshLayout;

    private ListView libraryListView;

    private SeparatedListAdapter adapter;

    private List<Map<String,?>> central;
    private List<Map<String,?>> samsung;

    public LibraryFragment() {
    }

    public Map<String,?> createItem(String title, String caption) {
        Map<String,String> item = new HashMap<>();
        item.put(ITEM_TITLE, title);
        item.put(ITEM_CAPTION, caption);
        return item;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        central = new LinkedList<>();
        samsung = new LinkedList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.app_name);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);
        swipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        adapter = new SeparatedListAdapter(getContext());
        adapter.addSection(getResources().getString(R.string.central_library), new SimpleAdapter(getContext(), central, R.layout.list_complex,
                new String[] { ITEM_TITLE, ITEM_CAPTION }, new int[] { R.id.list_complex_title, R.id.list_complex_caption }));
        adapter.addSection(getResources().getString(R.string.samsung_library), new SimpleAdapter(getContext(), samsung, R.layout.list_complex,
                new String[] { ITEM_TITLE, ITEM_CAPTION }, new int[] { R.id.list_complex_title, R.id.list_complex_caption }));

        libraryListView = (ListView) view.findViewById(R.id.library_list);
        libraryListView.setAdapter(adapter);
        libraryListView.setOnScrollListener(OnScrollChange);

        GetLibraryReserve getLibraryReserve = new GetLibraryReserve();
        getLibraryReserve.execute(URL);

        return view;
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            GetLibraryReserve getLibraryReserve = new GetLibraryReserve();
            getLibraryReserve.execute(URL);
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(libraryListView != null && libraryListView.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = libraryListView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = libraryListView.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    class GetLibraryReserve extends AsyncTask<String, Void, Void> {
        protected void onPreExecute() {
            if(!NetworkUtil.isNetworkConnected(getActivity())) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                cancel(true);
            }

        }

        @Override
        protected Void doInBackground(String... params) {
            updateList(NetworkUtil.httpRequest(params[0]));
            return null;
        }
    }

    private void updateList(final String html) {
        if (html != null) {
            new Thread() {
                public void run() {
                    try {
                        Document document = Jsoup.parse(html);
                        Elements libraryReserveElements = document.getElementsByClass("listTable");
                        Document document1 = Jsoup.parse(libraryReserveElements.get(0).html());
                        Document document2 = Jsoup.parse(libraryReserveElements.get(1).html());

                        Elements centralColumn = document1.getElementsByClass("author");
                        Elements centralColumn2 = document1.getElementsByClass("bookTitle");
                        Elements centralColumn3 = document1.getElementsByClass("title2");
                        Elements samsungColumn = document2.getElementsByClass("num");
                        Elements samsungColumn2 = document2.getElementsByClass("bookTitle");
                        Elements samsungColumn3 = document2.getElementsByClass("title2");
                        Elements samsungColumn4 = document2.getElementsByClass("author");

                        ArrayList<String> centralInfoArray = new ArrayList<>();
                        ArrayList<String> centralReserveArray = new ArrayList<>();
                        ArrayList<String> samsungInfoArray = new ArrayList<>();
                        ArrayList<String> samsungReserveArray = new ArrayList<>();

                        for (int i = 0; i < centralColumn2.size(); i++) {
                            centralInfoArray.add(centralColumn.get(3 * i).text() + "\t" +
                                    centralColumn2.get(i).text() + "\t" +
                                    centralColumn3.get(2 * i).text() +
                                    centralColumn3.get(2 * i + 1).text());
                            centralReserveArray.add(centralColumn.get(3 * i + 2).text());
                        }

                        for (int j = 0; j < samsungColumn.size(); j++) {
                            samsungInfoArray.add(samsungColumn.get(j).text() + "\t" +
                                    samsungColumn2.get(j).text() + "\t" +
                                    samsungColumn3.get(2 * j).text() +
                                    samsungColumn3.get(2 * j + 1).text());
                            samsungReserveArray.add(samsungColumn4.get(2 * j + 1).text());
                        }

                        final List<Map<String,?>> centralScanList = new LinkedList<>();
                        for (int i = 0; i < centralInfoArray.size(); i++) {
                            centralScanList.add(createItem(centralInfoArray.get(i), centralReserveArray.get(i)));
                        }

                        final List<Map<String,?>> samsungScanList = new LinkedList<>();
                        for (int j = 0; j < samsungInfoArray.size(); j++) {
                            samsungScanList.add(createItem(samsungInfoArray.get(j), samsungReserveArray.get(j)));
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                central.clear();
                                central.addAll(centralScanList);
                                samsung.clear();
                                samsung.addAll(samsungScanList);
                                adapter.notifyDataSetChanged();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    } catch (NullPointerException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } else {
            Toast.makeText(getActivity(), R.string.sync_library_seat_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
