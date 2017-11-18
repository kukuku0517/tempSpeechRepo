package com.google.cloud.android.speech.util;

import com.google.cloud.android.speech.data.realm.ClusterRealm;
import com.google.cloud.android.speech.data.realm.DirectoryRealm;
import com.google.cloud.android.speech.data.realm.PrimaryRealm;
import com.google.cloud.android.speech.data.realm.RecordRealm;
import com.google.cloud.android.speech.data.realm.SentenceRealm;
import com.google.cloud.android.speech.data.realm.TagRealm;
import com.google.cloud.android.speech.data.realm.WordRealm;
import com.google.cloud.android.speech.data.realm.primitive.IntegerRealm;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by samsung on 2017-10-07.
 */

public class RealmUtil {

    public static <T extends PrimaryRealm> T createObject(Realm realm, Class<T> clas) {

        RealmResults<T> result = realm.where(clas).findAllSorted("id");
        int lastId;
        if (result.size() == 0) {
            lastId = 0;
        } else {
            lastId = result.last().getId();
        }
        T record = realm.createObject(clas, lastId + 1);
        return record;
    }

    public static void updateWordRealm(WordRealm word, SentenceRealm sentence, String newWord) {
        word.setWord(newWord);
        StringBuilder builder = new StringBuilder();
        for (WordRealm wordRealm : sentence.getWordList()) {
            builder.append(wordRealm.getWord() + " ");
        }
        sentence.setSentence(builder.toString());
    }

    public static void mergeSentence(Realm realm, int recordId, int fromId, int toId, int from, int to) {
        RecordRealm record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
        SentenceRealm sentenceFrom = realm.where(SentenceRealm.class).equalTo("id", fromId).findFirst();
        SentenceRealm sentenceTo = realm.where(SentenceRealm.class).equalTo("id", toId).findFirst();
        RealmList<WordRealm> wordsFrom = sentenceFrom.getWordList();
        RealmList<WordRealm> wordsTo = sentenceTo.getWordList();

        if (sentenceFrom == null || sentenceTo == null) return;

        if (sentenceFrom.getStartMillis() < sentenceTo.getStartMillis()) {
            for (WordRealm word : wordsTo) {
                wordsFrom.add(word);
            }
            sentenceFrom.setSentence();
            sentenceFrom.setEndMillis(sentenceTo.getEndMillis());
            record.getSentenceRealms().remove(to);
            sentenceTo.deleteFromRealm();
        } else {
            for (WordRealm word : wordsFrom) {
                wordsTo.add(word);
            }
            sentenceTo.setSentence();
            sentenceTo.setEndMillis(sentenceFrom.getEndMillis());
            record.getSentenceRealms().remove(from);
            sentenceFrom.deleteFromRealm();
        }
    }

    public static void splitSentence(Realm realm, int recordId, int position, int sentenceId, int wordId, int clusterId) {

        RecordRealm record = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
        SentenceRealm origin = realm.where(SentenceRealm.class).equalTo("id", sentenceId).findFirst();
        WordRealm word = realm.where(WordRealm.class).equalTo("id", wordId).findFirst();
        SentenceRealm add = createObject(realm, SentenceRealm.class);
        ClusterRealm cluster = realm.where(ClusterRealm.class).equalTo("id", clusterId).findFirst();


        add.setEndMillis(origin.getEndMillis());
        origin.setEndMillis(word.getStartMillis());
        add.setStartMillis(word.getStartMillis());
        add.setCluster(cluster);


        RealmList<WordRealm> originWords = new RealmList<>();
        RealmList<WordRealm> addWords = new RealmList<>();

        RealmList<WordRealm> words = origin.getWordList();

        boolean split = false;
        for (WordRealm w : words) {
            if (w.getId() == wordId) split = true;
            if (!split) {
                originWords.add(w);
            } else {
                addWords.add(w);
            }
        }

        origin.setWordList(originWords);
        add.setWordList(addWords);
        origin.setSentence();
        add.setSentence();
        record.getSentenceRealms().add(position + 1, add);
    }

    public static int duplicateRecord(Realm realm, int recordId) {
        RecordRealm origin = realm.where(RecordRealm.class).equalTo("id", recordId).findFirst();
        RecordRealm copy = createObject(realm, RecordRealm.class);

        origin.setDuplicateId(copy.getId());
        copy.setStartMillis(origin.getStartMillis());
        copy.setVideoPath(origin.getVideoPath());
        copy.setAudioPath(origin.getAudioPath());
        copy.setDuration(origin.getDuration());
        copy.setTitle(origin.getTitle());
        copy.setOrigin(false);


        for (SentenceRealm oSentence : origin.getSentenceRealms()) {

            SentenceRealm cSentence = createObject(realm, SentenceRealm.class);
            cSentence.setStartMillis(oSentence.getStartMillis());
            cSentence.setEndMillis(oSentence.getEndMillis());
            cSentence.setSentence(oSentence.getSentence());

            for (WordRealm oWord : oSentence.getWordList()) {
                WordRealm cWord = createObject(realm, WordRealm.class);
                cWord.setWord(oWord.getWord());
                cWord.setHighlight(oWord.isHighlight());
                cWord.setStartMillis(oWord.getStartMillis());
                cWord.setEndMillis(oWord.getEndMillis());
                cWord.setSentenceId(cSentence.getId());
                cSentence.getWordList().add(cWord);
            }

            copy.getSentenceRealms().add(cSentence);

        }

        for (IntegerRealm oCluster : origin.getCluster()) {
            copy.getCluster().add(oCluster);
        }

        for (TagRealm tagRealm : origin.getTagList()) {
            copy.getTagList().add(tagRealm);
        }


        return copy.getId();

    }

    public static void dirTodir(Realm realm, int fromId, int toId) {

        DirectoryRealm from = realm.where(DirectoryRealm.class).equalTo("id", fromId).findFirst();
        DirectoryRealm to = realm.where(DirectoryRealm.class).equalTo("id", toId).findFirst();
        DirectoryRealm upper = realm.where(DirectoryRealm.class).equalTo("id", from.getUpperId()).findFirst();

        int depth = to.getDepth() + 1;

        for(int i=0;i<upper.getDirectoryRealms().size();i++){
            if(upper.getDirectoryRealms().get(i).getId()==fromId){
                upper.getDirectoryRealms().remove(i);
                break;
            }
        }
        from.setUpperId(toId);
        from.setDepth(depth);
        to.getDirectoryRealms().add(from);

    }

    public static void recordToDir(Realm realm, int fromId, int toId){

        RecordRealm from = realm.where(RecordRealm.class).equalTo("id", fromId).findFirst();
        DirectoryRealm to = realm.where(DirectoryRealm.class).equalTo("id", toId).findFirst();
        DirectoryRealm upper = realm.where(DirectoryRealm.class).equalTo("id", from.getDirectoryId()).findFirst();

        if(upper!=null){
            for(int i=0;i<upper.getRecordRealms().size();i++){
                if(upper.getRecordRealms().get(i).getId()==fromId){
                    upper.getRecordRealms().remove(i);
                    break;
                }
            }
        }

        from.setDirectoryId(toId);
        to.getRecordRealms().add(from);

    }
}
