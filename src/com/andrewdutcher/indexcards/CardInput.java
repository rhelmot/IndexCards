package com.andrewdutcher.indexcards;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.graphics.*;

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
	
	public String[] getLines() {
		String[] initial = textBox.getText().toString().split("\n");
		ArrayList<String> out = new ArrayList<String>();
		Paint tp = textBox.getPaint();
		for (int i = 0; i < initial.length; i++) {
			while (tp.measureText(initial[i]) > textBox.getWidth()) {
				//Log.d("andrew", "Line '"+ initial[i] +"' too long");
				int safe = 0;
				int space;
				for (space = 0;
					tp.measureText(initial[i], 0, space) < textBox.getWidth();
					space = initial[i].indexOf(" ", space+1)) {
						safe = space;
						//Log.d("andrew", "Space found: " + ((Integer) safe).toString());
				}
				if (safe == 0)
					safe = space;
				out.add(initial[i].substring(0, safe + 1));
				initial[i] = initial[i].substring(safe + 1);
			}
			out.add(initial[i] + "\n");
		}
		return out.toArray(new String[out.size()]);
	}	
	public void set(String text) {
		textBox.setText(text);
	}
	
	public void setLines(String[] text) {
		String out = "";
		for (int i = 0; i < text.length; i++) {
			out += text[i];
		}
		set(out);
	}

	public void setTextSize(float f) {
		textBox.setTextSize(f/density);
	}
}
