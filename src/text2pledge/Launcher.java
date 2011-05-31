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
package text2pledge;

import text2pledge.client.gui.PledgeControlPanel;
import text2pledge.server.gui.PledgeDisplayFrame;

public class Launcher {
	
	private static String[] copyArgs(String[] arr, int from, int to) {
		if (from >= arr.length)
			return new String[0];

		String[] copy = new String[(to - from) + 1];
		int i = 0;
		
		while (from <= to) {
			copy[i++] = arr[from++];
		}
		return copy;
	}
	
	/** 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean runClient = false;
		String[] newArgs = args;
		
		if (args.length > 0) {
			if (args[0].equals("-s")) {
				runClient = false;
				newArgs = copyArgs(args,1, args.length-1);
			} else if (args[0].equals("-c")) {
				runClient = true;
				newArgs = copyArgs(args,1, args.length-1);
			}
		}
		
		if (runClient)
			PledgeControlPanel.main(newArgs);
		else
			PledgeDisplayFrame.main(newArgs);
	}
}
