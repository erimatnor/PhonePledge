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
package text2pledge.server;

import text2pledge.Pledge;
import text2pledge.command.DisplayCommand;

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
}
