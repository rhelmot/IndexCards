package com.andrewdutcher.indexcards;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.view.MotionEvent;

public class IndexCard {
	
	public String cardText;
	public Rect cardDim;
	public float rotation;
	public Paint fillStyle;
	public Paint textStyle;
	/*public Paint shadow;
	public Paint selshadow;*/
	public NinePatchDrawable shadow;
	public NinePatchDrawable selshadow;
	
	public Rect oCardDim;
	public float oRotation;
	public float scalefactor;
	
	public int[] touches = {-1, -1, -1};
	public float touchangref;
	public float touchdistref;
	public float touchxref;
	public float touchyref;
	public float touchzref;
	
	public int activemove;
	public int activesize;
	public int activerot;
	
	public float offsetx;
	public float offsety;
	
	public int rotx;
	public int roty;
	
	public long timetouch;
	public boolean selected = false;
	public int deltaz;
	
	public boolean animating;
	public AnimatedNums animdata;	//{x, y, width, height, rotation}
	
	public String debugdata;
	
	public IndexCard(String text, Rect bounds, NinePatchDrawable shadowimg, NinePatchDrawable selshadowimg)
	{
		/*shadow = new Paint();
		shadow.setColor(0xFF000000);
		shadow.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.OUTER));
		selshadow = new Paint(shadow);
		selshadow.setColor(0xFF56AAFF);*/
		shadow = shadowimg;
		selshadow = selshadowimg;
		
		cardText = text;
		cardDim = bounds;
		fillStyle = new Paint();
		fillStyle.setARGB(255, 220, 220, 220);
		fillStyle.setAntiAlias(true);
		textStyle = new Paint();
		textStyle.setARGB(255, 0, 0, 0);
		textStyle.setTypeface(Typeface.SANS_SERIF);
		textStyle.setTextAlign(Align.CENTER);
		updateTextStyle();
		rotation = 0;
	}
	
	public boolean draw(Canvas c)
	{
		if (animating)
		{
			double[] data;
			if (animdata.isActive())
				data = animdata.getValues();
			else
			{
				data = animdata.endvalues;
				animating = false;
				unsetRotPoint();
			}
			cardDim.offsetTo((int) data[0], (int) data[1]);
			cardDim.right = cardDim.left + (int) data[2];
			cardDim.bottom = cardDim.top + (int) data[3];
			rotation = (float) data[4];
			//Log.d("andrew", new Integer((int) data[2]).toString());
		}
		if (touches[1]==-1 && !animating)
			c.rotate(rotation, cardDim.left, cardDim.top);
		else
			c.rotate(rotation, rotx, roty);
		cardDim.inset(-2, -3);
		if (selected)
		{
			selshadow.setBounds(cardDim);
			selshadow.draw(c);
		}
		else
		{
			shadow.setBounds(cardDim);
			shadow.draw(c);
		}
		cardDim.inset(2, 3);
		c.drawRect(cardDim, fillStyle);
		updateTextStyle();
		c.drawText(cardText, (cardDim.left+cardDim.right)/2, (cardDim.top+cardDim.bottom)/2, textStyle);
		return animating;
		/*Rect r = new Rect();
		r.left = rotx;
		r.top = roty;
		r.right = r.left + 10;
		r.bottom = r.top + 10;
		c.drawRect(r, textStyle);*/
	}
	
	public void updateTextStyle()
	{
		textStyle.setTextSize(cardDim.height()/8);
	}
	
	public boolean addPoint(MotionEvent e, int index) {
		int id = e.getPointerId(index);
		if (touches[0] == -1)
		{
			touches[0] = id;
			touchxref = e.getX(index);
			touchyref = e.getY(index);
			oCardDim = new Rect(cardDim);
			timetouch = System.currentTimeMillis();
			selected = true;
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
			rotx = (int) touchxref;
			roty = (int) touchyref;
			
			setRotPoint();
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
		int action = e.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP)
		{
			int index = 0;
			if (action == MotionEvent.ACTION_POINTER_UP)
				index = e.getActionIndex();
			int id = e.getPointerId(index);
			if (touches[2] == id)
			{
				touches[2] = -1;
				//TODO: handle z changes
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
				rotx = (int) ((e.getX(index1)+e.getX(index2))/2);
				roty = (int) ((e.getY(index1)+e.getY(index2))/2);
				cardDim.right += oCardDim.width()*(scalefactor-1);
				cardDim.bottom += oCardDim.height()*(scalefactor-1);
				cardDim.offsetTo((int) (rotx-(offsetx*scalefactor)),(int) (roty-(offsety*scalefactor)));
				
				
			}
			if (touches[2] != -1)
			{
				int index2 = e.findPointerIndex(touches[2]);
				float cz = e.getY(index2);
				float dz = touchzref-cz;
				if (dz >= 29 || dz <= -29)
				{
					deltaz = (int)dz/30;
					touchzref += deltaz*30;
				}
			}
		}
	}
	
	public void setRotPoint() {
		double x1 = (touchxref-cardDim.left)/Math.cos(Math.toRadians(rotation));
		double h1 = (touchxref-cardDim.left)*Math.tan(Math.toRadians(rotation));
		double h2 = (touchyref-cardDim.top) - h1;
		double x2 = h2*Math.sin(Math.toRadians(rotation));
		offsetx = (float) (x1+x2);
		offsety = (float) (h2*Math.cos(Math.toRadians(rotation)));
		cardDim.offsetTo(rotx-(int)offsetx,roty-(int)offsety);
	}
	
	public void unsetRotPoint() {
		double y2 = offsety*scalefactor/Math.cos(Math.toRadians(rotation));
		double x2 = offsety*scalefactor*Math.tan(Math.toRadians(rotation));
		double x1 = offsetx*scalefactor - x2;
		double x = x1*Math.cos(Math.toRadians(rotation));
		double y1 = x1*Math.sin(Math.toRadians(rotation));
		double y = y1+y2;
		cardDim.set(oCardDim);
		cardDim.offsetTo(rotx-(int)x, roty-(int)y);
		cardDim.right = cardDim.left + (int) (cardDim.width()*scalefactor);
		cardDim.bottom = cardDim.top + (int) (cardDim.height()*scalefactor);
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
