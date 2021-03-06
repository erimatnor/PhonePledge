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
package com.github.erimatnor.phonepledge.command;

import com.github.erimatnor.phonepledge.Pledge;

public class InsertPledgeCommand extends DisplayCommand {
	private static final long serialVersionUID = 1L;

	public InsertPledgeCommand(Pledge p) {
		super(DisplayCommand.CMD_INSERT_PLEDGE, p, "Insert Pledge");
	}
	
	public Pledge getPledge() {
		return (Pledge)getData();
	}
}
