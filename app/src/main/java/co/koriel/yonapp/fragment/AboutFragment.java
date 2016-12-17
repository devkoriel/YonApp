package co.koriel.yonapp.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.flyco.dialog.entity.DialogMenuItem;
import com.flyco.dialog.listener.OnOperItemClickL;
import com.flyco.dialog.widget.NormalListDialog;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import co.koriel.yonapp.R;
import co.koriel.yonapp.helper.InAppBillingHelper;

public class AboutFragment extends FragmentBase {
    private final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiP6nqx4fVvwYlTGsdxt47jMBeUDeH1WpfW3ZIHebNLAW8+2ngq5FQ0Q64c13hYfNXCQWyhiC3daMSwK5rExQAoUAXtO68UazkPdWAmWHLzf/xPQV3cryV6uru3wsurmhE+3sTdpRomaMivRlm5TJdr4VNExgNm9UCtdenqfjFoTVAiyt/5+Q+wOsaMhct5p+tA1wTgyX//L8c4MRnPg/3I7AHcI74x+90yjFFgzwxM+NHBnLpEzF5yyPB7IMS/9oZhqWNKrD4/xMNHY3dCATqoZGBhHLNYpiLNNQ5vXu6x3Y1zLGNZVIg6LF1+niw3v3k8f9uifJoBmkff6zORcfpwIDAQAB";
    private InAppBillingHelper inAppBillingHelper;

    private ArrayList<HashMap<String, String>> arrayList;
    private SimpleAdapter simpleAdapter;

    private final String marketLink = "https://play.google.com/store/apps/details?id=co.koriel.yonapp";
    private String currentVersion;
    private String lastestVersion;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.home_menu_about);

        arrayList = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(getContext(), arrayList, android.R.layout.simple_list_item_2, new String[]{"item1", "item2"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setPadding(0,8,0,0);
                text1.setTextSize(15);
                text2.setPadding(0,15,0,0);
                text2.setTextSize(12);
                return view;
            }
        };
        
        ListView aboutList = (ListView) view.findViewById(R.id.about_list);
        View header = getLayoutInflater(savedInstanceState).inflate(R.layout.about_list_header, null, false);
        aboutList.addHeaderView(header);
        aboutList.setAdapter(simpleAdapter);
        aboutList.setOnItemClickListener(OnClickListItem);
        aboutList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        try {
            currentVersion = getActivity().getApplicationContext().getPackageManager().getPackageInfo(getActivity().getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            currentVersion = "오류 발생";
        }

        String[] strings = {"버전 정보", "현재 버전: " + currentVersion + "\n최신 버전: 확인 중...\n",
                "개발자 정보", "연세대학교 물리학과 허진수(Daniel Heo)",
                "개발자 블로그", "KORIEL's Blog",
                "연락하기", "문제점이나 건의사항이 있다면 연락주세요",
                "후원하기", "더 나은 연앱을 위해 기여할 수 있습니다"};

        for (int i = 0; i < 5; i++) {
            HashMap<String, String> map = new HashMap<> ();
            map.put("item1", strings[i * 2]);
            map.put("item2", strings[i * 2 + 1]);
            arrayList.add(map);
        }

        simpleAdapter.notifyDataSetChanged();

        VersionChecker versionChecker = new VersionChecker();
        versionChecker.execute();

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private AdapterView.OnItemClickListener OnClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            if (position == 1) {
                if (currentVersion.equals(lastestVersion)) {
                    new MaterialDialog.Builder(getContext())
                            .content(R.string.lastest_version)
                            .positiveText(R.string.dialog_ok)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else if (lastestVersion != null && !currentVersion.equals("오류 발생")) {
                    new MaterialDialog.Builder(getContext())
                            .content(R.string.check_update)
                            .positiveText(R.string.dialog_ok)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();

                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketLink));
                                    startActivity(intent);
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
                }
            } else if (position == 3) {
                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_link_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.check_blog)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();

                                String string ="https://koriel.co";
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(string));
                                startActivity(intent);
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
            } else if (position == 4) {
                new Thread() {
                    public void run() {
                        final ArrayList<DialogMenuItem> mMenuItems = new ArrayList<>();

                        mMenuItems.add(new DialogMenuItem("전화하기", R.drawable.ic_call_black_24dp));
                        mMenuItems.add(new DialogMenuItem("문자 보내기", R.drawable.ic_sms_black_24dp));
                        mMenuItems.add(new DialogMenuItem("이메일 보내기", R.drawable.ic_email_black_24dp));

                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final NormalListDialog dialog = new NormalListDialog(getContext(), mMenuItems);
                                    dialog.title("연락하기")
                                            .show();
                                    dialog.setOnOperItemClickL(new OnOperItemClickL() {
                                        @Override
                                        public void onOperItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            dialog.dismiss();

                                            switch (position) {
                                                case 0:
                                                    Uri uri = Uri.parse("tel:01089849546");
                                                    Intent it = new Intent(Intent.ACTION_DIAL, uri);
                                                    startActivity(it);
                                                    break;
                                                case 1:
                                                    Uri uri2 = Uri.parse("smsto:01089849546");
                                                    Intent it2 = new Intent(Intent.ACTION_SENDTO, uri2);
                                                    it2.putExtra("sms_body", "[연앱]");
                                                    startActivity(it2);
                                                    break;
                                                case 2:
                                                    try {
                                                        Uri uri3 = Uri.parse("mailto:danielheo94@gmail.com");
                                                        Intent it3 = new Intent(Intent.ACTION_SENDTO, uri3);
                                                        startActivity(it3);
                                                    } catch (ActivityNotFoundException e) {
                                                        e.printStackTrace();
                                                        Toast.makeText(getActivity(), "이메일을 보낼 수 있는 앱이 없습니다", Toast.LENGTH_SHORT).show();
                                                    }
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                    });
                                }
                            });
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            } else if (position == 5) {
                inAppBillingHelper = new InAppBillingHelper(getActivity(), base64EncodedPublicKey);
                final String[] skuList = {"donate_1000", "donate_5000", "donate_10000", "donate_50000", "donate_100000"};

                new Thread() {
                    public void run() {
                        final ArrayList<DialogMenuItem> mMenuItems = new ArrayList<>();
                        mMenuItems.add(new DialogMenuItem("1,000원 후원", R.drawable.ic_thumb_up_black_24dp));
                        mMenuItems.add(new DialogMenuItem("5,000원 후원", R.drawable.ic_thumb_up_black_24dp));
                        mMenuItems.add(new DialogMenuItem("10,000원 후원", R.drawable.ic_thumb_up_black_24dp));
                        mMenuItems.add(new DialogMenuItem("50,000원 후원", R.drawable.ic_thumb_up_black_24dp));
                        mMenuItems.add(new DialogMenuItem("100,000원 후원", R.drawable.ic_thumb_up_black_24dp));

                        try {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final NormalListDialog dialog = new NormalListDialog(getContext(), mMenuItems);
                                    dialog.title("후원하기")
                                            .show();
                                    dialog.setOnOperItemClickL(new OnOperItemClickL() {
                                        @Override
                                        public void onOperItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            dialog.dismiss();
                                            inAppBillingHelper.buy(skuList[position]);
                                        }
                                    });
                                }
                            });
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }
    };

    public class VersionChecker extends AsyncTask<String, String, String> {

        String newVersion;

        @Override
        protected String doInBackground(String... params) {

            try {
                newVersion = Jsoup.connect(marketLink + "&hl=en")
                        .timeout(30000)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get()
                        .select("div[itemprop=softwareVersion]")
                        .first()
                        .ownText();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return newVersion;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            updateVersion(result);
        }
    }

    private void updateVersion(String version) {
        lastestVersion = version;
        arrayList.get(0).put("item2", "현재 버전: " + currentVersion + "\n최신 버전: " + lastestVersion + "\n");
        simpleAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (inAppBillingHelper != null) {
            inAppBillingHelper.destroy();
        }
    }
}
