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
package phonepledge.command;

import java.io.Serializable;

public class DisplayCommand extends Command {
	private static final long serialVersionUID = 1L;
	public static final int CMD_INSERT_PLEDGE = 1;
	public static final int CMD_REVOKE_PLEDGE = 2;
	public static final int CMD_NEXT_PLEDGE = 3;
	public static final int CMD_SET_SLIDE_MODE = 4;
	public static final int CMD_SET_PLEDGE_MODE = 5;
	public static final int CMD_SET_TRANSITION_TIME = 6;
	public static final int CMD_SET_TICKER_SPEED = 7;
	public static final int CMD_SET_PHONE_NUMBER = 8;
	
	public DisplayCommand(int type) {
		super(type);
	}
	
	public DisplayCommand(int type, Serializable data) {
		super(type, data);
	}
	
	public DisplayCommand(int type, Serializable data, String desc) {
		super(type, data, desc);
	}
}
