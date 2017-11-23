package com.google.cloud.android.speech.view.recordList.fragment;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.cloud.android.speech.data.DTO.ObservableDTO;
import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.R;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.databinding.DialogRenameDirBinding;
import com.google.cloud.android.speech.event.DirEvent;
import com.google.cloud.android.speech.event.QueryEvent;
import com.google.cloud.android.speech.util.RealmUtil;
import com.google.cloud.android.speech.view.customView.rvInteractions.ItemTouchHelperCallBack;
import com.google.cloud.android.speech.view.recordList.adapter.ListRealmAdapter;
import com.google.cloud.android.speech.view.recordList.ListActivity;
import com.google.cloud.android.speech.view.recordList.handler.ListHandler;
import com.google.cloud.android.speech.event.ProcessIdEvent;
import com.google.cloud.android.speech.view.background.SpeechService;
import com.google.cloud.android.speech.databinding.FragmentResultListBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class ResultListFragment extends Fragment implements ListHandler {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_FILE_AUDIO_PERMISSION = 2;
    private static final int REQUEST_FILE_VIDEO_PERMISSION = 3;
    RecyclerView recyclerView;
    ListRealmAdapter adapter;
    Realm realm;
    FragmentResultListBinding binding;
    private SpeechService mSpeechService;
    RealmList<RealmObject> dirOrFiles = new RealmList<>();

    private int currentDirId = -1;
    ObservableDTO<Integer> depth = new ObservableDTO<>();
    ObservableDTO<String> currentFolder = new ObservableDTO<>();

    public ResultListFragment() {
        // Required empty public constructor
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

    public static ResultListFragment create(int pageNumber) {
        ResultListFragment fragment = new ResultListFragment();
        Bundle args = new Bundle();
        args.putInt("page", pageNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSpeechService != null) {
            getActivity().unbindService(mServiceConnection);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = ((ListActivity) getActivity()).realm;
        if (realm == null) {
            realm.init(getContext());
            ((ListActivity) getActivity()).realm = Realm.getDefaultInstance();
            realm = ((ListActivity) getActivity()).realm;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = new Intent(getActivity(), SpeechService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result_list, container, false);
        binding = DataBindingUtil.bind(view);
        binding.setHandler(this);
        binding.setDepth(depth);
        binding.setCurrentFolder(currentFolder);

//        for (final RecordRealm record : result) {
//            if (record.getDuration() == 0) {
//                String filePath = recRord.getAudioPath();
//                Uri mUri = Uri.fromFile(new File(filePath));
//                //TODO uncomment this
//                try {
//                    MediaPlayer mediaPlayer = new MediaPlayer();
//                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                    mediaPlayer.setDataSource(getActivity(), mUri);
//                    mediaPlayer.prepareAsync();
//                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                        @Override
//                        public void onPrepared(MediaPlayer mp) {
//                            realm.beginTransaction();
//                            record.setDuration(mp.getDuration());
//                            realm.commitTransaction();
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }

        recyclerView = binding.rvRecord;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ListRealmAdapter(dirOrFiles, getActivity());

        recyclerView.setAdapter(adapter);

        moveTodir(getDefaultDirectory().getId());

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallBack(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);


        realm.addChangeListener(new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm realm) {
                moveTodir(currentDirId);

            }
        });

        binding.rvRecord.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d("rv", String.valueOf(recyclerView.getVerticalScrollbarPosition() +":" +newState));
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        if (recyclerView.getVerticalScrollbarPosition()<0||recyclerView.getVerticalScrollbarPosition()>recyclerView.getScrollBarSize()||binding.fab.getVisibility() == View.GONE)
                            binding.fab.show();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        if (binding.fab.getVisibility() == View.VISIBLE)
                            binding.fab.hide();
                        break;

                }
            }
        });
        return view;
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessIdEvent(ProcessIdEvent event) {

//        if (event.isRecording()) {
//            binding.btnRecord.setEnabled(false);
//        } else {
//            binding.btnRecord.setEnabled(true);
//        }
//        if (event.isFiling()) {
//            binding.btnFile.setEnabled(false);
//        } else {
//            binding.btnFile.setEnabled(true);
//        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onProcessIdEvent(DirEvent event) {
        moveTodir(event.getId());

    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onPartialEvent(QueryEvent event) {
        String query = event.getQuery();
        if (query.length() == 0) {
            moveTodir(getDefaultDirectory().getId());
        } else {
            query(query);
        }
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

    @Override
    public void onClickBack(View v) {
        DirectoryRealm dir = realm.where(DirectoryRealm.class).equalTo("id", currentDirId).findFirst();
        moveTodir(dir.getUpperId());

    }

    private void moveTodir(int id) {
        DirectoryRealm dir = realm.where(DirectoryRealm.class).equalTo("id", id).findFirst();
        currentDirId = id;
        depth.setValue(dir.getDepth());

        int count = dir.getDepth();
        String path = dir.getName();
        int tempId = dir.getUpperId();
        while (count > 0) {
            DirectoryRealm tempDir = realm.where(DirectoryRealm.class).equalTo("id", tempId).findFirst();
            path = tempDir.getName() + "/" + path;
            tempId = tempDir.getUpperId();
            count--;
        }
        currentFolder.setValue(path);


        dirOrFiles.clear();


        for (RealmObject o : dir.getRecordRealms()) {
            if (((RecordRealm) o).isConverted()) {
                dirOrFiles.add(o);
            }
        }
        for (RealmObject o : dir.getDirectoryRealms()) {
            dirOrFiles.add(o);
        }

        Collections.sort(dirOrFiles, new Comparator<RealmObject>() {
            @Override
            public int compare(RealmObject o1, RealmObject o2) {
                if (o1 instanceof RecordRealm) {
                    if (o2 instanceof RecordRealm) {
                        return (int) (((RecordRealm) o2).getStartMillis() - ((RecordRealm) o1).getStartMillis());
                    } else {
                        return 1;
                    }
                } else {
                    if (o2 instanceof RecordRealm) {
                        return -1;
                    } else {
                        return 0;
                    }
                }

            }
        });

        adapter.updateData(dirOrFiles);
        adapter.notifyDataSetChanged();
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


    public void query(String query) {
        RealmResults<RecordRealm> recordRealms = realm.where(RecordRealm.class).contains("title", query).findAll();
        RealmResults<TagRealm> tagRealms = realm.where(TagRealm.class).contains("name", query).findAll();

        HashMap<Integer, RecordRealm> hashRealm = new HashMap<>();
        for (RecordRealm r : recordRealms) {
            hashRealm.put(r.getId(), r);
        }
        for (TagRealm tag : tagRealms) {
            for (RecordRealm r : tag.getRecords()) {
                hashRealm.put(r.getId(), r);
            }
        }

        dirOrFiles.clear();

        Iterator it = hashRealm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RecordRealm> pair = (Map.Entry) it.next();
            dirOrFiles.add(pair.getValue());
            it.remove();
        }

        adapter.updateData(dirOrFiles);
        adapter.notifyDataSetChanged();

    }
}
