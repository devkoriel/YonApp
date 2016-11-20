//
// Copyright 2016 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.9
//
package co.koriel.yonapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.flyco.dialog.listener.OnBtnClickL;
import com.flyco.dialog.widget.NormalDialog;

import java.util.ArrayList;

import co.amazonaws.mobile.AWSMobileClient;
import co.koriel.yonapp.fragment.SettingsFragment;
import co.koriel.yonapp.tab.TabPagerAdapter;
import co.koriel.yonapp.util.BackPressCloseHandler;

public class MainActivity extends AppCompatActivity {

    /** Bundle key for saving/restoring the toolbar title. */
    private final static String BUNDLE_KEY_TOOLBAR_TITLE = "title";

    /** The toolbar view control. */
    private Toolbar toolbar;

    private TabPagerAdapter tabPagerAdapter;
    private int tabPosition;

    /** The viewpager control */
    private ViewPager viewPager;

    private BackPressCloseHandler backPressCloseHandler;

    private ImageButton menuCapture;

    private Menu menu;

    /**
     * Initializes the Toolbar for use with the activity.
     */
    private void setupToolbar(final Bundle savedInstanceState) {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        // Set up the activity to use this toolbar. As a side effect this sets the Toolbar's title
        // to the activity's title.
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            // Some IDEs such as Android Studio complain about possible NPE without this check.
            assert getSupportActionBar() != null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        this.menu.findItem(R.id.action_share).setVisible(false);
        this.menu.findItem(R.id.action_sync).setVisible(false);
        return true;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backPressCloseHandler = new BackPressCloseHandler(this);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.status_bar_color));
        }

        // Obtain a reference to the mobile client. It is created in the Application class,
        // but in case a custom Application class is not used, we initialize it here if necessary.
        AWSMobileClient.initializeMobileClientIfNecessary(this);

        // Obtain a reference to the mobile client. It is created in the Application class.
        final AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();

        setContentView(R.layout.activity_main);

        setupToolbar(savedInstanceState);
        menuCapture = (ImageButton) findViewById(R.id.action_capture);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initializing the TabLayout
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        // Add tab into TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("홈"));
        tabLayout.addTab(tabLayout.newTab().setText("공지사항"));
        tabLayout.addTab(tabLayout.newTab().setText("시간표"));
        tabLayout.addTab(tabLayout.newTab().setText("도서관"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // Initializing ViewPager
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(4);

        // Creating TabPagerAdapter adapter
        tabPagerAdapter = new TabPagerAdapter(getSupportFragmentManager());

        // Set TabPagerAdapter on ViewPager
        viewPager.setAdapter(tabPagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout) {

            @Override
            public void onPageSelected(int position) {
                tabPosition = position;
                final TextView toolbarTitle = (TextView) findViewById(R.id.toolbarTitle);

                if (position == 0) {
                    int count;
                    if ((count = tabPagerAdapter.getItem(0).getChildFragmentManager().getBackStackEntryCount()) > 0) {
                        FragmentManager.BackStackEntry backStackEntry = tabPagerAdapter.getItem(0).getChildFragmentManager().getBackStackEntryAt(count - 1);
                        String tag = backStackEntry.getName();

                        if (tag.equals("oneline_board") || tag.equals("oneline_board_comment")) toolbarTitle.setText("한줄 게시판");
                        else if (tag.equals("solution_cloud")) toolbarTitle.setText("솔루션 클라우드");
                        else if (tag.equals("school_calendar")) toolbarTitle.setText("학사일정");
                        else if (tag.equals("book_search")) toolbarTitle.setText("도서 검색");
                        else if (tag.equals("menu")) toolbarTitle.setText("주간식단");
                        else if (tag.equals("paper_search")) toolbarTitle.setText("학위논문 검색");
                        else if (tag.equals("about")) toolbarTitle.setText("About");
                    }

                    menuCapture.setVisibility(View.INVISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(false);
                    menu.findItem(R.id.action_share).setVisible(false);
                } else if (position == 1) {
                    toolbarTitle.setText("연앱");
                    menuCapture.setVisibility(View.INVISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(false);
                    Log.d(MainActivity.class.getSimpleName(), String.valueOf(tabPagerAdapter.getItem(tabPosition).getChildFragmentManager().getBackStackEntryCount()));
                    if (tabPagerAdapter.getItem(tabPosition).getChildFragmentManager().getBackStackEntryCount() == 0) {
                        menu.findItem(R.id.action_share).setVisible(false);
                    } else {
                        menu.findItem(R.id.action_share).setVisible(true);
                    }
                } else if (position == 2) {
                    menuCapture.setVisibility(View.VISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(true);
                    menu.findItem(R.id.action_share).setVisible(false);
                } else {
                    menuCapture.setVisibility(View.INVISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(false);
                    menu.findItem(R.id.action_share).setVisible(false);
                }
            }
        });

        // Set TabSelectedListener
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                tabPosition = tab.getPosition();
                final TextView toolbarTitle = (TextView) findViewById(R.id.toolbarTitle);

                menu.findItem(R.id.action_share).setVisible(false);

                if (tabPosition == 0) {
                    int count;
                    if ((count = tabPagerAdapter.getItem(0).getChildFragmentManager().getBackStackEntryCount()) > 0) {
                        FragmentManager.BackStackEntry backStackEntry = tabPagerAdapter.getItem(0).getChildFragmentManager().getBackStackEntryAt(count - 1);
                        String tag = backStackEntry.getName();

                        if (tag.equals("oneline_board") || tag.equals("oneline_board_comment")) toolbarTitle.setText("한줄 게시판");
                        else if (tag.equals("solution_cloud")) toolbarTitle.setText("솔루션 클라우드");
                        else if (tag.equals("school_calendar")) toolbarTitle.setText("학사일정");
                        else if (tag.equals("book_search")) toolbarTitle.setText("도서 검색");
                        else if (tag.equals("menu")) toolbarTitle.setText("주간식단");
                        else if (tag.equals("paper_search")) toolbarTitle.setText("학위논문 검색");
                        else if (tag.equals("about")) toolbarTitle.setText("About");
                    }

                    menuCapture.setVisibility(View.INVISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(false);
                    menu.findItem(R.id.action_share).setVisible(false);
                } else if (tabPosition == 1) {
                    toolbarTitle.setText("연앱");
                    menuCapture.setVisibility(View.INVISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(false);
                    if (tabPagerAdapter.getItem(tabPosition).getChildFragmentManager().getBackStackEntryCount() == 0) {
                        menu.findItem(R.id.action_share).setVisible(false);
                    } else {
                        menu.findItem(R.id.action_share).setVisible(true);
                    }
                } else if (tabPosition == 2) {
                    toolbarTitle.setText("연앱");
                    menuCapture.setVisibility(View.VISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(true);
                    menu.findItem(R.id.action_share).setVisible(false);
                } else {
                    toolbarTitle.setText("연앱");
                    menuCapture.setVisibility(View.INVISIBLE);
                    menu.findItem(R.id.action_sync).setVisible(false);
                    menu.findItem(R.id.action_share).setVisible(false);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tabPosition);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!AWSMobileClient.defaultMobileClient().getIdentityManager().isUserSignedIn()) {
            // In the case that the activity is restarted by the OS after the application
            // is killed we must redirect to the splash activity to handle the sign-in flow.
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        final AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();

        // pause/resume Mobile Analytics collection
        awsMobileClient.handleOnResume();

        // register notification receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver,
            new IntentFilter(PushListenerService.ACTION_SNS_NOTIFICATION));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                new Thread() {
                    public void run() {
                        tabPagerAdapter.getItem(tabPosition).getChildFragmentManager()
                                .beginTransaction()
                                .replace(tabPagerAdapter.getItem(tabPosition).getView().getId(), new SettingsFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                }.start();
                break;

            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);
        // Save the title so it will be restored properly to match the view loaded when rotation
        // was changed or in case the activity was destroyed.
        if (toolbar != null) {
            bundle.putCharSequence(BUNDLE_KEY_TOOLBAR_TITLE, toolbar.getTitle());
        }
    }

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ArrayList<String> arrayList = intent.getStringArrayListExtra(PushListenerService.INTENT_SNS_NOTIFICATION_DATA);

            final NormalDialog dialog = new NormalDialog(MainActivity.this);
            dialog.style(NormalDialog.STYLE_TWO)
                    .bgColor(Color.parseColor("#383838"))//
                    .cornerRadius(5)//
                    .btnText("취소", "확인")
                    .title(arrayList.get(1))
                    .content(arrayList.get(2))//
                    .contentGravity(Gravity.CENTER)//
                    .contentTextColor(Color.parseColor("#ffffff"))//
                    .dividerColor(Color.parseColor("#222222"))//
                    .btnTextSize(15.5f, 15.5f)//
                    .btnTextColor(Color.parseColor("#ffffff"), Color.parseColor("#ffffff"))//
                    .btnPressColor(Color.parseColor("#2B2B2B"))//
                    .widthScale(0.85f)//
                    .show();

            dialog.setOnBtnClickL(
                    new OnBtnClickL() {
                        @Override
                        public void onBtnClick() {
                            dialog.dismiss();
                        }
                    },
                    new OnBtnClickL() {
                        @Override
                        public void onBtnClick() {
                            dialog.dismiss();

                            Intent restartIntent = getIntent();
                            restartIntent.putStringArrayListExtra("arrayList", arrayList);
                            finish();
                            startActivity(restartIntent);
                        }
                    });
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        // Obtain a reference to the mobile client.
        final AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();

        // pause/resume Mobile Analytics collection
        awsMobileClient.handleOnPause();

        // unregister notification receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    @Override
    public void onBackPressed() {

        int count;
        if ((count = tabPagerAdapter.getItem(tabPosition).getChildFragmentManager().getBackStackEntryCount()) > 0) {
            tabPagerAdapter.getItem(tabPosition).getChildFragmentManager().popBackStack();

            if (tabPosition == 1 && count == 2) {
                menu.findItem(R.id.action_share).setVisible(true);
            }
        } else {
            backPressCloseHandler.onBackPressed();
        }
    }
}