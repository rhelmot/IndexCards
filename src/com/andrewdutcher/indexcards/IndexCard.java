package com.andrewdutcher.indexcards;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ActionMode;
import android.graphics.*;

public class IndexCard {
	public Rect cardDim;
	public float rotation;
	public float offsetx;			// distance from card coordinates (UL corner)
	public float offsety;			// to rot point, cardspace
	public double[] savedSpot;		//[x, y, width, height, offsetx, offsety, rotation]

	public CardDrawer parent;
	public CardSide currentside;
	public ArrayList<CardSide> sides;
	public int sidenum;

	public long timetouch;
	public boolean singletouch = false;
	public boolean holdtouch;
	public int deltaz;

	public boolean selected = false;
	public boolean multiSelected = false;
	public boolean animating;
	public boolean editing;
	public AnimatedNums animdata;
	public int animpurpose;
	
	private double velx = 0;
	private double vely = 0;
	private double velt = 0;
	private double lastx;
	private double lasty;
	private double lastt;
	
	private Rect oCardDim;
	private float oRotation;
	private float oOffsetx;
	private float oOffsety;
	private float scalefactor = 1;
	
	private int[] touches = {-1, -1, -1};
	private float touchangref;
	private float touchdistref;
	private float touchxref;
	private float touchyref;
	private float touchzref;
	
	public String debugdata;
	
	public IndexCard(CardDrawer context) {
		ArrayList<CardSide> temp = new ArrayList<CardSide>();
		CardSide tempd = new CardSide();
		temp.add(tempd);
		init(context, temp, 0, new Rect(100, 60, 200, 120), 0, new double[0]);
	}
	
	public IndexCard(CardDrawer context, Bundle serialdata) {
		int x = serialdata.getInt("x");
		int y = serialdata.getInt("y");
		ArrayList<CardSide> temp = new ArrayList<CardSide>();
		Parcelable[] cardpak = serialdata.getParcelableArray("sides");
		for (int i = 0; i < cardpak.length; i++)
			temp.add(new CardSide((Bundle) cardpak[i]));
		init(context, temp, serialdata.getInt("currentside"), new Rect(x,y,x+serialdata.getInt("w"),y+serialdata.getInt("h")),serialdata.getFloat("rot"), serialdata.getDoubleArray("savedspot"));
		if (serialdata.getBoolean("editing")) {
			cardDim = new Rect((int)parent.editspace[0],(int)parent.editspace[1],(int)(parent.editspace[0]+parent.editspace[2]),(int)(parent.editspace[1]+parent.editspace[3]));
			parent.input.show(this);
		}
	}
	
	public void init(CardDrawer context, ArrayList<CardSide> sidelist, int sidenumber, Rect bounds, float rotdeg, double[] spot)  {
		parent = context;
		editing = false;
		animating = false;
		cardDim = bounds;
		oCardDim = new Rect(bounds);
		rotation = rotdeg;
		savedSpot = spot;
		sides = sidelist;
		sidenum = sidenumber;
		currentside = sides.get(sidenumber);
	}
	
	public Bundle serialize() {
		Bundle out = new Bundle();
		if (animating) {
			double[] finals = animdata.endvalues;
			if (animpurpose == 3)
				finals = savedSpot;
			else if (animpurpose == 4)
				finals = parent.editspace;
			cardDim = new Rect((int) finals[0], (int) finals[1], (int) finals[2], (int) finals[3]);
			if (editing) {
				parent.state = 2;
				parent.mActionMode = parent.startActionMode(parent.singleSelectedAction);
				parent.currentCard = this;
			}
		}
		out.putInt("x", cardDim.left);
		out.putInt("y", cardDim.top);
		out.putInt("w", cardDim.width());
		out.putInt("h", cardDim.height());
		out.putFloat("rot", rotation);
		out.putBoolean("editing", editing);
		if (!editing)
			saveSpot();
		out.putDoubleArray("savedspot", savedSpot);
		Parcelable[] sidepak = new Parcelable[sides.size()];
		for (int i = 0; i < sidepak.length; i++) {
			sidepak[i] = sides.get(i).serialize();
		}
		out.putParcelableArray("sides", sidepak);
		out.putInt("currentside", sidenum);
		return out;
	}
	
	public void draw(Canvas c) {
		processInertia();
		boolean singlePurpose = false;
		if (animating) {
			double[] data;
			if (animdata.isActive())
				data = animdata.getValues();
			else {
				data = animdata.endvalues;
				animating = false;
				if (animpurpose == 1) {
					parent.input.show(this);
					animpurpose = 0;
					singlePurpose = true;
				} else if (animpurpose == 3 || animpurpose== 4) {
					sidenum = (sidenum + 1) % sides.size();
					currentside = sides.get(sidenum);
					animdata = new AnimatedNums(data, savedSpot, 200, AnimatedNums.getArrayOfEases(Easing.EASEOUT, 7));
					animating = true;
					animpurpose = (animpurpose == 3) ? 0 : 1;
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
		c.rotate(rotation, cardDim.left+offsetx, cardDim.top+offsety);
		cardDim.inset(-2, -3);
		if (selected || multiSelected) {
			parent.selshadowimg.setBounds(cardDim);
			parent.selshadowimg.draw(c);
		} else {
			parent.shadowimg.setBounds(cardDim);
			parent.shadowimg.draw(c);
		}
		cardDim.inset(2, 3);
		c.drawRect(cardDim, currentside.fillStyle);
		if (!editing || animating || singlePurpose) {
			Paint textStyle = currentside.getTextStyle(cardDim.height(), cardDim.width());
			float linespace = textStyle.getFontMetrics().descent - textStyle.getFontMetrics().ascent;
			float starty = cardDim.centerY() - (linespace*(currentside.lines.length-1)/2) + textStyle.getFontMetrics().descent + textStyle.getFontMetrics().bottom/2;
			for (int i = 0; i < currentside.lines.length; i++) {
				c.drawText(currentside.lines[i], cardDim.centerX(), starty, textStyle);
				starty += linespace;
			}
		}
	}
	
	public boolean addPoint(MotionEvent e, int index) {
		if (editing) {					//this should never happen
			parent.input.hide();		//but just in case
			return true;
		}
		holdtouch = false;
		int id = e.getPointerId(index);
		if (touches[0] == -1) {
			touches[0] = id;
			touchxref = e.getX(index);
			touchyref = e.getY(index);
			setRotPoint(touchxref, touchyref);
			oCardDim = new Rect(cardDim);
			timetouch = System.currentTimeMillis();
			selected = true;
			if (!singletouch) {
				holdtouch = true;
				new Handler().postDelayed(new Runnable() {
					public void run() {
						if (holdtouch)
							longtap();
					}
				}, 1100);
			}
		} else if (touches[1] == -1) {
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
		} else if (touches[2] == -1) {
			touches[2] = id;
			touchzref = e.getY(index);
		}
		return true;
	}
	
	public void processTouches(MotionEvent e) {
		if (animating)
			return;
		int action = e.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
			holdtouch = false;
			int index = 0;
			if (action == MotionEvent.ACTION_POINTER_UP)
				index = e.getActionIndex();
			int id = e.getPointerId(index);
			if (touches[2] == id) {
				touches[2] = -1;
			} else if (touches[1] == id) {
				int index0 = e.findPointerIndex(touches[0]);
				touches[1] = -1;
				touchxref = e.getX(index0);
				touchyref = e.getY(index0);
				setRotPoint(touchxref, touchyref);
				oCardDim = new Rect(cardDim);
			} else if (touches[0] == id) {
				if (touches[1] == -1) {
					touches[0] = -1;
					selected = false;
					if (singletouch) {
						singletouch = false;
						doubletap();
					} else {
						long ctime = System.currentTimeMillis();
						long dtime = ctime - timetouch;
						if (dtime < 150) {
							timetouch = ctime;
							singletouch = true;
							new Handler().postDelayed(new Runnable() {
								public void run() {
									if (singletouch) {
										singletouch = false;
										singletap();
									}
								}
							}, 250);
						}
					}
				} else {
					int index0 = e.findPointerIndex(touches[1]);
					touches[0] = touches[1];
					touches[1] = -1;
					touchxref = e.getX(index0);
					touchyref = e.getY(index0);
					setRotPoint(touchxref, touchyref);
					oCardDim = new Rect(cardDim);
				}
			}
		}
		else if (e.getAction() == MotionEvent.ACTION_MOVE) {
			timetouch -= 20;
			if (touches[0] == -1) {}
			else if (touches[1] == -1) {
				int index = e.findPointerIndex(touches[0]);
				cardDim.set(oCardDim);
				int dx = (int) (e.getX(index)-touchxref);
				int dy = (int) (e.getY(index)-touchyref);
				if (!holdtouch || (dx*dx + dy*dy > 100)) {
					holdtouch = false;
					cardDim.offset(dx,dy);
				}
			} else {
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
			if (touches[2] != -1) {
				int index2 = e.findPointerIndex(touches[2]);
				float cz = e.getY(index2);
				float dz = touchzref-cz;
				if (dz > 40) {
					deltaz++;
					touchzref = cz;
				} else if (dz < -40) {
					deltaz--;
					touchzref = cz;
				}
			}
		}
	}
	
	public void setRotPoint(float x, float y) {
		if (offsetx != 0 || offsety != 0)
			unsetRotPoint();
		double x1 = (x-cardDim.left)/Math.cos(Math.toRadians(rotation));
		double y1 = (x-cardDim.left)*Math.tan(Math.toRadians(rotation));
		double y2 = (y-cardDim.top) - y1;
		double x2 = y2*Math.sin(Math.toRadians(rotation));
		offsetx = (float) (x1+x2);
		offsety = (float) (y2*Math.cos(Math.toRadians(rotation)));
		cardDim.offsetTo((int)(x-offsetx),(int)(y-offsety));
	}
	
	public void unsetRotPoint() {
		double x1 = offsety*Math.tan(Math.toRadians(rotation));
		double y1 = offsety/Math.cos(Math.toRadians(rotation));
		double x2 = offsetx - x1;
		double x = x2*Math.cos(Math.toRadians(rotation));
		double y2 = x2*Math.sin(Math.toRadians(rotation));
		double y = y1+y2;
		cardDim.offsetTo((int)(cardDim.left+offsetx-x), (int)(cardDim.top+offsety-y));
		cardDim.right = cardDim.left + (int) (oCardDim.width()*scalefactor);
		cardDim.bottom = cardDim.top + (int) (oCardDim.height()*scalefactor);
		offsetx = 0;
		offsety = 0;
		scalefactor = 1;
	}
	
	public Point cs2as (Point cs) {
		float sangle = getangle(offsetx, offsety, cs.x, cs.y) + rotation;
		float sdist = getdist(offsetx, offsety, cs.x, cs.y);
		Point as = new Point(cardDim.left + (int) offsetx, cardDim.top + (int) offsety);
		as.offset((int) (sdist*Math.cos(Math.toRadians(sangle))), (int) (sdist*Math.sin(Math.toRadians(sangle))));
		return as;
	}
	
	private double applyFriction(double vel, float coefficient) {
		double fr = -coefficient * Math.atan(vel/(coefficient*3));
		if (Math.abs(fr) > Math.abs(vel))
			return -vel;
		else
			return fr;
	}
	
	private double applyFriction(double vel) {
		return applyFriction(vel, 0.5f);
	}
	
	public Point getCenter() {
		return cs2as(new Point(cardDim.width()/2, cardDim.height()/2));
	}
	
	private boolean isOffscreen(Point check) {
		return check.x < 0 || check.x > parent.getWidth() || check.y < 0 || check.y > parent.getHeight();
	}
	
	private boolean isOffscreen() {
		return isOffscreen(getCenter());
	}
	
	private void processInertia() {
		Point ccenter = getCenter();
		//String[] dk = {((Integer) ccenter.x).toString()};
		//sides.get(0).lines = dk;
		if (touches[0] == -1) {
			if ((velx != 0 && vely != 0) || isOffscreen(ccenter)) { 
				cardDim.offset((int) velx, (int) vely);
				parent.invalidate();
				
				if (ccenter.x < 0) {
					double px = -((float)ccenter.x)/5f;
					velx = Math.min(px + velx, (-(float)(ccenter.x))/10f + 1);
				} else if (ccenter.x > parent.getWidth()) {
					double px = ((float)(parent.getWidth() - ccenter.x))/5f;
					velx = Math.max(px + velx, ((float)(parent.getWidth() - ccenter.x))/10f - 1);
				} else {
					velx += applyFriction(velx);
				}
				
				if (ccenter.y < 0) {
					double py = -((float)ccenter.y)/5f;
					vely = Math.min(py + vely, (-(float)(ccenter.y))/10f + 1);
				} else if (ccenter.y > parent.getHeight()) {
					double py = ((float)(parent.getHeight() - ccenter.y))/5f;
					vely = Math.max(py + vely, ((float)(parent.getHeight() - ccenter.y))/10f - 1);
				} else {
					vely += applyFriction(vely);
				}
			}
		} else {
			velx = cardDim.left - lastx;
			vely = cardDim.top - lasty;
			lastx = cardDim.left;
			lasty = cardDim.top;
		}
		if (touches[1] == -1) {
			if (velt != 0) {
				rotation += velt;
				parent.invalidate();
				if (velt > 0.3)
					velt -= 0.3;
				else if (velt < -0.3)
					velt += 0.3;
				else
					velt = 0;
			}
		} else {
			velt = rotation - lastt;
			lastt = rotation;
		}
	}
	
	public void setRotOffset(float x, float y) {
		unsetRotPoint();
		cardDim.offset((int) ((x * Math.cos(Math.toRadians(rotation))) - (y * Math.sin(Math.toRadians(rotation))) - x), (int) ((x * Math.sin(Math.toRadians(rotation))) + (y * Math.cos(Math.toRadians(rotation))) - y));
		offsetx = x;
		offsety = y;
	}
	
	public void cancelTouches() {
		if (touches[1] != -1)
			unsetRotPoint();
		touches[0] = -1;
		touches[1] = -1;
		touches[2] = -1;
	}
	
	public void singletap() {
		if (parent.state == 0)
			edit();
		else if (parent.state == 3) {
			multiToggle();
		}
	}
	
	public void doubletap() {
		flip();
	}

	public void longtap() {
		multiSelect();
	}
	
	public void edit() {
		setRotOffset(cardDim.width()/2, cardDim.height()/2);
		saveSpot();
		animdata = new AnimatedNums(savedSpot, parent.editspace, 500);
		animating = true;
		animpurpose = 1;
		parent.state = 1;
		parent.currentCard = this;
		parent.invalidate();
	}
	
	public void flip() {
		saveSpot();
		double temp[] = {	savedSpot[0] - (cardDim.height() * Math.sin(Math.toRadians(rotation)) / 2),
							savedSpot[1] + (cardDim.height() * Math.cos(Math.toRadians(rotation)) / 2),
							cardDim.width(), 0, offsetx, offsety, rotation};
		animdata = new AnimatedNums(savedSpot, temp, 200, AnimatedNums.getArrayOfEases(Easing.EASEIN, 7));
		animating = true;
		animpurpose = 3;
		parent.invalidate();
	}
	
	public void multiSelect() {
		if (multiSelected)
			return;
		multiSelected = true;
		parent.selectedCards.add(this);
		if (parent.state == 0) {
			if (parent.mActionMode == null)
				parent.mActionMode = parent.startActionMode(parent.multiSelectedAction);
		}
		parent.invalidate();
	}
	
	public void multiDeselect() {
		if (!multiSelected)
			return;
		multiSelected = false;
		parent.selectedCards.remove(this);
		if (parent.state == 3 && parent.selectedCards.size() == 0) {
			parent.mActionMode.finish();
		}
		parent.invalidate();
	}
	
	public void multiToggle() {
		if (multiSelected)
			multiDeselect();
		else
			multiSelect();
	}
	
	public void saveSpot() {
		double[] temp = {(double) cardDim.left, (double) cardDim.top, (double) cardDim.width(), (double) cardDim.height(), (double) offsetx, (double) offsety, (double) rotation};
		savedSpot = temp;
	}
	
	public void delete() {
		animating = true;
		animpurpose = 2;
		setRotOffset(cardDim.width()/2, cardDim.height()/2);
		saveSpot();
		double[] dest = {cardDim.left + offsetx, cardDim.top + offsety, 0, 0, 0, 0, 360};
		animdata = new AnimatedNums(savedSpot, dest, 500);
	}
	
	public boolean doesPointTouch(int x, int y) {
		int cx = cardDim.left;
		int cy = cardDim.top;
		if (offsetx != 0 || offsety != 0)
		{
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
