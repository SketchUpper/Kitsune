package org.xtimms.kitsune.core.common.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

public final class TintTextView extends AppCompatTextView {

	public TintTextView(Context context) {
		super(context);
	}

	public TintTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TintTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top, @Nullable Drawable right, @Nullable Drawable bottom) {
		repaint(left);
		repaint(right);
		repaint(bottom);
		repaint(top);
		super.setCompoundDrawables(left, top, right, bottom);
	}

	@Override
	public void setCompoundDrawablesRelative(@Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end, @Nullable Drawable bottom) {
		repaint(start);
		repaint(end);
		repaint(bottom);
		repaint(top);
		super.setCompoundDrawablesRelative(start, top, end, bottom);
	}

	private void repaint(@Nullable Drawable drawable) {
		if (drawable != null) {
			DrawableCompat.setTintList(drawable, getTextColors());
			DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		final Drawable[] drawables = getCompoundDrawables();
		for (Drawable o : drawables) {
			repaint(o);
		}
	}
}
