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
		tc.setRotOffset(5,3);
		tc.rotation = 180;
		double[] start = {r.left, r.top, 10, 6, 5, 3, 180};
		double[] end = {this.getWidth()/2-250, this.getHeight()/2-150, 500, 300, 250, 150, 0};
		Easing[] ease = AnimatedNums.getArrayOfEases(Easing.EASEOUT, 7);
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
				if (tc.doesPointTouch((int) tx, (int) ty))
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
