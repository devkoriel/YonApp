package co.koriel.yonapp.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.flyco.dialog.entity.DialogMenuItem;
import com.flyco.dialog.listener.OnOperItemClickL;
import com.flyco.dialog.widget.NormalListDialog;

import java.util.ArrayList;
import java.util.HashMap;

import co.koriel.yonapp.R;

public class AboutFragment extends FragmentBase {

    private TextView toolbarTitle;

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
        toolbarTitle.setText("About");

        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        SimpleAdapter simpleAdapter = new SimpleAdapter(getContext(), arrayList, android.R.layout.simple_list_item_2, new String[]{"item1", "item2"}, new int[]{android.R.id.text1, android.R.id.text2}) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setPadding(0,8,0,0);
                text1.setTextSize(15);
                text1.setSingleLine(true);
                text1.setEllipsize(TextUtils.TruncateAt.END);
                text2.setPadding(0,15,0,0);
                text2.setTextSize(12);
                return view;
            }
        };
        
        ListView aboutList = (ListView) view.findViewById(R.id.about_list);
        aboutList.setAdapter(simpleAdapter);
        aboutList.setOnItemClickListener(OnClickListItem);
        registerForContextMenu(aboutList);
        aboutList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        View header = getLayoutInflater(savedInstanceState).inflate(R.layout.about_list_header, null, false);
        aboutList.addHeaderView(header);

        HashMap<String, String> map = new HashMap<> ();
        HashMap<String, String> map2 = new HashMap<> ();
        HashMap<String, String> map3 = new HashMap<> ();
        HashMap<String, String> map4 = new HashMap<> ();
        map.put("item1", "버전 정보");
        try {
            map.put("item2", "연세대학교 연앱 " + getActivity().getApplicationContext().getPackageManager().getPackageInfo(getActivity().getApplicationContext().getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            map.put("item2", "오류 발생");
            e.printStackTrace();
        }
        arrayList.add(map);
        map2.put("item1", "개발자 정보");
        map2.put("item2", "연세대학교 물리학과 허진수(Daniel Heo)");
        arrayList.add(map2);
        map3.put("item1", "개발자 블로그");
        map3.put("item2", "KORIEL's Blog");
        arrayList.add(map3);
        map4.put("item1", "연락하기");
        map4.put("item2", "문제점이나 건의사항이 있다면 연락주세요");
        arrayList.add(map4);
        simpleAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private AdapterView.OnItemClickListener OnClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
            if (position == 3) {
                String string ="https://koriel.co";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(string));
                startActivity(intent);
            }
            else if (position == 4) {

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
            }
        }
    };
}
