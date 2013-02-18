package com.andrewdutcher.indexcards;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.view.MotionEvent;
import android.view.View;

public class CardDrawer extends View {
	
	public ArrayList<IndexCard> cards;
	public ArrayList<Integer> zorder;
	
	private NinePatchDrawable shadowimg;
	private NinePatchDrawable selshadowimg;
	
	public void addnew() {
		
		Rect r = new Rect();
		r.left = this.getWidth()/2-5;
		r.top = this.getHeight()/2-3;
		r.right = r.left+10;
		r.bottom = r.top+6;
		cards.add(new IndexCard(this, "Hello, world!", r, 0, shadowimg, selshadowimg));
		int index = cards.size()-1;
		zorder.add(index);
		IndexCard tc = cards.get(index);
		tc.oCardDim = new Rect(r);
		tc.rotx = this.getWidth()/2;
		tc.roty = this.getHeight()/2;
		tc.touchxref = (float) tc.rotx;
		tc.touchyref = (float) tc.roty;
		tc.setRotPoint();
		tc.rotation = 180;
		double[] start = {(double)r.left, (double)r.top, 10, 6, 180};
		double[] end = {(double) this.getWidth()/2-250, (double) this.getHeight()/2-150, 500, 300, 0};
		Easing[] ease = AnimatedNums.getArrayOfEases(Easing.EASEOUT, 5);
		tc.animdata = new AnimatedNums(start,end,700,ease);
		tc.animating = true;
		invalidate();
	}
	
	public CardDrawer(Context context) {
		super(context);
		shadowimg = (NinePatchDrawable) context.getResources().getDrawable(R.drawable.shadow);
		shadowimg.setDither(true);
		selshadowimg = (NinePatchDrawable) context.getResources().getDrawable(R.drawable.selshadow);
		selshadowimg.setDither(true);
		cards = new ArrayList<IndexCard>();
		zorder = new ArrayList<Integer>();
	}
	protected void onDraw(Canvas c) {
		for (int i = 0; i < zorder.size(); i++) {
			c.save();
			cards.get(zorder.get(i)).draw(c);
			c.restore();
		}
	}
	public boolean onTouchEvent(MotionEvent e) {
		Integer action = e.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
		{
			int index = 0;
			if (action == MotionEvent.ACTION_POINTER_DOWN)
			{
				index = (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			}
			float tx = e.getX(index);
			float ty = e.getY(index);
			for (int i = zorder.size() - 1; i >= 0; i--) {	//TODO: adapt to z-indexing proper
				IndexCard tc = cards.get(zorder.get(i));
				if (tc.animating)
					continue;
				/*Vector am = new Vector(false, tx - tc.cardDim.left, ty - tc.cardDim.top, 0);
				Vector ab = new Vector(false, tc.cardDim.width()*Math.cos(Math.toRadians(tc.rotation)), tc.cardDim.width()*Math.sin(Math.toRadians(tc.rotation)), 0);
				Vector ad = new Vector(false, tc.cardDim.height()*Math.cos(Math.toRadians(tc.rotation-90)), tc.cardDim.height()*Math.sin(Math.toRadians(tc.rotation-90)), 0);
				Double amb = am.dot(ab);
				Double amd = am.dot(ad);
				Double abb = ab.dot(ab);
				Double add = ad.dot(ad);
				if ((amb > 0 && abb > amb) != (amd > 0 && add > amd)) {		//lots of math to check if point is within transformed rectangle
					return tc.addPoint(e, index);
				}*/
				double w1 = tc.cardDim.width()*Math.cos(Math.toRadians(tc.rotation));
				double h1 = tc.cardDim.width()*Math.sin(Math.toRadians(tc.rotation));		//some of these will be negative
				double w2 = -tc.cardDim.height()*Math.sin(Math.toRadians(tc.rotation));
				double h2 = tc.cardDim.height()*Math.cos(Math.toRadians(tc.rotation));
				Vector v1 = new Vector(tx - tc.cardDim.left, ty - tc.cardDim.top, 0);
				Vector v2 = new Vector(tx - (tc.cardDim.left + w1), ty - (tc.cardDim.top + h1), 0);
				Vector v3 = new Vector(tx - (tc.cardDim.left + w1 + w2), ty - (tc.cardDim.top + h1 + h2), 0);
				Vector v4 = new Vector(tx - (tc.cardDim.left + w2), ty - (tc.cardDim.top + h2), 0);
				double[] vectorsums = {v1.add(v2).get2DAngle(),v2.add(v3).get2DAngle(),v3.add(v4).get2DAngle(),v4.add(v1).get2DAngle()};
				for (int j = 0; j < 4; j++)
				{
					vectorsums[j] = (vectorsums[j] - tc.rotation) % 360;
					if (vectorsums[j] < 0)
						vectorsums[j] += 360; //angles range from -90 to 270 for some odd reason -- let's fix that
				}
				if (vectorsums[0] < 180 &&
					vectorsums[1] < 270 && vectorsums[1] > 90 &&
					vectorsums[2] > 180 &&	
					(vectorsums[3] > 270 || vectorsums[3] < 90))
				{
					if (tc.addPoint(e, index))
					{
						invalidate();
						return true;
					}
					else
						return false;
					
				}
			}
			return false;
		}
		else if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL)
		{
			int size = cards.size();
			for (int i = 0; i < size; i++)
			{
				cards.get(i).processTouches(e);
				int deltaz = cards.get(i).deltaz;
				if (deltaz != 0)
				{
					cards.get(i).deltaz = 0;
					int currentz = zorder.indexOf(i);
					int newz = currentz + deltaz;
					if (newz >= zorder.size())
						newz = zorder.size()-1;
					else if (newz < 0)
						newz = 0;
					int j = zorder.get(newz);
					zorder.set(newz, i);
					zorder.set(currentz, j);
				}
			}
			invalidate();
			return true;
		}
		else
		{
			//return true;
		}
		return false;
	}
}
