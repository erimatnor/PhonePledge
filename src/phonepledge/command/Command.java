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

public class Command implements Serializable {
	private static final long serialVersionUID = 1L;
	private int type;
	private String description = null;
	private Object data = null;
	
	public Command(int type) {
		this.type = type;
	}
	
	public Command(int type, Serializable data) {
		this(type);
		this.data = data;
	}
	
	public Command(int type, Serializable data, String desc) {
		this(type, data);
		this.description = desc;
	}
	
	public int getType() {
		return type;
	}
	
	public Object getData() {
		return data;
	}
	
	@Override
	public String toString() {
		if (description == null)
			return "Command Type " + type;
		return description;
	}
}
