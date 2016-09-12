package org.xvdr.robotv.setup;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.robotv.R;

import java.util.List;

public class SetupFragmentTimeshift extends GuidedStepFragment {

    static final String TAG = "SetupFragmentTimeshift";

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getResources().getString(R.string.setup_root_timeshift_title);
        String breadcrumb = "";
        String description = getResources().getString(R.string.setup_timeshift_desc);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_timetable_white_48dp);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        boolean timeshift = SetupUtils.getTimeshiftEnabled(getActivity());

        actions.add(new GuidedAction.Builder()
                .id(0)
                .title(getString(R.string.setup_root_item_disabled))
                .checkSetId(1)
                .checked(!timeshift)
                .build());

        actions.add(new GuidedAction.Builder()
                .id(1)
                .title(getString(R.string.setup_root_item_enabled))
                .checkSetId(1)
                .checked(timeshift)
                .build());
    }

    public void onGuidedActionClicked(GuidedAction action) {
        SetupUtils.setTimeshiftEnabled(getActivity(), action.getId() == 1);
        getFragmentManager().popBackStack();
    }
}

