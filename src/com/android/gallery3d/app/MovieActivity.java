/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ShareActionProvider;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;

/**
 * This activity plays a video from a specified URI.
 *
 * The client of this activity can pass a logo bitmap in the intent (KEY_LOGO_BITMAP)
 * to set the action bar logo so the playback process looks more seamlessly integrated with
 * the original activity.
 */
public class MovieActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "MovieActivity";
    private static final String KEY_LOGO_BITMAP = "logo-bitmap";

    private MoviePlayer mPlayer;
    private boolean mFinishOnCompletion;
    private Uri mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.movie_view);
        View rootView = findViewById(R.id.movie_view_root);
        rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        Intent intent = getIntent();
        initializeActionBar(intent);
        mFinishOnCompletion = intent.getBooleanExtra(
                MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        mPlayer = new MoviePlayer(rootView, this, intent.getData(), savedInstanceState,
                !mFinishOnCompletion) {
            @Override
            public void onCompletion() {
                if (mFinishOnCompletion) {
                    finish();
                }
            }
        };
        if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
            int orientation = intent.getIntExtra(
                    MediaStore.EXTRA_SCREEN_ORIENTATION,
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            if (orientation != getRequestedOrientation()) {
                setRequestedOrientation(orientation);
            }
        }
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);
    }

    private void initializeActionBar(Intent intent) {
        mUri = intent.getData();
        final ActionBar actionBar = getActionBar();
        Bitmap logo = intent.getParcelableExtra(KEY_LOGO_BITMAP);
        if (logo != null) {
            actionBar.setLogo(new BitmapDrawable(getResources(), logo));
        }
        actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_HOME_AS_UP);

        String title = intent.getStringExtra(Intent.EXTRA_TITLE);
        if (title != null) {
            actionBar.setTitle(title);
        } else {
            // Displays the filename as title, reading the filename from the
            // interface: {@link android.provider.OpenableColumns#DISPLAY_NAME}.
            AsyncQueryHandler queryHandler =
                    new AsyncQueryHandler(getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie,
                        Cursor cursor) {
                    try {
                        if ((cursor != null) && cursor.moveToFirst()) {
                            String displayName = cursor.getString(0);

                            // Just show empty title if other apps don't set
                            // DISPLAY_NAME
                            actionBar.setTitle((displayName == null) ? "" :
                                    displayName);
                        }
                    } finally {
                        Utils.closeSilently(cursor);
                    }
                }
            };
            queryHandler.startQuery(0, null, mUri,
                    new String[] {OpenableColumns.DISPLAY_NAME}, null, null,
                    null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.movie, menu);
        ShareActionProvider provider = GalleryActionBar.initializeShareActionProvider(menu);

        if (provider != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, mUri);
            provider.setShareIntent(intent);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        super.onStart();
    }

    @Override
    protected void onStop() {
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .abandonAudioFocus(null);
        super.onStop();
    }

    @Override
    public void onPause() {
        mPlayer.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        mPlayer.onResume();
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPlayer.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        mPlayer.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mPlayer.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mPlayer.onKeyUp(keyCode, event)
                || super.onKeyUp(keyCode, event);
    }
}
