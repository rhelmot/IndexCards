package com.andrewdutcher.indexcards;

import android.util.Log;
import java.util.ArrayList;

public class Debug {
	public static int flagnum = 0;
	public static void log() {
		flagnum++;
		log("hit flag ", flagnum);
	}
	
	public static void log(String msg) {
		Log.d("andrew", msg);
	}
	
	public static void log(String msg, int data) {
		log(msg + ((Integer) data).toString());
	}
	
	public static void log(String msg, float data) {
		log(msg + ((Float) data).toString());
	}
	
	public static void log(String msg, boolean data) {
		log(msg + ((Boolean) data).toString());
	}

	public static void log(String msg, ArrayList data) {
		String out = msg;
		for (int i = 0; i < data.size(); i++) {
			out += "\n" + data.get(i).toString();
		}
		log(out);
	}
}
