package org.xvdr.timers.activity;

import android.content.Intent;
import android.os.Bundle;

import org.robotv.client.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.timers.fragment.CreateTimerFragment;
import org.xvdr.timers.fragment.TimerFragment;
import org.xvdr.ui.DataServiceActivity;
import org.xvdr.ui.MovieStepFragment;

public class TimerActivity extends DataServiceActivity {

    private MovieStepFragment actionFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        TimerFragment fragment = (TimerFragment) getFragmentManager().findFragmentById(R.id.timer_fragment);
        setServiceListener(fragment);
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, EpgSearchActivity.class);
        startActivity(intent);
        return true;
    }

    public void selectEvent(Movie event) {
        DataService service = getService();

        if(service == null) {
            // TODO - handle error case
            return;
        }

        actionFragment = new CreateTimerFragment();
        actionFragment.startGuidedStep(
                this,
                event,
                service,
                R.id.timer_fragment
        );
    }

}
