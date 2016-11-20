package co.koriel.yonapp.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.mobile.push.PushManager;
import co.amazonaws.mobile.push.SnsTopic;
import co.koriel.yonapp.R;


public class SettingsFragment extends FragmentBase {
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1363;
    private PushManager pushManager;

    private CheckBox enablePushCheckBox;
    private ListView topicsListView;
    private ArrayAdapter<SnsTopic> topicsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_demo_push, container, false);
        enablePushCheckBox = (CheckBox) view.findViewById(R.id.push_demo_enable_push_checkbox);
        topicsListView = (ListView) view.findViewById(R.id.push_demo_topics_list);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("환경설정");

        pushManager = AWSMobileClient.defaultMobileClient().getPushManager();

        enablePushCheckBox.setChecked(pushManager.isPushEnabled());
        enablePushCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNotification(enablePushCheckBox.isChecked());
            }
        });

        topicsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SnsTopic snsTopic = topicsAdapter.getItem(position);
                toggleSubscription(snsTopic, true);
            }
        });

        topicsAdapter = new ArrayAdapter<SnsTopic>(getActivity(),
                R.layout.list_item_text_with_checkbox) {
            @Override
            public View getView(final int position, final View convertView,
                                final ViewGroup parent) {
                final CheckedTextView view = (CheckedTextView) super.getView(position, convertView,
                        parent);
                view.setChecked(getItem(position).isSubscribed());
                view.setEnabled(pushManager.isPushEnabled());
                return view;
            }

            @Override
            public boolean isEnabled(final int position) {
                return pushManager.isPushEnabled();
            }
        };
        topicsAdapter.addAll(pushManager.getTopics().values());
        topicsListView.setAdapter(topicsAdapter);

        final GoogleApiAvailability api = GoogleApiAvailability.getInstance();

        final int code = api.isGooglePlayServicesAvailable(getActivity());

        if (ConnectionResult.SUCCESS != code) {
            final String errorString = api.getErrorString(code);

            if (api.isUserResolvableError(code)) {
                api.showErrorDialogFragment(getActivity(), code, REQUEST_GOOGLE_PLAY_SERVICES);
                enablePushCheckBox.setEnabled(false);
                return;
            } else {
                showErrorMessage(R.string.push_demo_error_message_google_play_services_unavailable);
                enablePushCheckBox.setEnabled(false);
                return;
            }
        }
    }

    private void toggleNotification(final boolean enabled) {
        final ProgressDialog dialog = showWaitingDialog(
                R.string.push_demo_wait_message_update_notification);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                // register device first to ensure we have a push endpoint.
                pushManager.registerDevice();

                // if registration succeeded.
                if (pushManager.isRegistered()) {
                    try {
                        pushManager.setPushEnabled(enabled);
                        // Automatically subscribe to the default SNS topic
                        if (enabled) {
                            pushManager.subscribeToTopic(pushManager.getDefaultTopic());
                        }
                        return null;
                    } catch (final AmazonClientException ace) {
                        return ace.getMessage();
                    }
                }
                return "Failed to register for push notifications.";
            }

            @Override
            protected void onPostExecute(final String errorMessage) {
                dialog.dismiss();
                topicsAdapter.notifyDataSetChanged();
                enablePushCheckBox.setChecked(pushManager.isPushEnabled());

                if (errorMessage != null) {
                    showErrorMessage(R.string.push_demo_error_message_update_notification,
                            errorMessage);
                }
            }
        }.execute();
    }

    private void toggleSubscription(final SnsTopic snsTopic, final boolean showConfirmation) {
        if (snsTopic.isSubscribed() && showConfirmation) {
            new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.push_demo_confirm_message_unsubscribe,
                            snsTopic.getDisplayName()))
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            toggleSubscription(snsTopic, false);
                        }
                    })
                    .show();
            return;
        }

        final ProgressDialog dialog = showWaitingDialog(
                R.string.push_demo_wait_message_update_subscription);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                try {
                    if (snsTopic.isSubscribed()) {
                        pushManager.unsubscribeFromTopic(snsTopic);
                    } else {
                        pushManager.subscribeToTopic(snsTopic);
                    }
                    return null;
                } catch (final AmazonClientException ace) {
                    return ace.getMessage();
                }
            }

            @Override
            protected void onPostExecute(final String errorMessage) {
                dialog.dismiss();
                topicsAdapter.notifyDataSetChanged();

                if (errorMessage != null) {
                    showErrorMessage(R.string.push_demo_error_message_update_subscription,
                            errorMessage);
                }
            }
        }.execute();
    }

    private AlertDialog showErrorMessage(final int resId, final Object... args) {
        return new AlertDialog.Builder(getActivity()).setMessage(getString(resId, (Object[]) args))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private ProgressDialog showWaitingDialog(final int resId, final Object... args) {
        return ProgressDialog.show(getActivity(),
                getString(R.string.push_demo_progress_dialog_title),
                getString(resId, (Object[]) args));
    }
}
