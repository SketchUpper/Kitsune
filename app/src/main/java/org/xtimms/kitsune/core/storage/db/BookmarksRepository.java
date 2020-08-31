package org.xtimms.kitsune.core.storage.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xtimms.kitsune.core.models.MangaBookmark;
import org.xtimms.kitsune.core.models.MangaChapter;
import org.xtimms.kitsune.core.models.MangaHeader;
import org.xtimms.kitsune.core.models.MangaPage;

import java.lang.ref.WeakReference;

public final class BookmarksRepository extends SQLiteRepository<MangaBookmark> {

	private static final String TABLE_NAME = "bookmarks";
	private static final String[] PROJECTION = new String[]{
			"id",                        //0
			"manga_id",                    //1
			"name",                        //2
			"summary",                    //3
			"genres",                    //4
			"url",                        //5
			"thumbnail",                //6
			"provider",                    //7
			"status",                    //8
			"rating",                    //9
			"chapter_id",                //10
			"page_id",                    //11
			"created_at",                //12
			"removed"                    //13
	};

	@Nullable
	private static WeakReference<BookmarksRepository> sInstanceRef = null;

	@NonNull
	public static BookmarksRepository get(Context context) {
		BookmarksRepository instance = null;
		if (sInstanceRef != null) {
			instance = sInstanceRef.get();
		}
		if (instance == null) {
			instance = new BookmarksRepository(context);
			sInstanceRef = new WeakReference<>(instance);
		}
		return instance;
	}

	private BookmarksRepository(Context context) {
		super(context);
	}


	@Override
	protected void toContentValues(@NonNull MangaBookmark bookmark, @NonNull ContentValues cv) {
		cv.put(PROJECTION[0], bookmark.id);
		cv.put(PROJECTION[1], bookmark.manga.id);
		cv.put(PROJECTION[2], bookmark.manga.name);
		cv.put(PROJECTION[3], bookmark.manga.summary);
		cv.put(PROJECTION[4], bookmark.manga.genres);
		cv.put(PROJECTION[5], bookmark.manga.url);
		cv.put(PROJECTION[6], bookmark.manga.thumbnail);
		cv.put(PROJECTION[7], bookmark.manga.provider);
		cv.put(PROJECTION[8], bookmark.manga.status);
		cv.put(PROJECTION[9], bookmark.manga.rating);
		cv.put(PROJECTION[10], bookmark.chapterId);
		cv.put(PROJECTION[11], bookmark.pageId);
		cv.put(PROJECTION[12], bookmark.createdAt);
		cv.put(PROJECTION[13], 0);
	}

	@NonNull
	@Override
	protected String getTableName() {
		return TABLE_NAME;
	}

	@NonNull
	@Override
	protected Object getId(@NonNull MangaBookmark bookmark) {
		return bookmark.id;
	}

	@NonNull
	@Override
	protected String[] getProjection() {
		return PROJECTION;
	}

	@NonNull
	@Override
	protected MangaBookmark fromCursor(@NonNull Cursor cursor) {
		return new MangaBookmark(
				cursor.getLong(0),
				new MangaHeader(
						cursor.getLong(1),
						cursor.getString(2),
						cursor.getString(3),
						cursor.getString(4),
						cursor.getString(5),
						cursor.getString(6),
						cursor.getString(7),
						cursor.getInt(8),
						cursor.getShort(9)
				),
				cursor.getLong(10),
				cursor.getLong(11),
				cursor.getLong(12)
		);
	}

	@Nullable
	public MangaBookmark find(MangaHeader manga, MangaChapter chapter, MangaPage page) {
		try (Cursor cursor = mStorageHelper.getReadableDatabase().query(
				getTableName(),
				getProjection(),
				"manga_id = ? AND chapter_id = ? AND page_id = ?",
				new String[]{String.valueOf(manga.id), String.valueOf(chapter.id), String.valueOf(page.id)},
				null,
				null,
				null
		)) {
			if (cursor.moveToFirst()) {
				return fromCursor(cursor);
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
}
