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
package text2pledge.client;

import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import text2pledge.Pledge;
import text2pledge.command.DisplayCommand;
import text2pledge.command.InsertPledgeCommand;
import text2pledge.command.StatusCommand;
import text2pledge.server.PledgeServer;

public class PledgeClient {
	Socket sock;
	boolean isConnected = false;
	boolean shouldExit = false;
	InetAddress serverAddress;
	ClientReceiver receiver = new ClientReceiver();
	ClientSender sender = new ClientSender();
	StatusCommandListener statusListener;
	
	public PledgeClient(StatusCommandListener statusListener) {
		this.statusListener = statusListener;
		
		try {
			serverAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.err.println("Client: " + e.getMessage());
		}
	}
	
	class ClientReceiver implements Runnable {
		ObjectInputStream inStream;
		boolean shouldExit = false;
		Thread thr;
		
		public boolean start() {
			if (thr != null && thr.isAlive())
				return false;
			
			thr = new Thread(this);
			thr.start();
			return true;
		}
		
		public void stop() {
			if (thr == null)
				return;
			
			shouldExit = true;
			thr.interrupt();
			try {
				thr.join(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		private void cleanup() {
			try {
				inStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			shouldExit = false;
			
			System.out.println("Client receiver running...");
			
			try {
				inStream = new ObjectInputStream(sock.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			while (!shouldExit) {
				Object obj = null;
				try {
					obj = inStream.readObject();
				} catch (EOFException e) {
					
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} catch (ClassNotFoundException e) {
					System.err.println("Class not found: " + e.getMessage());
					e.printStackTrace();
					continue;
				}
				
				if (obj instanceof StatusCommand) {
					statusListener.onStatusCommand((StatusCommand)obj);
				}	
			}
			
			cleanup();
		}
	}
	
	class ClientSender implements Runnable {
		ObjectOutputStream outStream;
		boolean shouldExit = false;
		Thread thr;
		
		LinkedBlockingQueue<DisplayCommand> commandQueue = 
			new LinkedBlockingQueue<DisplayCommand>();
		
		public boolean start() {
			if (thr != null && thr.isAlive())
				return false;
			
			thr = new Thread(this);
			thr.start();
			return true;
		}
		
		public void stop() {
			if (thr == null)
				return;
			
			shouldExit = true;
			thr.interrupt();
			try {
				thr.join(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void cleanup() {
			try {
				outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			shouldExit = false;
			
			System.out.println("Client sender running...");
			
			try {
				outStream = new ObjectOutputStream(sock.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			while (!shouldExit) {
				DisplayCommand cmd = null;
				
				try { 
					cmd = commandQueue.poll(2, TimeUnit.SECONDS);
					
					if (cmd == null)
						continue;
					
					outStream.writeObject(cmd);
				} catch (InvalidClassException e) {
					System.err.println("Bad class: " + e.getMessage());
				} catch (NotSerializableException e) {
					System.err.println("Not serializable: " + e.getMessage());
				} catch (EOFException e) {
					// Server closed connection
					shouldExit = true;
					break;
				} catch (IOException e) {
					shouldExit = true;
					e.printStackTrace();
					break;
				} catch (InterruptedException e) {
					e.printStackTrace();
					// Check if we should exit
				}
			}
			cleanup();
		}
	}
	
	public boolean start() {
		if (!isConnected) {
			if (!connect())
				return false;
		}
		
		if (!sender.start())
			return false;
		
		if (!receiver.start())
			return false;
		
		return true;
	}
	
	public void stop() {
		sender.stop();
		receiver.stop();
		disconnect();
	}
	
	public void disconnect() {
		if (!isConnected)
			return;
		
		stop();
		
		try {
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		isConnected = false;
	}
	
	private boolean connect() {
		
		try {
			sock = new Socket(serverAddress, PledgeServer.SERVER_PORT);
			isConnected = true;
		} catch (ConnectException e) {
			System.err.printf("Could not connect to server at %s:%d. Is it running?\n",
						serverAddress, PledgeServer.SERVER_PORT);
		} catch (SocketTimeoutException e) {
			System.err.println("Timeout connecting to Visualizer: " + e.getMessage());
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isConnected;
	}
	
	/**
	 * Send a pledge to the Visualizer.
	 * 
	 * @param p
	 * @return
	 */
	public boolean sendPledge(Pledge p) {
		return sender.commandQueue.add(new InsertPledgeCommand(p));
	}
	
	public boolean sendCommand(DisplayCommand cmd) {
		return sender.commandQueue.add(cmd);
	}
}