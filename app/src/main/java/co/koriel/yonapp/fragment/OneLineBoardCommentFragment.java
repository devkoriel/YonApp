package co.koriel.yonapp.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.wang.avi.AVLoadingIndicatorView;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import co.amazonaws.mobile.AWSConfiguration;
import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.models.nosql.OnelineBoardCommentDO;
import co.amazonaws.models.nosql.OnelineLikeDO;
import co.amazonaws.models.nosql.OnelineNicknameDO;
import co.amazonaws.models.nosql.OnelineReportDO;
import co.koriel.yonapp.MainActivity;
import co.koriel.yonapp.R;
import co.koriel.yonapp.fragment.adapter.OneLineBoardCommentAdapter;
import co.koriel.yonapp.fragment.adapter.OneLineBoardCommentItem;
import co.koriel.yonapp.fragment.viewer.PictureViewerActivity;
import co.koriel.yonapp.helper.MathHelper;
import co.koriel.yonapp.tab.TabPager;
import co.koriel.yonapp.util.NetworkUtil;

public class OneLineBoardCommentFragment extends FragmentBase implements CompoundButton.OnCheckedChangeListener {
    private final String S3_PREFIX_PUBLIC_IMG_ONELINE = "public/img/oneline";

    private DynamoDBMapper dynamoDBMapper;
    private MainActivity mainActivity;

    private SharedPreferences prefs;

    private TextView contentDateAndIdView;
    private TextView contentTime;
    private TextView contentView;
    private ImageView contentImageView;
    private AVLoadingIndicatorView contentAVIView;
    private ImageView imageHeader;
    private TextView attachHeader;
    private TextView likeHeader;
    private TextView commentHeader;
    private CheckBox checkBoxAnonymous;
    private boolean isNickStatic;
    private EditText comment;

    private SwipeMenuListView commentListView;
    private OneLineBoardCommentAdapter oneLineBoardCommentAdapter;
    private ArrayList<OneLineBoardCommentItem> commentArrangedList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SwipeMenuItem reportItem;
    private SwipeMenuItem deleteItem;

    private String id;
    private String ContentDateAndId;
    private String Content;
    private String writerGcmTokenKey;
    private boolean isContentNickStatic;
    private boolean isContentPicture;
    private boolean isContentGif;
    private long ContentTimestamp;

    private UpdateCommentThread updateCommentThread;

    public OneLineBoardCommentFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        Drawable reportDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_thumb_down_white_24dp);
        DrawableCompat.setTint(reportDrawable, ContextCompat.getColor(getContext(), R.color.tab_indicator));

        Drawable deleteDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete_white_24dp);
        DrawableCompat.setTint(deleteDrawable, ContextCompat.getColor(getContext(), R.color.main_background));

        reportItem = new SwipeMenuItem(getContext());
        reportItem.setBackground(R.color.main_background);
        reportItem.setIcon(reportDrawable);
        reportItem.setWidth(MathHelper.dpToPx(60));

        deleteItem = new SwipeMenuItem(getContext());
        deleteItem.setBackground(R.color.tab_indicator);
        deleteItem.setIcon(deleteDrawable);
        deleteItem.setWidth(MathHelper.dpToPx(60));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainActivity.viewPager.setAllowedSwipeDirection(TabPager.SwipeDirection.RIGHT);
        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        TextView toolbarTitle = (TextView) mainActivity.findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.home_menu_oneline);
        return inflater.inflate(R.layout.fragment_one_line_board_comment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View header = getLayoutInflater(savedInstanceState).inflate(R.layout.one_line_board_comment_header_view, null, false);

        contentDateAndIdView = (TextView) header.findViewById(R.id.comment_content_date_and_id);
        contentTime = (TextView) header.findViewById(R.id.comment_content_time);
        contentView = (TextView) header.findViewById(R.id.comment_content);
        contentImageView = (ImageView) header.findViewById(R.id.comment_img);
        contentAVIView = (AVLoadingIndicatorView) header.findViewById(R.id.comment_avi);
        imageHeader = (ImageView) header.findViewById(R.id.image_attach);
        attachHeader = (TextView) header.findViewById(R.id.text_picture_attached);
        likeHeader = (TextView) header.findViewById(R.id.like_count);
        commentHeader = (TextView) header.findViewById(R.id.comment_count);

        Bundle bundle;
        if((bundle = getArguments()) != null) {
            id = bundle.getString("id");
            ContentDateAndId = bundle.getString("ContentDateAndId");
            Content = bundle.getString("Content");
            ContentTimestamp = bundle.getLong("Timestamp");
            writerGcmTokenKey = bundle.getString("writerGcmTokenKey");
            isContentNickStatic = bundle.getBoolean("isNickStatic");
            isContentPicture = bundle.getBoolean("isPicture");
            isContentGif = bundle.getBoolean("isGif");
        }

        checkBoxAnonymous = (CheckBox) view.findViewById(R.id.checkbox_anonymous);
        final String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        if (ContentDateAndId.split("]")[1].equals(userId)) {
            if (!isContentNickStatic) {
                checkBoxAnonymous.setChecked(!(isNickStatic = false));
            } else {
                new Thread() {
                    public void run() {
                        OnelineNicknameDO nickname = dynamoDBMapper.load(OnelineNicknameDO.class, userId);

                        if (nickname != null) {
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    checkBoxAnonymous.setChecked(!(isNickStatic = true));
                                }
                            });
                        } else {
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    checkBoxAnonymous.setChecked(!(isNickStatic = false));
                                }
                            });
                        }
                    }
                }.start();
            }
        } else {
            checkBoxAnonymous.setChecked(prefs.getBoolean("oneline_comment_anonymous", true));
        }
        checkBoxAnonymous.setOnCheckedChangeListener(this);
        if (isNickStatic = !checkBoxAnonymous.isChecked()) new HasNickname().start();
        comment = (EditText) view.findViewById(R.id.oneline_board_comment_edittext);
        comment.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(!NetworkUtil.isNetworkConnected(mainActivity)) {
                        Toast.makeText(getContext(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (comment.getText().toString().matches("")) {
                        Toast.makeText(getContext(), R.string.please_write_comment, Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        PostComment postComment = new PostComment();
                        postComment.execute(comment.getText().toString());
                        comment.setText("");
                        InputMethodManager inputManager = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(mainActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                    return true;
                }
                return false;
            }
        });

        Button postButton = (Button) mainActivity.findViewById(R.id.oneline_board_comment_post_button);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!NetworkUtil.isNetworkConnected(mainActivity)) {
                    Toast.makeText(getContext(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                } else if (comment.getText().toString().matches("")) {
                    Toast.makeText(getContext(), R.string.please_write_comment, Toast.LENGTH_SHORT).show();
                } else {
                    PostComment postComment = new PostComment();
                    postComment.execute(comment.getText().toString());
                    comment.setText("");
                    InputMethodManager inputManager = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(mainActivity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_one_line);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);

        commentArrangedList = new ArrayList<>();
        oneLineBoardCommentAdapter = new OneLineBoardCommentAdapter();
        commentListView = (SwipeMenuListView) view.findViewById(R.id.oneline_board_comment_list);
        commentListView.addHeaderView(header);
        commentListView.setAdapter(oneLineBoardCommentAdapter);
        commentListView.setMenuCreator(swipeMenuCreator);
        commentListView.setSwipeDirection(SwipeMenuListView.DIRECTION_RIGHT);
        commentListView.setOnMenuItemClickListener(onMenuItemClickListener);
        registerForContextMenu(commentListView);
        commentListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        commentListView.setOnScrollListener(OnScrollChange);

        swipeRefreshLayout.setRefreshing(true);
        updateComment(true);
    }

    class HasNickname extends Thread {
        public void run() {
            OnelineNicknameDO nickname = dynamoDBMapper.load(OnelineNicknameDO.class, AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

            if (nickname == null) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkBoxAnonymous.setChecked(true);
                        isNickStatic = false;

                        new MaterialDialog.Builder(getContext())
                                .iconRes(R.drawable.ic_error_outline_black_48dp)
                                .limitIconToDefaultSize()
                                .title(R.string.no_nickname)
                                .content(R.string.please_register_nickname)
                                .positiveText(R.string.dialog_ok)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                });
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        prefs.edit().putBoolean("oneline_comment_anonymous", b).apply();
        isNickStatic = !b;

        final String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        if (ContentDateAndId.split("]")[1].equals(userId)) {
            if (!isContentNickStatic) {
                checkBoxAnonymous.setChecked(!(isNickStatic = false));
            } else {
                new Thread() {
                    public void run() {
                        try {
                            OnelineNicknameDO nickname = dynamoDBMapper.load(OnelineNicknameDO.class, userId);

                            if (nickname != null) {
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkBoxAnonymous.setChecked(!(isNickStatic = true));
                                    }
                                });
                            } else {
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        checkBoxAnonymous.setChecked(!(isNickStatic = false));
                                    }
                                });
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        } else if (isNickStatic) {
            new HasNickname().start();
        }
    }

    private class PostComment extends AsyncTask<String, Void, Void> {
        private String userId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            swipeRefreshLayout.setRefreshing(true);

            userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            if (userId == null) {
                Toast.makeText(getContext(), R.string.error_occured, Toast.LENGTH_SHORT).show();
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

                String ip = NetworkUtil.getExternalIp();

                OnelineBoardCommentDO onelineCommentToWrite = new OnelineBoardCommentDO();

                onelineCommentToWrite.setCommentDateAndId(date + "]" + userId);
                onelineCommentToWrite.setContentDateAndId(ContentDateAndId);
                onelineCommentToWrite.setTimestamp(System.currentTimeMillis() / 1000);
                onelineCommentToWrite.setWriterGcmTokenKey(writerGcmTokenKey);
                onelineCommentToWrite.setComment(params[0]);
                onelineCommentToWrite.setIp(ip);
                onelineCommentToWrite.setContent(Content);
                onelineCommentToWrite.setContentTimestamp(Long.toString(ContentTimestamp));
                onelineCommentToWrite.setNickStatic(isNickStatic);

                dynamoDBMapper.save(onelineCommentToWrite);

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
            updateComment(false);
        }
    }

    private SwipeMenuCreator swipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void create(SwipeMenu menu) {
            switch (menu.getViewType()) {
                case 0:
                    menu.addMenuItem(deleteItem);
                    break;

                case 1:
                    menu.addMenuItem(reportItem);
                    break;
            }
        }
    };

    private SwipeMenuListView.OnMenuItemClickListener onMenuItemClickListener = new SwipeMenuListView.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {
            if(!NetworkUtil.isNetworkConnected(mainActivity)) {
                Toast.makeText(getContext(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                return false;
            }

            final String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            if (userId == null) {
                Toast.makeText(getContext(), R.string.error_occured, Toast.LENGTH_SHORT).show();
                return false;
            }

            switch (menu.getViewType()) {
                case 0:
                    new Thread() {
                        public void run() {
                            OnelineBoardCommentDO onelineBoardCommentDO = new OnelineBoardCommentDO();
                            onelineBoardCommentDO.setCommentDateAndId(commentArrangedList.get(position).getCommentDateAndId());
                            dynamoDBMapper.delete(onelineBoardCommentDO);

                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    swipeRefreshLayout.setRefreshing(true);
                                    updateComment(false);
                                }
                            });
                        }
                    }.start();
                    break;

                case 1:
                    new Thread() {
                        public void run() {
                            TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
                            dateFormatter.setTimeZone(tz);
                            Date today = new Date();
                            String date = dateFormatter.format(today);

                            OnelineReportDO reportToFind = new OnelineReportDO();
                            reportToFind.setContentDateAndId(commentArrangedList.get(position).getCommentDateAndId());

                            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                            expressionAttributeValues.put(":userId", new AttributeValue().withS(userId));

                            DynamoDBQueryExpression queryExpressionReport = new DynamoDBQueryExpression()
                                    .withIndexName("contentDateAndId-index")
                                    .withHashKeyValues(reportToFind)
                                    .withFilterExpression("contains(reportDateAndId, :userId)")
                                    .withExpressionAttributeValues(expressionAttributeValues)
                                    .withConsistentRead(false);

                            PaginatedQueryList<OnelineReportDO> onelineReportDOs = dynamoDBMapper.query(OnelineReportDO.class, queryExpressionReport);

                            if (onelineReportDOs.isEmpty()) {
                                OnelineReportDO onelineReportDO = new OnelineReportDO();
                                onelineReportDO.setReportDateAndId(date + "]" + userId);
                                onelineReportDO.setContentDateAndId(commentArrangedList.get(position).getCommentDateAndId());
                                onelineReportDO.setContentTimestamp(commentArrangedList.get(position).getTimestamp());
                                onelineReportDO.setType(true);
                                dynamoDBMapper.save(onelineReportDO);

                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), R.string.action_report, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), R.string.already_action_report, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }.start();
                    break;

                default:
                    return false;
            }
            return false;
        }
    };

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            updateComment(false);
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(commentListView != null && commentListView.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = commentListView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = commentListView.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    private void updateComment(final boolean isFirst) {
        try {
            if(!NetworkUtil.isNetworkConnected(mainActivity)) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                return;
            }

            updateCommentThread = new UpdateCommentThread();
            updateCommentThread.setPriority(Thread.MAX_PRIORITY);
            updateCommentThread.start();

            UpdateLikeThread updateLikeThread = new UpdateLikeThread();
            updateLikeThread.setPriority(Thread.MIN_PRIORITY);
            updateLikeThread.start();

            final Date today = new Date();
            contentDateAndIdView.setText(id);
            contentTime.setText(beautifyTime(today.getTime()/1000 - ContentTimestamp));
            contentView.setText(Content);

            if (isFirst) {
                if (isContentPicture) {
                    if (getImageThread.getState() == Thread.State.NEW) getImageThread.start();
                    imageHeader.setVisibility(View.VISIBLE);
                    attachHeader.setVisibility(View.VISIBLE);
                    contentAVIView.setVisibility(View.VISIBLE);
                    contentAVIView.show();
                }
            }

        } catch (NullPointerException|IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private Thread getImageThread = new Thread() {
        public void run() {
            final AmazonS3 s3 =
                    new AmazonS3Client(AWSMobileClient.defaultMobileClient().getIdentityManager().getCredentialsProvider());
            s3.setRegion(Region.getRegion(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION));

            final URL presignedUrl = s3.generatePresignedUrl(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, S3_PREFIX_PUBLIC_IMG_ONELINE + "/" + ContentDateAndId,
                    new Date(new Date().getTime() + 60 * 60 * 1000));

            try {
                if (isContentGif) {
                    contentImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(getContext())
                                    .load(presignedUrl.toString())
                                    .asGif()
                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                    .listener(new RequestListener<String, GifDrawable>() {
                                        @Override
                                        public boolean onException(Exception e, String model, Target<GifDrawable> target, boolean isFirstResource) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(final GifDrawable resource, String model, Target<GifDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                            contentAVIView.hide();
                                            contentAVIView.setVisibility(View.GONE);

                                            contentImageView.setVisibility(View.VISIBLE);

                                            return false;
                                        }
                                    })
                                    .into(contentImageView);
                        }
                    });
                } else {
                    contentImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(getContext())
                                    .load(presignedUrl.toString())
                                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                    .listener(new RequestListener<String, GlideDrawable>() {
                                        @Override
                                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                            contentAVIView.hide();
                                            contentAVIView.setVisibility(View.GONE);

                                            contentImageView.setVisibility(View.VISIBLE);
                                            contentImageView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent intent = new Intent(getContext(), PictureViewerActivity.class);
                                                    intent.putExtra("url", presignedUrl.toString());
                                                    startActivity(intent);
                                                }
                                            });

                                            return false;
                                        }
                                    })
                                    .into(contentImageView);
                        }
                    });
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    };

    private class UpdateLikeThread extends Thread {
        public void run() {
            try {
                OnelineLikeDO likesToFind = new OnelineLikeDO();
                likesToFind.setContentDateAndId(ContentDateAndId);

                DynamoDBQueryExpression queryExpressionLike = new DynamoDBQueryExpression()
                        .withIndexName("contentDateAndId-index")
                        .withHashKeyValues(likesToFind)
                        .withConsistentRead(false);

                final PaginatedQueryList<OnelineLikeDO> likes = dynamoDBMapper.query(OnelineLikeDO.class, queryExpressionLike);

                likeHeader.post(new Runnable() {
                    @Override
                    public void run() {
                        likeHeader.setText(String.valueOf(likes.size()));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateCommentThread extends Thread {
        public void run() {
            try {
                OnelineBoardCommentDO commentsToFind = new OnelineBoardCommentDO();
                commentsToFind.setContentDateAndId(ContentDateAndId);

                Condition rangeKeyConditionComment = new Condition()
                        .withComparisonOperator(ComparisonOperator.GE.toString())
                        .withAttributeValueList(new AttributeValue().withN(Long.toString(ContentTimestamp)));

                DynamoDBQueryExpression queryExpressionComment = new DynamoDBQueryExpression()
                        .withIndexName("_ContentDateAndId-index")
                        .withHashKeyValues(commentsToFind)
                        .withRangeKeyCondition("Timestamp", rangeKeyConditionComment)
                        .withScanIndexForward(true)
                        .withConsistentRead(false);

                final PaginatedQueryList<OnelineBoardCommentDO> comments = dynamoDBMapper.query(OnelineBoardCommentDO.class, queryExpressionComment);

                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });

                commentArrangedList.clear();
                for(int i = 0; i < comments.size(); i++) {
                    OnelineBoardCommentDO onelineBoardCommentDO = comments.get(i);
                    if(onelineBoardCommentDO.getComment() != null) {
                        OneLineBoardCommentItem oneLineBoardCommentItem = new OneLineBoardCommentItem();
                        long timeBefore = (System.currentTimeMillis() / 1000) - onelineBoardCommentDO.getTimestamp();
                        String timeBeforeString = beautifyTime(timeBefore);

                        String dateAndId = onelineBoardCommentDO.getCommentDateAndId();

                        if (onelineBoardCommentDO.isNickStatic()) {
                            UpdateNicknameThread updateNicknameThread = new UpdateNicknameThread(i, dateAndId);
                            updateNicknameThread.setPriority(NORM_PRIORITY);
                            updateNicknameThread.start();
                        } else {
                            if (dateAndId.split("]")[1].equals(ContentDateAndId.split("]")[1])) {
                                oneLineBoardCommentItem.setId(dateAndId.split("-")[5].split(":")[1] + getResources().getString(R.string.writer_suffix));
                            } else {
                                oneLineBoardCommentItem.setId(dateAndId.split("-")[5].split(":")[1]);
                            }
                        }

                        oneLineBoardCommentItem.setTimeBefore(timeBeforeString);
                        oneLineBoardCommentItem.setIndex(i + 1);
                        oneLineBoardCommentItem.setComment(onelineBoardCommentDO.getComment());
                        oneLineBoardCommentItem.setCommentDateAndId(dateAndId);
                        oneLineBoardCommentItem.setNickStatic(onelineBoardCommentDO.isNickStatic());
                        oneLineBoardCommentItem.setTimestamp(onelineBoardCommentDO.getTimestamp());

                        commentArrangedList.add(oneLineBoardCommentItem);
                    }
                }
                commentListView.post(new Runnable() {
                    @Override
                    public void run() {
                        commentHeader.setText(String.valueOf(comments.size()));
                        oneLineBoardCommentAdapter.clearItems();
                        oneLineBoardCommentAdapter.addAllItems(commentArrangedList);
                        oneLineBoardCommentAdapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateNicknameThread extends Thread {
        private int position;
        private String dateAndId;
        private String nickname;

        public UpdateNicknameThread(int position, String dateAndId) {
            this.position = position;
            this.dateAndId = dateAndId;
        }

        public void run() {
            try {
                String userId = this.dateAndId.split("]")[1];

                OnelineNicknameDO nicknameLoaded = dynamoDBMapper.load(OnelineNicknameDO.class, userId);

                if (nicknameLoaded != null) {
                    if (userId.equals(ContentDateAndId.split("]")[1])) {
                        this.nickname = nicknameLoaded.getNickname() + getResources().getString(R.string.writer_suffix);
                    } else {
                        this.nickname = nicknameLoaded.getNickname();
                    }
                } else {
                    if (userId.equals(ContentDateAndId.split("]")[1])) {
                        this.nickname = this.dateAndId.split("-")[5].split(":")[1] + getResources().getString(R.string.writer_suffix);
                    } else {
                        this.nickname = this.dateAndId.split("-")[5].split(":")[1];
                    }
                }

                if (updateCommentThread != null && updateCommentThread.isAlive()) {
                    updateCommentThread.join();
                }

                commentListView.post(new Runnable() {
                    @Override
                    public void run() {
                        ((OneLineBoardCommentItem) oneLineBoardCommentAdapter.getItem(position)).setId(nickname);
                        oneLineBoardCommentAdapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String beautifyTime(long seconds) {
        if(seconds > 31540000) return (seconds / 31540000) + getResources().getString(R.string.year_ago);
        else if(seconds > 2628000) return  (seconds / 2628000) + getResources().getString(R.string.month_ago);
        else if(seconds > 604800) return  (seconds / 604800) + getResources().getString(R.string.week_ago);
        else if(seconds > 86400) return  (seconds / 86400) + getResources().getString(R.string.day_ago);
        else if(seconds > 3600) return  (seconds / 3600) + getResources().getString(R.string.hour_ago);
        else if(seconds > 60) return  (seconds / 60) + getResources().getString(R.string.minute_ago);
        else return getResources().getString(R.string.just_ago);
    }
}
