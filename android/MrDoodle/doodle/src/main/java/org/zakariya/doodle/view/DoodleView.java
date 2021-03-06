package org.zakariya.doodle.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import org.zakariya.doodle.model.Doodle;

/**
 * Base view for doodling
 */
public class DoodleView extends View {

	private static final String TAG = "DoodleView";
	private Doodle doodle;
	private ViewTreeObserver.OnGlobalLayoutListener layoutListener;

	public DoodleView(Context context) {
		super(context);
	}

	public DoodleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Doodle getDoodle() {
		return doodle;
	}

	public void setDoodle(Doodle doodle) {
		this.doodle = doodle;
		doodle.setDoodleView(this);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (layoutListener == null) {

			layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (getWidth() != doodle.getWidth() || getHeight() != doodle.getHeight()) {
						doodle.resize(getWidth(), getHeight());
					}
				}
			};
		}

		getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
	}

	@Override
	protected void onDetachedFromWindow() {
		if (layoutListener != null) {
			getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
		}
		super.onDetachedFromWindow();
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (doodle != null) {
			doodle.draw(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (doodle != null) {
			return doodle.onTouchEvent(event);
		} else {
			return super.onTouchEvent(event);
		}
	}

}
