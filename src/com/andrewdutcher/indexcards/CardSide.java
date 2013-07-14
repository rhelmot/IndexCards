package com.andrewdutcher.indexcards;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;

public class CardSide {
	public String text = "";
	public String[] lines = {};
	public Paint textStyle = new Paint();
	public Paint fillStyle = new Paint();
	public float textSize = 0.125f;
	
	public CardSide() {
		fillStyle.setARGB(255, 220, 220, 220);
		textStyle.setARGB(255, 0, 0, 0);
		textStyle.setTypeface(Typeface.SANS_SERIF);
		textStyle.setTextAlign(Paint.Align.CENTER);
		
	}
	
	public CardSide(Bundle serialdata) {
		textStyle.setTypeface(Typeface.SANS_SERIF);
		textStyle.setTextAlign(Paint.Align.CENTER);
		textStyle.setColor(serialdata.getInt("textcolor"));
		fillStyle.setColor(serialdata.getInt("fillcolor"));
		text = serialdata.getString("text");
		lines = serialdata.getStringArray("lines");
	}
	
	public Bundle serialize() {
		Bundle out = new Bundle();
		out.putString("text", text);
		out.putStringArray("lines", lines);
		out.putInt("textcolor", textStyle.getColor());
		out.putInt("fillcolor", fillStyle.getColor());
		return out;
	}
	
	public Paint getTextStyle(int height, int width) {
		float ratio = ((float)height)/((float)width);
		//default: height = width * 3/5
		float adjustedratio = (float) (0.6/ratio);
		textStyle.setTextSize(height * textSize);
		textStyle.setTextScaleX(adjustedratio);
		return textStyle;
	}
}
