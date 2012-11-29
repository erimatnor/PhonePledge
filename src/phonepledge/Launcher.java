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

import java.io.IOException;
import java.net.URISyntaxException;

import phonepledge.client.gui.PledgeControlPanel;
import phonepledge.server.gui.PledgeDisplayFrame;

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
	
	private static void runThreadedApp(final String[] args) {
		Thread server = new Thread(new Runnable() {
			@Override
			public void run() {
				PledgeDisplayFrame.main(args);
			}
		});

		server.start();
		PledgeControlPanel.main(args);
	}
	
	/** 
	 * @param args
	 */
	public static void main(final String[] args) {
		String[] newArgs = args;
		
		if (args.length > 0) {
			boolean runClient = false, spawnProcess = true;
			
			if (args[0].equals("-s")) {
				runClient = false;
				newArgs = copyArgs(args,1, args.length-1);
			} else if (args[0].equals("-c")) {
				runClient = true;
				newArgs = copyArgs(args,1, args.length-1);
			} else if (args[0].equals("-x")) {
				newArgs = copyArgs(args,1, args.length-1);
				spawnProcess = false;
			}

			if (!spawnProcess)
				runThreadedApp(newArgs);
			else if (runClient)
				PledgeControlPanel.main(newArgs);
			else
				PledgeDisplayFrame.main(newArgs);
		} else {
			try {
				String pathToJar = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

				ProcessBuilder pb = new ProcessBuilder("java",
						"-Xmx512m", "-classpath", 
						pathToJar, "phonepledge.Launcher", "-x");
				try {
					pb.start();
				} catch (IOException e) {
					System.err.println("Could not spawn new PhonePledge process");
				}
			} catch (URISyntaxException e) {
				System.err.println("Could not find Launcher jar path");
			}
		}
	}
}
