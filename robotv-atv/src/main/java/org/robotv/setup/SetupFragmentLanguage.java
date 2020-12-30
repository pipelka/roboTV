package org.robotv.setup;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.robotv.robotv.R;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class SetupFragmentLanguage extends GuidedStepSupportFragment {

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getResources().getString(R.string.setup_language_title);
        String breadcrumb = "";
        String description = getResources().getString(R.string.setup_language_desc);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_language_white_48dp);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List actions, Bundle savedInstanceState) {
        TreeMap<String, String> list = SetupUtils.getLanguages();
        String code = SetupUtils.getLanguage(getContext());

        int id = 0;

        for(HashMap.Entry<String, String> entry : list.entrySet()) {
            actions.add(new GuidedAction.Builder()
                        .id(id)
                        .title(entry.getKey())
                        .checkSetId(1)
                        .checked(code.equals(entry.getValue()))
                        .build());
            id++;
        }
    }

    public void onGuidedActionClicked(GuidedAction action) {
        TreeMap<String, String> list = SetupUtils.getLanguages();
        int index = (int)action.getId();

        String[] codes = list.values().toArray(new String[0]);

        if(index >= 0) {
            SetupUtils.setLanguage(getActivity(), codes[index]);
        }

        getFragmentManager().popBackStack();
    }
}
