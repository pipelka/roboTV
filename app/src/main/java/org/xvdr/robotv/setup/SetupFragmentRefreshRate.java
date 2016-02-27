package org.xvdr.robotv.setup;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.xvdr.robotv.R;

import java.util.List;

public class SetupFragmentRefreshRate extends GuidedStepFragment {

    static final String TAG = "SetupFragmentRefreshRate";

    private String[] mRefreshRates;

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getResources().getString(R.string.setup_refreshrate_title);
        String breadcrumb = "";
        String description = getResources().getString(R.string.setup_refreshrate_desc);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_tv_white_48dp);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(List actions, Bundle savedInstanceState) {
        int langIndex = SetupUtils.getLanguageIndex(getActivity());

        mRefreshRates = getResources().getStringArray(R.array.refresh_rate_name_array);
        int index = SetupUtils.getRefreshRateIndex(getActivity());

        int id = 0;

        for(String rate : mRefreshRates) {
            actions.add(new GuidedAction.Builder()
                        .id(id)
                        .title(rate)
                        .checkSetId(1)
                        .checked(id == index)
                        .build());
            id++;
        }
    }

    public void onGuidedActionClicked(GuidedAction action) {
        String[] rates = getResources().getStringArray(R.array.refresh_rate_value_array);
        final float[] refreshRateValueArray = new float[rates.length];

        for(int i = 0; i < rates.length; i++) {
            refreshRateValueArray[i] = Float.parseFloat(rates[i]);
        }

        SetupUtils.setRefreshRate(getActivity(), refreshRateValueArray[(int) action.getId()]);
        getFragmentManager().popBackStack();
    }
}
