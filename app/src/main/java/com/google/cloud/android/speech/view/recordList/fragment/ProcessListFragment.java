package com.google.cloud.android.speech.view.recordList.fragment;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.data.DTO.RecordDTO;
import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.databinding.DialogRenameDirBinding;
import com.google.cloud.android.speech.event.DirEvent;
import com.google.cloud.android.speech.event.PartialFileEvent;
import com.google.cloud.android.speech.util.AudioUtil;
import com.google.cloud.android.speech.util.DateUtil;
import com.google.cloud.android.speech.util.FileUtil;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.recordList.adapter.ListRealmAdapter;
import com.google.cloud.android.speech.view.recordList.adapter.TagRealmAdapter;
import com.google.cloud.android.speech.view.recordList.handler.ProcessItemHandler;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.google.cloud.android.speech.view.recordList.handler.ProcessHandler;
import com.google.cloud.android.speech.event.ProcessIdEvent;
import com.google.cloud.android.speech.event.PartialRecordEvent;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.view.recordList.handler.TagHandler;
import com.google.cloud.android.speech.view.recording.RecordActivity;
import com.google.cloud.android.speech.databinding.FragmentProcessListBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;


public class ProcessListFragment extends Fragment implements ProcessHandler, ProcessItemHandler {
    private static final String TAG = "Speech";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 0;
    private static final int REQUEST_FILE_AUDIO_PERMISSION = 1;
    private static final int REQUEST_FILE_VIDEO_PERMISSION = 2;
    private static final int REQUEST_AUDIO = 2;
    private static final int REQUEST_VIDEO = 3;
    private static EditText editTextTitle;
    private static NestedScrollView svMain;
    private static ProcessListFragment instance;

    private final int LOW = 16000;
    private final int MID = 22050;
    private final int HIGH = 44100;

    private static ObservableDTO<Integer> recogMode = new ObservableDTO<>();
    private static ObservableDTO<Integer> sampleRate = new ObservableDTO<>();
    private static ObservableDTO<Boolean> speaker = new ObservableDTO<>();
    private static ObservableDTO<String> curFolder = new ObservableDTO<>();


//    private static ObjectAnimator scrollAnimator = new ObjectAnimator();

    private int mPageNumber;
    private int dirId;
    private int recordId;
    private int fileId;
    private Realm realm;
    private String title;
    private String filePath;
    private String tempTitle;
    private RecordDTO record = new RecordDTO();
    private RecordDTO file = new RecordDTO();
    private SpeechService mSpeechService;
    private FragmentProcessListBinding binding;
    private CardView cvMp3;
    private RecyclerView originalRecyclerView;
    private RecyclerView addRecyclerView;
    private EditText editTextTag;
    private TagRealmAdapter originalAdapter;
    private RecyclerView.LayoutManager originalLayout;
    private TagRealmAdapter addAdapter;
    private RecyclerView.LayoutManager addLayout;
    private ArrayList<TagRealm> originalTags = new ArrayList<>();
    private ArrayList<Integer> tags = new ArrayList<>();
    private ArrayList<TagRealm> addedTags = new ArrayList<>();
    private ArrayList<Integer> tempTags;

    RecyclerView dirRecyclerView;
    ListRealmAdapter dirAdapter;

    @BindingAdapter("recogMode")
    public static void onModeClick(CardView v, boolean isMode) {
        Log.d("mode", String.valueOf(isMode));
        if (isMode) {
            v.setSelected(true);
        } else {
            v.setSelected(false);
        }
    }

    @BindingAdapter("samplerate")
    public static void onSampleClick(CardView v, boolean isMode) {
        Log.d("mode", String.valueOf(isMode));
        if (isMode) {
            v.setSelected(true);
        } else {
            v.setSelected(false);
        }
    }

    @BindingAdapter("speaker")
    public static void onSpeakerClick(CardView v, boolean isMode) {
        Log.d("mode", String.valueOf(isMode));
        if (isMode) {
            v.setSelected(true);
        } else {
            v.setSelected(false);
        }
    }

    @BindingAdapter("qVisible")
    public static void setVisible(final CardView v, boolean visible) {
        Animation on = v.getAnimation();

        if (visible) {
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.fade_in));
            }
        } else {
            if (v.getVisibility() == View.VISIBLE) {
                Animation animation = AnimationUtils.loadAnimation(v.getContext(), R.anim.fade_out);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        v.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                v.startAnimation(animation);
            }


        }
    }

    @Override
    public void onClickMode(View v, int id) {
        recogMode.setValue(id);
//        svMain.smoothScrollTo(0,getBottom());
    }

    @Override
    public void onClickSamplerate(View v, int rate) {
        sampleRate.setValue(rate);
    }

    @Override
    public void onClickSpeaker(View v) {
        v.setSelected(!v.isSelected());
        TextView tv = (TextView) v.findViewById(R.id.tv_speaker);
        tv.setText(v.isSelected() ? "ON" : "OFF");

    }


    /**************************************************************/
    public ProcessListFragment() {
        // Required empty public constructor
    }

    public static ProcessListFragment create(int pageNumber) {
        if (instance == null) {
            instance = new ProcessListFragment();
        }
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        instance.setArguments(args);
        return instance;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.notifyProcess();
            Log.d("lifecycle", "service con");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            Log.d("lifecycle", "service discon");
            mSpeechService = null;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = ((ListActivity) getActivity()).realm;
        if (realm == null) {
            realm.init(getContext());
            ((ListActivity) getActivity()).realm = Realm.getDefaultInstance();
            realm = ((ListActivity) getActivity()).realm;
        }
        mPageNumber = getArguments().getInt("page");
        EventBus.getDefault().register(this);

        Log.d("lifecycle", "process create");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("lifecycle", "process resume");
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);

        }

        Intent intent = new Intent(getActivity(), SpeechService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mServiceConnection, getActivity().BIND_AUTO_CREATE);

        binding.svMain.smoothScrollTo(0,0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        recogMode.setValue(0);
        sampleRate.setValue(0);
        speaker.setValue(false);
        curFolder.setValue("");
        Log.d("lifecycle", "process oncreateview");
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_process_list, container, false);
        View view = binding.getRoot();
        //here data must be an instance of the class MarsDataProvider
//        binding.includeRecord.setRecord(record);
//        binding.includeFile.setRecord(file);
//        binding.includeRecord.setHandler(this);
//        binding.includeRecord.setItemHandler(this);
        ArrayList<Integer> sampleRateList = AudioUtil.getAvailableSampleRate();
        for (int i : sampleRateList) {
            switch (i) {
                case LOW:
                    binding.cvLow.setClickable(true);
                    break;
                case MID:
                    binding.cvMid.setClickable(true);
                    break;
                case HIGH:
                    binding.cvHigh.setClickable(true);
                    break;

            }
        }

        binding.setRecogMode(recogMode);
        binding.setSamplerate(sampleRate);
        binding.setSpeaker(speaker);
        binding.setCurFolder(curFolder);
        binding.setHandler(this);
        binding.setItemHandler(this);

        originalRecyclerView = binding.rvOriginTags;
        addRecyclerView = binding.rvAddTags;
        editTextTag = binding.etTag;
        editTextTitle = binding.etTitle;
        svMain = binding.svMain;
//        svMain.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
////                scrollAnimator.cancel();
//                if(scrollAnimator.isRunning()){
//
//                    scrollAnimator.end();
//                }
//                return false;
//            }
//        });

        editTextTag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    RealmResults<TagRealm> tags = realm.where(TagRealm.class).findAll();
                    originalTags.clear();
                    for (TagRealm tag : tags) {
                        originalTags.add(tag);
                    }
                    originalAdapter.notifyDataSetChanged();
                } else {
                    RealmResults<TagRealm> tags = realm.where(TagRealm.class).contains("name", s.toString()).findAll();
                    originalTags.clear();
                    for (TagRealm tag : tags) {
                        originalTags.add(tag);
                    }
                    originalAdapter.notifyDataSetChanged();
                }
            }
        });

        RealmResults<TagRealm> tags = realm.where(TagRealm.class).findAll();
        for (TagRealm tag : tags) {
            originalTags.add(tag);
        }

        originalAdapter = new TagRealmAdapter(getContext(), originalTags, new TagHandler() {
            @Override
            public void onClickTag(View v, TagRealm tag) {
                for (int i = 0; i < originalTags.size(); i++) {
                    if (originalTags.get(i).getId() == tag.getId()) {
                        originalTags.remove(i);
                        break;
                    }
                }
                addedTags.add(tag);
                addAdapter.notifyDataSetChanged();
                originalAdapter.notifyDataSetChanged();
            }
        });

        addAdapter = new TagRealmAdapter(getContext(), addedTags, new TagHandler() {
            @Override
            public void onClickTag(View v, TagRealm tag) {
                for (int i = 0; i < addedTags.size(); i++) {
                    if (addedTags.get(i).getId() == tag.getId()) {
                        addedTags.remove(i);
                        break;
                    }
                }
                originalTags.add(tag);
                addAdapter.notifyDataSetChanged();
                originalAdapter.notifyDataSetChanged();
            }
        });

        originalAdapter.setHasStableIds(true);
        originalLayout = new GridLayoutManager(getContext(), 3);
        originalRecyclerView.setAdapter(originalAdapter);
        originalRecyclerView.setLayoutManager(originalLayout);

        addAdapter.setHasStableIds(true);
        addLayout = new GridLayoutManager(getContext(), 3);
        addRecyclerView.setAdapter(addAdapter);
        addRecyclerView.setLayoutManager(addLayout);


        dirRecyclerView = binding.rvDir;
        dirRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        dirAdapter = new ListRealmAdapter(dirOrFiles, getActivity());
        dirRecyclerView.setAdapter(dirAdapter);
        moveTodir(getDefaultDirectory().getId());

        realm.addChangeListener(new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm realm) {
                moveTodir(currentDirId);
            }
        });

        return view;
    }


    private int currentDirId = -1;
    ObservableDTO<Integer> depth = new ObservableDTO<>();
    RealmList<RealmObject> dirOrFiles = new RealmList<>();

    private void moveTodir(int id) {
        DirectoryRealm dir = realm.where(DirectoryRealm.class).equalTo("id", id).findFirst();
        currentDirId = id;
        depth.setValue(dir.getDepth());
        dirOrFiles.clear();

        for (RealmObject o : dir.getDirectoryRealms()) {
            dirOrFiles.add(o);
        }
        curFolder.setValue("현재폴더: " + dir.getName());
        dirAdapter.updateData(dirOrFiles);
        dirAdapter.notifyDataSetChanged();
    }

    private DirectoryRealm getDefaultDirectory() {
        DirectoryRealm dir = realm.where(DirectoryRealm.class).equalTo("depth", 0).findFirst();
        if (dir == null) {
            realm.beginTransaction();
            dir = RealmUtil.createObject(realm, DirectoryRealm.class);
            dir.setDepth(0);
            dir.setName("default");
            realm.commitTransaction();
        }

        return dir;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (mSpeechService != null) {
            getActivity().unbindService(mServiceConnection);

            Log.d("lifecycle", "list unbind longrunningRequestRetrofit in stop");
        }

        Log.d("lifecycle", "process stop");
    }


    public void setRecordItem(int recordId) {
        this.recordId = recordId;
        realm.beginTransaction();
        record.setRealm(realm.where(RecordRealm.class).equalTo("id", recordId).findFirst());
        realm.commitTransaction();

    }

    public void setFileItem(int fileId) {
        this.fileId = fileId;
        realm.beginTransaction();

        file.setRealm(realm.where(RecordRealm.class).equalTo("id", fileId).findFirst());


        realm.commitTransaction();
    }

//    tempTitle, tempTags, mediaPath, REQUEST_AUDIO, currentDirId)

//    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
//    public void onMessageEvent(FileEvent event) {
//        EventBus.getDefault().removeStickyEvent(event);
//        title = event.getTitle();
//        tags = event.getTags();
//        filePath = event.getFilePath();
//        dirId = event.getDirId();
//        mSpeechService.createFileRecord(dirId);
//
//        switch (event.getRequestCode()) {
//            case REQUEST_AUDIO:
//                mSpeechService.recognizeFileStream(title, tags, filePath);
//                break;
//            case REQUEST_VIDEO:
//                mSpeechService.recognizeVideoStream(title, tags, filePath);
//                break;
//        }
//    }

    private void startFileRecognition(String title, ArrayList<Integer> tags, String filePath, int dirId, int requestCode) {
        mSpeechService.createFileRecord(dirId);
        switch (requestCode) {
            case REQUEST_AUDIO:
                mSpeechService.recognizeFileStream(title, tags, filePath);
                break;
            case REQUEST_VIDEO:
                mSpeechService.recognizeVideoStream(title, tags, filePath);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessIdEvent(DirEvent event) {
        moveTodir(event.getId());
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onPartialTimerEvent(PartialRecordEvent event) {
        if (binding == null && record == null) {
        } else {
            binding.tvRecordProcess.setText(DateUtil.durationToNumFormat(event.getSecond() * 1000));
        }
        EventBus.getDefault().removeStickyEvent(event);
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onPartialTimerEvent(PartialFileEvent event) {
        if (binding == null && record == null) {
        } else {
            binding.tvFileProcess.setText(event.getMessage());
        }
        EventBus.getDefault().removeStickyEvent(event);
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessIdEvent(ProcessIdEvent event) {


        if (event.isRecording()) {
            binding.cvRecord.setVisibility(View.VISIBLE);
            realm.beginTransaction();
            record.setRealm(realm.where(RecordRealm.class).equalTo("id", event.getRecordId()).findFirst());
            realm.commitTransaction();
            binding.tvRecordTitle.setText(record.getTitle());
        } else {
            binding.cvRecord.setVisibility(View.GONE);
        }
        if (event.isFiling()) {
            binding.cvFile.setVisibility(View.VISIBLE);
            realm.beginTransaction();
            record.setRealm(realm.where(RecordRealm.class).equalTo("id", event.getFileId()).findFirst());
            realm.commitTransaction();
            binding.tvFileTitle.setText(record.getTitle());
        } else {
            binding.cvFile.setVisibility(View.GONE);
        }


    }


    @Override
    public void onClickStartRecognition(View view) {
        String title = binding.etTitle.getText().toString();
        ArrayList<Integer> tagIds = new ArrayList<Integer>();
        File file = new File(FileUtil.getFilename(title));
        if (file.exists()) {
            Toast.makeText(getContext(), "이미 존재하는 제목 입니다", Toast.LENGTH_SHORT).show();
            svMain.smoothScrollTo(0, binding.tvTitle.getTop());
            return;
        } else if (title.equals("")) {
            Toast.makeText(getContext(), "제목을 입력하세요", Toast.LENGTH_SHORT).show();
            svMain.smoothScrollTo(0, binding.tvTitle.getTop());
            return;
        } else {
            if (addedTags.size() != 0) {
                for (TagRealm t : addedTags) {
                    tagIds.add(t.getId());
                }
            } else {
                Toast.makeText(getContext(), "태그를 지정하세요", Toast.LENGTH_SHORT).show();
                svMain.smoothScrollTo(0, binding.tvTagList.getTop());
                return;
            }
        }

        if (recogMode.getValue() == REQUEST_RECORD_AUDIO_PERMISSION) {
            int sampleRateInteger = LOW;
            switch (sampleRate.getValue()) {
                case 0:
                    sampleRateInteger = LOW;
                    break;
                case 1:
                    sampleRateInteger = MID;
                    break;
                case 2:
                    sampleRateInteger = HIGH;
                    break;
                default:
                    Toast.makeText(getContext(), "녹음을 할수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
            }

            Intent intent = new Intent(getContext(), RecordActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("sampleRate", sampleRateInteger);
            intent.putExtra("tags", Parcels.wrap(tagIds));
            intent.putExtra("dirId", currentDirId);
            startActivity(intent);
        } else if (recogMode.getValue() == REQUEST_FILE_AUDIO_PERMISSION) {
            tempTitle = title;
            tempTags = tagIds;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            startActivityForResult(intent, REQUEST_AUDIO);

        } else if (recogMode.getValue() == REQUEST_FILE_VIDEO_PERMISSION) {
            tempTitle = title;
            tempTags = tagIds;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            startActivityForResult(intent, REQUEST_VIDEO);
        }

        Toast.makeText(getContext(), String.valueOf(recogMode.getValue()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClickTagAdd(View v) {
        String tagName = binding.etTag.getText().toString();
        TagRealm tag = realm.where(TagRealm.class).equalTo("name", tagName).findFirst();

        if (tag == null) {
            realm.beginTransaction();
            tag = RealmUtil.createObject(realm, TagRealm.class);
            tag.setName(tagName);
            tag.setCount(1);
            Random rnd = new Random();
            tag.setColorCode(rnd.nextInt(200));
            realm.commitTransaction();
            originalTags.add(tag);
            originalAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(getContext(), "이미 존재하는 이름입니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String mediaPath = FileUtil.getPath(getActivity(), data.getData());
        startFileRecognition(tempTitle, tempTags, mediaPath, currentDirId, requestCode);

    }


    @Override
    public void onClickStopRecord(View view) {
        if (mSpeechService == null) {
            this.mSpeechService = ((ListActivity) getActivity()).mSpeechService;
        }

        this.mSpeechService.stopSpeechRecognizing();
        Log.d(TAG, "stop");
//        binding.includeRecord
    }

    @Override
    public void onClickStopFile(View view) {

    }

    @Override
    public void onRecordItemClick(View view) {
        Intent intent = new Intent(getActivity(), RecordActivity.class);
        intent.putExtra("id", recordId);
        getActivity().startActivity(intent);
    }

    @Override
    public void onClickMakeDir(View view) {
        final DialogRenameDirBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_rename_dir, null, false);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(binding.getRoot());

        binding.setTitle("새 폴더");
        binding.setCategory1("이름");
        final Dialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.findViewById(R.id.rl_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = binding.etRename.getText().toString();
                if (s.length() == 0) {
                    Toast.makeText(getContext(), "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    DirectoryRealm directoryRealm = realm.where(DirectoryRealm.class).equalTo("id", currentDirId).findFirst();
                    realm.beginTransaction();
                    DirectoryRealm newDir = RealmUtil.createObject(realm, DirectoryRealm.class);
                    newDir.setName(binding.etRename.getText().toString());
                    newDir.setDepth(directoryRealm.getDepth() + 1);
                    newDir.setUpperId(directoryRealm.getId());
                    directoryRealm.getDirectoryRealms().add(newDir);
                    realm.commitTransaction();
                    dialog.dismiss();
                }
            }
        });
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


    }

}
