package org.xvdr.robotv.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;


public class AccountService extends Service {
    private static final String TAG = "RobotTvAccountService";
    private DummyAuthenticator mAuthenticator;
    public static final String ACCOUNT_NAME = "RoboTvAccount";

    public static Account getAccount(String accountType) {
        return new Account(ACCOUNT_NAME, accountType);
    }

    @Override
    public void onCreate() {
        mAuthenticator = new DummyAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

    public class DummyAuthenticator extends AbstractAccountAuthenticator {
        public DummyAuthenticator(Context context) {
            super(context);
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse,
                String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse,
                String s, String s2, String[] strings, Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                Account account, Bundle bundle) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse,
                Account account, String s, Bundle bundle) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuthTokenLabel(String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                Account account, String s, Bundle bundle) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse,
                Account account, String[] strings) throws NetworkErrorException {
            throw new UnsupportedOperationException();
        }
    }

}

