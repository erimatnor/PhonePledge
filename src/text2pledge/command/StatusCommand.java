/*
 *  This file is part of Text2Pledge.
 *
 *  Text2Pledge is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Text2Pledge is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Text2Pledge.  If not, see <http://www.gnu.org/licenses/>.
 */
package text2pledge.command;

import java.io.Serializable;

public class StatusCommand extends Command {
	private static final long serialVersionUID = 1L;
	public static final int CMD_DISPLAY_MODE_PLEDGE = 1;
	public static final int CMD_DISPLAY_MODE_SLIDE = 2;
	public static final int CMD_PLEDGE_LIST = 3;
	public static final int CMD_TICKER_SPEED = 4;
	public static final int CMD_TRANSITION_TIME = 5;

	public StatusCommand(int type) {
		super(type);
	}
	
	public StatusCommand(int type, Serializable data) {
		super(type, data);
	}
	
	public StatusCommand(int type, Serializable data, String desc) {
		super(type, data, desc);
	}
}
