package com.folioreader.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.folioreader.Config;
import com.folioreader.R;
import com.folioreader.tts.HTMLParser;
import com.folioreader.tts.TextToSpeechWrapper;
import com.folioreader.ui.base.HtmlTask;
import com.folioreader.ui.base.HtmlTaskCallback;
import com.folioreader.util.AppUtil;
import com.folioreader.util.UiUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TTSActivity extends AppCompatActivity implements HtmlTaskCallback {
    public final static String RESUME_POINT_EXTRA = "TTS_ACTIVITY_RESUME_POINT_EXTRA";
    public final static String SENTENCE_EXTRA = "TTS_ACTIVITY_SENTENCE_EXTRA";
    private final static String TAG = TTSActivity.class.getSimpleName();
    private final static String STREAM_URL_EXTRA = "TTS_STREAM_URL_EXTRA";
    private final static String CHAPTER_URLS_EXTRA = "TTS_CHAPTER_URLS_EXTRA";
    private final static String CURRENT_CHAPTER_EXTRA = "TTS_CURRENT_CHAPTER_EXTRA";
    //
    private String streamUrl;
    private ArrayList<String> chapterUrlList;
    private int currentChapterIndex;
    ///
    private int highlightColor;
    private TextView mContentView;
    private ScrollView mScrollLayout;
    private ImageButton mPlayBtn;
    //
    private TextToSpeechWrapper mTtsWrapper;
    private int mSentenceNumber = 0;
    private List<String> mSentences;
    private boolean mIsSpeaking = false;
    private String mContentText;
    private TextToSpeech.OnUtteranceCompletedListener mCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {

        @Override
        public void onUtteranceCompleted(String s) {
            mSentenceNumber++;
            speak();
        }
    };
    private int checkedItem;

    public static Intent getStartIntent(Context context,
                                        String streamUrl, ArrayList<String> chapterUrlList,
                                        int currentChapterIndex, String resumePoint) {
        Intent startIntent = new Intent(context, TTSActivity.class);
        startIntent.putExtra(STREAM_URL_EXTRA, streamUrl);
        startIntent.putExtra(CHAPTER_URLS_EXTRA, chapterUrlList);
        startIntent.putExtra(CURRENT_CHAPTER_EXTRA, currentChapterIndex);
        startIntent.putExtra(RESUME_POINT_EXTRA, resumePoint);
        return startIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        if (getIntent() != null) {
            Intent intent = getIntent();
            streamUrl = intent.getStringExtra(STREAM_URL_EXTRA);
            chapterUrlList = intent.getStringArrayListExtra(CHAPTER_URLS_EXTRA);
            currentChapterIndex = intent.getIntExtra(CURRENT_CHAPTER_EXTRA, 0);
            TTSResumePoint resumePoint = TTSResumePoint.parseResumePoint(getIntent().getStringExtra(RESUME_POINT_EXTRA));
            if (resumePoint.getChapterIndex() == currentChapterIndex)
                mSentenceNumber = resumePoint.getSentenceIndex();
            else
                mSentenceNumber = 0;
        }

        if (streamUrl == null || chapterUrlList == null || chapterUrlList.isEmpty()) {
            Log.e(TAG, "url info is not provided");
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Config mConfig = AppUtil.getSavedConfig(this);
        boolean mIsNightMode = mConfig != null && mConfig.isNightMode();

        mContentView = findViewById(R.id.mainContent);
        mScrollLayout = findViewById(R.id.layout_summary);

        mTtsWrapper = new TextToSpeechWrapper(TTSActivity.this);

        mPlayBtn = findViewById(R.id.play_pause_button);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsSpeaking)
                    speak();
                else
                    pauseTTS();
            }
        });

        findViewById(R.id.voice_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Voice> voices = mTtsWrapper.getVoices();
                Voice selected = mTtsWrapper.getCurrentVoice();
                if (voices == null || selected == null)
                    return;

                String[] animals = new String[voices.size()];
                int index = 0;
                checkedItem = -1;
                for (Voice v : voices) {
                    animals[index] = v.getName();
                    if (Objects.equals(v.getName(), selected.getName()))
                        checkedItem = index;
                    index++;
                }

                // setup the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(TTSActivity.this);
                builder.setTitle("Select a voice");
                // add a radio button list
                builder.setSingleChoiceItems(animals, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // user checked an item
                        checkedItem = which;
                    }
                });
                // add OK and Cancel buttons
                builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichBtn) {
                        if (checkedItem != -1) {
                            Voice voice = voices.get(checkedItem);
                            Config config = AppUtil.getSavedConfig(TTSActivity.this);
                            config.setVoiceName(voice.getName());
                            AppUtil.saveConfig(TTSActivity.this, config);
                            mTtsWrapper.setVoice(voice);
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);
                // create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        if (mConfig != null)
            UiUtil.setColorIntToDrawable(mConfig.getThemeColor(), ((ImageView) findViewById(R.id.btn_close)).getDrawable());

        if (mIsNightMode) {
            findViewById(R.id.toolbar).setBackgroundColor(ContextCompat.getColor(this, R.color.black));
            findViewById(R.id.layout_summary).setBackgroundColor(ContextCompat.getColor(this, R.color.night_background_color));
            mContentView.setTextColor(ContextCompat.getColor(this, R.color.night_default_font_color));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int color;
            if (mIsNightMode) {
                color = ContextCompat.getColor(this, R.color.black);
            } else {
                int[] attrs = {android.R.attr.navigationBarColor};
                TypedArray typedArray = getTheme().obtainStyledAttributes(attrs);
                color = typedArray.getColor(0, ContextCompat.getColor(this, R.color.white));
            }
            getWindow().setNavigationBarColor(color);
        }

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeActivity();
            }
        });

        highlightColor = ContextCompat.getColor(this,
                R.color.highlight_yellow);

        initContent(mSentenceNumber);

    }

    private void initContent(int sentenceIndex) {
        mSentences = new ArrayList<>();
        mContentText = "";
        String chapterUrl = streamUrl + chapterUrlList.get(currentChapterIndex).substring(1);
        mSentenceNumber = sentenceIndex;
        new HtmlTask(this).execute(chapterUrl);
        mPlayBtn.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsSpeaking)
            pauseTTS();
        if (mTtsWrapper != null)
            mTtsWrapper.onDestroy();
    }

    @Override
    public void onBackPressed() {
        closeActivity();
    }

    private void closeActivity() {
        Intent result = new Intent();
        result.putExtra(RESUME_POINT_EXTRA, new TTSResumePoint(currentChapterIndex, mSentenceNumber).serialize());
        if (mSentenceNumber >= 0 && mSentenceNumber < mSentences.size())
            result.putExtra(SENTENCE_EXTRA, mSentences.get(mSentenceNumber));
        setResult(RESULT_OK, result);
        finish();
    }

    void speak() {
        if (mSentences != null && mTtsWrapper != null) {
            Log.d(TAG, "tts> continue reading...");
            if (mSentenceNumber <= mSentences.size() - 1) {
                mIsSpeaking = true;
                mPlayBtn.setImageResource(R.drawable.ic_pause);
                String sentence = mSentences.get(mSentenceNumber);
                mTtsWrapper.speak(sentence, mCompletedListener);
                highlight(sentence);
                scrollTo(sentence);
            } else {
                if (currentChapterIndex < chapterUrlList.size()) {
                    currentChapterIndex++;
                    initContent(0);
                } else {
                    pauseTTS();
                }
            }
        }
    }

    void pauseTTS() {
        if (this.mTtsWrapper != null && mIsSpeaking) {
            mTtsWrapper.stop();
            mIsSpeaking = false;
            mContentView.setText(mContentText);
            mPlayBtn.setImageResource(R.drawable.ic_play);
        }
    }

    void highlight(String sentence) {
        int start = mContentText.indexOf(sentence);
        SpannableString str = new SpannableString(mContentText);
        if (start >= 0) {
            str.setSpan(new BackgroundColorSpan(highlightColor), start, start + sentence.length(), 0);
        } else {
            Log.d(TAG, " tts > couldn't highlight");
        }
        mContentView.setText(str);
    }

    void scrollTo(String sentence) {
        if (sentence == null || sentence.trim().isEmpty() || mContentView.getLayout() == null)
            return;
        int indexOfWord = mContentText.indexOf(sentence);
        int lineNumber = mContentView.getLayout().getLineForOffset(indexOfWord);
        int offset = mContentView.getLayout().getLineTop(lineNumber);
        mScrollLayout.smoothScrollTo(0, offset);
    }

    @Override
    public void onReceiveHtml(String html) {
        mContentText = HTMLParser.getText(html);
        mSentences = HTMLParser.parseSentences(html);
        if (mSentenceNumber >= mSentences.size())
            mSentenceNumber = 0;
        mContentView.setText(mContentText);
        mPlayBtn.setEnabled(true);
        if (mIsSpeaking) {
            speak();
        }
    }

    @Override
    public void onError() {
        Log.e(TAG, "couldn't load the content");
    }

    static class TTSResumePoint {
        private final int chapterIndex;
        private final int sentenceIndex;

        public TTSResumePoint(int chapterIndex, int sentenceIndex) {
            this.chapterIndex = chapterIndex;
            this.sentenceIndex = sentenceIndex;
        }

        public static TTSResumePoint parseResumePoint(String resumeString) {
            TTSResumePoint resumePoint = new TTSResumePoint(-1, 0);
            if (resumeString == null) {
                return resumePoint;
            }
            String[] tokens = resumeString.split(":");
            if (tokens.length != 2) {
                return resumePoint;
            }
            try {
                int resumeChapter = Integer.parseInt(tokens[0]);
                int resumeSentence = Integer.parseInt(tokens[1]);
                return new TTSResumePoint(resumeChapter, resumeSentence);
            } catch (NumberFormatException ignore) {
            }
            return resumePoint;
        }

        public int getChapterIndex() {
            return chapterIndex;
        }

        public int getSentenceIndex() {
            return sentenceIndex;
        }

        public String serialize() {
            return chapterIndex + ":" + sentenceIndex;
        }
    }
}