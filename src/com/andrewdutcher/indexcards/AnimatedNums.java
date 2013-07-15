package com.andrewdutcher.indexcards;

public class AnimatedNums {
	
	public int runtime;
	public long starttime;
	
	public double[] startvalues;
	public double[] endvalues;
	
	public Easing[] easesettings;
	
	public AnimatedNums(double[] start, double[] end, int time,	Easing[] eases) {
		starttime = System.currentTimeMillis();
		runtime = time;
		startvalues = start;
		endvalues = end;
		easesettings = eases;
	}
	public AnimatedNums(double[] start, double[] end, int time) {
		this(start, end, time, AnimatedNums.getArrayOfEases(Easing.EASEINOUT, end.length));
	}
	
	public boolean isActive() {
		if (System.currentTimeMillis() > starttime+runtime)
			return false;
		return true;
	}
	
	public double[] getValues() {
		double t = (System.currentTimeMillis() - starttime)/((double)runtime);
		//Log.d("andrew",new Double(t).toString());
		double[] out = new double[startvalues.length];
		for (int i = 0; i < startvalues.length; i++)
		{
			double p;
			switch (easesettings[i]) {
			case LINEAR:
			default:
				p = t;
				break;
			case EASEIN:
				p = t*t;
				break;
			case EASEINOUT:
				p = -0.5*(Math.cos(Math.PI*t)-1);
				break;
			case EASEOUT:
				p = -t*(t-2);
				break;
			}
			out[i] = startvalues[i] + ((endvalues[i] - startvalues[i]) * p);
		}
		return out;
	}

	public static Easing[] getArrayOfEases(Easing ease, int length) {
		Easing[] eases = new Easing[length];
		for (int i = 0; i < eases.length; i++)
			eases[i] = ease;
		return eases;
	}
}
