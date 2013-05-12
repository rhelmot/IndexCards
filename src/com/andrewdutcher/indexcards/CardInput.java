package com.andrewdutcher.indexcards;

import android.content.Context;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class CardInput {
	public MainActivity context;
	public EditText textBox;
	public float density;
	
	public CardInput(MainActivity baseContext, EditText inputTextBox, float screenDensity) {
		context = baseContext;
		textBox = inputTextBox;
		density = screenDensity;
	}
	
	public void show() {
		RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams((int) context.mview.editspace[2]-50, (int) context.mview.editspace[3]-90); 
		rllp.topMargin = 81;
		rllp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		textBox.setLayoutParams(rllp);
		textBox.setVisibility(EditText.VISIBLE);
		((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(textBox, InputMethodManager.SHOW_FORCED);
	}
	
	public void hide() {
		textBox.setVisibility(EditText.INVISIBLE);
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textBox.getWindowToken(), 0);
	}
	
	public String get() {
		return textBox.getText().toString();
	}
	
	public void set(String text) {
		textBox.setText(text);
	}

	public void setTextSize(float f) {
		textBox.setTextSize(f/density);
	}
}
