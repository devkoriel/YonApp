package co.koriel.yonapp.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import java.util.TimeZone;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.models.nosql.OnelineBoardCommentDO;
import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.util.NetworkUtil;

public class OneLineBoardCommentFragment extends FragmentBase {
    private PostComment postComment;

    private TextView contentDateAndIdView;
    private TextView contentView;
    private TextView commentHeader;
    private EditText comment;

    private ListView commentList;
    private ArrayList<String> arrayList;
    private ArrayList<OnelineBoardCommentDO> scanList;
    private ArrayAdapter<String> arrayAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private DynamoDBMapper dynamoDBMapper;

    private long timeBefore;
    private String timeBeforeString;

    private String ContentDateAndId;
    private String Content;
    private String writerGcmTokenKey;
    private long ContentTimestamp;
    private int commentCount;

    private AdapterView.AdapterContextMenuInfo acmi;

    public OneLineBoardCommentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        return inflater.inflate(R.layout.fragment_one_line_board_comment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        contentDateAndIdView = (TextView) view.findViewById(R.id.comment_content_date_and_id);
        contentView = (TextView) view.findViewById(R.id.comment_content);
        commentHeader = (TextView) view.findViewById(R.id.comment_count);

        if(getArguments() != null) {
            ContentDateAndId = getArguments().getString("ContentDateAndId");
            Content = getArguments().getString("Content");
            ContentTimestamp = Long.valueOf(getArguments().getString("Timestamp"));
            writerGcmTokenKey = getArguments().getString("writerGcmTokenKey");
        }

        comment = (EditText) getActivity().findViewById(R.id.oneline_board_comment_edittext);
        comment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(!NetworkUtil.isNetworkConnected(getActivity())) {
                        Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (comment.getText().toString().matches("")) {
                        Toast.makeText(getActivity(), "댓글을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        swipeRefreshLayout.setRefreshing(true);
                        postComment = new PostComment();
                        postComment.execute(comment.getText().toString());
                        updateComment();
                        comment.setText("");
                        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    return true;
                }
                return false;
            }
        });

        Button postButton = (Button) getActivity().findViewById(R.id.oneline_board_comment_post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetworkUtil.isNetworkConnected(getActivity())) {
                    Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                } else if (comment.getText().toString().matches("")) {
                    Toast.makeText(getActivity(), "댓글을 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    swipeRefreshLayout.setRefreshing(true);
                    postComment = new PostComment();
                    postComment.execute(comment.getText().toString());
                    updateComment();
                    comment.setText("");
                    InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_one_line);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);
        swipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        arrayList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(getContext(), R.layout.simple_list_item_1, arrayList);
        commentList = (ListView) view.findViewById(R.id.oneline_board_comment_list);
        commentList.setAdapter(arrayAdapter);
        registerForContextMenu(commentList);
        commentList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        commentList.setOnScrollListener(OnScrollChange);

        updateComment();
    }

    private class PostComment extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
                dateFormatter.setTimeZone(tz);
                Date today = new Date();
                String date = dateFormatter.format(today);

                String ip = NetworkUtil.getExternalIp();

                if (AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID() == null) {
                    Toast.makeText(getActivity(), "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    return null;
                } else {
                    DataBase.onelineBoardComment.setCommentDateAndId(date + "]" + AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
                    DataBase.onelineBoardComment.setContentDateAndId(ContentDateAndId);
                    DataBase.onelineBoardComment.setTimestamp(System.currentTimeMillis() / 1000);
                    DataBase.onelineBoardComment.setWriterGcmTokenKey(writerGcmTokenKey);
                    DataBase.onelineBoardComment.setComment(params[0]);
                    DataBase.onelineBoardComment.setIp(ip);
                    DataBase.onelineBoardComment.setContent(Content);
                    DataBase.onelineBoardComment.setContentTimestamp(Long.toString(ContentTimestamp));
                    dynamoDBMapper.save(DataBase.onelineBoardComment);
                    return null;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            updateComment();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.oneline_board_comment_list) {
            MenuInflater inflater = getActivity().getMenuInflater();
            acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (scanList.get(acmi.position).getCommentDateAndId().split("]")[1].equals(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID())) {
                inflater.inflate(R.menu.menu_oneline_comment, menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_comment_delte:
                new Thread() {
                    public void run() {
                        OnelineBoardCommentDO onelineBoardCommentDO = new OnelineBoardCommentDO();
                        onelineBoardCommentDO.setCommentDateAndId(scanList.get(acmi.position).getCommentDateAndId());
                        dynamoDBMapper.delete(onelineBoardCommentDO);
                        swipeRefreshLayout.post(new Runnable() {
                            @Override public void run() {
                                swipeRefreshLayout.setRefreshing(true);
                            }
                        });
                        updateComment();
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
            updateComment();
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(commentList != null && commentList.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = commentList.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = commentList.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    private void updateComment() {
        new Thread() {
            public void run() {
                try {
                    if(!NetworkUtil.isNetworkConnected(getActivity())) {
                        Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                    PaginatedScanList<OnelineBoardCommentDO> comments = dynamoDBMapper.scan(OnelineBoardCommentDO.class, scanExpression);
                    scanList = new ArrayList<>();
                    arrayList.clear();

                    commentCount = 0;
                    for(OnelineBoardCommentDO comment : comments) {
                        if (comment.getContentDateAndId().equals(ContentDateAndId)) {
                            scanList.add(comment);
                            commentCount++;
                        }
                    }

                    Collections.sort(scanList, new Comparator<OnelineBoardCommentDO>() {
                        @Override
                        public int compare(OnelineBoardCommentDO lhs, OnelineBoardCommentDO rhs) {
                            return lhs.getTimestamp() < rhs.getTimestamp() ? -1 : lhs.getTimestamp() > rhs.getTimestamp() ? 1:0;
                        }
                    });

                    arrayList.clear();
                    for(int i = 0; i < scanList.size(); i++) {
                        if(scanList.get(i).getComment() != null) {
                            timeBefore = (System.currentTimeMillis() / 1000) - scanList.get(i).getTimestamp();
                            timeBeforeString = beautifyTime(timeBefore);
                            arrayList.add(scanList.get(i).getComment() + " [" + scanList.get(i).getCommentDateAndId().split("-")[5].split(":")[1] + "], " + timeBeforeString);
                        }
                    }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final Date today = new Date();
                                contentDateAndIdView.setText("[" + ContentDateAndId.split("-")[5].split(":")[1] + "] " + beautifyTime(today.getTime()/1000 - ContentTimestamp));
                                contentView.setText(Content);
                                commentHeader.setText("댓글+" + commentCount);
                                arrayAdapter.notifyDataSetChanged();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private String beautifyTime(long seconds) {
        if(seconds > 31540000) return (seconds / 31540000) + "년 전";
        else if(seconds > 2628000) return  (seconds / 2628000) + "개월 전";
        else if(seconds > 604800) return  (seconds / 604800) + "주 전";
        else if(seconds > 86400) return  (seconds / 86400) + "일 전";
        else if(seconds > 3600) return  (seconds / 3600) + "시간 전";
        else if(seconds > 60) return  (seconds / 60) + "분 전";
        else return "방금 전";
    }
}
