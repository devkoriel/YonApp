package co.koriel.yonapp.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.flyco.dialog.listener.OnBtnClickL;
import com.flyco.dialog.widget.NormalDialog;

import java.util.ArrayList;

import co.amazonaws.mobile.AWSMobileClient;
import co.koriel.yonapp.R;
import co.koriel.yonapp.SignInActivity;
import co.koriel.yonapp.util.NetworkUtil;

public class HomeFragment extends FragmentBase implements View.OnClickListener{

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.app_name);

        return inflater.inflate(R.layout.fragment_demo_home, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton oneLineBoardButton = (ImageButton) view.findViewById(R.id.one_line_board_button);
        ImageButton solutionCloudButton = (ImageButton) view.findViewById(R.id.solution_cloud_button);
        ImageButton schoolCalendarButton = (ImageButton) view.findViewById(R.id.school_calendar_button);
        ImageButton bookSearchButton = (ImageButton) view.findViewById(R.id.book_search_button);
        ImageButton busButton = (ImageButton) view.findViewById(R.id.bus_button);
        ImageButton convenienceButton = (ImageButton) view.findViewById(R.id.convenience_button);
        ImageButton aboutButton = (ImageButton) view.findViewById(R.id.about_button);
        ImageButton menuButton = (ImageButton) view.findViewById(R.id.menu_button);
        ImageButton paperButton = (ImageButton) view.findViewById(R.id.paper_button);
        ImageButton logoutButton = (ImageButton) view.findViewById(R.id.logout_button);

        oneLineBoardButton.setOnClickListener(this);
        solutionCloudButton.setOnClickListener(this);
        schoolCalendarButton.setOnClickListener(this);
        bookSearchButton.setOnClickListener(this);
        busButton.setOnClickListener(this);
        convenienceButton.setOnClickListener(this);
        aboutButton.setOnClickListener(this);
        menuButton.setOnClickListener(this);
        paperButton.setOnClickListener(this);
        logoutButton.setOnClickListener(this);

        final ArrayList<String> arrayList = getActivity().getIntent().getStringArrayListExtra("arrayList");
        getActivity().getIntent().removeExtra("arrayList");
        if (arrayList != null) {
            new Thread() {
                public void run() {
                    try {
                        getFragmentManager().beginTransaction()
                                .replace(R.id.home_root_container, new OneLineBoardFragment())
                                .addToBackStack(null)
                                .commit();

                        Bundle bundle = new Bundle();
                        bundle.putString("ContentDateAndId", arrayList.get(3));
                        bundle.putString("Content", arrayList.get(5));
                        bundle.putString("Timestamp", arrayList.get(6));
                        bundle.putString("writerGcmTokenKey", arrayList.get(4));

                        OneLineBoardCommentFragment oneLineBoardCommentFragment = new OneLineBoardCommentFragment();
                        oneLineBoardCommentFragment.setArguments(bundle);

                        getFragmentManager().beginTransaction()
                                .replace(R.id.home_root_container, oneLineBoardCommentFragment)
                                .addToBackStack(null)
                                .commit();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    @Override
    public void onClick(View v) {
        if(!NetworkUtil.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), "인터넷에 연결해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (v.getId()) {
            case R.id.one_line_board_button:
                new Thread() {
                    public void run() {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, new OneLineBoardFragment())
                                .addToBackStack("oneline_board")
                                .commit();
                    }
                }.start();
                break;
            case R.id.solution_cloud_button:
                new Thread() {
                    public void run() {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, new ContentDeliveryFragment())
                                .addToBackStack("solution_cloud")
                                .commit();
                    }
                }.start();
                break;
            case R.id.school_calendar_button:
                new Thread() {
                    public void run() {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, new SchoolCalendarFragment())
                                .addToBackStack("school_calendar")
                                .commit();
                    }
                }.start();
                break;
            case R.id.book_search_button:
                new Thread() {
                    public void run() {
                        final Bundle bundle = new Bundle();
                        bundle.putString("option", "도서 검색");

                        LibrarySearchFragment librarySearchFragment = new LibrarySearchFragment();
                        librarySearchFragment.setArguments(bundle);
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, librarySearchFragment)
                                .addToBackStack("book_search")
                                .commit();
                    }
                }.start();
                break;
            case R.id.paper_button:
                new Thread() {
                    public void run() {
                        final Bundle bundle = new Bundle();
                        bundle.putString("option", "학위논문 검색");

                        LibrarySearchFragment librarySearchFragment = new LibrarySearchFragment();
                        librarySearchFragment.setArguments(bundle);
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, librarySearchFragment)
                                .addToBackStack("paper_search")
                                .commit();
                    }
                }.start();
                break;
            case R.id.bus_button:
                final String sBus ="http://www.yonsei.ac.kr/sc/campus/traffic1.jsp";
                Intent intentB = new Intent(Intent.ACTION_VIEW, Uri.parse(sBus));
                startActivity(intentB);
                break;
            case R.id.convenience_button:
                String sConvenience ="http://www.yonsei.ac.kr/sc/campus/convenience1.jsp?default:category_id=23&type=";
                Intent intentC = new Intent(Intent.ACTION_VIEW, Uri.parse(sConvenience));
                startActivity(intentC);
                break;
            case R.id.about_button:
                new Thread() {
                    public void run() {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, new AboutFragment())
                                .addToBackStack("about")
                                .commit();
                    }
                }.start();
                break;
            case R.id.menu_button:
                new Thread() {
                    public void run() {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, new MenuFragment())
                                .addToBackStack("menu")
                                .commit();
                    }
                }.start();
                break;
            case R.id.logout_button:
                final NormalDialog dialog = new NormalDialog(getContext());
                dialog.isTitleShow(false)
                        .bgColor(Color.parseColor("#383838"))//
                        .cornerRadius(5)//
                        .btnText("취소", "확인")
                        .content("로그아웃 할까요?")//
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

                                getActivity().finish();
                                AWSMobileClient.defaultMobileClient().getIdentityManager().signOut();
                                startActivity(new Intent(getContext(), SignInActivity.class));
                            }
                        });
                break;
            default:
                break;
        }
    }
}
