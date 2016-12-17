package co.koriel.yonapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceFragmentCompatFix;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.models.nosql.OnelineNicknameDO;
import co.koriel.yonapp.R;


public class SettingsFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener {
    private EditTextPreference editTextPreference;
    private DynamoDBMapper dynamoDBMapper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText(R.string.home_menu_settings);

        dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        editTextPreference = (EditTextPreference) findPreference("oneline_static_nickname");
    }

    @Override
    public void onResume() {
        super.onResume();

        new Thread() {
            public void run() {
                OnelineNicknameDO nicknameToFind = new OnelineNicknameDO();
                nicknameToFind.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());

                DynamoDBQueryExpression queryExpressionNickname = new DynamoDBQueryExpression()
                        .withIndexName("userId-index")
                        .withHashKeyValues(nicknameToFind)
                        .withConsistentRead(false);

                final PaginatedQueryList<OnelineNicknameDO> nicknameQueryList = dynamoDBMapper.query(OnelineNicknameDO.class, queryExpressionNickname);



                if (nicknameQueryList.size() > 0) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String nickname = nicknameQueryList.get(0).getNickname();
                            getPreferenceManager().getSharedPreferences().edit().putString("oneline_static_nickname", nickname).apply();
                            editTextPreference.setText(nickname);
                        }
                    });
                }
            }
        }.start();

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String s) {
        if (s.equals("oneline_static_nickname")) {
            final String userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
            final String nickname = sharedPreferences.getString(s, "");

            if (nickname.equals("")) {
                new Thread() {
                    public void run() {
                        OnelineNicknameDO onelineNicknameDO = new OnelineNicknameDO();
                        onelineNicknameDO.setUserId(userId);
                        dynamoDBMapper.delete(onelineNicknameDO);
                    }
                }.start();

                return;
            } else if (nickname.contains(" ")) {
                sharedPreferences.edit().remove(s).apply();
                editTextPreference.setText("");

                new MaterialDialog.Builder(getContext())
                        .iconRes(R.drawable.ic_error_outline_black_48dp)
                        .limitIconToDefaultSize()
                        .title(R.string.cannot_include_space)
                        .content(R.string.please_register_again)
                        .positiveText(R.string.dialog_ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();

                return;
            }

            final MaterialDialog pDialog = new MaterialDialog.Builder(getContext())
                    .title(R.string.registering)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .progressIndeterminateStyle(false)
                    .show();

             new Thread() {
                 public void run() {
                     OnelineNicknameDO nicknameToFind = new OnelineNicknameDO();
                     nicknameToFind.setNickname(nickname);

                     DynamoDBQueryExpression queryExpressionNickname = new DynamoDBQueryExpression()
                             .withIndexName("nickname-index")
                             .withHashKeyValues(nicknameToFind)
                             .withConsistentRead(false);

                     final PaginatedQueryList<OnelineNicknameDO> nicknameQueryList = dynamoDBMapper.query(OnelineNicknameDO.class, queryExpressionNickname);

                     if (nicknameQueryList.size() > 0) {
                         if (!nicknameQueryList.get(0).getUserId().equals(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID())) {
                             sharedPreferences.edit().remove(s).apply();
                             editTextPreference.setText("");
                             getActivity().runOnUiThread(new Runnable() {
                                 @Override
                                 public void run() {
                                     pDialog.dismiss();
                                     new MaterialDialog.Builder(getContext())
                                             .iconRes(R.drawable.ic_error_outline_black_48dp)
                                             .limitIconToDefaultSize()
                                             .title(R.string.already_occupied_nickname)
                                             .content(R.string.please_register_again)
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
                         } else {
                             getActivity().runOnUiThread(new Runnable() {
                                 @Override
                                 public void run() {
                                     pDialog.dismiss();
                                 }
                             });
                         }
                     } else {
                         OnelineNicknameDO onelineNicknameDO = new OnelineNicknameDO();
                         onelineNicknameDO.setUserId(userId);
                         onelineNicknameDO.setNickname(nickname);

                         dynamoDBMapper.save(onelineNicknameDO);

                         getActivity().runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                                 pDialog.dismiss();
                                 new MaterialDialog.Builder(getContext())
                                         .iconRes(R.drawable.ic_done_black_48dp)
                                         .limitIconToDefaultSize()
                                         .title(R.string.registered)
                                         .content(R.string.social_gathering_warning)
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
             }.start();
        }
    }
}
