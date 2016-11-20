package co.koriel.yonapp.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.models.nosql.OnelineBoardCommentDO;
import co.amazonaws.models.nosql.OnelineBoardDO;
import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.util.NetworkUtil;

public class OneLineBoardFragment extends FragmentBase {

    private PostContent postContent;

    private EditText content;

    private ListView contentList;
    private ArrayList<HashMap<String, String>> arrayList;
    private SimpleAdapter simpleAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PaginatedScanList<OnelineBoardDO> onelines;
    private ArrayList<OnelineBoardDO> scanList;
    private ArrayList<String> commentList;
    private int commentCount;

    private AdapterView.AdapterContextMenuInfo acmi;

    private DynamoDBMapper dynamoDBMapper;

    private long timeBefore;
    private String timeBeforeString;
    private long timeLastPost;
    private long timeLastLastPost;

    Parcelable state;

    public static PaginatedScanList<OnelineBoardCommentDO> onelinesComment;

    public OneLineBoardFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_one_line_board, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("한줄 게시판");
        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_one_line);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);
        swipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        content = (EditText) view.findViewById(R.id.oneline_board_edittext);
        content.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(!NetworkUtil.isNetworkConnected(getActivity())) {
                        Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (content.getText().toString().matches("")) {
                        Toast.makeText(getActivity(), "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        swipeRefreshLayout.setRefreshing(true);
                        postContent = new PostContent();
                        postContent.execute(content.getText().toString());

                        content.setText("");
                        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    return true;
                }
                return false;
            }
        });

        Button postButton = (Button) view.findViewById(R.id.oneline_board_post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetworkUtil.isNetworkConnected(getActivity())) {
                    Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                } else if (content.getText().toString().matches("")) {
                    Toast.makeText(getActivity(), "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    swipeRefreshLayout.setRefreshing(true);
                    postContent = new PostContent();
                    postContent.execute(content.getText().toString());

                    content.setText("");
                    InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        arrayList = new ArrayList<> ();
        simpleAdapter = new SimpleAdapter(getContext(), arrayList, android.R.layout.simple_list_item_2, new String[]{"item1", "item2"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setPadding(0,8,0,0);
                text1.setTextSize(13);
                text1.setSingleLine(true);
                text1.setEllipsize(TextUtils.TruncateAt.END);
                text2.setPadding(0,15,0,0);
                text2.setTextSize(8);
                text2.setTextColor(Color.GRAY);
                return view;
            }
        };
        contentList = (ListView) view.findViewById(R.id.oneline_board_list);
        contentList.setAdapter(simpleAdapter);
        contentList.setOnItemClickListener(OnClickListItem);
        registerForContextMenu(contentList);
        contentList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        contentList.setOnScrollListener(OnScrollChange);
        View header = getLayoutInflater(savedInstanceState).inflate(R.layout.content_list_header, null, false);
        contentList.addHeaderView(header);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateContent();
    }

    private class PostContent extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (timeLastLastPost == 0 && timeLastPost != 0) timeLastLastPost = System.currentTimeMillis() - timeLastPost;
            else if ((timeLastLastPost + java.lang.System.currentTimeMillis() - timeLastPost) / 2 < 5000) {
                timeLastLastPost = System.currentTimeMillis() - timeLastPost;
                Toast.makeText(getActivity(), "너무 자주 작성할 수 없습니다", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                cancel(true);
            }
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
                dateFormatter.setTimeZone(tz);
                Date today = new Date();
                String date = dateFormatter.format(today);

                dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                String date2 = dateFormatter.format(today);

                String ip = NetworkUtil.getExternalIp();

                if (AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID() == null) {
                    Toast.makeText(getActivity(), "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    return null;
                }
                DataBase.onelineBoard.setDateAndId(date + "]" + AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
                DataBase.onelineBoard.setDate(date2);
                DataBase.onelineBoard.setTimestamp(java.lang.System.currentTimeMillis() / 1000);
                DataBase.onelineBoard.setContent(params[0]);
                DataBase.onelineBoard.setWriterGcmTokenKey(DataBase.userInfo.getGcmTokenKey());
                DataBase.onelineBoard.setIp(ip);

                dynamoDBMapper.save(DataBase.onelineBoard);
                return null;
            } catch (NullPointerException e) {
                e.printStackTrace();
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            timeLastPost = java.lang.System.currentTimeMillis();
            updateContent();
        }
    }

    private AdapterView.OnItemClickListener OnClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            if (position == 0) return;

            new Thread() {
                public void run() {
                    try {
                        if(!NetworkUtil.isNetworkConnected(getActivity())) {
                            Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                            interrupt();
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString("ContentDateAndId", scanList.get(position - 1).getDateAndId());
                        bundle.putString("Content", scanList.get(position - 1).getContent());
                        bundle.putString("Timestamp", Long.toString(scanList.get(position - 1).getTimestamp()));
                        bundle.putString("writerGcmTokenKey", scanList.get(position - 1).getWriterGcmTokenKey());

                        OneLineBoardCommentFragment oneLineBoardCommentFragment = new OneLineBoardCommentFragment();
                        oneLineBoardCommentFragment.setArguments(bundle);

                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, oneLineBoardCommentFragment)
                                .addToBackStack("oneline_board_comment")
                                .commit();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.oneline_board_list) {
            MenuInflater inflater = getActivity().getMenuInflater();
            acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (scanList.get(acmi.position - 1).getDateAndId().split("]")[1].equals(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID())) {
                inflater.inflate(R.menu.menu_oneline, menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_delete:
                new Thread() {
                 public void run() {
                     OnelineBoardDO onelineBoardDO = new OnelineBoardDO();
                     onelineBoardDO.setDateAndId(scanList.get(acmi.position - 1).getDateAndId());
                     dynamoDBMapper.delete(onelineBoardDO);
                     swipeRefreshLayout.post(new Runnable() {
                         @Override public void run() {
                             swipeRefreshLayout.setRefreshing(true);
                         }
                     });
                     updateContent();
                 }
                }.start();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            updateContent();
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(contentList != null && contentList.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = contentList.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = contentList.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    private void updateContent() {
        new Thread() {
            public void run() {
                try {
                    if(!NetworkUtil.isNetworkConnected(getActivity())) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                    onelines = dynamoDBMapper.scan(OnelineBoardDO.class, scanExpression);
                    onelinesComment = dynamoDBMapper.scan(OnelineBoardCommentDO.class, scanExpression);
                    scanList = new ArrayList<>();
                    commentList = new ArrayList<>();
                    arrayList.clear();
                    for(OnelineBoardDO oneline : onelines) {
                        scanList.add(oneline);
                    }

                    for(OnelineBoardCommentDO comment : onelinesComment) {
                        commentList.add(comment.getContentDateAndId());
                    }

                    Collections.sort(scanList, new Comparator<OnelineBoardDO>() {
                        @Override
                        public int compare(OnelineBoardDO lhs, OnelineBoardDO rhs) {
                            return lhs.getTimestamp() > rhs.getTimestamp() ? -1 : lhs.getTimestamp() < rhs.getTimestamp() ? 1:0;
                        }
                    });

                    arrayList.clear();
                    for(int i = 0; i < scanList.size(); i++) {
                        if(scanList.get(i).getContent() != null) {
                            HashMap<String, String> map = new HashMap<> ();
                            timeBefore = (java.lang.System.currentTimeMillis() / 1000) - scanList.get(i).getTimestamp();
                            if(timeBefore > 31540000) timeBeforeString = (timeBefore / 31540000) + "년 전";
                            else if(timeBefore > 2628000) timeBeforeString = (timeBefore / 2628000) + "개월 전";
                            else if(timeBefore > 604800) timeBeforeString = (timeBefore / 604800) + "주 전";
                            else if(timeBefore > 86400) timeBeforeString = (timeBefore / 86400) + "일 전";
                            else if(timeBefore > 3600) timeBeforeString = (timeBefore / 3600) + "시간 전";
                            else if(timeBefore > 60) timeBeforeString = (timeBefore / 60) + "분 전";
                            else timeBeforeString = "방금 전";
                            commentCount = Collections.frequency(commentList, scanList.get(i).getDateAndId());
                            map.put("item1", scanList.get(i).getContent());
                            map.put("item2", "[" + scanList.get(i).getDateAndId().split("-")[5].split(":")[1] + "]  " + timeBeforeString + ", 댓글+" + commentCount);
                            arrayList.add(map);
                        }
                    }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                simpleAdapter.notifyDataSetChanged();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onPause() {
        state = contentList.onSaveInstanceState();
        super.onPause();
    }
}
