package org.xtimms.kitsune.ui.reader.pager;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.xtimms.kitsune.core.common.transformers.DepthPageTransformer;
import org.xtimms.kitsune.core.common.transformers.TabletPageTransformer;
import org.xtimms.kitsune.core.common.transformers.ZoomInPageTransformer;
import org.xtimms.kitsune.core.common.transformers.ZoomOutPageTransformer;
import org.xtimms.kitsune.ui.reader.OnOverScrollListener;

public class OverScrollPager extends ViewPager {

	private static final float SWIPE_TOLERANCE = .25f;

	private float mStartX;
	@Nullable
	private OnOverScrollListener mOverScrollListener;

	public OverScrollPager(@NonNull Context context) {
		this(context, null);
	}

	public OverScrollPager(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public void setOnOverScrollListener(@Nullable OnOverScrollListener listener) {
		mOverScrollListener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			if (ev.getActionMasked() == MotionEvent.ACTION_DOWN && ev.getPointerCount() == 1) {
				final int currentItem = getCurrentItem();
				if (currentItem == 0 || currentItem == getItemCount() - 1) {
					mStartX = ev.getX();
				}
			}

			return super.onInterceptTouchEvent(ev);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent ev) {
		if (mOverScrollListener == null || ev.getPointerCount() != 1) {
			return super.onTouchEvent(ev);
		}
		final int currentItem = getCurrentItem();
		try {
			if (currentItem == 0) {
				if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
					float displacement = ev.getX() - mStartX;

					if (ev.getX() > mStartX && displacement > getMeasuredWidth() * SWIPE_TOLERANCE) {
						mOverScrollListener.onOverScrolledStart();
						return true;
					}
					mStartX = 0f;
				}
			} else if (currentItem == getItemCount() - 1) {
				if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
					float displacement = mStartX - ev.getX();

					if (ev.getX() < mStartX && displacement > getMeasuredWidth() * SWIPE_TOLERANCE) {
						mOverScrollListener.onOverScrolledEnd();
						return true;
					}
					mStartX = 0f;
				}
			}

			return super.onTouchEvent(ev);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public int getItemCount() {
		PagerAdapter adapter = getAdapter();
		return adapter == null ? 0 : adapter.getCount();
	}
}
