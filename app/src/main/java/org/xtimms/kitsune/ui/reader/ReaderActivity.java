package org.xtimms.kitsune.ui.reader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.xtimms.kitsune.core.common.base.AppBaseActivity;
import org.xtimms.kitsune.R;
import org.xtimms.kitsune.core.common.AppShortcutHelper;
import org.xtimms.kitsune.core.common.dialogs.BottomSheetMenuDialog;
import org.xtimms.kitsune.core.common.dialogs.MenuDialog;
import org.xtimms.kitsune.ui.reader.webtoon.WebtoonReaderFragment;
import org.xtimms.kitsune.utils.AnimationUtils;
import org.xtimms.kitsune.utils.CollectionsUtils;
import org.xtimms.kitsune.utils.ErrorUtils;
import org.xtimms.kitsune.utils.IntentUtils;
import org.xtimms.kitsune.utils.ThemeUtils;
import org.xtimms.kitsune.core.ListWrapper;
import org.xtimms.kitsune.core.models.MangaBookmark;
import org.xtimms.kitsune.core.models.MangaChapter;
import org.xtimms.kitsune.core.models.MangaDetails;
import org.xtimms.kitsune.core.models.MangaHeader;
import org.xtimms.kitsune.core.models.MangaHistory;
import org.xtimms.kitsune.core.models.MangaPage;
import org.xtimms.kitsune.core.storage.db.BookmarksRepository;
import org.xtimms.kitsune.core.storage.db.HistoryRepository;
import org.xtimms.kitsune.core.storage.files.ThumbnailsStorage;
import org.xtimms.kitsune.core.storage.settings.AppSettings;
import org.xtimms.kitsune.core.storage.settings.ReaderSettings;
import org.xtimms.kitsune.core.helpers.BrightnessHelper;
import org.xtimms.kitsune.ui.preview.chapters.ChaptersListAdapter;
import org.xtimms.kitsune.ui.reader.pager.PagerReaderFragment;
import org.xtimms.kitsune.ui.reader.pager.RtlPagerReaderFragment;
import org.xtimms.kitsune.ui.reader.thumbview.OnThumbnailClickListener;
import org.xtimms.kitsune.ui.reader.thumbview.ThumbViewFragment;
import org.xtimms.kitsune.ui.tools.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("ALL")
public final class ReaderActivity extends AppBaseActivity implements View.OnClickListener,
		SeekBar.OnSeekBarChangeListener, ChaptersListAdapter.OnChapterClickListener,
		LoaderManager.LoaderCallbacks<ListWrapper<MangaPage>>, View.OnSystemUiVisibilityChangeListener,
		ReaderCallback, OnThumbnailClickListener, MenuDialog.OnMenuItemClickListener<MangaPage>,
		DialogInterface.OnClickListener, DialogInterface.OnCancelListener, ReaderModeDialog.OnReaderModeChangeListener,
		OnOverScrollListener, FitWindowsFrameLayout.Callback {

	public static final String ACTION_READING_CONTINUE = "org.xtimms.kitsune.ACTION_READING_CONTINUE";
	public static final String ACTION_BOOKMARK_OPEN = "org.xtimms.kitsune.ACTION_BOOKMARK_OPEN";

	private static final long PAGE_ID_FIRST = Long.MIN_VALUE;
	private static final long PAGE_ID_LAST = Long.MAX_VALUE;
	private static final long PAGE_ID_UNDEFINED = 0L;

	private int pendingProgress;
	private int pendingProgressMax;

	private static final int REQUEST_PAGE_SAVE = 61;
	private static final int REQUEST_PAGE_SHARE = 62;

	private AppCompatSeekBar mSeekBar;
	private ViewGroup mContentPanel;
	private Toolbar mToolbar;
	private View mBottomBar;
	private TextView mTextViewPage;
	private ReaderStatusBar mStatusBar;
	private BrightnessHelper mBrightnessHelper;

	private FrameLayout mFrame;
	private ReaderFragment mReader = null;
	private ReaderSettings mSettings;
	private HistoryRepository mHistoryRepository;
	private BookmarksRepository mBookmarksRepository;
	private MangaDetails mManga;
	private MangaChapter mChapter;
	private ArrayList<MangaPage> mPages;
	private long mPageId;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reader);
		mToolbar = findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		enableHomeAsUp();
		mSettings = AppSettings.get(this).readerSettings;
		mBrightnessHelper = new BrightnessHelper(getWindow());

		mStatusBar = findViewById(R.id.statusBar);
		mSeekBar = findViewById(R.id.seekBar);
		mContentPanel = findViewById(R.id.contentPanel);
		mBottomBar = findViewById(R.id.bottomBar);
		mFrame = findViewById(R.id.frame);
		mTextViewPage = findViewById(R.id.textView_page);
		this.<FitWindowsFrameLayout>findViewById(R.id.root).setCallback(this);

		mSeekBar.setOnSeekBarChangeListener(this);
		findViewById(R.id.action_menu).setOnClickListener(this);
		findViewById(R.id.action_thumbnails).setOnClickListener(this);
		mStatusBar.setIsActive(mSettings.isStatusBarEnbaled());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			int color = ContextCompat.getColor(this, R.color.transparent_dark);
			window.setStatusBarColor(color);
			window.setNavigationBarColor(color);
		}

		mHistoryRepository = HistoryRepository.get(this);
		mBookmarksRepository = BookmarksRepository.get(this);

		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (ACTION_READING_CONTINUE.equals(action)) {
			MangaHeader mangaHeader = MangaHeader.from(Objects.requireNonNull(intent.getExtras()));
			new ResumeReadingTask(this).start(mangaHeader);
		} else if (ACTION_BOOKMARK_OPEN.equals(action)) {
			MangaBookmark bookmark = MangaBookmark.from(Objects.requireNonNull(intent.getExtras()));
			if (bookmark == null) {
				Toast.makeText(this, R.string.bookmark_not_found, Toast.LENGTH_SHORT).show();
				finish();
			}
			new BookmarkOpenTask(this).start(bookmark);
		} else {
			mManga = intent.getParcelableExtra("manga");
			mChapter = intent.getParcelableExtra("chapter");
			mPageId = intent.getLongExtra("page_id", PAGE_ID_UNDEFINED);
			onMangaReady();
		}
	}

	public void onMangaReady() {
		assert mManga != null && mChapter != null;
		setTitle(mManga.name);
		setSubtitle(mChapter.name);
		setupReader(mHistoryRepository.getPreset(mManga, mSettings.getDefaultPreset()));
		getLoaderManager().initLoader(0, mChapter.toBundle(), this).forceLoad();
	}

	public void onMangaReady(Result result) {
		if (result.chapter == null) {
			new AlertDialog.Builder(this)
					.setMessage(R.string.requested_chapter_not_found)
					.setCancelable(true)
					.setOnCancelListener(this)
					.setNegativeButton(R.string.close, this)
					.create()
					.show();
		}
		this.mManga = result.mangaDetails;
		this.mChapter = result.chapter;
		this.mPageId = result.pageId;
		onMangaReady();
	}

	@Override
	protected void onPause() {
		if (mHistoryRepository != null && mPages != null) {
			if (!mHistoryRepository.quickUpdate(mManga, mChapter, mReader.getCurrentPage())) {
				addToHistory();
			}
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		final ReaderConfig mConfig;
		mConfig = ReaderConfig.load(this);
		setKeepScreenOn(mSettings.isWakelockEnabled());
		mStatusBar.setIsActive(mSettings.isStatusBarEnbaled());
		if (mToolbar.getVisibility() != View.VISIBLE) {
			mStatusBar.show();
		}
		if (mSettings.isCustomBackground()) {
			mFrame.setBackgroundColor(mSettings.getBackgroundColor());
		} else {
			mFrame.setBackgroundColor(ThemeUtils.getThemeAttrColor(this, android.R.attr.windowBackground));
		}
		if (mManga != null && mReader != null && mHistoryRepository != null && mPages != null) {
			setupReader(getReaderPreset());
		}
		if (mSettings.isBrightnessEnabled()) {
			mBrightnessHelper.setBrightness(mConfig.brightnessValue);
		} else {
			mBrightnessHelper.reset();
		}
	}

	@Override
	protected void onStop() {
		new AppShortcutHelper(this).update(mHistoryRepository);
		super.onStop();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus && mToolbar.getVisibility() != View.VISIBLE) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_reader, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_chapters) {
			final ChaptersDialogFragment dialogFragment = new ChaptersDialogFragment();
			final Bundle args = new Bundle();
			args.putParcelableArrayList("chapters", mManga.chapters);
			args.putLong("current_id", mChapter.id);
			dialogFragment.setArguments(args);
			dialogFragment.show(getSupportFragmentManager(), "chapters_list");
			return true;
		} else return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.action_menu:
				final MangaPage page = mReader.getCurrentPage();
				final MangaBookmark bookmark = mBookmarksRepository.find(mManga, mChapter, page);
				final BottomSheetMenuDialog<MangaPage> menu = new BottomSheetMenuDialog<>(this);
				menu.setItemClickListener(this);
				if (bookmark == null) {
					menu.addItem(R.id.action_page_bookmark_add, R.drawable.ic_bookmark_add_black, R.string.create_bookmark);
				} else {
					menu.addItem(R.id.action_page_bookmark_remove, R.drawable.ic_bookmark_remove_black, R.string.remove_bookmark);
				}
				menu.addItem(R.id.action_page_save, R.drawable.ic_save_white, R.string.save_image)
						.addItem(R.id.action_page_share, R.drawable.ic_share_white, R.string.share_image)
						.addItem(R.id.action_open_browser, R.drawable.ic_open_in_black, R.string.open_in_browser)
						.addItem(R.id.action_reader_mode, R.drawable.ic_aspect_ratio_black, R.string.reader_mode)
						.addItem(R.id.action_reader_settings, R.drawable.ic_settings_black, R.string.reader_options)
						.create(page)
						.show();
				break;
			case R.id.action_thumbnails:
				final ThumbViewFragment dialogFragment = new ThumbViewFragment();
				final Bundle args = new Bundle();
				args.putParcelableArrayList("pages", mPages);
				dialogFragment.setArguments(args);
				dialogFragment.show(getSupportFragmentManager(), "thumb_view");
				break;
			case R.id.button_next:
				onOverScrolledEnd();
				break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mTextViewPage.setText(getString(R.string.page_x_of_n, progress + 1, seekBar.getMax() + 1));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mReader.scrollToPage(seekBar.getProgress());
	}

	@Override
	public void onChapterClick(int pos, MangaChapter chapter) {
		Fragment f = getSupportFragmentManager().findFragmentByTag("chapters_list");
		if (f instanceof DialogFragment) {
			((DialogFragment) f).dismiss();
		}
		AnimationUtils.setVisibility(mContentPanel, View.VISIBLE);
		mChapter = chapter;
		setSubtitle(mChapter.name);

		getLoaderManager().restartLoader(0, mChapter.toBundle(), this).forceLoad();
	}

	@Override
	public boolean onChapterLongClick(int pos, MangaChapter chapter) {
		return false;
	}

	@NonNull
	@Override
	public Loader<ListWrapper<MangaPage>> onCreateLoader(int id, Bundle args) {
		return new ChapterLoader(this, mManga, MangaChapter.from(args));
	}

	@Override
	public void onLoadFinished(Loader<ListWrapper<MangaPage>> loader, ListWrapper<MangaPage> data) {
		AnimationUtils.setVisibility(mContentPanel, View.GONE);
		if (data.isFailed()) {
			new AlertDialog.Builder(this)
					.setTitle(R.string.failed_to_load_pages)
					.setMessage(ErrorUtils.getErrorMessageDetailed(this, data.getError()))
					.setCancelable(true)
					.setOnCancelListener(this)
					.setPositiveButton(R.string.retry, this)
					.setNegativeButton(R.string.close, this)
					.create()
					.show();
		} else if (data.isEmpty()) {
			new AlertDialog.Builder(this)
					.setMessage(R.string.no_pages_found)
					.setCancelable(true)
					.setOnCancelListener(this)
					.setNegativeButton(R.string.close, this)
					.create()
					.show();
		} else {
			mPages = data.get();
			mSeekBar.setMax(mPages.size() - 1);
			mSeekBar.setProgress(0);
			mReader.setPages(mPages);
			final int page;
			if (mPageId == PAGE_ID_LAST) {
				page = mPages.size() - 1;
			} else if (mPageId == PAGE_ID_FIRST || mPageId == PAGE_ID_UNDEFINED) {
				page = 0;
			} else {
				page = CollectionsUtils.findPagePositionById(mPages, mPageId);
			}
			mPageId = PAGE_ID_UNDEFINED;
			mReader.scrollToPage(page == -1 ? 0 : page);
			addToHistory();
		}
	}

	@Override
	public void onLoaderReset(Loader<ListWrapper<MangaPage>> loader) {

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (isFinishing()) {
			return super.onKeyDown(keyCode, event);
		}
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (mSettings.isVolumeKeysEnabled()) {
					if (!mReader.movePrevious()) {
						prevChapter();
					}
					return true;
				} else return super.onKeyDown(keyCode, event);
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (mSettings.isVolumeKeysEnabled()) {
					if (!mReader.moveNext()) {
						nextChapter();
					}
					return true;
				} else return super.onKeyDown(keyCode, event);
			default:
				return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isFinishing()) {
			return super.onKeyUp(keyCode, event);
		}
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				toggleUi();
				return true;
			case KeyEvent.KEYCODE_SPACE:
				if (!mReader.moveNext()) {
					nextChapter();
				}
			case KeyEvent.KEYCODE_DPAD_UP:
				if (!mReader.moveUp()) {
					prevChapter();
				}
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (!mReader.moveDown()) {
					nextChapter();
				}
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (!mReader.moveLeft()) {
					onOverScrolledStart();
				}
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (!mReader.moveRight()) {
					onOverScrolledEnd();
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return mSettings.isVolumeKeysEnabled() || super.onKeyUp(keyCode, event);
			default:
				return super.onKeyUp(keyCode, event);
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	@Override
	public void showUi() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		AnimationUtils.setVisibility(mToolbar, View.VISIBLE);
		AnimationUtils.setVisibility(mBottomBar, View.VISIBLE);
	}

	private void hideUi() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		AnimationUtils.setVisibility(mToolbar, View.GONE);
		AnimationUtils.setVisibility(mBottomBar, View.GONE);
	}

	private void addToHistory() {
		final MangaPage page = mReader.getCurrentPage();
		if (page != null) {
			final MangaHistory history = new MangaHistory(mManga, mChapter, mManga.chapters.size(), page, getReaderPreset());
			mHistoryRepository.updateOrAdd(history);
		}
	}

	@Override
	public void onPageChanged(int page) {
		mSeekBar.setProgress(page);
	}

	public void toggleUi() {
		if (mToolbar.getVisibility() == View.VISIBLE) {
			hideUi();
			mStatusBar.show();
		} else {
			showUi();
			mStatusBar.hide();
		}
	}

	@Override
	public void onThumbnailClick(int position) {
		mReader.scrollToPage(position);
	}

	@Override
	public void onMenuItemClick(int id, MangaPage page) {
		final int pos = mReader.getCurrentPageIndex();
		switch (id) {
			case R.id.action_page_bookmark_add:
				new BookmarkTask(this).start(new BookmarkTask.Request(
						mManga,
						mChapter,
						mReader.getCurrentPage()
				));
				break;
			case R.id.action_page_bookmark_remove:
				final MangaBookmark bookmark = mBookmarksRepository.find(mManga, mChapter, page);
				if (bookmark != null) {
					mBookmarksRepository.remove(bookmark);
					new ThumbnailsStorage(this).remove(bookmark);
				}
				break;
			case R.id.action_open_browser:
				IntentUtils.openBrowser(this, page.url);
				break;
			case R.id.action_page_save:
				checkPermissions(REQUEST_PAGE_SAVE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				break;
			case R.id.action_page_share:
				checkPermissions(REQUEST_PAGE_SHARE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				break;
			case R.id.action_reader_mode:
				if (mHistoryRepository != null && mPages != null) {
					final MangaHistory history = new MangaHistory(mManga, mChapter, mManga.chapters.size(), Objects.requireNonNull(mReader.getCurrentPage()), getReaderPreset());
					mHistoryRepository.updateOrAdd(history);
					new ReaderModeDialog(this, history)
							.setListener(this)
							.show();
				}
				break;
			case R.id.action_reader_settings:
				startActivity(new Intent(this, SettingsActivity.class)
						.setAction(SettingsActivity.ACTION_SETTINGS_READER));
				break;
			default:
				stub();
		}
	}

	@Override
	protected void onPermissionGranted(int requestCode, String permission) {
		final MangaPage page = mReader.getCurrentPage();
		switch (requestCode) {
			case REQUEST_PAGE_SAVE:
				new ImageSaveTask(this, false).start(page);
				break;
			case REQUEST_PAGE_SHARE:
				new ImageSaveTask(this, true).start(page);
				break;

		}
	}

	/**
	 * Only for errors
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
			case DialogInterface.BUTTON_POSITIVE: //retry
				AnimationUtils.setVisibility(mContentPanel, View.VISIBLE);
				getLoaderManager().restartLoader(0, mChapter.toBundle(), this).forceLoad();
				break;
			case DialogInterface.BUTTON_NEGATIVE: //close
				onCancel(dialog);
		}
	}

	/**
	 * Only for errors
	 */
	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}

	@Override
	public void onReaderModeChanged(short mode) {
		final MangaHistory history = new MangaHistory(mManga, mChapter, mManga.chapters.size(), Objects.requireNonNull(mReader.getCurrentPage()), mode);
		mHistoryRepository.updateOrAdd(history);
		setupReader(mode);
	}

	@Override
	public void onOverScrolledStart() {
		if (mReader instanceof RtlPagerReaderFragment) {
			nextChapter();
		} else {
			prevChapter();
		}
	}

	@Override
	public void onOverScrolledEnd() {
		if (mReader instanceof RtlPagerReaderFragment) {
			prevChapter();
		} else {
			nextChapter();
		}
	}

	private void prevChapter() {
		final int pos = mManga.chapters.indexOf(mChapter);
		if (pos == -1 || pos == 0) {
			return;
		}
		AnimationUtils.setVisibility(mContentPanel, View.VISIBLE);
		mChapter = mManga.chapters.get(pos - 1);
		mPageId = PAGE_ID_LAST;
		setSubtitle(mChapter.name);
		Toast toast = Toast.makeText(this, getString(R.string.prev_chapter, mChapter.name), Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		getLoaderManager().restartLoader(0, mChapter.toBundle(), this).forceLoad();
	}

	private void nextChapter() {
		final int pos = mManga.chapters.indexOf(mChapter);
		if (pos == -1 || pos == mManga.chapters.size() - 1) {
			return;
		}
		AnimationUtils.setVisibility(mContentPanel, View.VISIBLE);
		mPageId = PAGE_ID_FIRST;
		mChapter = mManga.chapters.get(pos + 1);
		setSubtitle(mChapter.name);
		Toast toast = Toast.makeText(this, getString(R.string.next_chapter_, mChapter.name), Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		getLoaderManager().restartLoader(0, mChapter.toBundle(), this).forceLoad();
	}

	private void setupReader(int preset) {
		Bundle savedState;
		if (mReader != null) {
			savedState = new Bundle();
			mReader.onSaveInstanceState(savedState);
		} else {
			savedState = null;
		}
		switch (preset) {
			case 0:
				mReader = new PagerReaderFragment();
				mSeekBar.setVisibility(View.VISIBLE);
				mTextViewPage.setVisibility(View.VISIBLE);
				break;
			case 1:
				mReader = new RtlPagerReaderFragment();
				mSeekBar.setVisibility(View.VISIBLE);
				mTextViewPage.setVisibility(View.VISIBLE);
				break;
			case 2:
				mReader = new WebtoonReaderFragment();
				mSeekBar.setVisibility(View.VISIBLE);
				mTextViewPage.setVisibility(View.VISIBLE);
				break;
			default:
				stub();
				mReader = new PagerReaderFragment();
		}
		mReader.setArguments(savedState);
		getFragmentManager().beginTransaction()
				.replace(R.id.reader, mReader)
				.commit();
	}

	private short getReaderPreset() {
		if (mReader == null) {
			return mHistoryRepository.getPreset(mManga, mSettings.getDefaultPreset());
		} else if (mReader instanceof WebtoonReaderFragment) {
			return 2;
		} else if (mReader instanceof RtlPagerReaderFragment) {
			return 1;
		} else if (mReader instanceof PagerReaderFragment) {
			return 0;
		} else {
			return 0;//TODO
		}
	}

	final static class Result {

		MangaDetails mangaDetails;
		MangaChapter chapter;
		long pageId;
	}
}
