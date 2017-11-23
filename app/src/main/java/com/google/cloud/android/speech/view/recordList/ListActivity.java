package com.google.cloud.android.speech.view.recordList;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.cloud.android.speech.event.FileEvent;
import com.google.cloud.android.speech.event.QueryEvent;
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

public class ListActivity extends AppCompatActivity {


    ActivityListBinding binding;
    public SpeechService mSpeechService;
    public Realm realm;
    private PagerAdapter mPagerAdapter;
    private String tempTitle;
    private ArrayList<Integer> tempTags;

    private static final int REQUEST_RECORD = 1;
    private static final int REQUEST_AUDIO = 2;
    private static final int REQUEST_VIDEO = 3;

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lifecycle", String.valueOf(binding.tlList.getSelectedTabPosition()));
        switch (binding.tlList.getSelectedTabPosition()) {
            case 0:
                binding.searchView.setVisibility(View.GONE);
                break;
            case 1:
                binding.ivTitle.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lifecycle","pause");
    }

    private void transitionTitle(int index) {
        if (index == 0) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    binding.searchView.setVisibility(View.GONE);
                    binding.ivTitle.setVisibility(View.VISIBLE);
                    binding.ivTitle.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.fade_in));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            binding.searchView.startAnimation(animation);


        } else {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    binding.ivTitle.setVisibility(View.GONE);
                    binding.searchView.setVisibility(View.VISIBLE);
                    binding.searchView.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.fade_in));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            binding.ivTitle.startAnimation(animation);

        }
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
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(null);
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        binding.searchView.setVisibility(View.GONE);
        binding.vpList.setAdapter(mPagerAdapter);
        binding.vpList.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                transitionTitle(position);
                binding.tlList.getTabAt(position).select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        binding.tlList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                transitionTitle(tab.getPosition());
                binding.vpList.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

//        binding.searchView.
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                EventBus.getDefault().postSticky(new QueryEvent(query));
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                EventBus.getDefault().postSticky(new QueryEvent(newText));
                return true;
            }
        });

        binding.searchView.setBackgroundColor(ContextCompat.getColor(this,R.color.transparent));
    }


    public void openDialog(int permission, int currentDirId) {
        NewRecordDialog dialog = new NewRecordDialog();
        dialog.setRequestCode(permission, currentDirId);
        dialog.show(getSupportFragmentManager(), "NewRecordDialogFragment");
    }

    private int dirId;

//    @Override
//    public void onDialogPositiveClick(String title, ArrayList<Integer> tags, int requestCode, int dirId) {
//        this.dirId = dirId;
//
//        if (requestCode == REQUEST_RECORD) {
//            Intent intent = new Intent(this, RecordActivity.class);
//            intent.putExtra("title", title);
//            intent.putExtra("tags", Parcels.wrap(tags));
//            intent.putExtra("dirId", dirId);
//
//            startActivity(intent);
//        } else if (requestCode == REQUEST_AUDIO) {
//            tempTitle = title;
//            tempTags = tags;
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            intent.setType("audio/*");
//
//            startActivityForResult(intent, REQUEST_AUDIO);
//
//        } else if (requestCode == REQUEST_VIDEO) {
//            tempTitle = title;
//            tempTags = tags;
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            intent.setType("video/*");
//            startActivityForResult(intent, REQUEST_VIDEO);
//        }
//    }


//    @Override
//    public void onDialogNegativeClick() {
//
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        String mediaPath;
//        switch (requestCode) {
//            case REQUEST_AUDIO:
//                if (data != null) {
//                    mediaPath = FileUtil.getPath(getBaseContext(), data.getData());
//                    binding.vpList.setCurrentItem(1);
//                    EventBus.getDefault().postSticky(new FileEvent(tempTitle, tempTags, mediaPath, REQUEST_AUDIO, dirId));
//                }
//                break;
//            case REQUEST_VIDEO:
//                if (data != null) {
//                    mediaPath = FileUtil.getPath(getBaseContext(), data.getData());
//                    binding.vpList.setCurrentItem(1);
//                    EventBus.getDefault().postSticky(new FileEvent(tempTitle, tempTags, mediaPath, REQUEST_VIDEO, dirId));
//                }
//                break;
//        }
//
//    }


    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // 해당하는 page의 Fragment를 생성합니다.

            switch (position) {
                case 0:
                    return ProcessListFragment.create(position);
                case 1:
                    return ResultListFragment.create(position);

            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

    }
}
