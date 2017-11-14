package com.google.cloud.android.speech.view.recordList;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
import com.google.cloud.android.speech.view.recordList.fragment.ProcessListFragment;
import com.google.cloud.android.speech.view.recordList.fragment.ResultListFragment;
import com.google.cloud.android.speech.view.recording.RecordActivity;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.databinding.ActivityListBinding;


import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.StringTokenizer;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class ListActivity extends AppCompatActivity implements  NewRecordDialog.NewRecordDialogListener {


    ActivityListBinding binding;
    private static final int AUDIO_FILE_REQUEST = 0;
    private static final int VIDEO_FILE_REQUEST = 1;
    String TAG = "SpeechAPI";

    public SpeechService mSpeechService;
    public Realm realm;

//
//
//    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
//    public void onProcessIdEvent(ProcessIdEvent event) {
//
//        Log.d("lifecycle","list process event");
//        if (event.IS_RECORDING()) {
//            binding.fabRecord.setEnabled(false);
//            ((ProcessListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_process_list)).setRecordItem(event.getRecordId());
//        } else {
//            binding.fabRecord.setEnabled(true);
//        }
//        if (event.isFiling()) {
//            binding.fabFile.setEnabled(false);
//            ((ProcessListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_process_list)).setFileItem(event.getFileId());
//        } else {
//            binding.fabFile.setEnabled(true);
//        }
//    }


//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder binder) {
//            mSpeechService = SpeechService.from(binder);
//            mSpeechService.notifyProcess();
//            Log.d("lifecycle","service con");
//
//
//            //TODO enable after end
//
//
////            recordId = mSpeechService.getRecordId();
//////            mSpeechService.addListener(mSpeechServiceListener);
////            mStatus.setVisibility(View.VISIBLE);
////            serviceBinded = true;
//
////            realm.executeTransaction(new Realm.Transaction() {
////                @Override
////                public void execute(Realm realm) {
////                    Log.d(TAG, "in service" + String.valueOf(recordId));
////                    record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
////
////                }
////            });
////
////            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
////            mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
//////        final ArrayList<String> results = savedInstanceState == null ? null :
//////                savedInstanceState.getStringArrayList(STATE_RESULTS);
//////
////
////            mAdapter = new RecordRealmAdapter(record.getSentenceRealms(), true, true, context);
////            mRecyclerView.setAdapter(mAdapter);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//
//            Log.d("lifecycle","service discon");
//            mSpeechService = null;
//        }
//
//    };


    PagerAdapter mPagerAdapter;

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
//                    .deleteRealmIfMigrationNeeded()
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

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_FILE_AUDIO_PERMISSION = 2;


    public void openDialog(int permission){
            NewRecordDialog dialog = new NewRecordDialog();
            dialog.setRequestCode(permission);
            dialog.show(getSupportFragmentManager(), "NewRecordDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(String title, String tag, int requestCode) {
        StringTokenizer st = new StringTokenizer(tag);
        final ArrayList<String> tags = new ArrayList<>();
        while (st.hasMoreTokens()) {
            tags.add(st.nextToken());
        }


        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
//            mSpeechService.initSpeechRecognizing(title, tags);
            Intent intent = new Intent(this, RecordActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("tags", tags);
startActivity(intent);

        } else if (requestCode == REQUEST_FILE_AUDIO_PERMISSION) {

            tempTitle = title;
            tempTags = tags;

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType("audio/*");

            startActivityForResult(intent, AUDIO_FILE_REQUEST);

//            mSpeechService.recognizeFileStream(FileUtil.getFilename(fileUri));
//            mSpeechService.recognizeFileStream(title, tags, fileUri);
//            try {
//                FileInputStream fileInputStream = new FileInputStream(file);
//
//                mSpeechService.recognizeInputStream(fileInputStream);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
        }

    }

    String tempTitle;
    ArrayList<String> tempTags;

    @Override
    public void onDialogNegativeClick() {

    }


    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case AUDIO_FILE_REQUEST:
                if(data!=null){
                    String audioPath = getPath(getBaseContext(), data.getData());
                    Log.i(TAG, audioPath);
                    binding.vpList.setCurrentItem(1);
                    EventBus.getDefault().postSticky(new FileEvent(tempTitle, tempTags, audioPath));
                }
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
        switch(item.getItemId()){
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
