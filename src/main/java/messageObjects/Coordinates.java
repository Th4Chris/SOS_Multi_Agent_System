package messageObjects;

import java.io.Serializable;

public class Coordinates implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3932601623142821953L;
	private int x;
	private int y;
	
	public Coordinates(int x, int y) {
		this.setX(x);
		this.setY(y);
	}
	
	public double getDistance(Coordinates c) {		
		return Math.sqrt(Math.pow(Math.abs(x - c.getX()), 2) + Math.pow(Math.abs(y - c.getY()), 2));
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
}
