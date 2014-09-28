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
package com.github.erimatnor.phonepledge.googlevoice;

import com.github.erimatnor.phonepledge.Pledge;
import com.github.erimatnor.phonepledge.client.gui.PledgeControlPanel;
import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

public class GoogleVoiceConnector implements Runnable {
    private static final int DEFAULT_REFRESH_TIME_MILLIS = 20000;
	private String user, pass;
    private volatile Voice voice = null;
	private volatile boolean shouldExit = false;
	private volatile boolean shouldSendReply = false;
    private HashSet<Pledge> pledgeSet = new HashSet<Pledge>();
	private PledgeControlPanel pledgeControl;
	private String replyMessage = null;
	private long refreshCounter = 0;
	
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
	
	public void connect(String user, String pass) throws IOException {
        voice = new Voice(user, pass, "PhonePledge", false);
		pledgeControl.setPhoneNumber(voice.getPhoneNumber());
		this.user = user;
		this.pass = pass;
        System.out.println("Connected to account " + getUserName());
	}
	
	public boolean refreshSMS(final Voice voice) throws IOException {

        System.out.println("Refreshing sms");

        final Collection<SMSThread> smsThreads = voice.getSMSThreads();

		if (smsThreads == null || smsThreads.isEmpty()) {
            System.out.println("No SMSes in thread");
            return false;
        }

        System.out.println("Got " + smsThreads.size() + " SMSes");

        // Convert to array so we can access SMS in reverse, i.e., oldest first
		final SMSThread[] smsThreadArr = smsThreads.toArray(new SMSThread[smsThreads.size()]);

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
				
				Pledge p = new Pledge(sms.getContent(), sms.getFrom().getName());
				
				if (!pledgeSet.contains(p)) {
					pledgeSet.add(p);
					pledgeControl.addPledge(p);
					
					if (shouldSendReply &&
						replyMessage != null && 
						refreshCounter != 0) {
						System.out.println("sending reply to " + sms.getFrom().getNumber());
						
                        voice.sendSMS(sms.getFrom().getNumber(),
                                      replyMessage);
                    }
				}
			}
		}
		refreshCounter++;
		
		return true;
	}

	@Override
	public void run() {

        while (!shouldExit) {

            try {
                // Check if we need to create a new Voice session to ensure
                // we have an up-to-date auth token.
                if (voice == null) {
                    connect(user, pass);
                }

                refreshSMS(voice);

                Thread.sleep(DEFAULT_REFRESH_TIME_MILLIS);
            } catch (InterruptedException e) {
                shouldExit = true;
            } catch (IOException e) {
                // Trigger a new login next run
                voice = null;
                System.err.println("SMS refresh failed: " + e);
            }
        }
	}
	public void signalExit() {
		shouldExit = true;	
	}
	
	public void start() {
        final Thread googleVoiceThread = new Thread(this);
        googleVoiceThread.setDaemon(true);
        googleVoiceThread.setName("google-voice");
		googleVoiceThread.start();
	}
}
