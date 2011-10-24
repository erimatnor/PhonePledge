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
package phonepledge.server.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

import phonepledge.Pledge;
import phonepledge.command.DisplayCommand;
import phonepledge.command.StatusCommand;
import phonepledge.server.DisplayCommandListener;
import phonepledge.server.PledgeServer;


public class PledgeDisplayPanel extends JPanel 
	implements DisplayCommandListener, ComponentListener, 
		MouseListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	PledgeServer server = null;
	BufferedImage background = null;
	BufferedImage logo = null;
	TickerLabel tickerLabel;
	Dimension backgroundSize = null;
	String telephoneNrFontName = "sansserif";
	final SlidePane slidePane;
	final PledgePane pledgePane;
	Pledge welcomePledge = new Pledge("Welcome to PhonePledge!");
	boolean slideMode = false;
	Timer idleTimer;
	private String pledgeInstruction = "Text your pledge and message to";
	private String phoneNumber = "XXX-XXX-XXXX";
	// Hash containing all pledges received so far. Ensures no duplicates
	// are added to the pledge list
	private HashSet<Pledge> pledgeHash = new HashSet<Pledge>();
	// List containing all pledges received so far, retaining order
	private ArrayList<Pledge> pledgeList = new ArrayList<Pledge>();
	static Color tickerColor = new Color(0, 0, 80);
	static Color backgroundColor = Color.white;
	// Position and thickness of blue L
	int cornerX;
	int cornerY;
	int rectThickness = 40;
	int blueLWidth;
	int idleTimeout = 5000;
	RenderingHints renderHints;
	
	public PledgeDisplayPanel() {
		CodeSource codeSource = PledgeDisplayPanel.class.getProtectionDomain().getCodeSource();
		File jarFile = null, logoFile = null;
		
		try {
			jarFile = new File(codeSource.getLocation().toURI().getPath());
			logoFile = new File(jarFile.getParentFile().getAbsolutePath() + 
					"/logo.png");
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		
		try {
			if (logoFile != null && logoFile.exists())
				logo = ImageIO.read(logoFile);
			else 
				logo = ImageIO.read(getClass().getResource("pp-logo-large.png"));
		} catch (IOException e) {
			System.err.println("Could not load logo");
			e.printStackTrace();
		}
		
		tickerLabel = new TickerLabel();
		tickerLabel.setVisible(true);
		slidePane = new SlidePane();
		slidePane.setVisible(false);
		pledgePane = new PledgePane();
		readProperties();
		
		idleTimer = new Timer(idleTimeout, new IdleTimeout());
		idleTimer.start();
		setBackground(backgroundColor);
		setForeground(Color.black);
		setLayout(null);
		renderHints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		renderHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		addComponentListener(this);
		add(tickerLabel);
 		add(pledgePane);
		add(slidePane);
		addMouseListener(this);
	}
	
	private void readProperties() {
		Properties props = new Properties();
		
		try {
			props.load(new FileInputStream("display.properties"));
		} catch (FileNotFoundException e) {
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		String value;
		
		if ((value = (String)props.getProperty("NextSlideTimeout")) != null) {
			try {
				int val = Integer.parseInt(value);
				System.out.println("NextSlideTimeout=" + val);
				slidePane.setNextSlideTimeout(val * 1000);
			} catch (NumberFormatException e) {
				System.err.println("Bad NextSLideTimeout value " + value);
			}
		} 

		if ((value = (String)props.getProperty("TelephoneNumberFont")) != null) {
			telephoneNrFontName = value;
		} 
		
		if ((value = (String)props.getProperty("PledgeFont")) != null) {
			pledgePane.setFont(value);
		} 

		if ((value = (String)props.getProperty("PledgeFontSize")) != null) {
			try {
				int val = Integer.parseInt(value);
				pledgePane.setFontSize(val);
			} catch (NumberFormatException e) {
				System.err.println("Bad PledgeFontSize value " + value);
			}
		}
		
		if ((value = (String)props.getProperty("TickerFont")) != null) {
			tickerLabel.setFont(value);
		} 
		
		if ((value = (String)props.getProperty("TickerFontSize")) != null) {
			try {
				int val = Integer.parseInt(value);
				tickerLabel.setFontSize(val);
			} catch (NumberFormatException e) {
				System.err.println("Bad TickerFontSize value " + value);
			}
		}
		
		if ((value = (String)props.getProperty("NextPledgeTimeout")) != null) {
			try {
				int val = Integer.parseInt(value);
				System.out.println("NextPledgeTimeout=" + val);
				pledgePane.setNextPledgeTimeout(val * 1000);
			} catch (NumberFormatException e) {
				System.err.println("Bad NextPledgeTimeout value " + value);
			}
		}
		
		if ((value = (String)props.getProperty("TickerHz")) != null) {
			try {
				int val = Integer.parseInt(value);
				System.out.println("TickerHz=" + val);
				tickerLabel.setTickerHz(val);
			} catch (NumberFormatException e) {
				System.err.println("Bad TickerHz value " + value);
			}
		}
		if ((value = (String)props.getProperty("TicksPerSecond")) != null) {
			try {
				int val = Integer.parseInt(value);
				System.out.println("TicksPerSecond=" + val);
				tickerLabel.setTicksPerSecond(val);
			} catch (NumberFormatException e) {
				System.err.println("Bad TicksPerSecond value " + value);
			}
		}
		
		if ((value = (String)props.getProperty("PledgeInstruction")) != null) {
			System.out.println("PledgeInstruction=" + value);
			pledgeInstruction = value;
		}
		
		if ((value = (String)props.getProperty("PhoneNumber")) != null) {
			System.out.println("PhoneNumber=" + value);
			phoneNumber = value;
		}

		if ((value = (String)props.getProperty("TickerColor")) != null) {
			String rgbstr[] = value.split(",", 0);
			
			if (rgbstr.length > 2) {
				int rgb[] = new int[3];
		
				for (int i = 0; i < rgb.length; i++)
					rgb[i] = Integer.parseInt(rgbstr[i]);
				
				tickerColor = new Color(rgb[0], rgb[1], rgb[2]);
				System.out.println("TickerColor=" + value);
			}
		}
		if ((value = (String)props.getProperty("BackgroundColor")) != null) {
			String rgbstr[] = value.split(",", 0);
			
			if (rgbstr.length > 2) {
				int rgb[] = new int[3];
		
				for (int i = 0; i < rgb.length; i++)
					rgb[i] = Integer.parseInt(rgbstr[i]);
				
				backgroundColor = new Color(rgb[0], rgb[1], rgb[2]);
				System.out.println("BackgroundColor=" + value);
			}
		}
	}
	
	class IdleTimeout implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!pledgePane.isTextShowing())
				setSlideMode();
		}
	}
	
	@Override
	public void setServer(PledgeServer server) {
		this.server = server;
	}
	
	private boolean sendStatusCommand(StatusCommand cmd) {
		if (server == null)
			return false;
		
		server.sendStatusCommand(cmd);
		
		return true;
	}
	
	private String[] createTickerString() {
		if (pledgeList.size() == 0)
			return null;
		
		String[] texts = new String[pledgeList.size()];
		
		for (int i = 0; i < texts.length; i++) {
			texts[i] = pledgeList.get(i).getText();
		}
		
		return texts;
	}

	public void showWelcomePledge() {
		Pledge p = new Pledge("Welcome to PhonePledge!");
		p.setState(Pledge.STATE_ACCEPTED);
		addPledge(p);
	}
	
	private void updateBackground() {
		int width = getWidth();
		int height = getHeight();
		int x = 0, y = 0;
		
		background = new BufferedImage(width, 
				height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = (Graphics2D)background.getGraphics();

		g2.setRenderingHints(renderHints);
		
		//System.out.printf("width1=%d height1=%d\n", width, height);
		
		double xOffset = (double)width / 12;
		double yOffset = (double)height / 12;
		
		// Fill with white background
		g2.setColor(backgroundColor);
		g2.fillRect(0, 0, width, height);

		int logoWidth = (int)(width / 4);
		Image scaledLogo = logo.getScaledInstance(logoWidth, -1, Image.SCALE_SMOOTH);
		y = (int)(height - scaledLogo.getHeight(this) - yOffset);
		cornerX = (int)xOffset;
		
		// Draw logo 
		g2.drawImage(scaledLogo, cornerX, y, 
				scaledLogo.getWidth(this), scaledLogo.getHeight(this), this);

		// Draw phone number
		g2.setFont(new Font(telephoneNrFontName, Font.BOLD, width / 30));
		int phoneNumberWidth = g2.getFontMetrics().stringWidth(phoneNumber);
		int phoneNumberHeight = g2.getFontMetrics().getHeight();
		g2.setColor(tickerColor);
		
		// Move text down a bit
		y += 15;
		
		g2.drawString(phoneNumber, 
				width - (int)xOffset - phoneNumberWidth, 
				y + phoneNumberHeight - 10);

		// Draw phone number instruction
		g2.setFont(new Font(telephoneNrFontName, Font.BOLD, width / 70));
		int instructionWidth = g2.getFontMetrics().stringWidth(pledgeInstruction);
		//int instructionHeight = g2.getFontMetrics().getHeight();
		g2.setColor(Color.black);
		
		g2.drawString(pledgeInstruction, 
				width - (int)xOffset - (phoneNumberWidth / 2) - (instructionWidth / 2), 
				y - 10);
		
		double rectWidth = rectThickness;
		double rectHeight = height - (scaledLogo.getHeight(this) + yOffset*2.5);
		
		// Draw blue L
		g2.setColor(tickerColor);
		g2.fillRect(cornerX, (int)yOffset, 
				(int)rectWidth, (int)rectHeight);
		
		cornerY = (int)yOffset + (int)rectHeight;
		blueLWidth = (int)(width - xOffset*2);
		
		g2.fillRect((int)xOffset, cornerY - rectThickness, 
				blueLWidth, rectThickness);

	    // Calculate positions of our tickerLabel
		x = (int)xOffset;
		y = cornerY - rectThickness;
		
		tickerLabel.setLocation(x, y);
		tickerLabel.setSize(blueLWidth, rectThickness);
		
		//System.out.printf("Ticker x=%d y=%d width=%d height=%d\n",
			//	x, y, blueLWidth, rectThickness);
		
		// Calculate positions of other components
		int innerRectHeight = (int)(rectHeight - rectThickness);
		int innerRectWidth = (int)(blueLWidth - rectThickness);
		int innerRectStrutX = (innerRectWidth / 7);
		int innerRectStrutY = (innerRectHeight / 10);
		int dx = (int)(innerRectWidth - innerRectStrutX);
		int dy = (int)(innerRectHeight - innerRectStrutY); 
		
		pledgePane.setSize(dx, dy);
		
		// Use aspect ration of, e.g., 800x600 for images
		dx = (int)(dy + dy / 3); 
		
		slidePane.setSize(dx, dy);
		
		x = (int)(innerRectWidth / 2 - (pledgePane.getWidth() / 2) + xOffset + rectThickness);
		y = (int)(innerRectHeight / 2 - (pledgePane.getHeight() / 2) + yOffset);
		pledgePane.setLocation(x, y);

		x = (int)(innerRectWidth / 2 - (slidePane.getWidth() / 2) + xOffset + rectThickness);
		y = (int)(innerRectHeight / 2 - (slidePane.getHeight() / 2) + yOffset);
		slidePane.setLocation(x, y);
	}
	
	@Override
	public void setSize(Dimension d) {
		super.setSize(d);
		updateBackground();
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		updateBackground();
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHints(renderHints);
	    // Draw background
		super.paintComponent(g2);
		g2.drawImage(background, 0, 0, getWidth(), getHeight(), this);
	}
	
	private class PledgeInserter implements Runnable {
		private Pledge p;
		
		PledgeInserter(Pledge p) {
			this.p = p;
		}
		@Override
		public void run() {
			// Use an Undefined pledge as a marker to show next pledge
			if (p.getState() == Pledge.STATE_UNDEFINED) {
			} else if (p.getState() == Pledge.STATE_ACCEPTED) {
				setPledgeMode(false);

	    		if (welcomePledge != null && !p.equals(welcomePledge)) {
	        		revokePledge(welcomePledge);
	        		welcomePledge = null;
	        	}
				if (!pledgeHash.contains(p)) {
					pledgeList.add(p);
					pledgeHash.add(p);
					tickerLabel.setText(createTickerString());
				}
				pledgePane.addText(p.getText());
			}
		}
	}
	
	private void setPledgeMode(boolean repopulate) {
		if (!slideMode)
			return;
		slideMode = false;
		slidePane.stop();
		slidePane.setVisible(false);
		
		// If the pledgePane is empty, fill the queue again.
		if (repopulate && pledgePane.getQueueSize() == 0) {
			Iterator<Pledge> it = pledgeList.iterator();
			
			while (it.hasNext()) {
				pledgePane.addText(it.next().getText());
			}
		}
		pledgePane.setVisible(true);
		pledgePane.start();
		repaint();
		
		// Give feedback to the controller
		sendStatusCommand(new StatusCommand(StatusCommand.CMD_DISPLAY_MODE_PLEDGE));
	}
	
	private void setSlideMode() {
		if (slideMode)
			return;
		slideMode = true;
		pledgePane.stop();
		pledgePane.setVisible(false);
		slidePane.setVisible(true);
		slidePane.start();
		repaint();

		// Give feedback to the controller
		sendStatusCommand(new StatusCommand(StatusCommand.CMD_DISPLAY_MODE_SLIDE));
	}
	
	public void addPledge(Pledge p) {
		System.out.println("Adding pledge " + p.getFrom() + ": " + p.getText());
		javax.swing.SwingUtilities.invokeLater(new PledgeInserter(p));
	}
	
	public void revokePledge(final Pledge p) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pledgePane.removeText(p.getText());
			}
		});

		pledgeHash.remove(p);
		pledgeList.remove(p);
		tickerLabel.setText(createTickerString());
	}
	
	@Override
	public void onInsertPledge(Pledge p) {
		addPledge(p);
	}

	@Override
	public void onSetSlideMode(DisplayCommand cmd) {
		//System.out.println("Set slide mode");
		setSlideMode();
	}

	@Override
	public void onSetPledgeMode(DisplayCommand cmd) {
		//System.out.println("Set pledge mode");
		setPledgeMode(true);
	}

	@Override
	public void onNextPledge(DisplayCommand cmd) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (slideMode)
					slidePane.nextSlide();
				else
					pledgePane.nextText();
			}
		});
	}

	@Override
	public void onRevokePledge(DisplayCommand cmd) {
		Pledge p = (Pledge)cmd.getData();
		//System.out.println("Revoke pledge: " + p);
		revokePledge(p);
	}

	@Override
	public void onSetTransitionTime(DisplayCommand cmd) {
		final int value = ((Integer)cmd.getData()).intValue();
		//System.out.println("Set transition time to " + value);
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pledgePane.setNextPledgeTimeout(value * 1000);
				slidePane.setNextSlideTimeout(value * 1000);
			}
		});
	}
	
	@Override
	public void onSetTickerSpeed(DisplayCommand cmd) {
		final int value = ((Integer)cmd.getData()).intValue();
		//System.out.println("Set transition time to " + value);
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tickerLabel.setTicksPerSecond(value);
			}
		});
	}

	@Override
	public void onSetPhoneNumber(DisplayCommand cmd) {
		phoneNumber = (String)cmd.getData();
		System.out.println("Setting phone number to " + phoneNumber);
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateBackground();
			}
		});
	}
	
	@Override
	public void sendInitialStatus(PledgeServer.StatusSender s) {
		
		// Send pledge/slide mode:
		if (slideMode)
			s.send(new StatusCommand(StatusCommand.CMD_DISPLAY_MODE_SLIDE));
		else
			s.send(new StatusCommand(StatusCommand.CMD_DISPLAY_MODE_PLEDGE));
		
		// Send ticker speed
		s.send(new StatusCommand(StatusCommand.CMD_TICKER_SPEED, 
				new Integer(tickerLabel.getTicksPerSecond())));
		
		// Send transition time
		s.send(new StatusCommand(StatusCommand.CMD_TRANSITION_TIME, 
				new Integer(pledgePane.getNextPledgeTimeout() / 1000)));
		
		// Send accepted pledges
		Pledge[] list = pledgeList.toArray(new Pledge[pledgeList.size()]);
		
		// Do not send welcome pledge
		if (list.length > 0) {
			
			if (list[0].equals(welcomePledge))
				list[0] = null;
		
				StatusCommand cmd = new StatusCommand(StatusCommand.CMD_PLEDGE_LIST, list);
				s.send(cmd);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.getActiveWindow().requestFocus();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		//System.out.println("Panel: Mouse entered");
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		//System.out.println("Panel: Mouse exited");
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		//System.out.println("Panel: Mouse pressed");
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		//System.out.println("Panel: Mouse released");
		this.requestFocus();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		
	}

	@Override
	public void componentMoved(ComponentEvent e) {
	}

	@Override
	public void componentResized(ComponentEvent e) {
		//System.out.println("Component resized");
	    updateBackground();
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}
}
