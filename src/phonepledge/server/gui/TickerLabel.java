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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.SimpleAttributeSet;


public class TickerLabel extends JComponent {
	private static final long serialVersionUID = 1L;
	private volatile int currOffset[] = { 0, 0 };
	SimpleAttributeSet center;
	int ticksPerSecond = 100;
	BufferedImage tickerLogo = null;
	Image tickerLogoScaled = null;
	ImageIcon icon;
	JPanel[] p = new JPanel[2];
	int pIndex = 0;
	int pIndexOld = 1;
	Image tickerImg = null;
	// Fraction of the screen width to move every step
	private TickerTimeout tickerTimeout;
	String fontName = "sansserif";
	int fontSize = 18;
	Font font = new Font(fontName, Font.PLAIN, fontSize);
	private String[] text = null;
	private String[] nextText = null;
	private Color tickerFontColor = Color.white;
	int textWidth;
	int textHeight;
	private int width = 0;
	volatile int tickerHz = 20;
	
	public TickerLabel(String[] newText) {
		CodeSource codeSource = PledgeDisplayPanel.class.getProtectionDomain().getCodeSource();
		File jarFile = null, logoFile = null;
		center = new SimpleAttributeSet();
		
		try {
			jarFile = new File(codeSource.getLocation().toURI().getPath());
			logoFile = new File(jarFile.getParentFile().getAbsolutePath() + 
					"/ticker.png");
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
		}
		
		try {
			if (logoFile != null && logoFile.exists())
				tickerLogo = ImageIO.read(logoFile);
			else
				tickerLogo = ImageIO.read(getClass().getResource("pp-logo-small.png"));
		} catch (IOException e) {
			System.err.println("Could not load logo image");
			e.printStackTrace();
		}
		nextText = newText;
		nextText();
		this.setBorder(null);
		tickerTimeout = new TickerTimeout();
		setOpaque(false);
		setFont(font);
		p[pIndex] = new JPanel();
		p[pIndexOld] = new JPanel();
		p[pIndex].setVisible(false);
		p[pIndexOld].setVisible(false);
		tickerLogoScaled = tickerLogo.getScaledInstance(-1, 40, Image.SCALE_SMOOTH);
		icon = new ImageIcon(tickerLogoScaled);
	}

	public TickerLabel() {
		this(null);
	}

	public void setFont(String name) {
		fontName = name;
		font = new Font(name, Font.PLAIN, fontSize);
		setFont(font);
	}
	
	public void setFontSize(int size) {
		fontSize = size;
		font = new Font(fontName, Font.PLAIN, size);
		setFont(font);
	}
	
	public void setFontColor(Color c) {
		tickerFontColor = c;
	}
	
	public void setWidth(int width) {
		if (this.width == width)
			return;
		
		this.width = width;
		Dimension d = new Dimension(width, getHeight());
		setMinimumSize(d);
	    setPreferredSize(d);
	    setMaximumSize(d);
	    setSize(d);
		repaint();
	}

	public boolean start() {
		return tickerTimeout.start();
	}
	
	public void stop() {
		tickerTimeout.stop();
	}
	
	public void setTickerHz(int hz) {
		tickerHz = hz;	
	}
	
	public void setTicksPerSecond(int ticks) {
		ticksPerSecond = ticks;
	}
	
	public int getTicksPerSecond() {
		return ticksPerSecond;
	}

	private boolean nextText() {
		text = nextText;
		
		if (text == null)
			return false;
		
		// Remove old Panels
		this.removeAll();
		p[pIndex] = new JPanel();
		p[pIndex].setOpaque(false);
		p[pIndex].setForeground(tickerFontColor);
		p[pIndex].setLayout(new BoxLayout(p[pIndex], BoxLayout.LINE_AXIS));
		int totHeight = icon.getIconHeight();
		int totWidth = 0;
		int strutWidth = 40;
		final FontMetrics metrics = getFontMetrics(font);
		
		for (int i = 0; i < text.length; i++) {
			JLabel l = new JLabel(text[i]);
			l.setIcon(icon);
			l.setFont(font);
			l.setForeground(tickerFontColor);
			l.setIconTextGap(5);
			l.setOpaque(false);
			int width = metrics.stringWidth(text[i]) + 
				icon.getIconWidth() + l.getIconTextGap();
			l.setSize(width, totHeight);
			p[pIndex].add(l);
			Component strut = Box.createHorizontalStrut(strutWidth);
			p[pIndex].add(strut);
			totWidth += width + strutWidth;

			//System.out.println("Adding ticker text: " + text[i] + " size=" + l.getSize());
		}
		p[pIndex].setSize(totWidth, totHeight);
		p[pIndex].doLayout();
		p[pIndex].setVisible(true);
		p[pIndex].setLocation(getWidth(), 0);
		add(p[pIndex]);
		//System.out.println("p size=" + p.getSize());
		
		return true;
	}
	
	class TickerTimeout implements Runnable {
		Thread tickerThread = null;
		boolean shouldExit = false;
		
		TickerTimeout() {
		}
		
		@Override
		public void run() {
			shouldExit = false;
			long prevTime, timeDiff;
			double currOffsetDouble[] = { 0, 0 };
			prevTime = System.nanoTime();
			
			while (!shouldExit) {

				try {
					Thread.sleep(tickerHz);
				} catch (InterruptedException e) {
					e.printStackTrace();
					continue;
				}

				long currTime = System.nanoTime();
				timeDiff = currTime - prevTime;
				
				double ticksPerNanoSecond = (double)ticksPerSecond / 1000000000;
				double numTicks = timeDiff * ticksPerNanoSecond;
				
				currOffsetDouble[0] += numTicks;
				currOffset[0] = (int)currOffsetDouble[0];
				currOffsetDouble[1] += numTicks;
				currOffset[1] = (int)currOffsetDouble[1];
				
				//System.out.printf("currOffset=%d width=%d p.width=%d\n",
					//	currOffset, getWidth(), p.getWidth());
				
				if (currOffset[pIndex] > getWidth() && 
						currOffset[pIndex] > p[pIndex].getWidth()) {
					
					pIndex = (pIndex + 1) % 2;
					pIndexOld = (pIndex + 1) % 2;
					
					if (nextText())
						add(p[pIndexOld]);

					currOffsetDouble[pIndex] = 0;
					currOffset[pIndex] = 0;
				}
				
				if (currOffset[pIndexOld] > (p[pIndexOld].getWidth() + getWidth())) {
						p[pIndexOld].setVisible(false);
				}
				
				if (currOffset[pIndex] > p[pIndex].getWidth()) {
					currOffsetDouble[pIndexOld] = 0;
					currOffset[pIndexOld] = 0;
					p[pIndexOld].setVisible(true);
				}
				
				p[pIndex].setLocation(getWidth() - currOffset[pIndex], 0);
				p[pIndexOld].setLocation(getWidth() - currOffset[pIndexOld], 0);
				
				repaint();

				prevTime = currTime;
			}
		}
		
		public boolean start() {
			if (tickerThread != null)
				return false;
			
			tickerThread = new Thread(this);
			tickerThread.start();
			return true;
		}
		
		public void stop() {
			if (tickerThread == null)
				return;
			
			shouldExit = true;
			tickerThread.interrupt();
			tickerThread = null;
		}
	}
	
	public String[] getText() {
		return text;
	}

	public void setText(String[] text) {
			if (text == nextText)
				return;
			
			nextText = text;
			
			if (this.text == null) {
				nextText();
				start();
			}
	}
}
