package org.xvdr.timers.fragment;

import android.os.Bundle;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.recordings.model.FolderList;
import org.xvdr.robotv.R;
import org.xvdr.robotv.client.Connection;
import org.xvdr.robotv.setup.SetupUtils;

import java.util.List;

public class CreateTimerFragmentFolder extends CreateTimerStepFragment {

    protected FolderList mFolderList;

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        return createGuidance(getString(R.string.timer_add_select_folder));
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        Connection connection = new Connection("roboTV:createTimerFolders", SetupUtils.getLanguage(getActivity()));
        connection.open(SetupUtils.getServer(getActivity()));

        mFolderList = FolderList.load(connection);

        connection.close();

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

        for(String folder : mFolderList) {
            GuidedAction action = new GuidedAction.Builder(getActivity())
            .id(i++)
            .title(folder)
            .checkSetId(1)
            .checked(currentFolder.equals(folder))
            .build();

            actions.add(action);
        }

        GuidedAction customFolder = new GuidedAction.Builder(getActivity())
        .id(i)
        .title(newFolder)
        .description(getString(R.string.timer_add_create_new_folder))
        .editable(true)
        .build();

        actions.add(customFolder);

    }

    public void onGuidedActionClicked(GuidedAction action) {
        SetupUtils.setRecordingFolder(action.getTitle().toString());
        getFragmentManager().popBackStack();
    }

}
