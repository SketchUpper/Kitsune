package org.xtimms.kitsune.ui.mangalist.updates;

import android.content.Context;
import android.content.Intent;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.xtimms.kitsune.R;
import org.xtimms.kitsune.utils.ImageUtils;
import org.xtimms.kitsune.core.models.MangaFavourite;
import org.xtimms.kitsune.source.MangaProvider;
import org.xtimms.kitsune.ui.preview.PreviewActivity;

import java.util.ArrayList;

final class MangaUpdatesAdapter extends RecyclerView.Adapter<MangaUpdatesAdapter.HistoryHolder> {

	private final ArrayList<MangaFavourite> mDataset;

	MangaUpdatesAdapter(ArrayList<MangaFavourite> dataset) {
		setHasStableIds(true);
		mDataset = dataset;
	}

	@Override
	public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new HistoryHolder(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_manga_list, parent, false));
	}

	@Override
	public void onBindViewHolder(HistoryHolder holder, int position) {
		MangaFavourite item = mDataset.get(position);
		holder.text1.setText(item.name);
		holder.text2.setText(holder.itemView.getResources().getQuantityString(
				R.plurals.chapters_new, item.newChapters, item.newChapters
		));
		holder.summary.setText(item.genres);
		ImageUtils.setThumbnail(holder.imageView, item.thumbnail, MangaProvider.getDomain(item.provider));
		holder.itemView.setTag(item);
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

	@Override
	public long getItemId(int position) {
		return mDataset.get(position).id;
	}

	@Override
	public void onViewRecycled(HistoryHolder holder) {
		ImageUtils.recycle(holder.imageView);
		super.onViewRecycled(holder);
	}

	class HistoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

		final TextView text1;
		final TextView text2;
		final TextView summary;
		final ImageView imageView;

		HistoryHolder(View itemView) {
			super(itemView);
			text1 = itemView.findViewById(android.R.id.text1);
			text2 = itemView.findViewById(android.R.id.text2);
			summary = itemView.findViewById(android.R.id.summary);
			imageView = itemView.findViewById(R.id.imageView);

			itemView.setOnClickListener(this);
		}

		@Override
		public void onClick(View view) {
			final Context context = view.getContext();
			final MangaFavourite item = mDataset.get(getAdapterPosition());
            context.startActivity(new Intent(context.getApplicationContext(), PreviewActivity.class)
                    .putExtra("manga", item));
        }
	}
}
