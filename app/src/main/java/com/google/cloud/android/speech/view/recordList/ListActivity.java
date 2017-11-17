package com.google.cloud.android.speech.view.recordList;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.cloud.android.speech.event.FileEvent;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.view.recordList.fragment.ProcessListFragment;
import com.google.cloud.android.speech.view.recordList.fragment.ResultListFragment;
import com.google.cloud.android.speech.view.recording.RecordActivity;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.databinding.ActivityListBinding;


import org.greenrobot.eventbus.EventBus;
import org.parceler.Parcels;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ListActivity extends AppCompatActivity implements NewRecordDialog.NewRecordDialogListener {


    ActivityListBinding binding;
    String TAG = "SpeechAPI";

    public SpeechService mSpeechService;
    public Realm realm;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onStop() {
        super.onStop();
//        if (mSpeechService != null) {
//            unbindService(mServiceConnection);
//
//            Log.d("lifecycle","list unbind longrunningRequestRetrofit in stop");
//        }

//        EventBus.getDefault().unregister(this);
        Log.d("lifecycle", "list stop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lifecycle", "list resume");
//        EventBus.getDefault().register(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
//
//        Intent intent = new Intent(this, SpeechService.class);
//        startService(intent);
//        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (realm == null) {
            realm.init(this);
            RealmConfiguration config = new RealmConfiguration.Builder()
                    .deleteRealmIfMigrationNeeded()
                    .build();
            Realm.setDefaultConfiguration(config);
            realm = Realm.getDefaultInstance();
        }
        Log.d("lifecycle", "list crate");


        binding = DataBindingUtil.setContentView(this, R.layout.activity_list);
//        binding.setHandler(this);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(null);
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());


        binding.vpList.setAdapter(mPagerAdapter);

        binding.vpList.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                binding.tlList.getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        binding.tlList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.vpList.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private static final int REQUEST_RECORD = 1;
    private static final int REQUEST_AUDIO = 2;
    private static final int REQUEST_VIDEO = 3;


    public void openDialog(int permission) {
        NewRecordDialog dialog = new NewRecordDialog();
        dialog.setRequestCode(permission);
        dialog.show(getSupportFragmentManager(), "NewRecordDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(String title, ArrayList<Integer> tags, int requestCode) {

        if (requestCode == REQUEST_RECORD) {
            Intent intent = new Intent(this, RecordActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("tags", Parcels.wrap(tags));
            startActivity(intent);
        } else if (requestCode == REQUEST_AUDIO) {
            tempTitle = title;
            tempTags = tags;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            startActivityForResult(intent, REQUEST_AUDIO);

        } else if (requestCode == REQUEST_VIDEO) {
            tempTitle = title;
            tempTags = tags;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            startActivityForResult(intent, REQUEST_VIDEO);
        }
    }

    String tempTitle;
    ArrayList<Integer> tempTags;

    @Override
    public void onDialogNegativeClick() {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String mediaPath;
        switch (requestCode) {
            case REQUEST_AUDIO:
                if (data != null) {
                    mediaPath = FileUtil.getPath(getBaseContext(), data.getData());
                    binding.vpList.setCurrentItem(1);
                    EventBus.getDefault().postSticky(new FileEvent(tempTitle, tempTags, mediaPath, REQUEST_AUDIO));
                }
                break;
            case REQUEST_VIDEO:
                if (data != null) {
                    mediaPath = FileUtil.getPath(getBaseContext(), data.getData());
                    binding.vpList.setCurrentItem(1);
                    EventBus.getDefault().postSticky(new FileEvent(tempTitle, tempTags, mediaPath, REQUEST_VIDEO));
                }
                break;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                Toast.makeText(this, R.string.ready, Toast.LENGTH_SHORT).show();
                mSpeechService.stopForeground(true);
                break;
        }
        return true;
    }

    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // 해당하는 page의 Fragment를 생성합니다.

            switch (position) {
                case 0:
                    return ResultListFragment.create(position);
                case 1:
                    return ProcessListFragment.create(position);

            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

    }
}
