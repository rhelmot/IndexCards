package com.andrewdutcher.indexcards;

import android.util.Log;

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
}
