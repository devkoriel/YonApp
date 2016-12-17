package co.koriel.yonapp.helper;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONObject;

import java.util.ArrayList;

import co.koriel.yonapp.util.IabHelper;
import co.koriel.yonapp.util.IabResult;
import co.koriel.yonapp.util.Purchase;

public class InAppBillingHelper {
    private String PUBLIC_KEY;

    private final int REQUEST_CODE = 1001;

    private Activity mActivity;
    private IInAppBillingService mService;
    private IabHelper mHelper;

    public InAppBillingHelper(Activity _act, String _public_key) {
        this.mActivity = _act;
        this.PUBLIC_KEY = _public_key;
        init();
    }

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    public void init() {
        Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        intent.setPackage("com.android.vending");
        mActivity.bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);

        mHelper = new IabHelper(mActivity, PUBLIC_KEY);
        mHelper.enableDebugLogging(true);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Toast.makeText(mActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                }
                AlreadyPurchaseItems();
            }
        });
    }

    public void destroy() {
        if (mServiceConn != null)
            mActivity.unbindService(mServiceConn);
    }

    public void AlreadyPurchaseItems() {
        try {
            Bundle ownedItems = mService.getPurchases(3, mActivity.getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");

            if (response == 0) {
                ArrayList purchaseDataList  = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                String[] tokens = new String[purchaseDataList.size()];

                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = (String)purchaseDataList.get(i);
                    JSONObject jo   = new JSONObject(purchaseData);
                    tokens[i]   = jo.getString("purchaseToken");

                    mService.consumePurchase(3, mActivity.getPackageName(), tokens[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buy(String itemId) {
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, mActivity.getPackageName(), itemId, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

            if (pendingIntent != null) {
                mHelper.launchPurchaseFlow(mActivity, itemId, REQUEST_CODE, mPurchaseFinishedListener, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
            } else {
                Toast.makeText(mActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(mActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener   = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isFailure()) {
                AlreadyPurchaseItems();
            } else {
                Toast.makeText(mActivity, "후원해주셔서 감사합니다", Toast.LENGTH_SHORT).show();

                AlreadyPurchaseItems();
            }
        }
    };

    public void activityResult(int _requestCode, int _resultCode, Intent _data) {
        if (mHelper == null)
            return;

        if (!mHelper.handleActivityResult(_requestCode, _resultCode, _data))
            return;
    }
}
