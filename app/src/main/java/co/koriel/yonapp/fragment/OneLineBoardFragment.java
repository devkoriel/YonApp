package co.koriel.yonapp.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.amazonaws.AmazonClientException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import co.amazonaws.mobile.AWSConfiguration;
import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.mobile.content.ContentItem;
import co.amazonaws.mobile.content.ContentProgressListener;
import co.amazonaws.mobile.content.UserFileManager;
import co.amazonaws.mobile.util.ImageSelectorUtils;
import co.amazonaws.models.nosql.OnelineBoardCommentDO;
import co.amazonaws.models.nosql.OnelineBoardDO;
import co.amazonaws.models.nosql.OnelineLikeDO;
import co.amazonaws.models.nosql.OnelineNicknameDO;
import co.amazonaws.models.nosql.OnelineReportDO;
import co.koriel.yonapp.R;
import co.koriel.yonapp.db.DataBase;
import co.koriel.yonapp.fragment.adapter.OneLineBoardAdapter;
import co.koriel.yonapp.fragment.adapter.OneLineBoardItem;
import co.koriel.yonapp.util.NetworkUtil;
import co.koriel.yonapp.util.SerializeObject;

public class OneLineBoardFragment extends FragmentBase {
    private final int RESULTS_LIMIT = 20;
    private final int DAY_SECONDS = 86400000;
    private final String INITIAL_POST_DATE = "2016-11-16";
    private static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 93;
    private static final int PICK_IMAGE_REQUEST = 1;

    private final boolean REFRESH = true;
    private final boolean APPEND = false;
    private final int UPDATE_ALL = -1;

    private MaterialDialog onelineWriteDialog;
    private PostContent postContent;

    private boolean isNickStatic;
    private boolean isPicture;
    private SharedPreferences prefs;

    private ListView contentListView;
    private OneLineBoardAdapter oneLineBoardAdapter;
    private ArrayList<OnelineBoardDO> contentScanList;
    private ArrayList<OneLineBoardItem> contentArrangedList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private AdapterView.AdapterContextMenuInfo acmi;

    private DynamoDBMapper dynamoDBMapper;
    private UserFileManager userFileManager;

    private String attachedImagePath;

    private final String S3_PREFIX_PUBLIC_IMG_ONELINE = "public/img/oneline";

    private long timeBefore;
    private String timeBeforeString;
    private long timeLastPost;
    private long timeLastLastPost;

    Parcelable state;

    private boolean mLockListView;
    private boolean isEndOfList;

    private UpdateContentThread updateContentThread;

    public OneLineBoardFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mLockListView = false;

        isNickStatic = !prefs.getBoolean("oneline_anonymous", true);
        isPicture = false;
        onelineWriteDialog = new MaterialDialog.Builder(getContext())
                .title(R.string.oneline_input)
                .content(R.string.oneline_input_content)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .inputRangeRes(2, 200, R.color.edittext_error_color)
                .positiveText(R.string.write)
                .negativeText(R.string.dialog_cancel)
                .neutralText(R.string.attach_picture)
                .showListener(onelineWriteDialogOnShowListener)
                .cancelable(false)
                .input(R.string.oneline_input_hint, R.string.oneline_input_prefill, false, onelineWriteDialogInputCallback)
                .checkBoxPromptRes(R.string.anonymous, !isNickStatic, onelineWriteDialogOnCheckedChangeListener)
                .build();

        AWSMobileClient.defaultMobileClient()
                .createUserFileManager(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, S3_PREFIX_PUBLIC_IMG_ONELINE, AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION,
                        new UserFileManager.BuilderResultHandler() {
                            @Override
                            public void onComplete(final UserFileManager userFileManager) {
                                if (!isAdded()) {
                                    userFileManager.destroy();
                                    return;
                                }

                                OneLineBoardFragment.this.userFileManager = userFileManager;
                            }
                        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do something that differs the Activity's menu here
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_write).setVisible(true);
        menu.findItem(R.id.action_share).setVisible(false);
        menu.findItem(R.id.action_capture).setVisible(false);
        menu.findItem(R.id.action_sync).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_write:
                onelineWriteDialog.getActionButton(DialogAction.NEUTRAL).setText(R.string.attach_picture);
                onelineWriteDialog.show();
                onelineWriteDialog.setPromptCheckBoxChecked(!isNickStatic);
                if (!onelineWriteDialog.isPromptCheckBoxChecked()) new HasNickname().start();
                return true;

            default:
                return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_one_line_board, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.home_menu_oneline);
        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_one_line);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);

        contentScanList = new ArrayList<>();
        contentArrangedList = new ArrayList<>();
        oneLineBoardAdapter = new OneLineBoardAdapter();
        View header = getLayoutInflater(savedInstanceState).inflate(R.layout.content_list_header, null, false);
        contentListView = (ListView) view.findViewById(R.id.oneline_board_list);
        contentListView.addHeaderView(header);
        contentListView.setAdapter(oneLineBoardAdapter);
        contentListView.setOnItemClickListener(OnClickListItem);
        registerForContextMenu(contentListView);
        contentListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        contentListView.setOnScrollListener(OnScrollChange);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String sContentScanList = SerializeObject.ReadSettings(getContext(), "csl.dat");
        String sContentArrangedList = SerializeObject.ReadSettings(getContext(), "cal.dat");
        if (!sContentScanList.equalsIgnoreCase("") && !sContentArrangedList.equalsIgnoreCase("")) {

            Object objCsl = SerializeObject.stringToObject(sContentScanList);
            if (objCsl instanceof ArrayList) {
                contentScanList.clear();
                contentScanList.addAll((ArrayList<OnelineBoardDO>) objCsl);
            }

            Object objCal = SerializeObject.stringToObject(sContentArrangedList);
            if (objCal instanceof ArrayList) {
                contentArrangedList.clear();
                contentArrangedList.addAll((ArrayList<OneLineBoardItem>) objCal);
            }

            oneLineBoardAdapter.clearItems();
            oneLineBoardAdapter.addAllItems(contentArrangedList);
            oneLineBoardAdapter.notifyDataSetChanged();
        } else {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
            updateContent(RESULTS_LIMIT, UPDATE_ALL, REFRESH);
        }
    }

    CompoundButton.OnCheckedChangeListener onelineWriteDialogOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            prefs.edit().putBoolean("oneline_anonymous", b).apply();
            if (isNickStatic = !b) new HasNickname().start();
        }
    };

    MaterialDialog.InputCallback onelineWriteDialogInputCallback = new MaterialDialog.InputCallback() {
        @Override
        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
            if(!NetworkUtil.isNetworkConnected(getActivity())) {
                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
            } else {
                swipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(true);
                    }
                });

                postContent = new PostContent();
                postContent.execute(input.toString());

                try {
                    dialog.getInputEditText().setText("");
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    DialogInterface.OnShowListener onelineWriteDialogOnShowListener = new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialogInterface) {
            final MaterialDialog writeDialog = ((MaterialDialog) dialogInterface);
            final MDButton imageAttachButton = writeDialog.getActionButton(DialogAction.NEUTRAL);
            final MDButton cancelButton = writeDialog.getActionButton(DialogAction.NEGATIVE);

            imageAttachButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String buttonString = imageAttachButton.getText().toString();

                    if (buttonString.equals(getResources().getString(R.string.attach_picture))) {
                        final Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }

                        if (ContextCompat.checkSelfPermission(activity,
                                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
                            return;
                        }

                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                    } else if (buttonString.equals(getResources().getString(R.string.attach_picture_cancel))) {
                        isPicture = false;
                        imageAttachButton.setText(R.string.attach_picture);
                    } else {
                    }
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (writeDialog.getInputEditText().getText().toString().length() > 0) {
                        new MaterialDialog.Builder(getContext())
                                .title(R.string.check_cancel_write)
                                .positiveText(R.string.dialog_ok)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                        writeDialog.dismiss();
                                        writeDialog.getInputEditText().setText("");
                                    }
                                })
                                .negativeText(R.string.dialog_cancel)
                                .show();
                    } else {
                        writeDialog.dismiss();
                    }
                }
            });
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            isPicture = true;

            Uri uri = data.getData();

            try {
                attachedImagePath = ImageSelectorUtils.getFilePathFromUri(getActivity(), uri);
                onelineWriteDialog.getActionButton(DialogAction.NEUTRAL).setText(R.string.attach_picture_cancel);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                isPicture = false;

                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_error_outline_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.attach_picture_failure_title_text)
                        .content(R.string.attach_picture_failure_text)
                        .positiveText(R.string.dialog_ok)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String permissions[], final int[] grantResults) {

        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                final Intent intent = ImageSelectorUtils.getImageSelectionIntent();
                startActivityForResult(intent, 0);
            } else {
                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_error_outline_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.content_permission_failure_title_text)
                        .content(R.string.content_permission_failure_text)
                        .positiveText(R.string.dialog_ok)
                        .show();
            }
        }
    }

    class HasNickname extends Thread {
        public void run() {
            OnelineNicknameDO nickname = dynamoDBMapper.load(OnelineNicknameDO.class, AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

            if (nickname == null) {
                isNickStatic = false;

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onelineWriteDialog.setPromptCheckBoxChecked(true);

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

    private class PostContent extends AsyncTask<String, Void, Void> {
        private String userId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            if (userId == null) {
                Toast.makeText(getActivity(), R.string.error_occured, Toast.LENGTH_SHORT).show();
                cancel(true);
            }

            if (timeLastLastPost == 0 && timeLastPost != 0) timeLastLastPost = System.currentTimeMillis() - timeLastPost;
            else if ((timeLastLastPost + java.lang.System.currentTimeMillis() - timeLastPost) / 2 < 5000) {
                timeLastLastPost = System.currentTimeMillis() - timeLastPost;
                Toast.makeText(getActivity(), R.string.warn_too_often, Toast.LENGTH_SHORT).show();
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
                final String date = dateFormatter.format(today);

                if (isPicture) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final File file = new File(attachedImagePath);

                            userFileManager.uploadContent(file, date + "]" + userId, new ContentProgressListener() {
                                @Override
                                public void onSuccess(ContentItem contentItem) {

                                }

                                @Override
                                public void onProgressUpdate(String filePath, boolean isWaiting, long bytesCurrent, long bytesTotal) {

                                }

                                @Override
                                public void onError(String filePath, Exception ex) {

                                }
                            });
                        }
                    }).start();
                }

                dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                final String date2 = dateFormatter.format(today);

                String ip = NetworkUtil.getExternalIp();

                OnelineBoardDO onelineToWrite = new OnelineBoardDO();

                onelineToWrite.setDateAndId(date + "]" + userId);
                onelineToWrite.setDate(date2);
                onelineToWrite.setTimestamp(java.lang.System.currentTimeMillis() / 1000);
                onelineToWrite.setContent(params[0]);
                onelineToWrite.setWriterGcmTokenKey(DataBase.userInfo.getGcmTokenKey());
                onelineToWrite.setIp(ip);
                onelineToWrite.setNickStatic(isNickStatic);
                onelineToWrite.setPicture(isPicture);

                dynamoDBMapper.save(onelineToWrite);
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
            updateContent(contentArrangedList.size(), UPDATE_ALL, REFRESH);
        }
    }

    private AdapterView.OnItemClickListener OnClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            if (position == 0) return;

            if(!NetworkUtil.isNetworkConnected(getActivity())) {
                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread() {
                public void run() {
                    try {
                        String sContentScanList = SerializeObject.objectToString(contentScanList);
                        if (sContentScanList != null && !sContentScanList.equalsIgnoreCase("")) {
                            SerializeObject.WriteSettings(getContext(), sContentScanList, "csl.dat");
                        } else {
                            SerializeObject.WriteSettings(getContext(), "", "csl.dat");
                        }

                        String sContentArrangedList = SerializeObject.objectToString(contentArrangedList);
                        if (sContentArrangedList != null && !sContentArrangedList.equalsIgnoreCase("")) {
                            SerializeObject.WriteSettings(getContext(), sContentArrangedList, "cal.dat");
                        } else {
                            SerializeObject.WriteSettings(getContext(), "", "cal.dat");
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString("id", contentArrangedList.get(position - 1).getId());
                        bundle.putString("ContentDateAndId", contentArrangedList.get(position - 1).getContentDateAndId());
                        bundle.putString("Content", contentArrangedList.get(position - 1).getContent());
                        bundle.putString("Timestamp", Long.toString(contentArrangedList.get(position - 1).getTimestamp()));
                        bundle.putString("writerGcmTokenKey", contentArrangedList.get(position - 1).getWriterGcmTokenKey());
                        bundle.putString("isNickStatic", Boolean.toString(contentArrangedList.get(position - 1).isNickStatic()));
                        bundle.putString("isPicture", Boolean.toString(contentArrangedList.get(position - 1).isPicture()));

                        OneLineBoardCommentFragment oneLineBoardCommentFragment = new OneLineBoardCommentFragment();
                        oneLineBoardCommentFragment.setArguments(bundle);

                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, oneLineBoardCommentFragment)
                                .addToBackStack(getResources().getString(R.string.home_menu_oneline))
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
            if (contentArrangedList.get(acmi.position - 1).getContentDateAndId().split("]")[1].equals(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID())) {
                inflater.inflate(R.menu.menu_oneline_my, menu);
            } else {
                inflater.inflate(R.menu.menu_oneline, menu);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(!NetworkUtil.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
            return false;
        }

        final String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
        if (userId == null) {
            Toast.makeText(getActivity(), R.string.error_occured, Toast.LENGTH_SHORT).show();
            return true;
        }

        switch(item.getItemId()) {
            case R.id.action_like:
                new Thread() {
                    public void run() {
                        TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
                        dateFormatter.setTimeZone(tz);
                        Date today = new Date();
                        String date = dateFormatter.format(today);

                        OnelineLikeDO likesToFind = new OnelineLikeDO();
                        likesToFind.setContentDateAndId(contentArrangedList.get(acmi.position - 1).getContentDateAndId());

                        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                        expressionAttributeValues.put(":userId", new AttributeValue().withS(userId));

                        DynamoDBQueryExpression queryExpressionLike = new DynamoDBQueryExpression()
                                .withIndexName("contentDateAndId-index")
                                .withHashKeyValues(likesToFind)
                                .withFilterExpression("contains(likeDateAndId, :userId)")
                                .withExpressionAttributeValues(expressionAttributeValues)
                                .withConsistentRead(false);

                        PaginatedQueryList<OnelineLikeDO> onelineLikeDOs = dynamoDBMapper.query(OnelineLikeDO.class, queryExpressionLike);

                        if (onelineLikeDOs.isEmpty()) {
                            OnelineLikeDO onelineLikeDO = new OnelineLikeDO();
                            onelineLikeDO.setLikeDateAndId(date + "]" + userId);
                            onelineLikeDO.setContentDateAndId(contentArrangedList.get(acmi.position - 1).getContentDateAndId());
                            onelineLikeDO.setContentTimestamp(contentArrangedList.get(acmi.position - 1).getTimestamp());
                            dynamoDBMapper.save(onelineLikeDO);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateContent(contentArrangedList.size(), acmi.position - 1, REFRESH);
                                    Toast.makeText(getActivity(), R.string.action_like, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.already_action_like, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }.start();
                return true;
            case R.id.action_report:
                new Thread() {
                    public void run() {
                        TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
                        dateFormatter.setTimeZone(tz);
                        Date today = new Date();
                        String date = dateFormatter.format(today);

                        OnelineReportDO reportToFind = new OnelineReportDO();
                        reportToFind.setContentDateAndId(contentArrangedList.get(acmi.position - 1).getContentDateAndId());

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
                            onelineReportDO.setContentDateAndId(contentArrangedList.get(acmi.position - 1).getContentDateAndId());
                            onelineReportDO.setContentTimestamp(contentArrangedList.get(acmi.position - 1).getTimestamp());
                            dynamoDBMapper.save(onelineReportDO);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.action_report, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.already_action_report, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }.start();
                return true;
            case R.id.action_delete:
                new Thread() {
                    public void run() {
                        OnelineBoardDO onelineBoardDO = new OnelineBoardDO();
                        onelineBoardDO.setDateAndId(contentArrangedList.get(acmi.position - 1).getContentDateAndId());
                        dynamoDBMapper.delete(onelineBoardDO);

                        swipeRefreshLayout.post(new Runnable() {
                            @Override
                            public void run() {
                                swipeRefreshLayout.setRefreshing(true);
                            }
                        });

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateContent(contentArrangedList.size(), UPDATE_ALL, REFRESH);
                            }
                        });
                    }
                }.start();
                return true;
            case R.id.action_copy:
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("copied_from_yonapp", contentArrangedList.get(acmi.position - 1).getContent());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            default:
                return super.onContextItemSelected(item);
        }
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            updateContent(RESULTS_LIMIT, UPDATE_ALL, REFRESH);
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            int count = totalItemCount - visibleItemCount;

            if(firstVisibleItem >= count && totalItemCount != 0 && !mLockListView && !isEndOfList) {
                updateContent(RESULTS_LIMIT, UPDATE_ALL, APPEND);
            }

            boolean enable = false;
            if(contentListView != null && contentListView.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = contentListView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = contentListView.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    private void updateContent(final int count, final int position, final boolean isRefresh) {
        mLockListView = true;

        try {
            if(!NetworkUtil.isNetworkConnected(getActivity())) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getActivity(), R.string.please_connect_internet, Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        updateContentThread = new UpdateContentThread(count, position, isRefresh);
        updateContentThread.setPriority(Thread.MAX_PRIORITY);
        updateContentThread.start();
    }

    class UpdateContentThread extends Thread {
        private int count;
        private int position;
        private boolean isRefresh;

        public UpdateContentThread(final int count, final int position, final boolean isRefresh) {
            this.count = count;
            this.position = position;
            this.isRefresh = isRefresh;
        }

        public void run() {
            if (position == -1) {
                try {
                    TimeZone tz = TimeZone.getTimeZone("Asia/Seoul");
                    DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                    dateFormatter.setTimeZone(tz);

                    long dayCount = 0;
                    long updateTime;
                    if (isRefresh) {
                        isEndOfList = false;
                        contentArrangedList.clear();
                        updateTime = System.currentTimeMillis();
                    } else {
                        updateTime = (contentScanList.get(contentScanList.size() - 1).getTimestamp() - 1) * 1000;
                    }

                    contentScanList.clear();
                    while (contentScanList.size() < count) {
                        Date today = new Date(updateTime - dayCount);
                        String date = dateFormatter.format(today);

                        final OnelineBoardDO onelineToFind = new OnelineBoardDO();
                        onelineToFind.setDate(date);

                        Condition rangeKeyCondition;
                        if (contentScanList.size() == 0) {
                            rangeKeyCondition = new Condition()
                                    .withComparisonOperator(ComparisonOperator.LE.toString())
                                    .withAttributeValueList(new AttributeValue().withN(Long.toString(updateTime / 1000)));
                        } else {
                            rangeKeyCondition = new Condition()
                                    .withComparisonOperator(ComparisonOperator.LE.toString())
                                    .withAttributeValueList(new AttributeValue().withN(Long.toString(contentScanList.get(contentScanList.size() - 1).getTimestamp() - 1)));
                        }

                        DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                                .withIndexName("Date-Timestamp-index")
                                .withHashKeyValues(onelineToFind)
                                .withRangeKeyCondition("Timestamp", rangeKeyCondition)
                                .withScanIndexForward(false)
                                .withConsistentRead(false)
                                .withLimit(count - contentScanList.size());

                        contentScanList.addAll(dynamoDBMapper.queryPage(OnelineBoardDO.class, queryExpression).getResults());

                        dayCount += DAY_SECONDS;

                        if (date.equals(INITIAL_POST_DATE)) {
                            isEndOfList = true;
                            break;
                        }
                    }

                    final int preSize = contentArrangedList.size();
                    for(int i = 0; i < contentScanList.size(); i++) {
                        OnelineBoardDO onelineBoardDO = contentScanList.get(i);
                        if(onelineBoardDO.getContent() != null) {
                            timeBefore = (System.currentTimeMillis() / 1000) - onelineBoardDO.getTimestamp();
                            timeBeforeString = beautifyTime(timeBefore);

                            final String dateAndId = onelineBoardDO.getDateAndId();
                            final boolean isNickStatic = onelineBoardDO.isNickStatic();
                            final boolean isPicture = onelineBoardDO.isPicture();

                            OneLineBoardItem oneLineBoardItem = new OneLineBoardItem();
                            if (isNickStatic) {
                                UpdateNicknameThread updateNicknameThread = new UpdateNicknameThread(preSize + i, dateAndId);
                                updateNicknameThread.setPriority(NORM_PRIORITY);
                                updateNicknameThread.start();
                            } else {
                                oneLineBoardItem.setId(dateAndId.split("-")[5].split(":")[1]);
                            }

                            UpdateCommentCountThread updateCommentCountThread = new UpdateCommentCountThread(preSize + i, dateAndId);
                            updateCommentCountThread.setPriority(NORM_PRIORITY);
                            updateCommentCountThread.start();

                            UpdateLikeCountThread updateLikeCountThread = new UpdateLikeCountThread(preSize + i, dateAndId);
                            updateLikeCountThread.setPriority(MIN_PRIORITY);
                            updateLikeCountThread.start();

                            oneLineBoardItem.setTimeBefore(timeBeforeString);
                            oneLineBoardItem.setContent(onelineBoardDO.getContent());
                            oneLineBoardItem.setContentDateAndId(dateAndId);
                            oneLineBoardItem.setTimestamp(onelineBoardDO.getTimestamp());
                            oneLineBoardItem.setWriterGcmTokenKey(onelineBoardDO.getWriterGcmTokenKey());
                            oneLineBoardItem.setNickStatic(isNickStatic);
                            oneLineBoardItem.setPicture(isPicture);

                            contentArrangedList.add(oneLineBoardItem);
                        }
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            oneLineBoardAdapter.clearItems();
                            oneLineBoardAdapter.addAllItems(contentArrangedList);
                            oneLineBoardAdapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                            mLockListView = false;
                        }
                    });
                } catch (NullPointerException|AmazonClientException|IllegalStateException|IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    OnelineLikeDO likeToFind = new OnelineLikeDO();
                    likeToFind.setContentDateAndId(contentArrangedList.get(position).getContentDateAndId());

                    OnelineBoardCommentDO commentToFind = new OnelineBoardCommentDO();
                    commentToFind.setContentDateAndId(contentArrangedList.get(position).getContentDateAndId());

                    DynamoDBQueryExpression queryExpressionLike = new DynamoDBQueryExpression()
                            .withIndexName("contentDateAndId-index")
                            .withHashKeyValues(likeToFind)
                            .withConsistentRead(false);

                    DynamoDBQueryExpression queryExpressionComment = new DynamoDBQueryExpression()
                            .withIndexName("_ContentDateAndId-index")
                            .withHashKeyValues(commentToFind)
                            .withConsistentRead(false);

                    final PaginatedQueryList<OnelineLikeDO> onelineLikeDOs = dynamoDBMapper.query(OnelineLikeDO.class, queryExpressionLike);
                    final PaginatedQueryList<OnelineBoardCommentDO> onelineBoardCommentDOs = dynamoDBMapper.query(OnelineBoardCommentDO.class, queryExpressionComment);

                    contentArrangedList.get(position).setLikeCount(onelineLikeDOs.size());
                    contentArrangedList.get(position).setCommentCount(onelineBoardCommentDOs.size());

                    String sContentArrangedList = SerializeObject.objectToString(contentArrangedList);
                    if (sContentArrangedList != null && !sContentArrangedList.equalsIgnoreCase("")) {
                        SerializeObject.WriteSettings(getContext(), sContentArrangedList, "cal.dat");
                    } else {
                        SerializeObject.WriteSettings(getContext(), "", "cal.dat");
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            oneLineBoardAdapter.removeItem(position);
                            oneLineBoardAdapter.addItem(position, contentArrangedList.get(position));
                            oneLineBoardAdapter.notifyDataSetChanged();
                            mLockListView = false;
                        }
                    });
                } catch (NullPointerException|AmazonClientException|IllegalStateException|IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
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
                    this.nickname = nicknameLoaded.getNickname();
                } else {
                    this.nickname = this.dateAndId.split("-")[5].split(":")[1];
                }

                if (updateContentThread != null && updateContentThread.isAlive()) {
                    updateContentThread.join();
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ((OneLineBoardItem) oneLineBoardAdapter.getItem(position)).setId(nickname);
                            oneLineBoardAdapter.notifyDataSetChanged();
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (NullPointerException|AmazonClientException|IllegalStateException|IndexOutOfBoundsException|InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateCommentCountThread extends Thread {
        private int position;
        private String dateAndId;
        private int count;

        public UpdateCommentCountThread(int position, String dateAndId) {
            this.position = position;
            this.dateAndId = dateAndId;
        }

        public void run() {
            try {
                OnelineBoardCommentDO commentsToFind = new OnelineBoardCommentDO();
                commentsToFind.setContentDateAndId(dateAndId);

                DynamoDBQueryExpression queryExpressionComments = new DynamoDBQueryExpression()
                        .withIndexName("_ContentDateAndId-index")
                        .withHashKeyValues(commentsToFind)
                        .withConsistentRead(false);

                final PaginatedQueryList<OnelineBoardCommentDO> comments = dynamoDBMapper.query(OnelineBoardCommentDO.class, queryExpressionComments);

                count = comments.size();

                if (updateContentThread != null && updateContentThread.isAlive()) {
                    updateContentThread.join();
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ((OneLineBoardItem) oneLineBoardAdapter.getItem(position)).setCommentCount(count);
                            oneLineBoardAdapter.notifyDataSetChanged();
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (NullPointerException|AmazonClientException|IllegalStateException|IndexOutOfBoundsException|InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateLikeCountThread extends Thread {
        private int position;
        private String dateAndId;
        private int count;

        public UpdateLikeCountThread(int position, String dateAndId) {
            this.position = position;
            this.dateAndId = dateAndId;
        }

        public void run() {
            try {
                OnelineLikeDO likesToFind = new OnelineLikeDO();
                likesToFind.setContentDateAndId(dateAndId);

                DynamoDBQueryExpression queryExpressionLikes = new DynamoDBQueryExpression()
                        .withIndexName("contentDateAndId-index")
                        .withHashKeyValues(likesToFind)
                        .withConsistentRead(false);

                final PaginatedQueryList<OnelineLikeDO> likes = dynamoDBMapper.query(OnelineLikeDO.class, queryExpressionLikes);

                count = likes.size();

                if (updateContentThread != null && updateContentThread.isAlive()) {
                    updateContentThread.join();
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ((OneLineBoardItem) oneLineBoardAdapter.getItem(position)).setLikeCount(count);
                            oneLineBoardAdapter.notifyDataSetChanged();
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (NullPointerException|AmazonClientException|IllegalStateException|IndexOutOfBoundsException|InterruptedException e) {
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

    @Override
    public void onPause() {
        state = contentListView.onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (updateContentThread != null && updateContentThread.isAlive()) {
            updateContentThread.interrupt();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().deleteFile("csl.dat");
        getContext().deleteFile("cal.dat");
    }
}
