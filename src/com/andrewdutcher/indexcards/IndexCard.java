package com.andrewdutcher.indexcards;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ActionMode;

public class IndexCard {
	
	public String cardText;
	public String[] cardLines;
	public Rect cardDim;
	public float rotation;
	public Paint fillStyle;
	public Paint textStyle;
	
	public Rect oCardDim;
	public float oRotation;
	public float scalefactor = 1;
	
	public int[] touches = {-1, -1, -1};
	public float touchangref;
	public float touchdistref;
	public float touchxref;
	public float touchyref;
	public float touchzref;
	
	public float offsetx;			//distance from card corner to rot point, cardspace
	public float offsety;
	
	public float oOffsetx;
	public float oOffsety;
	
	public boolean selected = false;
	public int deltaz;
	
	public long timetouch;
	public boolean singletouch;
	public boolean holdtouch;
	
	public boolean editing;
	public boolean drawcontrols;
	public boolean animating;
	public AnimatedNums animdata;	//{x, y, width, height, rotation}
	public double[] savedSpot = {};		//[x, y, width, height, offsetx, offsety, rotation]
	
	public String debugdata;
	
	public CardDrawer parent;
	
	public IndexCard(CardDrawer context, String text, String[] lines, Rect bounds, float rotdeg) {
		double[] temp = {};
		init(context, text, lines, bounds, rotdeg, temp);
	}
	
	public IndexCard(CardDrawer context, Bundle serialdata) {
		int x = serialdata.getInt("x");
		int y = serialdata.getInt("y");
		init(context, serialdata.getString("text"), serialdata.getStringArray("lines"), new Rect(x,y,x+serialdata.getInt("w"),y+serialdata.getInt("h")),serialdata.getFloat("rot"), serialdata.getDoubleArray("savedspot"));
		if (serialdata.getBoolean("editing")) {
			cardDim = new Rect((int)parent.editspace[0],(int)parent.editspace[1],(int)(parent.editspace[0]+parent.editspace[2]),(int)(parent.editspace[1]+parent.editspace[3]));
			parent.input.show(this);
		}
	}
	
	public void init(CardDrawer context, String text, String[] lines, Rect bounds, float rotdeg, double[] spot) 
	{
		parent = context;
		
		editing = false;
		animating = false;
		
		cardText = text;
		cardLines = lines;
		cardDim = bounds;
		oCardDim = new Rect(bounds);
		rotation = rotdeg;
		savedSpot = spot;
		
		fillStyle = new Paint();
		fillStyle.setARGB(255, 220, 220, 220);
		fillStyle.setAntiAlias(true);
		
		textStyle = new Paint();
		textStyle.setARGB(255, 0, 0, 0);
		textStyle.setTypeface(Typeface.SANS_SERIF);
		textStyle.setTextAlign(Align.CENTER);
		updateTextStyle();
	}
	
	public Bundle serialize()
	{
		Bundle out = new Bundle();
		if (animating) {
			double[] finals = animdata.endvalues;
			cardDim = new Rect((int) finals[0], (int) finals[1], (int) finals[2], (int) finals[3]);
			if (editing) {
				drawcontrols = true;
				parent.state = 2;
				parent.mActionMode = parent.startActionMode(parent.singleSelectedAction);
				parent.currentCard = this;
			}
		}
		out.putString("text", cardText);
		out.putStringArray("lines", cardLines);
		out.putInt("x", cardDim.left);
		out.putInt("y", cardDim.top);
		out.putInt("w", cardDim.width());
		out.putInt("h", cardDim.height());
		out.putFloat("rot", rotation);
		out.putBoolean("editing", editing);
		if (!editing)
			saveSpot();
		out.putDoubleArray("savedSpot", savedSpot);
		return out;
	}
	
	public void draw(Canvas c)
	{
		if (animating) {
			double[] data;
			if (animdata.isActive())
				data = animdata.getValues();
			else {
				data = animdata.endvalues;
				animating = false;
				if (editing && parent.state == 1) {
					parent.input.show(this);
				}
			}
			cardDim.offsetTo((int) data[0], (int) data[1]);
			cardDim.right = cardDim.left + (int) data[2];
			cardDim.bottom = cardDim.top + (int) data[3];
			offsetx = (float) data[4];
			offsety = (float) data[5];
			rotation = (float) data[6];
			parent.invalidate();
			if (!animdata.isActive()) {
				oCardDim.set(cardDim);
				unsetRotPoint();
			}
		}
		//drawDebugPoint(cardDim.left, cardDim.top, c);
		c.rotate(rotation, cardDim.left+offsetx, cardDim.top+offsety);
		//drawDebugPoint(cardDim.left, cardDim.top, c);
		cardDim.inset(-2, -3);
		if (selected) {
			parent.selshadowimg.setBounds(cardDim);
			parent.selshadowimg.draw(c);
		} else {
			parent.shadowimg.setBounds(cardDim);
			parent.shadowimg.draw(c);
		}
		cardDim.inset(2, 3);
		c.drawRect(cardDim, fillStyle);
		updateTextStyle();
		if (!drawcontrols) {
			float linespace = textStyle.getFontMetrics().descent - textStyle.getFontMetrics().ascent;
			float starty = cardDim.centerY() - (linespace*(cardLines.length-1)/2) + (linespace/2);
			for (int i = 0; i < cardLines.length; i++) {
				c.drawText(cardLines[i], cardDim.centerX(), starty, textStyle);
				//drawDebugPoint(cardDim.centerX(), (int) starty, c);
				starty += linespace;
			}
		} else {
			//new Bitm
			//c.drawBitmap(R.drawable.ok, 100, 100, new Paint());
		}
		//drawDebugPoint(cardDim.left + (int) offsetx, cardDim.top + (int) offsety, c);
	}
	
	public void drawDebugPoint(int x, int y, Canvas c) {
		c.drawRect(new Rect(x,y,x+10,y+10), textStyle);
	}
	
	public void updateTextStyle() {
		textStyle.setTextSize(cardDim.height()/8);
	}
	
	public boolean addPoint(MotionEvent e, int index) {
		//Log.d("andrew","down");
		if (editing) {					//this should never happen
			parent.input.hide();	//but just in case
			return true;
		}
		int id = e.getPointerId(index);
		if (touches[0] == -1)
		{
			touches[0] = id;
			touchxref = e.getX(index);
			touchyref = e.getY(index);
			oCardDim = new Rect(cardDim);
			timetouch = System.currentTimeMillis();
			selected = true;
			if (!singletouch)
			{
				holdtouch = true;
				new Handler().postDelayed(new Runnable() {
					public void run() {
						if (holdtouch)
							Log.d("andrew","Longtap!");
					}
				}, 1100);
			}
		}
		else if (touches[1] == -1)
		{
			touches[1] = id;
			int index0 = e.findPointerIndex(touches[0]);
			touchdistref = getdist(e.getX(index0), e.getY(index0), e.getX(index), e.getY(index));
			touchangref = getangle(e.getX(index0), e.getY(index0), e.getX(index), e.getY(index));
			touchxref = (e.getX(index) + e.getX(index0)) / 2;
			touchyref = (e.getY(index) + e.getY(index0)) / 2;
			oCardDim = new Rect(cardDim);
			oRotation = rotation;
			
			setRotPoint(touchxref, touchyref);
			oOffsetx = offsetx;
			oOffsety = offsety;
		}
		else if (touches[2] == -1)
		{
			touches[2] = id;
			touchzref = e.getY(index);
		}
		return true;
	}
	public void processTouches(MotionEvent e)
	{
		holdtouch = false;
		int action = e.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP)
		{
			//Log.d("andrew", "up");
			int index = 0;
			if (action == MotionEvent.ACTION_POINTER_UP)
				index = e.getActionIndex();
			int id = e.getPointerId(index);
			if (touches[2] == id)
			{
				touches[2] = -1;
			}
			else if (touches[1] == id)
			{
				unsetRotPoint();
				
				int index0 = e.findPointerIndex(touches[0]);
				touches[1] = -1;
				touchxref = e.getX(index0);
				touchyref = e.getY(index0);
				oCardDim = new Rect(cardDim);
			}
			else if (touches[0] == id)
			{
				if (touches[1] == -1)
				{
					touches[0] = -1;
					selected = false;
					
					if (singletouch)
					{
						singletouch = false;
						Log.d("andrew","Double Touch!");
					}
					else
					{
						long ctime = System.currentTimeMillis();
						long dtime = ctime - timetouch;
						//Log.d("andrew", ((Long)dtime).toString());
						if (dtime < 150)
						{
							timetouch = ctime;
							singletouch = true;
							new Handler().postDelayed(new Runnable() {
	
								public void run() {
									if (singletouch)
									{
										singletouch = false;
										singletap();
										Log.d("andrew","Single Touch!");
									}
								}
								
							}, 250);
						}
					}
				}
				else
				{
					unsetRotPoint();
					
					int index0 = e.findPointerIndex(touches[1]);
					touches[0] = touches[1];
					touches[1] = -1;
					touchxref = e.getX(index0);
					touchyref = e.getY(index0);
					oCardDim = new Rect(cardDim);
				}
			}
		}
		else if (e.getAction() == MotionEvent.ACTION_MOVE)
		{
			//Log.d("andrew", "move");
			timetouch -= 20;
			if (touches[0] == -1)
			{}
			else if (touches[1] == -1)
			{
				int index = e.findPointerIndex(touches[0]);
				cardDim.set(oCardDim);
				int dx = (int) (e.getX(index)-touchxref);
				int dy = (int) (e.getY(index)-touchyref);
				cardDim.offset(dx,dy);
			}
			else
			{
				int index1 = e.findPointerIndex(touches[0]);
				int index2 = e.findPointerIndex(touches[1]);
				float frm = getangle(e.getX(index1), e.getY(index1), e.getX(index2), e.getY(index2));
				rotation = oRotation + frm - touchangref;
				cardDim.set(oCardDim);
				float ds = getdist(e.getX(index1), e.getY(index1), e.getX(index2), e.getY(index2));
				scalefactor = ds/touchdistref;
				if (scalefactor*oCardDim.height() > 600)
					scalefactor = 600/(float)oCardDim.height();
				else if (scalefactor*oCardDim.height() < 200)
					scalefactor = 200/(float)oCardDim.height();												//MATH
				float rotx = ((e.getX(index1)+e.getX(index2))/2);
				float roty = ((e.getY(index1)+e.getY(index2))/2);
				cardDim.set(oCardDim);
				offsetx = oOffsetx*scalefactor;
				offsety = oOffsety*scalefactor;
				cardDim.right += oCardDim.width()*(scalefactor-1);
				cardDim.bottom += oCardDim.height()*(scalefactor-1);
				cardDim.offsetTo((int) (rotx-offsetx),(int) (roty-offsety));
				
				
			}
			if (touches[2] != -1)
			{
				int index2 = e.findPointerIndex(touches[2]);
				float cz = e.getY(index2);
				float dz = touchzref-cz;
				if (dz > 40)
				{
					deltaz++;
					touchzref = cz;
				}
				else if (dz < -40)
				{
					deltaz--;
					touchzref = cz;
				}
			}
		}
	}
	
	public void setRotPoint(float x, float y) {
		double x1 = (x-cardDim.left)/Math.cos(Math.toRadians(rotation));
		double h1 = (x-cardDim.left)*Math.tan(Math.toRadians(rotation));
		double h2 = (y-cardDim.top) - h1;
		double x2 = h2*Math.sin(Math.toRadians(rotation));
		offsetx = (float) (x1+x2);
		offsety = (float) (h2*Math.cos(Math.toRadians(rotation)));
		cardDim.offsetTo((int)(x-offsetx),(int)(y-offsety));
	}
	
	public void unsetRotPoint() {
		double y2 = offsety/Math.cos(Math.toRadians(rotation));
		double x2 = offsety*Math.tan(Math.toRadians(rotation));
		double x1 = offsetx - x2;
		double x = x1*Math.cos(Math.toRadians(rotation));
		double y1 = x1*Math.sin(Math.toRadians(rotation));
		double y = y1+y2;
		cardDim.offsetTo((int)(cardDim.left+offsetx-x), (int)(cardDim.top+offsety-y));
		cardDim.right = cardDim.left + (int) (oCardDim.width()*scalefactor);
		cardDim.bottom = cardDim.top + (int) (oCardDim.height()*scalefactor);
		offsetx = 0;
		offsety = 0;
		scalefactor = 1;
	}
	
	public void setRotOffset(float x, float y) {
		unsetRotPoint();
		cardDim.offset((int) ((x * Math.cos(Math.toRadians(rotation))) - (y * Math.sin(Math.toRadians(rotation))) - x), (int) ((x * Math.sin(Math.toRadians(rotation))) + (y * Math.cos(Math.toRadians(rotation))) - y));
		offsetx = x;
		offsety = y;
	}
	
	public void cancelTouches() {
		if (touches[1] != -1)
		{
			unsetRotPoint();
		}
		touches[0] = -1;
		touches[1] = -1;
		touches[2] = -1;
	}
	
	public void singletap() {
		setRotOffset(cardDim.width()/2, cardDim.height()/2);
		saveSpot();
		animdata = new AnimatedNums(savedSpot, parent.editspace, 500);
		animating = true;
		editing = true;
		parent.state = 1;
		parent.currentCard = this;
		parent.invalidate();
	}
	
	public void doubletap() {
		
	}
	
	public void longtap() {
		
	}
	
	public void saveSpot() {
		double[] temp = {(double) cardDim.left, (double) cardDim.top, (double) cardDim.width(), (double) cardDim.height(), (double) offsetx, (double) offsety, (double) rotation};
		savedSpot = temp;
	}
	
	public boolean doesPointTouch(int x, int y) {
		int cx = cardDim.left;
		int cy = cardDim.top;
		if (offsetx != 0 || offsety != 0)
		{
			/*double y2 = offsety/Math.cos(Math.toRadians(rotation));
			double x2 = offsety*Math.tan(Math.toRadians(rotation));
			double x1 = offsetx - x2;
			cx = (int) (x1*Math.cos(Math.toRadians(rotation)));
			double y1 = x1*Math.sin(Math.toRadians(rotation));
			cy = (int) (y1+y2);*/
			Vector ux = new Vector(-offsetx, rotation);
			Vector uy = new Vector(-offsety, rotation + 90);
			cx += ux.getX() + uy.getX() + offsetx;
			cy += ux.getY() + uy.getY() + offsety;
		}
		double w1 = cardDim.width()*Math.cos(Math.toRadians(rotation));
		double h1 = cardDim.width()*Math.sin(Math.toRadians(rotation));		//some of these will be negative
		double w2 = -cardDim.height()*Math.sin(Math.toRadians(rotation));
		double h2 = cardDim.height()*Math.cos(Math.toRadians(rotation));
		Vector v1 = new Vector(x - cx, y - cy, 0);
		Vector v2 = new Vector(x - (cx + w1), y - (cy + h1), 0);
		Vector v3 = new Vector(x - (cx + w1 + w2), y - (cy + h1 + h2), 0);
		Vector v4 = new Vector(x - (cx + w2), y - (cy + h2), 0);
		double[] vectorsums = {v1.add(v2).get2DAngle(),v2.add(v3).get2DAngle(),v3.add(v4).get2DAngle(),v4.add(v1).get2DAngle()};
		for (int j = 0; j < 4; j++)
		{
			vectorsums[j] = (vectorsums[j] - rotation) % 360;
			if (vectorsums[j] < 0)
				vectorsums[j] += 360; //angles range from -90 to 270 for some odd reason -- let's fix that
		}
		return (vectorsums[0] < 180 &&
			vectorsums[1] < 270 && vectorsums[1] > 90 &&
			vectorsums[2] > 180 &&	
			(vectorsums[3] > 270 || vectorsums[3] < 90));
	}
	
	private float getangle(float x1, float y1, float x2, float y2) {
		float out = (float) Math.toDegrees(Math.atan((y1-y2)/(x1-x2)));
		if (x1 > x2)
			out += 180;
		return out;
	}
	private float getdist(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2, 2));
	}
}
