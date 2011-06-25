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
package phonepledge.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import phonepledge.Pledge;
import phonepledge.command.DisplayCommand;
import phonepledge.command.StatusCommand;


public class PledgeServer implements Runnable {
	public static final int SERVER_PORT = 6565;
	ServerSocket sock;
	ObjectOutputStream outStream;
	boolean shouldExit = false;
	Thread thr;
	DisplayCommandListener commandListener;
	ArrayList<PledgeReceiver> pledgeReceivers = new ArrayList<PledgeReceiver>();
	ArrayList<StatusSender> statusSenders = new ArrayList<StatusSender>();
	
	public PledgeServer(DisplayCommandListener commandListener) {
		this.commandListener = commandListener;
		this.commandListener.setServer(this);
	}
	
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
	
	public class StatusSender implements Runnable {
		boolean shouldExit = false;
		PledgeServer server;
		Socket sock;
		Thread thr;
		LinkedBlockingQueue<StatusCommand> sendQueue = 
			new LinkedBlockingQueue<StatusCommand>();
		
		StatusSender(Socket sock, PledgeServer server) {
			this.server = server;
			this.sock = sock;
		}
		
		private void cleanup() {
			server.removeSender(this);
			
			try {
				outStream.close();
				sock.close();
			} catch (SocketException e) {
				System.err.println("close: " + e.getMessage());
			} catch (IOException e) {
			}
		}
		
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
		
		public boolean send(StatusCommand cmd) {
			return sendQueue.offer(cmd);
		}
		
		@Override
		public void run() {
			try {
				outStream = new ObjectOutputStream(sock.getOutputStream());
			} catch (IOException e) {
				System.err.print("ObjectOutputStream: " + e.getMessage());
				cleanup();
				return;
			}
			
			commandListener.sendInitialStatus(this);
			
			while (!shouldExit) {
				StatusCommand cmd;
				try {
					cmd = sendQueue.poll(2, TimeUnit.SECONDS);
					if (cmd == null)
						continue;
					outStream.writeObject(cmd);
				} catch (SocketException e) {
					shouldExit = true;
					break;
				}catch (InterruptedException e) {
					// Just to check if we should exit
					continue;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			cleanup();
		}
	}
	
	class PledgeReceiver implements Runnable {
		PledgeServer pledgeServer;
		Socket sock;
		boolean shouldExit = false;
		ObjectInputStream inStream;
		Thread thr;
		
		public PledgeReceiver(Socket sock, PledgeServer ps) {
			this.sock = sock;
			this.pledgeServer = ps;
		}
		private void cleanup() {
			pledgeServer.removeReceiver(this);
			
			try {
				sock.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			
			try {
				inStream = new ObjectInputStream(sock.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
				cleanup();
				return;
			}
			
			while (!shouldExit) {
				DisplayCommand cmd;
				
				try {
					cmd = (DisplayCommand)inStream.readObject();
				} catch (SocketTimeoutException e) {
					// SO_TIMEOUT, check whether to exit
					continue;
				} catch (EOFException e) {
					System.out.println("Client disconnected");
					shouldExit = true;
					continue;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					e.printStackTrace();
					shouldExit = true;
					continue;
				}
				
				//System.out.println("Command: " + cmd);
				
				switch (cmd.getType()) {
				case DisplayCommand.CMD_INSERT_PLEDGE:
					commandListener.onInsertPledge((Pledge)cmd.getData());
					break;
				case DisplayCommand.CMD_NEXT_PLEDGE:
					commandListener.onNextPledge(cmd);
					break;
				case DisplayCommand.CMD_REVOKE_PLEDGE:
					commandListener.onRevokePledge(cmd);
					break;
				case DisplayCommand.CMD_SET_SLIDE_MODE:
					commandListener.onSetSlideMode(cmd);
					break;
				case DisplayCommand.CMD_SET_PLEDGE_MODE:
					commandListener.onSetPledgeMode(cmd);
					break;
				case DisplayCommand.CMD_SET_TRANSITION_TIME:
					commandListener.onSetTransitionTime(cmd);
					break;
				case DisplayCommand.CMD_SET_TICKER_SPEED:
					commandListener.onSetTickerSpeed(cmd);
					break;
				case DisplayCommand.CMD_SET_PHONE_NUMBER:
					commandListener.onSetPhoneNumber(cmd);
					break;
				}
			}

			cleanup();
		}
	}
	
	public synchronized boolean removeSender(StatusSender s) {
		return statusSenders.remove(s);
	}
	
	public synchronized boolean addSender(StatusSender s) {
		return statusSenders.add(s);
	}
	
	public synchronized boolean removeReceiver(PledgeReceiver pr) {
		return pledgeReceivers.remove(pr);
	}
	
	public synchronized boolean addReceiver(PledgeReceiver pr) {
		return pledgeReceivers.add(pr);
	}
	
	public synchronized void sendStatusCommand(StatusCommand cmd) {
		Iterator<StatusSender> it = statusSenders.iterator();

		while (it.hasNext()) {
			StatusSender s = it.next();
			s.send(cmd);
		}
	}
	
	@Override
	public void run() {
		try {
			sock = new ServerSocket(SERVER_PORT);
			/* 
			 * sock.bind(InetSocketAddress.createUnresolved("127.0.0.1", 
				PLEDGE_SERVER_PORT)); 
				*/
			sock.setSoTimeout(2000);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ServerSocket: " + e.getMessage());
			return;
		}
	
		while (!shouldExit) {
			Socket clientSock = null;
			
			try {
				clientSock = sock.accept();
				clientSock.setSoTimeout(1000);
				System.out.println("client connected");
			} catch (SocketTimeoutException e) {
				// SO_TIMEOUT, check if we should exit
				continue;
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("accept: " + e.getMessage());
				continue;
			}
			
			PledgeReceiver pr = new PledgeReceiver(clientSock, this);
			addReceiver(pr);
			pr.start();
			
			StatusSender s = new StatusSender(clientSock, this);
			addSender(s);
			s.start();
		}
	}
}