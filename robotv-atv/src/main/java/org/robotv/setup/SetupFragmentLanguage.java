package org.robotv.setup;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;

import org.robotv.robotv.R;

import java.util.List;

public class SetupFragmentLanguage extends GuidedStepFragment {

    private String[] mLanguageCode;

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
        int langIndex = SetupUtils.getLanguageIndex(getActivity());
        String[] languages= getResources().getStringArray(R.array.languages_array);
        mLanguageCode = getResources().getStringArray(R.array.iso639_code1);

        if(langIndex > languages.length - 1) {
            langIndex = 0;
        }

        int id = 0;

        for(String language : languages) {
            actions.add(new GuidedAction.Builder()
                        .id(id)
                        .title(language)
                        .checkSetId(1)
                        .checked(id == langIndex)
                        .build());
            id++;
        }
    }

    public void onGuidedActionClicked(GuidedAction action) {
        SetupUtils.setLanguage(getActivity(), mLanguageCode[(int)action.getId()]);
        getFragmentManager().popBackStack();
    }
}
