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
package phonepledge.googlevoice;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import phonepledge.Pledge;
import phonepledge.client.gui.PledgeControlPanel;


import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;

public class GoogleVoiceConnector implements Runnable {
	String user, pass;
	Voice voice = null;
	boolean isConnected = false;
	boolean shouldExit = false;
	volatile boolean shouldSendReply = false;
	Thread googleVoiceThread = null;
	HashSet<Pledge> pledgeSet = new HashSet<Pledge>();
	PledgeControlPanel pledgeControl;
	String replyMessage = null;
	long refreshCounter = 0;
	
	public GoogleVoiceConnector(PledgeControlPanel ps) {
		pledgeControl = ps;
		pledgeControl.setGoogleVoiceConnector(this);
	}
	
	public String getUserName() {
		return user;
	}
	
	public void setShouldSendReply(boolean shouldSend) {
		shouldSendReply = shouldSend;
		System.out.println("Send SMS reply=" + shouldSend);
	}
	
	public void setReplyMessage(String msg) {
		replyMessage = msg;
	}
	
	public boolean connect(String user, String pass) {
		try {
			voice = new Voice(user, pass, "PhonePledge", false);
		} catch (IOException e) {
			System.err.println("Could not connect to google voice account '" 
					+ user +"':" + e.getMessage());
			return false;
		}
		isConnected = true;
		pledgeControl.setPhoneNumber(voice.getPhoneNumber());
		this.user = user;
		this.pass = pass;
		return true;
	}
	
	public boolean refreshSMS() {
		Collection<SMSThread> smsThreads;
		
		if (!isConnected) {
			System.err.println("Not connected to Google voice");
			return false;
		}
		try {
			smsThreads = voice.getSMSThreads();
		} catch (IOException e) {
			System.err.println("Could not get SMS");
			return false;
		}
		
		if (smsThreads == null)
			return false;
	
		// Convert to array so we can access SMS in reverse, i.e., oldest first
		SMSThread[] smsThreadArr = smsThreads.toArray(new SMSThread[smsThreads.size()]);

		//System.out.println("-----------------------");
		
		for (int i = smsThreadArr.length - 1; i >= 0; i--) {
			
			SMSThread smsThread = smsThreadArr[i];
			Collection<SMS> smsList = smsThread.getAllSMS();
			
			SMS[] smsArr = smsList.toArray(new SMS[smsList.size()]);
			
			for (int j = smsArr.length - 1; j >= 0; j--) {
				SMS sms = smsArr[j];
				
				// Ignore from "Me" messages
				if (sms.getFrom().getName().equals("Me"))
					continue;
				
				String content = sms.getContent();
				String from = sms.getFrom().getName();
				
				Pledge p = new Pledge(content, from);
				
				if (!pledgeSet.contains(p)) {
					pledgeSet.add(p);
					pledgeControl.addPledge(p);
					
					if (shouldSendReply &&
						replyMessage != null && 
						refreshCounter != 0) {
						System.out.println("sending reply to " + sms.getFrom().getNumber());
						
						try {
							voice.sendSMS(sms.getFrom().getNumber(), 
									replyMessage);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		refreshCounter++;
		
		return true;
	}

	@Override
	public void run() {
		if (!isConnected) {
			if (!connect(user, pass))
				return;
			System.out.println("Connected to account " + getUserName());
		}
		
		while (!shouldExit) {
			System.out.println("Refreshing sms");
			
			refreshSMS();
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				shouldExit = true;
			}
		}
	}
	public void signalExit() {
		shouldExit = true;	
	}
	
	public void start() {
		googleVoiceThread = new Thread(this);
		googleVoiceThread.start();
	}
}
