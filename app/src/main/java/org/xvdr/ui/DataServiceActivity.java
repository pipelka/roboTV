package org.xvdr.ui;

import android.app.Activity;

import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

public class DataServiceActivity extends Activity {

    private DataServiceClient dataClient;
    private DataServiceClient.Listener listener;

    protected void setServiceListener(DataServiceClient.Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();

        // start data service
        dataClient = new DataServiceClient(this, listener);
        dataClient.bind();
    }

    @Override
    public void onStop() {
        super.onStop();
        dataClient.unbind();
    }

    protected DataService getService() {
        return dataClient.getService();
    }
}
