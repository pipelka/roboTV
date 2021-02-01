package org.robotv.setup;

import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.audio.AudioCapabilities;

import org.robotv.dataservice.DataServiceClient;
import org.robotv.robotv.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

public class SetupFragment extends GuidedStepSupportFragment {

    static final String TAG = "SetupFragment";

    static final int ACTION_SERVER = 1;
    static final int ACTION_LANGUAGE = 2;
    static final int ACTION_IMPORT = 4;
    static final int ACTION_PASSTHROUGH = 6;
    static final int ACTION_TUNNELEDPLAYBACK = 10;

    private GuidedAction mActionServer;
    private GuidedAction mActionLanguage;
    private GuidedAction mActionPassthrough;
    private GuidedAction mActionTunneledPlayback;

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.setup_root_title);
        String breadcrumb = "";
        String description = getString(R.string.setup_root_desc);
        Drawable icon = getActivity().getDrawable(R.drawable.ic_robotv_icon_white);

        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateButtonActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String server = SetupUtils.getServer(getActivity());

        mActionServer = new GuidedAction.Builder(getActivity())
                .id(ACTION_SERVER)
                .title(server)
                .description(getString(R.string.setup_root_server_desc))
                .editable(true)
                .build();

        mActionLanguage = new GuidedAction.Builder(getActivity())
                .id(ACTION_LANGUAGE)
                .title(getString(R.string.setup_root_language_title))
                .build();

        mActionPassthrough = new GuidedAction.Builder(getActivity())
                .id(ACTION_PASSTHROUGH)
                .title(getString(R.string.setup_root_passthrough_title))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(SetupUtils.getPassthrough(getActivity()))
                .build();

        mActionTunneledPlayback = new GuidedAction.Builder(getActivity())
                .id(ACTION_TUNNELEDPLAYBACK)
                .title(getString(R.string.setup_tunneled_playback))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(SetupUtils.getPassthrough(getActivity()))
                .build();

        /*mActionTimeshift = new GuidedAction.Builder(getActivity())
                .id(ACTION_TIMESHIFT)
                .title(getString(R.string.setup_root_timeshift_title))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(SetupUtils.getTimeshiftEnabled(getActivity()))
                .build();*/

        actions.add(mActionServer);
        actions.add(mActionLanguage);

        AudioCapabilities audioCapabilities = AudioCapabilities.getCapabilities(getActivity());

        if (audioCapabilities.supportsEncoding(AudioFormat.ENCODING_AC3)) {
            actions.add(mActionPassthrough);
        }

        actions.add(mActionTunneledPlayback);

        SetupActivity activity = (SetupActivity) getActivity();

        if (!TextUtils.isEmpty(activity.getInputId())) {
            actions.add(new GuidedAction.Builder(getActivity())
                .id(ACTION_IMPORT)
                .title(getString(R.string.setup_root_import_title))
                .description(getString(R.string.setup_root_import_desc))
                .hasNext(true)
                .build());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mActionLanguage.setDescription(SetupUtils.getDisplayLanguage(getContext()));

        boolean passthrough = SetupUtils.getPassthrough(getContext());
        mActionPassthrough.setChecked(passthrough);

        mActionTunneledPlayback.setChecked(
            SetupUtils.getTunneledVideoPlaybackEnabled(getContext())
        );
    }

    public void onGuidedActionClicked(GuidedAction action) {
        FragmentManager fm = getFragmentManager();

        switch((int)action.getId()) {
            case ACTION_SERVER:
                String server = mActionServer.getTitle().toString();
                SetupUtils.setServer(getActivity(), server);
                restartDataService();
                Log.d(TAG, "server: " + server);
                break;

            case ACTION_LANGUAGE:
                GuidedStepSupportFragment.add(fm, new SetupFragmentLanguage());
                break;

            case ACTION_PASSTHROUGH:
                SetupUtils.setPassthrough(getActivity(), action.isChecked());
                break;

            case ACTION_TUNNELEDPLAYBACK:
                SetupUtils.setTunneledVideoPlaybackEnabled(getActivity(), action.isChecked());
                break;

            case ACTION_IMPORT:
                importChannels();
                break;
        }
    }

    private void importChannels() {
        SetupFragmentImport progress = new SetupFragmentImport();

        getFragmentManager().beginTransaction().add(android.R.id.content, progress).commit();

        SetupActivity activity = (SetupActivity) getActivity();
        activity.registerChannels(progress);
    }

    private void restartDataService() {
        DataServiceClient client = new DataServiceClient(getContext());
        client.reconnect();
    }
}
