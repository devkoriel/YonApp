package co.koriel.yonapp.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.models.nosql.OnelineBoardDO;
import co.amazonaws.models.nosql.OnelineNicknameDO;
import co.koriel.yonapp.PushListenerService;
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
        ImageButton settingsButton = (ImageButton) view.findViewById(R.id.settings_button);

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
        settingsButton.setOnClickListener(this);

        final ArrayList<String> arrayList = getActivity().getIntent().getStringArrayListExtra("arrayList");
        if (arrayList != null && arrayList.get(0).equals(PushListenerService.TYPE_ONELINE_NEW_COMMENT)) {
            getActivity().getIntent().removeExtra("arrayList");
            new Thread() {
                public void run() {
                    try {
                        getFragmentManager().beginTransaction()
                                .replace(R.id.home_root_container, new OneLineBoardFragment())
                                .addToBackStack(null)
                                .commit();

                        Bundle bundle = new Bundle();
                        String contentDateAndId = arrayList.get(3);
                        bundle.putString("ContentDateAndId", contentDateAndId);
                        bundle.putString("Content", arrayList.get(5));
                        bundle.putString("Timestamp", arrayList.get(6));
                        bundle.putString("writerGcmTokenKey", arrayList.get(4));

                        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                        expressionAttributeValues.put(":dateAndIdToFind", new AttributeValue().withS(contentDateAndId));

                        DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
                        DynamoDBScanExpression scanExpressionOneline = new DynamoDBScanExpression()
                                .withFilterExpression("DateAndId = :dateAndIdToFind")
                                .withExpressionAttributeValues(expressionAttributeValues);

                        PaginatedScanList<OnelineBoardDO> onelineBoardDOs = dynamoDBMapper.scan(OnelineBoardDO.class, scanExpressionOneline);
                        boolean isNickStatic = onelineBoardDOs.get(0).isNickStatic();
                        bundle.putString("isNickStatic", Boolean.toString(isNickStatic));

                        if (isNickStatic) {
                            expressionAttributeValues.clear();
                            expressionAttributeValues.put(":userIdToFind", new AttributeValue().withS(contentDateAndId.split("]")[1]));

                            DynamoDBScanExpression scanExpressionNickname = new DynamoDBScanExpression()
                                    .withFilterExpression("userId = :userIdToFind")
                                    .withExpressionAttributeValues(expressionAttributeValues);

                            PaginatedScanList<OnelineNicknameDO> onelineNicknameDOs = dynamoDBMapper.scan(OnelineNicknameDO.class, scanExpressionNickname);

                            if (onelineNicknameDOs.size() > 0) {
                                bundle.putString("id", onelineNicknameDOs.get(0).getNickname());
                            } else {
                                bundle.putString("id", contentDateAndId.split("-")[5].split(":")[1]);
                            }
                        } else {
                            bundle.putString("id", contentDateAndId.split("-")[5].split(":")[1]);
                        }

                        OneLineBoardCommentFragment oneLineBoardCommentFragment = new OneLineBoardCommentFragment();
                        oneLineBoardCommentFragment.setArguments(bundle);

                        getFragmentManager().beginTransaction()
                                .replace(R.id.home_root_container, oneLineBoardCommentFragment)
                                .addToBackStack(null)
                                .commit();
                    } catch (NullPointerException|IndexOutOfBoundsException e) {
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
                                .addToBackStack(getResources().getString(R.string.home_menu_oneline))
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
                                .addToBackStack(getResources().getString(R.string.home_menu_solution_cloud))
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
                                .addToBackStack(getResources().getString(R.string.home_menu_school_schedule))
                                .commit();
                    }
                }.start();
                break;
            case R.id.book_search_button:
                new Thread() {
                    public void run() {
                        final Bundle bundle = new Bundle();
                        bundle.putString("option", getResources().getString(R.string.home_menu_book_search));

                        LibrarySearchFragment librarySearchFragment = new LibrarySearchFragment();
                        librarySearchFragment.setArguments(bundle);
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, librarySearchFragment)
                                .addToBackStack(getResources().getString(R.string.home_menu_book_search))
                                .commit();
                    }
                }.start();
                break;
            case R.id.paper_button:
                new Thread() {
                    public void run() {
                        final Bundle bundle = new Bundle();
                        bundle.putString("option", getResources().getString(R.string.home_menu_paper_search));

                        LibrarySearchFragment librarySearchFragment = new LibrarySearchFragment();
                        librarySearchFragment.setArguments(bundle);
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, librarySearchFragment)
                                .addToBackStack(getResources().getString(R.string.home_menu_paper_search))
                                .commit();
                    }
                }.start();
                break;
            case R.id.bus_button:
                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_link_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.check_bus_page)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();

                                final String sBus ="http://www.yonsei.ac.kr/sc/campus/traffic1.jsp";
                                Intent intentB = new Intent(Intent.ACTION_VIEW, Uri.parse(sBus));
                                startActivity(intentB);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.convenience_button:
                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_link_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.check_convenience_page)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();

                                String sConvenience ="http://www.yonsei.ac.kr/sc/campus/convenience1.jsp?default:category_id=23&type=";
                                Intent intentC = new Intent(Intent.ACTION_VIEW, Uri.parse(sConvenience));
                                startActivity(intentC);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.about_button:
                new Thread() {
                    public void run() {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.home_root_container, new AboutFragment())
                                .addToBackStack(getResources().getString(R.string.home_menu_about))
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
                                .addToBackStack(getResources().getString(R.string.home_menu_menu))
                                .commit();
                    }
                }.start();
                break;
            case R.id.logout_button:
                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_exit_to_app_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.check_logout)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();

                                getActivity().finish();
                                startActivity(new Intent(getContext(), SignInActivity.class));
                                AWSMobileClient.defaultMobileClient().getIdentityManager().signOut();
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.settings_button:
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.home_root_container, new SettingsFragment())
                        .addToBackStack(getResources().getString(R.string.home_menu_settings))
                        .commit();
                break;
            default:
                break;
        }
    }
}
