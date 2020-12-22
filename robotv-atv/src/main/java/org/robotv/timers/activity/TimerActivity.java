package org.robotv.timers.activity;

import android.content.Intent;
import android.os.Bundle;

import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.dataservice.DataService;
import org.robotv.timers.fragment.CreateTimerFragment;
import org.robotv.timers.fragment.TimerFragment;
import org.robotv.ui.DataServiceActivity;
import org.robotv.ui.MovieStepFragment;

public class TimerActivity extends DataServiceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        TimerFragment fragment = (TimerFragment) getSupportFragmentManager().findFragmentById(R.id.container);
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

        MovieStepFragment actionFragment = new CreateTimerFragment();
        actionFragment.startGuidedStep(
                this,
                event,
                service,
                R.id.container
        );
    }

}
