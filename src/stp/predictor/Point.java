package stp.predictor;

import java.text.DecimalFormat;

/**
 * Define the class for a point in x-y coordinates
 * 
 * @author chensu
 * 
 */
public class Point {
	public double x;
	public double y;

	private static DecimalFormat db_df = new java.text.DecimalFormat("0.00");

	public Point() {
	}

	public Point(double px, double py) {
		x = px;
		y = py;
	}

	public Point(Point p) {
		x = p.x;
		y = p.y;
	}

	public double distance(Point p2) {
		return Math.hypot(p2.x - x, p2.y - y);
	}
	
	public double distance(double x1,double y1){
		return distance(new Point(x1,y1));
	}

	public String toString() {
		return "(" + String.format("%1$-6s", db_df.format(x)) + ", "
				+ String.format("%1$-6s", db_df.format(y)) + ")";
	}
}
