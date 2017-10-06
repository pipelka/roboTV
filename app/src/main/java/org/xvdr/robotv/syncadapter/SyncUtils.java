package org.xvdr.robotv.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;

import org.xvdr.robotv.service.AccountService;

/**
 * Static helper methods for working with the SyncAdapter framework.
 */
public class SyncUtils {
    private static final String TAG = "SyncUtils";
    private static final String CONTENT_AUTHORITY = TvContract.AUTHORITY;
    private static final String ACCOUNT_TYPE = "org.xvdr.robotv.account";

    public static void setUpPeriodicSync(Context context, String inputId) {
        Account account = AccountService.getAccount(ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        if(!accountManager.addAccountExplicitly(account, null, null)) {
            Log.e(TAG, "Account already exists.");
        }

        ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);

        Bundle bundle = new Bundle();
        bundle.putString(SyncAdapter.BUNDLE_KEY_INPUT_ID, inputId);
        ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, bundle, SyncAdapter.SYNC_FREQUENCY_SEC);
    }

    public static void requestSync(String inputId, boolean skipChannels) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putString(SyncAdapter.BUNDLE_KEY_INPUT_ID, inputId);
        bundle.putBoolean("skip_channels", skipChannels);
        ContentResolver.requestSync(AccountService.getAccount(ACCOUNT_TYPE), CONTENT_AUTHORITY, bundle);
    }
}
