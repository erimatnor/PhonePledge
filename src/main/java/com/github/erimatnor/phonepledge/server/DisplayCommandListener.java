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
package com.github.erimatnor.phonepledge.server;


import com.github.erimatnor.phonepledge.Pledge;
import com.github.erimatnor.phonepledge.command.DisplayCommand;

public interface DisplayCommandListener {
	void sendInitialStatus(PledgeServer.StatusSender s);
	void setServer(PledgeServer p);
	void onInsertPledge(Pledge p);
	void onSetSlideMode(DisplayCommand cmd);
	void onSetPledgeMode(DisplayCommand cmd);
	void onNextPledge(DisplayCommand cmd);
	void onRevokePledge(DisplayCommand cmd);
	void onSetTransitionTime(DisplayCommand cmd);
	void onSetTickerSpeed(DisplayCommand cmd);
	void onSetPhoneNumber(DisplayCommand cmd);
}
