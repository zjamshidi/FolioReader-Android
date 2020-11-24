package com.folioreader.tts;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import com.folioreader.Config;
import com.folioreader.util.AppUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class TextToSpeechWrapper {
    private Context mContext;
    private TextToSpeech mTts;
    private TextToSpeech.OnUtteranceCompletedListener mExternalCompletedListener;
    private TextToSpeech.OnUtteranceCompletedListener mCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {

        @Override
        public void onUtteranceCompleted(String s) {
            if (mExternalCompletedListener != null) {
                mExternalCompletedListener.onUtteranceCompleted(s);
            }
        }
    };
    private TextToSpeech.OnInitListener mOnInitListener =
            new TextToSpeech.OnInitListener() {

                @SuppressWarnings("deprecation")
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        mTts.setLanguage(Locale.US);
                        mTts.setOnUtteranceCompletedListener(mCompletedListener);
                    }
                }
            };

    public TextToSpeechWrapper(Activity activity) {
        if (mTts == null) {
            mContext = activity;
            mTts = new TextToSpeech(activity, mOnInitListener);
        }
    }

    public void onDestroy() {
        if (mTts != null) {
            mTts.shutdown();
        }
        mTts = null;
    }

    public void speak(String text, TextToSpeech.OnUtteranceCompletedListener listener) {
//        Log.d("TextToSpeechWrapper", "tts> speak '" + text + "'");
        if (mTts != null) {
            mExternalCompletedListener = listener;
            Config config = AppUtil.getSavedConfig(mContext);
            String lastSelectedVoiceName = config.getVoiceName();

            Voice lastSelectedVoice = null;
            for (Voice v : mTts.getVoices()) {
                if (Objects.equals(v.getName(), lastSelectedVoiceName)) {
                    lastSelectedVoice = v;
                    break;
                }
            }
            if (lastSelectedVoice != null)
                mTts.setVoice(lastSelectedVoice);

            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "end");
            mTts.speak(text, TextToSpeech.QUEUE_ADD, params);
            mTts.playSilence(200, TextToSpeech.QUEUE_ADD, null);
        }
    }

    public void setVoice(Voice voice) {
        mTts.setVoice(voice);
    }

    public List<Voice> getVoices() {
        Set<Voice> voices = mTts != null ? mTts.getVoices() : null;
        List<Voice> possible = new ArrayList<>();
        if (voices == null)
            return possible;

        for (Voice v : voices) {
            if (v.getLocale() == Locale.US)
                possible.add(v);
        }
        return possible;
    }

    public Voice getCurrentVoice() {
        return mTts != null ? mTts.getVoice() : null;
    }

    public void stop() {
        if (mTts != null) {
            // need to disconnect the listener, otherwise it gets called
            // and will usually feed in more.
            mExternalCompletedListener = null;
            mTts.stop();
        }
    }

}
