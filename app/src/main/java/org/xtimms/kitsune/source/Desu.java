package org.xtimms.kitsune.source;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xtimms.kitsune.R;
import org.xtimms.kitsune.utils.network.NetworkUtils;
import org.xtimms.kitsune.core.MangaStatus;
import org.xtimms.kitsune.core.models.MangaChapter;
import org.xtimms.kitsune.core.models.MangaDetails;
import org.xtimms.kitsune.core.models.MangaGenre;
import org.xtimms.kitsune.core.models.MangaHeader;
import org.xtimms.kitsune.core.models.MangaPage;

import java.util.ArrayList;

@SuppressLint("DefaultLocale")
public final class Desu extends MangaProvider {

	public static final String CNAME = "network/desu.me";
	public static final String DNAME = "Desu";

	private final int[] mSorts = new int[] {
			R.string.sort_popular,
			R.string.sort_alphabetical,
			R.string.sort_updated
	};

	private final String[] mSortValues = new String[] {
			"popular",
			"name",
			"updated"
	};

	private final MangaGenre[] mGenres = new MangaGenre[]{
			new MangaGenre(R.string.genre_madness, "dementia"),
			new MangaGenre(R.string.genre_martialarts, "martial%20arts"),
			new MangaGenre(R.string.genre_vampires, "vampire"),
			new MangaGenre(R.string.genre_military, "military"),
			new MangaGenre(R.string.genre_harem, "harem"),
			new MangaGenre(R.string.genre_youkai, "demons"),
			new MangaGenre(R.string.genre_detective, "mystery"),
			new MangaGenre(R.string.genre_kids, "kids"),
			new MangaGenre(R.string.genre_josei, "josei"),
			new MangaGenre(R.string.genre_doujinshi, "doujinshi"),
			new MangaGenre(R.string.genre_drama, "drama"),
			new MangaGenre(R.string.genre_game, "game"),
			new MangaGenre(R.string.genre_historical, "historical"),
			new MangaGenre(R.string.genre_comedy, "comedy"),
			new MangaGenre(R.string.genre_space, "space"),
			new MangaGenre(R.string.genre_magic, "magic"),
			new MangaGenre(R.string.genre_cars, "cars"),
			new MangaGenre(R.string.genre_mecha, "mecha"),
			new MangaGenre(R.string.genre_music, "music"),
			new MangaGenre(R.string.genre_parodi, "parody"),
			new MangaGenre(R.string.genre_slice_of_life, "slice%20of%20life"),
			new MangaGenre(R.string.genre_police, "police"),
			new MangaGenre(R.string.genre_adventure, "adventure"),
			new MangaGenre(R.string.genre_psychological, "psychological"),
			new MangaGenre(R.string.genre_romance, "romance"),
			new MangaGenre(R.string.genre_samurai, "samurai"),
			new MangaGenre(R.string.genre_supernatural, "supernatural"),
			new MangaGenre(R.string.genre_shoujo, "shoujo"),
			new MangaGenre(R.string.genre_shoujo_ai, "shoujo%20ai"),
			new MangaGenre(R.string.genre_seinen, "seinen"),
			new MangaGenre(R.string.genre_shounen, "shounen"),
			new MangaGenre(R.string.genre_shounen_ai, "shounen%20ai"),
			new MangaGenre(R.string.genre_genderbender, "gender%20bender"),
			new MangaGenre(R.string.genre_sports, "sports"),
			new MangaGenre(R.string.genre_superpower, "super%20power"),
			new MangaGenre(R.string.genre_thriller, "thriller"),
			new MangaGenre(R.string.genre_horror, "horror"),
			new MangaGenre(R.string.genre_fantastic, "sci-fi"),
			new MangaGenre(R.string.genre_fantasy, "fantasy"),
			new MangaGenre(R.string.genre_school, "school"),
			new MangaGenre(R.string.genre_action, "action"),
			new MangaGenre(R.string.genre_ecchi, "ecchi"),
			new MangaGenre(R.string.genre_yuri, "yuri"),
			new MangaGenre(R.string.genre_yaoi, "yaoi")
	};

	public Desu(Context context) {
		super(context);
	}

	@NonNull
	@Override
	public ArrayList<MangaHeader> query(@Nullable String search, int page, int sortOrder, int additionalSortOrder, @NonNull String[] genres, @NonNull String[] types) throws Exception {
		String url = String.format(
				"http://desu.me/manga/api/?limit=20&order=%s&page=%d&genres=%s&search=%s",
				sortOrder == -1 ? "popular" : mSortValues[sortOrder],
				page + 1,
				TextUtils.join(",", genres),
				search == null ? "" : search
		);
		JSONArray ja = NetworkUtils.getJSONObject(url).getJSONArray("response");
		ArrayList<MangaHeader> list = new ArrayList<>(ja.length());
		for (int i = 0; i < ja.length(); i++) {
			JSONObject jo = ja.getJSONObject(i);
			int status = MangaStatus.STATUS_UNKNOWN;
			switch (jo.getString("status")) {
				case "released":
					status = MangaStatus.STATUS_COMPLETED;
					break;
				case "ongoing":
					status = MangaStatus.STATUS_ONGOING;
					break;
			}
			list.add(new MangaHeader(
					jo.getString("name"),
					jo.getString("russian"),
					jo.getString("genres"),
					"http://desu.me/manga/api/" + jo.getInt("id"),
					jo.getJSONObject("image").getString("x225"),
					CNAME,
					status,
					(byte) (jo.getDouble("score") * 10)
			));
		}
		return list;
	}

	@NonNull
	@Override
	public MangaDetails getDetails(MangaHeader header) throws Exception {
        assert header.url != null;
        JSONObject jo = NetworkUtils.getJSONObject(header.url).getJSONObject("response");
		MangaDetails details = new MangaDetails(
				header,
				jo.getString("description"),
				jo.getJSONObject("image").getString("original"),
				"" //not supported by desu.me
		);
		JSONArray ja = jo.getJSONObject("chapters").getJSONArray("list");
		final int total = ja.length();
		for (int i = 0; i < total; i++) {
			JSONObject chapter = ja.getJSONObject(total - i - 1);
			final String ch = chapter.getString("ch");
			details.chapters.add(new MangaChapter(
					chapter.isNull("title") ? "Глава " + ch : ch + " - " + chapter.getString("title"),
					i,
					details.url + "/chapter/" + chapter.getInt("id"),
					CNAME,
					"",
					chapter.getLong("date")*1000
			));
		}
		return details;
	}

	@NonNull
	@Override
	public ArrayList<MangaPage> getPages(String chapterUrl) throws Exception {
		JSONObject jo = NetworkUtils.getJSONObject(chapterUrl).getJSONObject("response");
		JSONArray ja = jo.getJSONObject("pages").getJSONArray("list");
		ArrayList<MangaPage> pages = new ArrayList<>(ja.length());
		for (int i = 0; i < ja.length(); i++) {
			jo = ja.getJSONObject(i);
			pages.add(new MangaPage(
					jo.getString("img"),
					CNAME
			));
		}
		return pages;
	}

	@Override
	public String getPageImage(MangaPage mangaPage) {
		return mangaPage.url;
	}

	@Override
	public MangaGenre[] getAvailableGenres() {
		return mGenres;
	}

	@Override
	public int[] getAvailableSortOrders() {
		return mSorts;
	}

}