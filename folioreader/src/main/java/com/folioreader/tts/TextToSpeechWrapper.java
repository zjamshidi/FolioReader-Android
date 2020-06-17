package com.folioreader.tts;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class TextToSpeechWrapper {
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
                        if (mTts.isLanguageAvailable(Locale.UK) != 0) {
                            mTts.setLanguage(Locale.UK);
                        } else {
                            mTts.setLanguage(Locale.US);
                        }
                        mTts.setOnUtteranceCompletedListener(mCompletedListener);
                    }
                }
            };

    public TextToSpeechWrapper(Activity activity) {
        if (mTts == null) {
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
        Log.d("TextToSpeechWrapper", "tts> speak '" + text + "'");
        if (mTts != null) {
            mExternalCompletedListener = listener;
            Set<String> a = new HashSet<>();
            a.add("male");//here you can give male if you want to select male voice.
            //Voice v=new Voice("en-us-x-sfg#female_2-local",new Locale("en","US"),400,200,true,a);
            Voice v = new Voice("en-us-x-sfg#male_2-local", new Locale("en", "US"), 400, 200, true, a);
            mTts.setVoice(v);

            HashMap<String, String> params = new HashMap<String, String>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "end");
            mTts.speak(text, TextToSpeech.QUEUE_ADD, params);
            mTts.playSilence(200, TextToSpeech.QUEUE_ADD, null);
        }
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
