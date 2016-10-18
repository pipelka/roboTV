package org.xvdr.timers.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.xvdr.recordings.model.Movie;
import org.xvdr.robotv.R;
import org.xvdr.robotv.service.DataService;
import org.xvdr.robotv.service.DataServiceClient;

import java.util.Collection;
import java.util.TreeSet;

public class TimerActivity extends Activity {

    private static final String TAG = "TimerActivity";
    private DataServiceClient dataClient;
    TreeSet<String> folderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start data service
        dataClient = new DataServiceClient(this, new DataServiceClient.Listener() {
            @Override
            public void onServiceConnected(DataService service) {
                folderList = service.getFolderList();
            }

            @Override
            public void onServiceDisconnected(DataService service) {
            }

            @Override
            public void onMovieCollectionUpdated(DataService service, Collection<Movie> collection) {
            }
        });

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

    public TreeSet<String> getFolderList() {
        return folderList;
    }
}
