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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import co.koriel.yonapp.R;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.SeparatedListAdapter;

public class SchoolCalendarFragment extends FragmentBase {
    private View view;

    public final static String ITEM_TITLE = "title";
    public final static String ITEM_CAPTION = "caption";

    public final static String URL = "http://www.yonsei.ac.kr/sc/support/calendar.jsp";

    private SwipeRefreshLayout swipeRefreshLayout;
    private GetSchoolCalendar getSchoolCalendar;

    private ListView calendarListView;

    private SeparatedListAdapter adapter;

    private LinkedList<List<Map<String,?>>> calendarList;

    private static boolean isFirst;

    public SchoolCalendarFragment() {
        // Required empty public constructor
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
        calendarList = new LinkedList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_school_calendar, container, false);
        isFirst = true;

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.home_menu_school_schedule);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);
        swipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        getSchoolCalendar = new GetSchoolCalendar();
        getSchoolCalendar.execute(URL);

        return view;
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            getSchoolCalendar = new GetSchoolCalendar();
            getSchoolCalendar.execute(URL);
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(calendarListView != null && calendarListView.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = calendarListView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = calendarListView.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    class GetSchoolCalendar extends AsyncTask<String, Void, Void> {
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

                        final Elements thElements = document.getElementsByTag("th");
                        Elements tdElements = document.getElementsByTag("td");

                        int rowSpanSum = 0;
                        calendarList.clear();
                        for (int i = 0; i < thElements.size(); i++) {
                            calendarList.add(new LinkedList<Map<String,?>>());
                            calendarList.get(i).clear();
                            for (int j = 0; j < Integer.parseInt(thElements.get(i).attr("rowspan")); j++) {
                                calendarList.get(i).add(createItem(tdElements.get(2 * rowSpanSum + 2 * j + 1).text(),
                                        tdElements.get(2 * rowSpanSum + 2 * j).text()));

                            }
                            rowSpanSum += Integer.parseInt(thElements.get(i).attr("rowspan"));
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFirst) {
                                    adapter = new SeparatedListAdapter(getContext());

                                    for (int i = 0; i < thElements.size(); i++) {
                                        adapter.addSection(thElements.get(i).text(), new SimpleAdapter(getContext(), calendarList.get(i), R.layout.list_complex_calendar,
                                                new String[] { ITEM_TITLE, ITEM_CAPTION }, new int[] { R.id.list_complex_title_calendar, R.id.list_complex_caption_calendar }));
                                    }

                                    calendarListView = (ListView) view.findViewById(R.id.school_calendar_list);
                                    calendarListView.setAdapter(adapter);
                                    calendarListView.setOnScrollListener(OnScrollChange);

                                    isFirst = false;
                                }

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
            Toast.makeText(getActivity(), R.string.sync_school_calendar_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
