package no.nordicsemi.android.nrfmeshprovisioner;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.meshprovisioner.Group;
import no.nordicsemi.android.meshprovisioner.MeshNetwork;
import no.nordicsemi.android.meshprovisioner.transport.ApplicationKey;
import no.nordicsemi.android.meshprovisioner.transport.MeshModel;
import no.nordicsemi.android.meshprovisioner.utils.AddressType;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;
import no.nordicsemi.android.meshprovisioner.utils.PublicationSettings;
import no.nordicsemi.android.nrfmeshprovisioner.di.Injectable;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentPubRetransmitIntervalSteps;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentPublicationResolution;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentPublicationSteps;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentPublishAddress;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentPublishTtl;
import no.nordicsemi.android.nrfmeshprovisioner.dialog.DialogFragmentRetransmitCount;
import no.nordicsemi.android.nrfmeshprovisioner.utils.Utils;
import no.nordicsemi.android.nrfmeshprovisioner.viewmodels.PublicationViewModel;

public class PublicationSettingsActivity extends AppCompatActivity implements Injectable,
        DialogFragmentPublishAddress.DialogFragmentPublishAddressListener,
        DialogFragmentPublicationSteps.DialogFragmentPublicationStepsListener,
        DialogFragmentPublicationResolution.DialogFragmentPublicationResolutionListener,
        DialogFragmentRetransmitCount.DialogFragmentRetransmitCountListener,
        DialogFragmentPubRetransmitIntervalSteps.DialogFragmentIntervalStepsListener,
        DialogFragmentPublishTtl.DialogFragmentPublishTtlListener {

    public static final int SET_PUBLICATION_SETTINGS = 2021;
    public static final String RESULT_ADDRESS_TYPE = "RESULT_ADDRESS_TYPE";
    public static final String RESULT_LABEL_UUID = "RESULT_LABEL_UUID";
    public static final String RESULT_PUBLISH_ADDRESS = "RESULT_PUBLISH_ADDRESS";
    public static final String RESULT_APP_KEY_INDEX = "RESULT_APP_KEY_INDEX";
    public static final String RESULT_CREDENTIAL_FLAG = "RESULT_CREDENTIAL_FLAG";
    public static final String RESULT_PUBLISH_TTL = "RESULT_PUBLISH_TTL";
    public static final String RESULT_PUBLICATION_STEPS = "RESULT_PUBLICATION_STEPS";
    public static final String RESULT_PUBLICATION_RESOLUTION = "RESULT_PUBLICATION_RESOLUTION";
    public static final String RESULT_PUBLISH_RETRANSMIT_COUNT = "RESULT_PUBLISH_RETRANSMIT_COUNT";
    public static final String RESULT_PUBLISH_RETRANSMIT_INTERVAL_STEPS = "RESULT_PUBLISH_RETRANSMIT_INTERVAL_STEPS";

    private static final int DEFAULT_PUB_RETRANSMIT_COUNT = 1;
    private static final int DEFAULT_PUB_RETRANSMIT_INTERVAL_STEPS = 1;
    private static final int DEFAULT_PUBLICATION_STEPS = 0;
    @SuppressWarnings("unused")
    private static final int DEFAULT_PUBLICATION_RESOLUTION = MeshParserUtils.RESOLUTION_100_MS;

    private PublicationViewModel mViewModel;
    private MeshModel mMeshModel;
    private AddressType mAddressType;
    private UUID mLabelUUID;
    private int mPublishAddress;
    private Integer mAppKeyIndex;
    private int mPublishTtl = MeshParserUtils.USE_DEFAULT_TTL;
    private int mPublicationSteps = DEFAULT_PUBLICATION_STEPS;
    private int mPublicationResolution;
    private int mPublishRetransmitCount = DEFAULT_PUB_RETRANSMIT_COUNT;
    private int mPublishRetransmitIntervalSteps = DEFAULT_PUB_RETRANSMIT_INTERVAL_STEPS;

    @Inject
    ViewModelProvider.Factory mViewModelFactory;

    @BindView(R.id.publish_address)
    TextView mPublishAddressView;
    @BindView(R.id.retransmit_count)
    TextView mRetransmitCountView;
    @BindView(R.id.interval_steps)
    TextView mIntervalStepsView;
    @BindView(R.id.publication_steps)
    TextView mPublicationStepsView;
    @BindView(R.id.publication_resolution)
    TextView mPublicationResolutionView;
    @BindView(R.id.app_key_index)
    TextView mAppKeyIndexView;
    @BindView(R.id.publication_ttl)
    TextView mPublishTtlView;
    @BindView(R.id.friendship_credential_flag)
    Switch mActionFriendshipCredentialSwitch;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publication_settings);
        ButterKnife.bind(this);

        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(PublicationViewModel.class);

        final MeshModel meshModel = mMeshModel = mViewModel.getSelectedModel().getValue();
        if (meshModel == null)
            finish();

        //Setup views
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_publication_settings);

        final View actionPublishAddress = findViewById(R.id.container_publish_address);
        final View actionRetransmitCount = findViewById(R.id.container_retransmission_count);
        final View actionIntervalSteps = findViewById(R.id.container_interval_steps);
        final View actionPublicationSteps = findViewById(R.id.container_publish_steps);
        final View actionResolution = findViewById(R.id.container_resolution);
        final View actionKeyIndex = findViewById(R.id.container_app_key_index);
        final View actionPublishTtl = findViewById(R.id.container_publication_ttl);

        if (savedInstanceState == null) {
            //noinspection ConstantConditions
            updateUi(meshModel);
        }

        actionPublishAddress.setOnClickListener(v -> {
            //noinspection ConstantConditions
            List<Group> groups = mViewModel.getMeshManagerApi().getMeshNetwork().getGroups();
            final DialogFragmentPublishAddress fragmentPublishAddress = DialogFragmentPublishAddress.
                    newInstance(meshModel.getPublicationSettings(), new ArrayList<>(groups));
            fragmentPublishAddress.show(getSupportFragmentManager(), null);
        });

        actionRetransmitCount.setOnClickListener(v -> {
            final DialogFragmentRetransmitCount fragmentRetransmitCount = DialogFragmentRetransmitCount.newInstance(mPublishRetransmitCount);
            fragmentRetransmitCount.show(getSupportFragmentManager(), null);
        });

        actionIntervalSteps.setOnClickListener(v -> {
            final DialogFragmentPubRetransmitIntervalSteps fragmentIntervalSteps = DialogFragmentPubRetransmitIntervalSteps.newInstance(mPublishRetransmitIntervalSteps);
            fragmentIntervalSteps.show(getSupportFragmentManager(), null);
        });

        actionPublicationSteps.setOnClickListener(v -> {
            final DialogFragmentPublicationSteps fragmentPublicationSteps = DialogFragmentPublicationSteps.newInstance(mPublicationSteps);
            fragmentPublicationSteps.show(getSupportFragmentManager(), null);
        });

        actionResolution.setOnClickListener(v -> {
            final DialogFragmentPublicationResolution fragmentPublicationResolution = DialogFragmentPublicationResolution.newInstance(mPublicationResolution);
            fragmentPublicationResolution.show(getSupportFragmentManager(), null);
        });

        actionKeyIndex.setOnClickListener(v -> {
            final Intent bindAppKeysIntent = new Intent(this, ManageAppKeysActivity.class);
            bindAppKeysIntent.putExtra(Utils.EXTRA_DATA, Utils.PUBLICATION_APP_KEY);
            startActivityForResult(bindAppKeysIntent, ManageAppKeysActivity.SELECT_APP_KEY);
        });

        actionPublishTtl.setOnClickListener(v -> {
            if (meshModel != null) {
                final PublicationSettings publicationSettings = meshModel.getPublicationSettings();
                final DialogFragmentPublishTtl fragmentPublishTtl;
                if (publicationSettings != null) {
                    fragmentPublishTtl = DialogFragmentPublishTtl.newInstance(meshModel.getPublicationSettings().getPublishTtl());
                } else {
                    fragmentPublishTtl = DialogFragmentPublishTtl.newInstance(MeshParserUtils.USE_DEFAULT_TTL);
                }
                fragmentPublishTtl.show(getSupportFragmentManager(), null);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.publication_apply, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_apply:
                setReturnIntent();
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ManageAppKeysActivity.SELECT_APP_KEY) {
            if (resultCode == RESULT_OK) {
                final ApplicationKey appKey = data.getParcelableExtra(ManageAppKeysActivity.RESULT_APP_KEY);
                if (appKey != null) {
                    mAppKeyIndex = appKey.getKeyIndex();
                    mAppKeyIndexView.setText(getString(R.string.app_key_index, appKey.getKeyIndex()));
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAddressType != null)
            outState.putInt(RESULT_ADDRESS_TYPE, mAddressType.ordinal());
        outState.putSerializable(RESULT_LABEL_UUID, mLabelUUID);
        outState.putInt(RESULT_PUBLISH_ADDRESS, mPublishAddress);
        outState.putInt(RESULT_APP_KEY_INDEX, mAppKeyIndex);
        outState.putBoolean(RESULT_CREDENTIAL_FLAG, mActionFriendshipCredentialSwitch.isChecked());
        outState.putInt(RESULT_PUBLISH_TTL, mPublishTtl);
        outState.putInt(RESULT_PUBLICATION_STEPS, mPublicationSteps);
        outState.putInt(RESULT_PUBLICATION_RESOLUTION, mPublicationResolution);
        outState.putInt(RESULT_PUBLISH_RETRANSMIT_COUNT, mPublishRetransmitCount);
        outState.putInt(RESULT_PUBLISH_RETRANSMIT_INTERVAL_STEPS, mPublishRetransmitIntervalSteps);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mAddressType = AddressType.fromValue(savedInstanceState.getInt(RESULT_ADDRESS_TYPE, -1));
        mLabelUUID = (UUID) savedInstanceState.getSerializable(RESULT_LABEL_UUID);
        mPublishAddress = savedInstanceState.getInt(RESULT_PUBLISH_ADDRESS);
        mAppKeyIndex = savedInstanceState.getInt(RESULT_APP_KEY_INDEX);
        mPublishTtl = savedInstanceState.getInt(RESULT_PUBLISH_TTL);
        mPublicationSteps = savedInstanceState.getInt(RESULT_PUBLICATION_STEPS);
        mPublicationResolution = savedInstanceState.getInt(RESULT_PUBLICATION_RESOLUTION);
        mPublishRetransmitCount = savedInstanceState.getInt(RESULT_PUBLISH_RETRANSMIT_COUNT);
        mPublishRetransmitIntervalSteps = savedInstanceState.getInt(RESULT_PUBLISH_RETRANSMIT_INTERVAL_STEPS);
        updateUi(savedInstanceState.getBoolean(RESULT_CREDENTIAL_FLAG));
    }

    public void setPublishAddress(final byte[] address) {
        if (address != null) {
            mPublishAddress = MeshParserUtils.unsignedBytesToInt(address[1], address[0]);
            mPublishAddressView.setText(MeshParserUtils.bytesToHex(address, true));
        }
    }

    @Override
    public void setPublishAddress(@NonNull final AddressType addressType, final int address) {
        mLabelUUID = null;
        mAddressType = addressType;
        mPublishAddress = address;
        mPublishAddressView.setText(MeshAddress.formatAddress(address, true));
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void setPublishAddress(@NonNull final AddressType addressType, @NonNull final String name, final int address) {
        mLabelUUID = null;
        mAddressType = addressType;
        mPublishAddress = address;
        mPublishAddressView.setText(MeshAddress.formatAddress(address, true));
        final MeshNetwork network = mViewModel.getMeshManagerApi().getMeshNetwork();
        final Group group = new Group(address, network.getMeshUUID());
        group.setName(name);
        network.addGroup(group);
    }

    @Override
    public void setPublishAddress(@NonNull final AddressType addressType, @NonNull final Group group) {
        mLabelUUID = null;
        mAddressType = addressType;
        mPublishAddress = group.getGroupAddress();
        mPublishAddressView.setText(MeshAddress.formatAddress(group.getGroupAddress(), true));
    }

    @Override
    public void setPublishAddress(@NonNull final AddressType addressType, @NonNull final UUID labelUuid) {
        mAddressType = addressType;
        mLabelUUID = labelUuid;
        mPublishAddressView.setText(labelUuid.toString().toUpperCase(Locale.US));
    }

    @Override
    public void setPublicationResolution(final int resolution) {
        mPublicationResolution = resolution;
        mPublicationResolutionView.setText(getResolutionSummary(resolution));
    }

    @Override
    public void setPublicationSteps(final int publicationSteps) {
        mPublicationSteps = publicationSteps;
        mPublicationStepsView.setText(getResources().getQuantityString(R.plurals.steps,
                publicationSteps, publicationSteps));
    }

    @Override
    public void setRetransmitCount(final int retransmitCount) {
        mPublishRetransmitCount = retransmitCount;
        mRetransmitCountView.setText(getResources().getQuantityString(R.plurals.retransmit_count,
                retransmitCount, retransmitCount));
    }

    @Override
    public void setRetransmitIntervalSteps(final int intervalSteps) {
        mPublishRetransmitIntervalSteps = intervalSteps;
        mIntervalStepsView.setText(getResources().getQuantityString(R.plurals.steps,
                intervalSteps, intervalSteps));
    }

    @Override
    public void setPublishTtl(final int ttl) {
        mPublishTtl = ttl;
        updateTtlUi(ttl);
    }

    private void updateUi(@NonNull final MeshModel model) {
        final PublicationSettings publicationSettings = model.getPublicationSettings();

        //Default app key index to the 0th key in the list of bound app keys
        if (!model.getBoundAppKeyIndexes().isEmpty()) {
            mAppKeyIndex = mMeshModel.getBoundAppKeyIndexes().get(0);
        }

        if (publicationSettings != null) {
            mPublishAddress = publicationSettings.getPublishAddress();
            mLabelUUID = publicationSettings.getLabelUUID();
            if(MeshAddress.isValidVirtualAddress(mPublishAddress)){
                //noinspection ConstantConditions
                mPublishAddressView.setText(publicationSettings.getLabelUUID().toString().toUpperCase(Locale.US));
            } else {
                mPublishAddressView.setText(MeshAddress.formatAddress(mPublishAddress, true));
            }

            mActionFriendshipCredentialSwitch.setChecked(publicationSettings.getCredentialFlag());
            mPublishTtl = publicationSettings.getPublishTtl();

            mPublicationSteps = publicationSettings.getPublicationSteps();
            mPublicationStepsView.setText(getResources().getQuantityString(R.plurals.steps, mPublicationSteps, mPublicationSteps));

            mPublicationResolution = publicationSettings.getPublicationResolution();
            mPublicationResolutionView.setText(getResolutionSummary(mPublicationResolution));

            mPublishRetransmitCount = publicationSettings.getPublishRetransmitCount();
            mRetransmitCountView.setText(getResources().getQuantityString(R.plurals.retransmit_count,
                    mPublishRetransmitCount, mPublishRetransmitCount));

            mPublishRetransmitIntervalSteps = publicationSettings.getPublishRetransmitIntervalSteps();
            mIntervalStepsView.setText(getResources().getQuantityString(R.plurals.steps,
                    mPublishRetransmitIntervalSteps, mPublishRetransmitIntervalSteps));

            if (!model.getBoundAppKeyIndexes().isEmpty()) {
                mAppKeyIndex = publicationSettings.getAppKeyIndex();
            }
            updateUi(publicationSettings.getCredentialFlag());
        }
        final int ttl = mPublishTtl;
        updateTtlUi(ttl);

    }

    private void updateUi(final boolean credentialFlag) {
        if (mLabelUUID == null) {
            mPublishAddressView.setText(MeshAddress.formatAddress(mPublishAddress, true));
        } else {
            mPublishAddressView.setText(mLabelUUID.toString().toUpperCase(Locale.US));
        }
        mAppKeyIndexView.setText(getString(R.string.app_key_index, mAppKeyIndex));
        mActionFriendshipCredentialSwitch.setChecked(credentialFlag);
        updateTtlUi(mPublishTtl);
        mPublicationStepsView.setText(getResources().getQuantityString(R.plurals.steps, mPublicationSteps, mPublicationSteps));
        mPublicationResolutionView.setText(getResolutionSummary(mPublicationResolution));
        mRetransmitCountView.setText(getResources().getQuantityString(R.plurals.retransmit_count,
                mPublishRetransmitCount, mPublishRetransmitCount));
        mIntervalStepsView.setText(getResources().getQuantityString(R.plurals.steps,
                mPublishRetransmitIntervalSteps, mPublishRetransmitIntervalSteps));

    }

    private void updateTtlUi(final int ttl) {
        if (MeshParserUtils.isDefaultPublishTtl(ttl)) {
            mPublishTtlView.setText(getString(R.string.uses_default_ttl));
        } else {
            mPublishTtlView.setText(String.valueOf(ttl));
        }
    }

    private String getResolutionSummary(final int resolution) {
        switch (resolution) {
            default:
            case MeshParserUtils.RESOLUTION_100_MS:
                return getString(R.string.resolution_summary_100_ms);
            case MeshParserUtils.RESOLUTION_1_S:
                return getString(R.string.resolution_summary_1_s);
            case MeshParserUtils.RESOLUTION_10_S:
                return getString(R.string.resolution_summary_10_s);
            case MeshParserUtils.RESOLUTION_10_M:
                return getString(R.string.resolution_summary_100_m);
        }

    }

    @SuppressWarnings("ConstantConditions")
    private void setReturnIntent() {
        Intent returnIntent = new Intent();
        int type = -1;
        if (mLabelUUID != null) {
            type = AddressType.VIRTUAL_ADDRESS.ordinal();
        } else {
            mAddressType = MeshAddress.getAddressType(mPublishAddress);
            if(mAddressType != null) {
                type = mAddressType.ordinal();
            }
        }

        returnIntent.putExtra(RESULT_ADDRESS_TYPE, type);
        returnIntent.putExtra(RESULT_LABEL_UUID, mLabelUUID);
        returnIntent.putExtra(RESULT_PUBLISH_ADDRESS, mPublishAddress);
        returnIntent.putExtra(RESULT_APP_KEY_INDEX, mAppKeyIndex);
        returnIntent.putExtra(RESULT_CREDENTIAL_FLAG, mActionFriendshipCredentialSwitch.isChecked());
        returnIntent.putExtra(RESULT_PUBLISH_TTL, mPublishTtl);
        returnIntent.putExtra(RESULT_PUBLICATION_STEPS, mPublicationSteps);
        returnIntent.putExtra(RESULT_PUBLICATION_RESOLUTION, mPublicationResolution);
        returnIntent.putExtra(RESULT_PUBLISH_RETRANSMIT_COUNT, mPublishRetransmitCount);
        returnIntent.putExtra(RESULT_PUBLISH_RETRANSMIT_INTERVAL_STEPS, mPublishRetransmitIntervalSteps);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
