package org.robotv.timers.activity;

import android.os.Bundle;

import org.robotv.dataservice.DataService;
import org.robotv.timers.fragment.CreateTimerFragment;
import org.robotv.client.model.Movie;
import org.robotv.robotv.R;
import org.robotv.timers.fragment.EpgSearchFragment;
import org.robotv.ui.DataServiceActivity;
import org.robotv.ui.MovieStepFragment;

public class EpgSearchActivity extends DataServiceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epgsearch);

        EpgSearchFragment fragment = (EpgSearchFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        setServiceListener(fragment);
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
