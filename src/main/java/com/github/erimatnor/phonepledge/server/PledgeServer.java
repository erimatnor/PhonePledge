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
import com.github.erimatnor.phonepledge.command.StatusCommand;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class PledgeServer implements Runnable {
	public static final int SERVER_PORT = 34500;
    private final DisplayCommandListener commandListener;
    private final List<PledgeReceiver> pledgeReceivers = new ArrayList<>();
    private final List<StatusSender> statusSenders = new ArrayList<>();
	private volatile boolean shouldExit = false;
	private volatile Thread thr;
	
	public PledgeServer(DisplayCommandListener commandListener) {
		this.commandListener = commandListener;
		this.commandListener.setServer(this);
	}
	
	public boolean start() {
		if (thr != null && thr.isAlive())
			return false;
		
		thr = new Thread(this);
        thr.setName("pledge-server");
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
	
	public static class StatusSender implements Runnable {
		private volatile boolean shouldExit = false;
        private volatile Thread thr;
		private final PledgeServer server;
		private final Socket sock;
		private final LinkedBlockingQueue<StatusCommand> sendQueue =
			new LinkedBlockingQueue<>();
		
		public StatusSender(Socket sock, PledgeServer server) {
			this.server = server;
			this.sock = sock;
		}

		public boolean start() {
			if (thr != null && thr.isAlive())
				return false;
			
			thr = new Thread(this);
            thr.setName("status-sender");
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

			try (ObjectOutputStream outStream = new ObjectOutputStream(sock.getOutputStream())) {

                server.commandListener.sendInitialStatus(this);

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
                    } catch (InterruptedException e) {
                        // Just to check if we should exit
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Client socket error: " + e);
            } finally {
                server.removeSender(this);
            }
		}
	}
	
	private static class PledgeReceiver implements Runnable {
		private final PledgeServer pledgeServer;
		private final Socket sock;
        private final DisplayCommandListener commandListener;
		private volatile boolean shouldExit = false;
		private volatile Thread thr;
		
		public PledgeReceiver(Socket sock, PledgeServer ps) {
			this.sock = sock;
			this.pledgeServer = ps;
            this.commandListener = pledgeServer.commandListener;
		}
		
		public boolean start() {
			if (thr != null && thr.isAlive())
				return false;
			
			thr = new Thread(this);
            thr.setName("pledge-receiver");
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

            try (ObjectInputStream inStream = new ObjectInputStream(sock.getInputStream())) {

                while (!shouldExit) {
                    DisplayCommand cmd;

                    try {
                        cmd = (DisplayCommand) inStream.readObject();
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
                            commandListener.onInsertPledge((Pledge) cmd.getData());
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
            } catch (IOException e) {
                System.err.println("Pledge receiver error: " + e);
            } finally {
                pledgeServer.removeReceiver(this);
            }
		}
	}
	
	private boolean removeSender(StatusSender s) {
        synchronized (statusSenders) {
            return statusSenders.remove(s);
        }
	}
	
	private boolean addSender(StatusSender s) {
        synchronized (statusSenders) {
            return statusSenders.add(s);
        }
	}
	
	private boolean removeReceiver(PledgeReceiver pr) {
        synchronized (pledgeReceivers) {
            return pledgeReceivers.remove(pr);
        }
	}
	
	private boolean addReceiver(PledgeReceiver pr) {
        synchronized (pledgeReceivers) {
            return pledgeReceivers.add(pr);
        }
	}
	
	public void sendStatusCommand(StatusCommand cmd) {
        synchronized (statusSenders) {
            for (StatusSender sender : statusSenders) {
                sender.send(cmd);
            }
        }
	}
	
	@Override
	public void run() {
		try (ServerSocket sock = new ServerSocket(SERVER_PORT)) {
			/* 
			 * sock.bind(InetSocketAddress.createUnresolved("127.0.0.1", 
				PLEDGE_SERVER_PORT)); 
				*/
            sock.setSoTimeout(2000);

            System.out.println("Pledge server running on TCP port " + SERVER_PORT);

            while (!shouldExit) {

                try {
                    final Socket clientSock = sock.accept();

                    try {
                        clientSock.setSoTimeout(1000);
                        System.out.println("client connected");

                        PledgeReceiver pr = new PledgeReceiver(clientSock, this);
                        addReceiver(pr);
                        pr.start();

                        StatusSender s = new StatusSender(clientSock, this);
                        addSender(s);
                        s.start();
                    } catch (SocketException e) {
                        clientSock.close();
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout on accept. Just for checking whether to quit.
                } catch (IOException e) {
                    System.err.println("Client socket error: " + e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ServerSocket: " + e.getMessage());
        }
	}
}