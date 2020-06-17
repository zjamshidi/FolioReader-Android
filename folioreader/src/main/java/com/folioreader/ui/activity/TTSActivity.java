package com.folioreader.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.R;
import com.folioreader.tts.HTMLParser;
import com.folioreader.tts.TextToSpeechWrapper;
import com.folioreader.ui.base.HtmlTask;
import com.folioreader.ui.base.HtmlTaskCallback;
import com.folioreader.util.AppUtil;
import com.folioreader.util.UiUtil;

import java.util.ArrayList;
import java.util.List;

public class TTSActivity extends AppCompatActivity implements HtmlTaskCallback {
    private final static String TAG = TTSActivity.class.getSimpleName();
    private final static String URL_EXTRA = "TTS_ACTIVITY_URL_EXTRA";
    private final static String INDEX_EXTRA = "TTS_ACTIVITY_INDEX_EXTRA";

    private int highlightColor;

    private TextToSpeechWrapper mTtsWrapper;
    private int mSentenceNumber = 0;
    private List<String> mSentences;
    private boolean mIsSpeaking = false;
    private String mContentText;
    private TextView mContentView;
    private ScrollView mScrollLayout;
    private ImageButton mPlayBtn;
    private TextToSpeech.OnUtteranceCompletedListener mCompletedListener = new TextToSpeech.OnUtteranceCompletedListener() {

        @Override
        public void onUtteranceCompleted(String s) {
            mSentenceNumber++;
            speak();
        }
    };

    public static Intent getStartIntent(Context context, String url, int index) {
        Intent startIntent = new Intent(context, TTSActivity.class);
        startIntent.putExtra(URL_EXTRA, url);
        startIntent.putExtra(INDEX_EXTRA, index);
        return startIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        String chapterUrl = getIntent().getStringExtra(URL_EXTRA);
        if (chapterUrl == null) {
            Log.e(TAG, "url is not provided");
            finish();
            return;
        }

        int index = getIntent().getIntExtra(INDEX_EXTRA, 0);
        if (index < 0) index = 0;
        mSentenceNumber = index;

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Config mConfig = AppUtil.getSavedConfig(this);
        boolean mIsNightMode = mConfig != null && mConfig.isNightMode();

        mContentView = findViewById(R.id.mainContent);
        mScrollLayout = findViewById(R.id.layout_summary);
        mSentences = new ArrayList<>();
        mContentText = "";
        new HtmlTask(this).execute(chapterUrl);

        mTtsWrapper = new TextToSpeechWrapper(TTSActivity.this);

        mPlayBtn = findViewById(R.id.play_pause_button);
        mPlayBtn.setEnabled(false);

        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsSpeaking)
                    speak();
                else
                    pauseTTS();
            }
        });

        if (mConfig != null)
            UiUtil.setColorIntToDrawable(mConfig.getThemeColor(), ((ImageView) findViewById(R.id.btn_close)).getDrawable());

        if (mIsNightMode) {
            findViewById(R.id.toolbar).setBackgroundColor(Color.BLACK);
            findViewById(R.id.layout_summary).setBackgroundColor(Color.BLACK);
            mContentView.setTextColor(Color.WHITE);
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
        result.putExtra(FolioReader.EXTRA_TTS_INDEX, mSentenceNumber);
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
                pauseTTS();
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
    }

    @Override
    public void onError() {
        Log.e(TAG, "couldn't load the content");
    }
}