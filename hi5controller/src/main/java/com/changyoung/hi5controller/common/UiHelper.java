package com.changyoung.hi5controller.common;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.changyoung.hi5controller.weldutil.WeldTextViewerActivity;
import com.google.android.gms.ads.AdView;

/**
 * Created by changmin on 2017. 4. 25
 */
public class UiHelper {
	public static void textViewActivity(Activity context, String title, String text) {
		Intent intent = new Intent(context, WeldTextViewerActivity.class);
		intent.putExtra("title", title);
		intent.putExtra("text", text);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			//noinspection unchecked
			context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(context).toBundle());
		} else {
			context.startActivity(intent);
		}
	}

	@SuppressWarnings("unused")
	public static void textViewDialog(Context context, String text) {
		if (text != null)
			text = "";
		TextView textView = new TextView(context);
		textView.setPadding(10, 10, 10, 10);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		textView.setText(text);

		ScrollView scrollView = new ScrollView(context);
		scrollView.addView(textView);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(scrollView);

		builder.setPositiveButton("닫기", (dialog, which) -> {
		});

		builder.show();
	}

	public static void adMobExitDialog(final Activity context, AdView adView) {
		TextView tvMessage = new TextView(context);
		tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		tvMessage.setTypeface(Typeface.DEFAULT_BOLD);
		tvMessage.setPadding(40, 10, 40, 0);
		tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
		tvMessage.setText("종료 하시겠습니까?");

		LinearLayout linearLayout = new LinearLayout(context);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.addView(adView);
		linearLayout.addView(tvMessage);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("HI5 용접 관리");
		builder.setView(linearLayout);
		builder.setPositiveButton("확인", (dialog, which) -> context.finish());
		builder.setNegativeButton("취소", (dialog, which) -> {
		});
		builder.setOnCancelListener(dialog -> linearLayout.removeView(adView));
		builder.setOnDismissListener(dialog -> linearLayout.removeView(adView));
		builder.show();
	}

	@SuppressWarnings("unused")
	public static TranslateAnimation getCenterTranslateAnimation(View parentView, View view) {
		return getCenterTranslateAnimation(parentView, view, 1.0f);
	}

	public static TranslateAnimation getCenterTranslateAnimation(View parentView, View view, float scale) {
		float fromX = view.getX();
		float toX = (parentView.getWidth() - view.getWidth()) / 2f - fromX;
		float fromY = view.getY();
		float toY = (parentView.getHeight() - view.getHeight()) / 2f - fromY;
		return new TranslateAnimation(
				TranslateAnimation.RELATIVE_TO_SELF, 0f,
				TranslateAnimation.ABSOLUTE, toX / scale,
				TranslateAnimation.RELATIVE_TO_SELF, 0f,
				TranslateAnimation.ABSOLUTE, toY / scale
		);
	}

	@SuppressWarnings("unused")
	public static void clearFocus(Activity activity) {
		View view = activity.getCurrentFocus();
		if (view != null)
			view.clearFocus();
	}

	public static void hideSoftKeyboard(Activity activity, View view, KeyEvent event) {
		if (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
			final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (view == null)
				view = activity.getCurrentFocus();
			try {
				if (view != null) {
					imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
					if (view.hasFocus())
						view.clearFocus();
				}
			} catch (NullPointerException e) {
				Log.i("hideSoftKeyboard", e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
	public static void showSoftKeyboard(Activity activity, View view) {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (view == null)
			view = activity.getCurrentFocus();
		try {
			if (view != null) {
				Log.i("showSoftKeyboard", "imm.showSoftInput");
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		} catch (NullPointerException e) {
			Log.i("showSoftKeyboard", e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
