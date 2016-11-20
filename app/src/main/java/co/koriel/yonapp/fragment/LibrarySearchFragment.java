package co.koriel.yonapp.fragment;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;

import co.koriel.yonapp.R;
import co.koriel.yonapp.util.NetworkUtil;

public class LibrarySearchFragment extends Fragment implements SearchView.OnQueryTextListener {
    private String SEARCH_STRING;

    private static final String BOOK_SEARCH_URL = "http://library.yonsei.ac.kr/search/tot/result?folder_id=null&q=";
    private static final String BOOK_SEARCH_OPTION_1 = "&bk_0=jttjmjttj&st=KWRD&si=";
    private static final String BOOK_SEARCH_OPTION_2 = "&oi=&os=&cpp=1000";

    private static final String PAPER_SEARCH_URL = "http://library.yonsei.ac.kr/search/tot/result?st=KWRD&commandType=advanced&si=";
    private static final String PAPER_SEARCH_OPTION_1 = "&q=";
    private static final String PAPER_SEARCH_OPTION_2 = "&b0=and&weight0=&si=TOTAL&q=&b1=and&weight1=&si=TOTAL&q=&weight2=&_lmt0=on&lmtsn=000000000001&lmtst=OR&_lmt0=on&_lmt0=on&_lmt0=on&lmt0=t&_lmt0=on&_lmt0=on&_lmt0=on&inc=TOTAL&_inc=on&_inc=on&_inc=on&_inc=on&_inc=on&_inc=on&lmt1=TOTAL&lmtsn=000000000003&lmtst=OR&lmt2=TOTAL&lmtsn=000000000006&lmtst=OR&rf=&rt=&range=000000000021&cpp=1000&msc=10000";

    private TextView bookSearchNumText;

    private ArrayList<HashMap<String, String>> arrayList;
    private SimpleAdapter simpleAdapter;

    private Elements itemElements;
    private Elements infoElements;
    private Elements locationElements;

    private String infoString;
    private String searchOption = "TOTAL";

    public LibrarySearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_search, container, false);

        if (getArguments() != null) {
            SEARCH_STRING = getArguments().getString("option");
        } else {
            SEARCH_STRING = "도서 검색";
        }

        Spinner spinner = (Spinner) view.findViewById(R.id.book_search_spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.main_background));

                if (parent.getItemAtPosition(position).equals("전체")) searchOption = "TOTAL";
                else if (parent.getItemAtPosition(position).equals("서명")) searchOption = "1";
                else if (parent.getItemAtPosition(position).equals("저자")) searchOption = "2";
                else if (parent.getItemAtPosition(position).equals("출판사")) searchOption = "3";
                else if (parent.getItemAtPosition(position).equals("주제어")) searchOption = "4";
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        SearchView searchView = (SearchView) view.findViewById(R.id.book_search_view);
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("검색...");
        searchView.setOnQueryTextListener(this);

        bookSearchNumText = (TextView) view.findViewById(R.id.book_search_num);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(SEARCH_STRING);

        arrayList = new ArrayList<> ();
        simpleAdapter = new SimpleAdapter(getContext(), arrayList, android.R.layout.simple_list_item_2, new String[]{"item1", "item2"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            public View getView(int position, View convertView, ViewGroup parent) {
                try {
                    View view = super.getView(position, convertView, parent);
                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                    text1.setTextSize(14);
                    text2.setTextSize(12);
                    text2.setTextColor(Color.GRAY);
                    return view;
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        ListView noticeListView = (ListView) view.findViewById(R.id.book_search_list);
        noticeListView.setOnItemClickListener(OnClickListItem);
        noticeListView.setAdapter(simpleAdapter);
        noticeListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        return view;
    }

    private AdapterView.OnItemClickListener OnClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        }
    };

    class GetSearchResult extends AsyncTask<String, Void, Void> {
        protected void onPreExecute() {
            if(!NetworkUtil.isNetworkConnected(getActivity())) {
                Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                cancel(true);
                return;
            }

            arrayList.clear();
            simpleAdapter.notifyDataSetChanged();
            bookSearchNumText.setText("검색 중...");

        }

        @Override
        protected Void doInBackground(String... params) {
            updateList(NetworkUtil.httpRequest(params[0]));
            return null;
        }
    }

    private void updateList(final String html) {
        new Thread() {
            public void run() {
                Document document = Jsoup.parse(html);
                Document document1;
                itemElements = document.getElementsByClass("items");

                arrayList.clear();
                for(int i = 0; i < itemElements.size(); i++) {
                    HashMap<String, String> map = new HashMap<>();
                    map.clear();
                    infoString = "";
                    document1 = Jsoup.parse(itemElements.get(i).html());

                    map.put("item1", document1.select("dd.title").text().replace("상세보기", ""));

                    infoElements = document1.select("dd.info");
                    for (int j = 0; j < infoElements.size(); j++) {
                        infoString += infoElements.get(j).text() + "\n";
                    }

                    infoString += "\n";

                    locationElements = document1.getElementsByClass("location");
                    for (int j = 0; j < locationElements.size(); j++) {
                        infoString += locationElements.get(j).text() + "\n";
                    }

                    map.put("item2", infoString);
                    arrayList.add(map);
                }
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bookSearchNumText.setText("총 " + itemElements.size() + "건");
                            simpleAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        GetSearchResult getSearchResult = new GetSearchResult();

        if (SEARCH_STRING.equals("도서 검색")) {
            getSearchResult.execute(BOOK_SEARCH_URL + query.replace(" ", "+") + BOOK_SEARCH_OPTION_1 + searchOption + BOOK_SEARCH_OPTION_2);
        } else if (SEARCH_STRING.equals("학위논문 검색")) {
            getSearchResult.execute(PAPER_SEARCH_URL + searchOption + PAPER_SEARCH_OPTION_1 + query.replace(" ", "+") + PAPER_SEARCH_OPTION_2);
        }

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }
}