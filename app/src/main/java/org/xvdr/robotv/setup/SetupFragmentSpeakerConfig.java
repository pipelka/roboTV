package org.xvdr.robotv.setup;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.robotv.R;

import java.util.List;

public class SetupFragmentSpeakerConfig extends GuidedStepFragment {

    static final String TAG = "SetupFragmentSpeakerConfig";

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getResources().getString(R.string.setup_root_speakerconfig_title);
        String breadcrumb = "";
        String description = getResources().getString(R.string.setup_speakerconfig_desc);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_speaker_white_48dp);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        int speakerConfig = SetupUtils.getSpeakerConfiguration(getActivity());

        actions.add(new GuidedAction.Builder()
                    .id(6)
                    .title(getString(R.string.setup_root_speakerconfig_digital_51))
                    .checkSetId(1)
                    .checked(speakerConfig == 6)
                    .build());

        actions.add(new GuidedAction.Builder()
                    .id(4)
                    .title(getString(R.string.setup_root_speakerconfig_surround))
                    .checkSetId(1)
                    .checked(speakerConfig == 4)
                    .build());

        actions.add(new GuidedAction.Builder()
                    .id(2)
                    .title(getString(R.string.setup_root_speakerconfig_stereo))
                    .checkSetId(1)
                    .checked(speakerConfig == 2)
                    .build());
    }

    public void onGuidedActionClicked(GuidedAction action) {
        SetupUtils.setSpeakerConfiguration(getActivity(), (int)action.getId());
        getFragmentManager().popBackStack();
    }
}
