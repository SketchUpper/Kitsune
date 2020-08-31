package org.xtimms.kitsune.ui.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.xtimms.kitsune.core.common.base.AppBaseActivity;
import org.xtimms.kitsune.R;
import org.xtimms.kitsune.core.common.dialogs.FavouriteDialog;
import org.xtimms.kitsune.core.common.dialogs.MenuDialog;
import org.xtimms.kitsune.utils.BroadcastUtils;
import org.xtimms.kitsune.utils.IntentUtils;
import org.xtimms.kitsune.utils.MenuUtils;
import org.xtimms.kitsune.core.ObjectWrapper;
import org.xtimms.kitsune.core.models.Category;
import org.xtimms.kitsune.core.models.MangaBookmark;
import org.xtimms.kitsune.core.models.MangaChapter;
import org.xtimms.kitsune.core.models.MangaDetails;
import org.xtimms.kitsune.core.models.MangaFavourite;
import org.xtimms.kitsune.core.models.MangaHeader;
import org.xtimms.kitsune.core.models.MangaHistory;
import org.xtimms.kitsune.core.storage.db.BookmarkSpecification;
import org.xtimms.kitsune.core.storage.db.FavouritesRepository;
import org.xtimms.kitsune.core.storage.db.HistoryRepository;
import org.xtimms.kitsune.ui.discover.bookmarks.BookmarkRemoveTask;
import org.xtimms.kitsune.ui.preview.bookmarks.BookmarksPage;
import org.xtimms.kitsune.ui.preview.chapters.ChaptersListAdapter;
import org.xtimms.kitsune.ui.preview.chapters.ChaptersPage;
import org.xtimms.kitsune.ui.preview.details.DetailsPage;
import org.xtimms.kitsune.ui.reader.ReaderActivity;
import org.xtimms.kitsune.core.storage.LocalRemoveService;
import org.xtimms.kitsune.core.storage.SaveRequest;
import org.xtimms.kitsune.core.storage.SaveService;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

@SuppressWarnings("ALL")
public final class PreviewActivity extends AppBaseActivity implements LoaderManager.LoaderCallbacks<ObjectWrapper<MangaDetails>>,
		ChaptersListAdapter.OnChapterClickListener, View.OnClickListener, BookmarkRemoveTask.OnBookmarkRemovedListener,
		FavouriteDialog.OnFavouriteListener, MenuDialog.OnMenuItemClickListener<MangaChapter>, ViewPager.OnPageChangeListener, SearchView.OnQueryTextListener, BroadcastUtils.DownloadsReceiverCallback {

	public static final String ACTION_PREVIEW = "org.xtimms.kitsune.ACTION_PREVIEW";

	//views
	//activity
	private ViewPager mPager;
	private SmoothProgressBar mProgressBar;
	private BottomAppBar mBottomBar;
	private Button mButton;
	//tabs
	private DetailsPage mDetailsPage;
	private ChaptersPage mChaptersPage;
	private BookmarksPage mBookmarksPage;
	//data
	private MangaHeader mMangaHeader;
	@Nullable
	private MangaDetails mMangaDetails;
	private HistoryRepository mHistory;
	private FavouritesRepository mFavourites;
	private BroadcastReceiver mDownloadsReceiver;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preview);
		setSupportActionBar(R.id.toolbar);
		enableHomeAsUp();

		mPager = findViewById(R.id.pager);
		mBottomBar = findViewById(R.id.bottomBar);
		mProgressBar = findViewById(R.id.progressBar);
		TabLayout mTabs = findViewById(R.id.tabs);
		mBottomBar.inflateMenu(R.menu.options_preview_bottombar);
		SearchView mSearchView = (SearchView) mBottomBar.getMenu().findItem(R.id.action_find_chapter).getActionView();
		mBottomBar.setOnMenuItemClickListener(this::onOptionsItemSelected);
		mSearchView.setQueryHint(getString(R.string.find_chapter));
		mSearchView.setOnQueryTextListener(this);

		final PagesAdapter adapter = new PagesAdapter(
				mDetailsPage = new DetailsPage(mPager),
				mChaptersPage = new ChaptersPage(mPager),
				mBookmarksPage = new BookmarksPage(mPager)
		);
		mPager.setAdapter(adapter);
		mTabs.setupWithViewPager(mPager);
		mPager.addOnPageChangeListener(this);

		mDetailsPage.buttonRead.setOnClickListener(this);
		mDetailsPage.buttonFavourite.setOnClickListener(this);

		mMangaHeader = getIntent().getParcelableExtra("manga");
		mMangaDetails = null;
		assert mMangaHeader != null;
		mHistory = HistoryRepository.get(this);
		mFavourites = FavouritesRepository.get(this);

		setTitle(mMangaHeader.name);
		setSubtitle(mMangaHeader.summary);
		mDetailsPage.updateContent(mMangaHeader, mMangaDetails);

		Bundle args = new Bundle(1);
		args.putParcelable("manga", mMangaHeader);
		getLoaderManager().initLoader(0, args, this).forceLoad();
		getLoaderManager().initLoader(1, new BookmarkSpecification().manga(mMangaHeader).orderByDate(true).toBundle(), mBookmarksPage).forceLoad();

		mDownloadsReceiver = BroadcastUtils.createDownloadsReceiver(this);
		registerReceiver(mDownloadsReceiver, new IntentFilter(BroadcastUtils.ACTION_DOWNLOAD_DONE));
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mDownloadsReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_preview, menu);
		MenuUtils.buildOpenWithSubmenu(this, mMangaHeader, menu.findItem(R.id.action_open_ext));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		final int page = mPager.getCurrentItem();
		menu.setGroupVisible(R.id.group_details, page == 0);
		return super.onPrepareOptionsMenu(menu);
	}

	private void onPrepareBottomBarMenu(Menu menu) {
		final MenuItem item = menu.findItem(R.id.action_reverse);
		item.setIcon(item.isChecked() ? R.drawable.ic_sort_numeric_reverse_white : R.drawable.ic_sort_numeric_white);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_share:
				IntentUtils.shareManga(this, mMangaHeader);
				return true;
			case R.id.action_shortcut:
				IntentUtils.createLauncherShortcutPreview(this, mMangaHeader);
				return true;
			case R.id.action_reverse:
				boolean reversed = !item.isChecked();
				if (mChaptersPage.setReversed(reversed)) {
					item.setChecked(reversed);
					item.setIcon(reversed ? R.drawable.ic_sort_numeric_reverse_white : R.drawable.ic_sort_numeric_white);
					return true;
				} else {
					return false;
				}
			case R.id.action_chapter_save_all:
			case R.id.action_chapter_remove_all:
				onMenuItemClick(item.getItemId(), null);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		final MangaHistory history = mHistory.find(mMangaHeader);
		if (history != null) {
			mChaptersPage.updateHistory(history);
			mDetailsPage.buttonRead.setText(R.string._continue);
		}
		final MangaFavourite favourite = mFavourites.get(mMangaHeader);
		mDetailsPage.buttonFavourite.setImageResource(favourite == null ? R.drawable.ic_tag_heart_outline_white_24dp : R.drawable.ic_tag_heart_black);
	}

	@NonNull
	@Override
	public Loader<ObjectWrapper<MangaDetails>> onCreateLoader(int id, Bundle args) {
		return new MangaDetailsLoader(this, args.getParcelable("manga"));
	}

	@Override
	public void onLoadFinished(Loader<ObjectWrapper<MangaDetails>> loader, ObjectWrapper<MangaDetails> data) {
		mProgressBar.setVisibility(View.GONE);
		if (data.isSuccess()) {
			mMangaDetails = data.get();
			assert mMangaDetails != null;
			setTitle(mMangaDetails.name);
			setSubtitle(mMangaDetails.summary);
			final MangaHistory history = mHistory.find(mMangaHeader);
			/*if (history != null) {
				int pos = CollectionsUtils.findChapterPositionById(mMangaDetails.chapters, history.chapterId);
				if (pos != -1 && LayoutUtils.findLastVisibleItemPosition(mRecyclerView) > pos) {
					mRecyclerView.scrollToPosition(Math.max(0, pos - 3));
				}
			}*/
			mDetailsPage.updateContent(mMangaHeader, mMangaDetails);
			mChaptersPage.setData(mMangaDetails.chapters, history, this);
		} else {
			mChaptersPage.setError();
			mDetailsPage.setError(data.getError());
		}
	}

	@Override
	public void onLoaderReset(Loader<ObjectWrapper<MangaDetails>> loader) {

	}

	@Override
	public void onChapterClick(int pos, MangaChapter chapter) {
		startActivity(new Intent(this, ReaderActivity.class)
				.putExtra("manga", mMangaDetails)
				.putExtra("chapter", chapter));
	}

	@Override
	public boolean onChapterLongClick(int pos, MangaChapter chapter) {
		final int totalChapters = mMangaDetails == null ? 0 : mMangaDetails.chapters.size();
		MenuDialog<MangaChapter> menu = new MenuDialog<MangaChapter>(this)
				.setTitle(chapter.name)
				.setItemClickListener(this);
		if (chapter.isSaved()) {
			menu.addItem(R.id.action_chapter_remove_this, R.string.remove_this_chapter);
			menu.addItem(R.id.action_chapter_remove_all, R.string.remove_all_chapters);
		} else {
			menu.addItem(R.id.action_chapter_save_this, R.string.save_this_chapter);
			if (pos > 0) {
				menu.addItem(R.id.action_chapter_save_prev, R.string.save_prev_chapters);
			}
			if (pos + 5 < totalChapters) {
				menu.addItem(R.id.action_chapter_save_5, R.string.save_next_5_chapters);
				if (pos + 10 < totalChapters) {
					menu.addItem(R.id.action_chapter_save_10, R.string.save_next_10_chapters);
					if (pos + 30 < totalChapters) {
						menu.addItem(R.id.action_chapter_save_30, R.string.save_next_30_chapters);
					}
				}
			}
			if (pos < totalChapters - 1) {
				menu.addItem(R.id.action_chapter_save_next, R.string.save_next_all_chapters);
			}
			menu.addItem(R.id.action_chapter_save_all, R.string.save_all_chapters);
		}
		menu.create(chapter)
				.show();
		return true;
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_read:
				startActivity(new Intent(this, ReaderActivity.class)
						.setAction(ReaderActivity.ACTION_READING_CONTINUE)
						.putExtra("manga", mMangaDetails));
				break;
			case R.id.button_favourite:
				if (mMangaDetails != null) {
					new FavouriteDialog(this, mMangaDetails)
							.setListener(this)
							.show();
				}
				break;
		}
	}

	@Override
	public void onFavouritesChanged(MangaDetails manga, @Nullable Category category) {
		Snackbar.make(
				mPager,
				category == null ? getString(R.string.unfavourited) : getString(R.string.added_to_x, category.name),
				Snackbar.LENGTH_SHORT
		).show();
		mDetailsPage.buttonFavourite.setImageResource(category == null ? R.drawable.ic_tag_heart_outline_white_24dp : R.drawable.ic_tag_heart_black);
	}

	@Override
	public void onMenuItemClick(@IdRes int id, MangaChapter mangaChapter) {
		if (mMangaDetails == null) {
			return;
		}
		switch (id) {
			case R.id.action_chapter_save_this:
				SaveService.start(this, new SaveRequest(mMangaDetails, mangaChapter));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_save_5:
				SaveService.start(this, new SaveRequest(mMangaDetails, mMangaDetails.chapters.subListFrom(mangaChapter, 5)));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_save_10:
				SaveService.start(this, new SaveRequest(mMangaDetails, mMangaDetails.chapters.subListFrom(mangaChapter, 10)));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_save_30:
				SaveService.start(this, new SaveRequest(mMangaDetails, mMangaDetails.chapters.subListFrom(mangaChapter, 30)));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_save_all:
				SaveService.start(this, new SaveRequest(mMangaDetails, mMangaDetails.chapters));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_save_next:
				SaveService.start(this, new SaveRequest(mMangaDetails, mMangaDetails.chapters.subListFrom(mangaChapter)));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_save_prev:
				SaveService.start(this, new SaveRequest(mMangaDetails, mMangaDetails.chapters.subListTo(mangaChapter)));
				Snackbar.make(mPager, R.string.downloading_started_, Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_remove_this:
				LocalRemoveService.start(this, mMangaHeader, mangaChapter);
				mangaChapter.removeFlag(MangaChapter.FLAG_CHAPTER_SAVED);
				mChaptersPage.update();
				Snackbar.make(mPager, getResources().getQuantityString(R.plurals.chapters_removed, 1, 1), Snackbar.LENGTH_SHORT).show();
				break;
			case R.id.action_chapter_remove_all:
				LocalRemoveService.start(this, mMangaHeader);
				int c = 0;
				for (MangaChapter o : mMangaDetails.chapters) {
					if (o.isSaved()) {
						c++;
						o.removeFlag(MangaChapter.FLAG_CHAPTER_SAVED);
					}
				}
				mChaptersPage.update();
				Snackbar.make(mPager, getResources().getQuantityString(R.plurals.chapters_removed, c, c), Snackbar.LENGTH_SHORT).show();
				break;
			default:
				stub();
		}
	}

	@Override
	public void onBookmarkRemoved(@NonNull MangaBookmark bookmark) {
		mBookmarksPage.onBookmarkRemoved(bookmark);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		invalidateOptionsMenu();
		if (position == 1) {
			onPrepareBottomBarMenu(mBottomBar.getMenu());
			mBottomBar.setVisibility(View.VISIBLE);
			mBottomBar.animate().translationY(0).setListener(null).start();
			//AnimationUtils.setVisibility(mBottomBar, View.VISIBLE);
		} else {
			mBottomBar.animate().translationY(mBottomBar.getHeight()).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mBottomBar.setVisibility(View.GONE);
				}
			}).start();
			//AnimationUtils.setVisibility(mBottomBar, View.GONE);
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}

	@Override
	public boolean onQueryTextSubmit(String s) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String s) {
		mChaptersPage.setFilter(s);
		return false;
	}

	@Override
	public void onChapterDownloaded(MangaChapter chapter) {
		if (mMangaDetails != null) {
			final int pos = mMangaDetails.chapters.indexOf(chapter);
			if (pos != -1) {
				mMangaDetails.chapters.get(pos).addFlag(MangaChapter.FLAG_CHAPTER_SAVED);
				mChaptersPage.update(pos);
			}
		}
	}
}
