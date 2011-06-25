/*
 *  This file is part of PhonePledge.
 *
 *  PhonePledge is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  PhonePledge is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PhonePledge.  If not, see <http://www.gnu.org/licenses/>.
 */
package phonepledge;

import java.awt.Point;
import java.io.Serializable;

public class Pledge implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int STATE_UNDEFINED = 0;
	public static final int STATE_ACCEPTED = 1;
	public static final int STATE_REJECTED = 2;
	private int state = STATE_UNDEFINED;
	private boolean isCanned = false;
	private String text;
	private String from;
	private Point position;
	private long creationTime;
	
	public Pledge(String text, String from, Point p) {
		this.text = text;
		this.from = from;
		this.position = p;
		this.creationTime = java.lang.System.currentTimeMillis();
	}
	public Pledge(String text, String from) {
		this(text, from, new Point(0,0));
	}
	public Pledge(String text) {
		this(text, "John Doe", new Point(0,0));
	}
	
	public String getText() {
		return text;
	}
	public Point getPosition() {
		return position;
	}
	public int getX() {
		return position.x;
	}
	public int getY() {
		return position.y;
	}
	public String getFrom() {
		return from;
	}
	public long getCreationTime() {
		return creationTime;
	}
	
	public void setState(int state) {
		if (state >= 0 && state <= STATE_REJECTED)
			this.state = state;
	}
	
	public int getState() {
		return state;
	}
	
	public void setCanned(boolean isCanned) {
		this.isCanned = isCanned;
	}
	
	public boolean isCanned() {
		return isCanned;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pledge) {
			Pledge p = (Pledge) obj;
			if (p.from.equals(this.from) && p.text.equals(this.text))
				return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return from.hashCode() ^ text.hashCode();
	}
	
	@Override
	public String toString() {
		return getFrom() + " : " + getText();
	}
}
