/*
 * Copyright (C) 2016 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.folioreader.android.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folioreader.BookInitFailedHandler;
import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.ReportHandler;
import com.folioreader.SendToKindleHandler;
import com.folioreader.ShareHandler;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.ui.base.OnSaveHighlight;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;
import com.folioreader.util.TTSLocatorListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener, TTSLocatorListener {

    private static final String LOG_TAG = HomeActivity.class.getSimpleName();
    private FolioReader folioReader;

    private ReadLocator resumePoint;
    private String ttsResumePoint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setOnClosedListener(this);

        getHighlightsAndSave();

        findViewById(R.id.btn_raw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Config config = AppUtil.getSavedConfig(getApplicationContext());
                if (config == null)
                    config = new Config();
                config = config.setNoteTakingEnabled(true)
                        .setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL)
                        .setCopyEnabled(true)
                        .setThemeColorRes(R.color.highlight_pink)
                        .setDefineEnabled(true)
                        .setSearchEnabled(true)
                        .setDistractionFreeModeEnabled(true)
                        .setShowSendToKindle(true);

                AppUtil.setShareHandler(new ShareHandler() {
                    @Override
                    public void share(Context context, String selectedText) {
                        Log.d("CustomShareHandler", "share tapped on " + selectedText);
                    }
                });

                AppUtil.setSendToKindleHandler(new SendToKindleHandler() {
                    @Override
                    public void sendToKindle(Context context) {
                        Log.d("SendToKindleHandler", "send to kindle :)");
                    }
                });

                AppUtil.setBookInitFailedHandler(new BookInitFailedHandler() {
                    @Override
                    public void onBookInitFailure(Context context) {
                        ((Activity) context).finish();
                    }
                });

                AppUtil.setReportHandler(new ReportHandler() {
                    @Override
                    public void report(Context context) {
                        Log.d("CustomReportHandler", "report tapped");
                    }
                });

                folioReader.setConfig(config, true)
                        .setTTSLocatorListener(HomeActivity.this)
                        .setTTSLocator(ttsResumePoint)
                        .setReadLocator(resumePoint)
                        .setReadLocatorListener(HomeActivity.this)
                        .openBook(R.raw.reedsy2, "BOOK TITLE");
            }
        });

        findViewById(R.id.btn_assest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Config config = AppUtil.getSavedConfig(getApplicationContext());
                if (config == null)
                    config = new Config();
                config = config.setNoteTakingEnabled(false)
                        .setAllowedDirection(Config.AllowedDirection.ONLY_HORIZONTAL)
                        .setThemeColorRes(R.color.highlight_blue)
                        .setCopyEnabled(false)
                        .setDefineEnabled(false)
                        .setSearchEnabled(false)
                        .setDistractionFreeModeEnabled(false);

                AppUtil.setShareHandler(null);
                AppUtil.setReportHandler(null);
                AppUtil.setBookInitFailedHandler(null);
                AppUtil.setSendToKindleHandler(null);

                folioReader.setConfig(config, true)
                        .openBook("file:///android_asset/TheSilverChair.epub", "New Book");
            }
        });
    }

    @Override
    public void saveTTSLocator(String ttsLocator) {
        Log.i(LOG_TAG, "-> saveTTSLocator -> " + ttsLocator);
        ttsResumePoint = ttsLocator;
    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson());
        resumePoint = readLocator;
    }

    /*
     * For testing purpose, we are getting dummy highlights from asset. But you can get highlights from your server
     * On success, you can save highlights to FolioReader DB.
     */
    private void getHighlightsAndSave() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HighLight> highlightList = null;
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    highlightList = objectMapper.readValue(
                            loadAssetTextAsString("highlights/highlights_data.json"),
                            new TypeReference<List<HighlightData>>() {
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (highlightList == null) {
                    folioReader.saveReceivedHighLights(highlightList, new OnSaveHighlight() {
                        @Override
                        public void onFinished() {
                            //You can do anything on successful saving highlight list
                        }
                    });
                }
            }
        }).start();
    }

    private String loadAssetTextAsString(String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ((str = in.readLine()) != null) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e("HomeActivity", "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "Error closing asset " + name);
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FolioReader.clear();
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
        Toast.makeText(this,
                "highlight id = " + highlight.getUUID() + " type = " + type,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed");
    }
}