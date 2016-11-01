package org.xvdr.timers.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;
import org.xvdr.timers.fragment.CreateTimerFragment;

public class TimerActivity extends Activity {

    private DataServiceClient dataClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start data service
        dataClient = new DataServiceClient(this, null);
        dataClient.bind();

        setContentView(R.layout.activity_timer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataClient.unbind();
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, EpgSearchActivity.class);
        startActivity(intent);
        return true;
    }

    public void selectEvent(Movie event) {
        DataService service = dataClient.getService();

        if(service == null) {
            // TODO - handle error case
            return;
        }

        new CreateTimerFragment().startGuidedStep(
                this,
                event,
                dataClient.getService(),
                R.id.timer_fragment
        );
    }

}
