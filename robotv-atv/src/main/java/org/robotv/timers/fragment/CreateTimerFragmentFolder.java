package org.robotv.timers.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.robotv.robotv.R;
import org.robotv.dataservice.DataService;
import org.robotv.setup.SetupUtils;

import java.util.List;
import java.util.TreeSet;

public class CreateTimerFragmentFolder extends CreateTimerStepFragment {

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.timer_add_select_folder));
    }

    @Override
    public void onCreateActions(@NonNull List actions, Bundle savedInstanceState) {
        String currentFolder = SetupUtils.getRecordingFolder();
        String newFolder = currentFolder.equals("") ? getString(R.string.timer_new_folder) : currentFolder;

        GuidedAction emptyFolder = new GuidedAction.Builder(getActivity())
                .id(0)
                .title(getString(R.string.timer_add_select_folder_none))
                .checkSetId(1)
                .checked(currentFolder.equals(""))
                .build();

        actions.add(emptyFolder);

        int i = 1;

        DataService service = getService();

        if(service != null) {
            TreeSet<String> folderList = getMovieController().getFolderList();

            for(String folder : folderList) {
                GuidedAction action = new GuidedAction.Builder(getActivity())
                        .id(i++)
                        .title(folder)
                        .checkSetId(1)
                        .checked(currentFolder.equals(folder))
                        .build();

                actions.add(action);
            }
        }

        GuidedAction customFolder = new GuidedAction.Builder(getActivity())
        .id(i)
        .title(newFolder)
        .description(getString(R.string.timer_add_create_new_folder))
        .editable(true)
        .build();

        actions.add(customFolder);

    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if(action.getId() != 0) {
            SetupUtils.setRecordingFolder(action.getTitle().toString());
        }
        else {
            SetupUtils.setRecordingFolder("");
        }

        getFragmentManager().popBackStack();
    }

}
