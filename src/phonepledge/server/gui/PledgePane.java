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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PledgePane extends JTextPane {
	private static final long serialVersionUID = 1L;
	public static int NEXT_PLEDGE_TIMEOUT = 10000;
	private static int textFadeInTime = 3000; // Milliseconds
	private float alphaStart = 0.0f;
	private float alphaIncrement = 0.02f;
	private float alpha = alphaStart;
	private int fadeInTimeout = 100;
	SimpleAttributeSet center;
	Timer textFadeInTimer;
	Timer textShowTimer;
	private String currentPledgeText = "";
	private int maxFontSize = 48;
	private Dimension oldDimension = null;
	// Queue of pledges that are removed when they have been showed
	private ArrayList<String> textQueue = new ArrayList<String>();
	
	public PledgePane() {
		super();
		setEditorKit(new CenteringEditorKit());
		setBorder(null);
		setEditable(false);
		setDragEnabled(false);
		setSelectionColor(getBackground());
		//setBackground(new Color(255,0,0, 0));
		//setForeground(Color.black);
		setOpaque(false);
	
		textFadeInTimer = new Timer(fadeInTimeout, new TextAnimator());
		textShowTimer = new Timer(NEXT_PLEDGE_TIMEOUT, new TextShowTimeout());
		textShowTimer.setRepeats(false);
		center = new SimpleAttributeSet();
		//StyleConstants.setBold(center, true);
		StyleConstants.setFontFamily(center, "Impact");
		StyleConstants.setFontSize(center, maxFontSize);
		StyleConstants.setForeground(center, Color.black);
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		setTextFadeInTime(textFadeInTime);
	}
	
	class TextAnimator implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			alpha += alphaIncrement;
			
			if (alpha > 1) {
				alpha = 1;
				textFadeInTimer.stop();
				textShowTimer.setInitialDelay(NEXT_PLEDGE_TIMEOUT);
				textShowTimer.restart();
			}
			
			repaint();
		}
	}
	
	public void setTextFadeInTime(int milliseconds) {
		alphaIncrement = 1.0f / ((float)milliseconds / (float)fadeInTimeout);
	}
	
	class TextShowTimeout implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			textShowTimer.stop();
			nextText();
		}
	}
	
	public boolean isTextShowing() {
		return textFadeInTimer.isRunning() || textShowTimer.isRunning();
	}

	public int getQueueSize() {
		return textQueue.size();
	}
	
	public void removeText(String text) {
		Vector<String> elms = new Vector<String>();
		elms.add(text);
		textQueue.removeAll(elms);
	}
	
	public void setNextPledgeTimeout(int milliseconds) {
		NEXT_PLEDGE_TIMEOUT = milliseconds;
	}
	
	public int getNextPledgeTimeout() {
		return NEXT_PLEDGE_TIMEOUT;
	}
	
	public void stop() {
		textFadeInTimer.stop();
		textShowTimer.stop();
	}
	
	public void start() {
		if (isTextShowing())
			return;
		
		nextText();
	}

	public static int getMaxFittingFontSize(Graphics g, Font font, 
			String string, Dimension size, int maxFontSize){
		return getMaxFittingFontSize(g, font, string, size.width, size.height, maxFontSize);
	}

	/* Calculate the max font size that fits within a graphics object with line wrapping */
	public static int getMaxFittingFontSize(Graphics g, Font font, String string, 
			int width, int height, int maxFontSize){
		int minFontSize = 0;
		int currFontSize = font.getSize();

		//System.out.println("width=" + width + " height=" + height);
		
		while (maxFontSize - minFontSize > 2){
			FontMetrics fm = g.getFontMetrics(new Font(font.getName(), 
					font.getStyle(), currFontSize));
			int fontWidth = fm.stringWidth(string);
			int fontHeight = fm.getLeading() + fm.getMaxAscent() + fm.getMaxDescent();
			int numLines = height / fontHeight;
			int currWidth = width * numLines;
			
			if ((fontWidth > currWidth) || (fontHeight > height)){
				maxFontSize = currFontSize;
				currFontSize = (maxFontSize + minFontSize) / 2;
			} else {
				minFontSize = currFontSize;
				currFontSize = (minFontSize + maxFontSize) / 2;
			}
		}

		return currFontSize;
	}
	
	public void updateAndScaleText(String text) {
		if (text == null || text.length() == 0)
			return;
		
		Dimension d = PledgePane.this.getSize();
		
    	/* Check if we really need to change the document */
		if (oldDimension != null && oldDimension.equals(d)) 
			return;
		
		oldDimension = d;
		
    	int fontSize = getMaxFittingFontSize(PledgePane.this.getGraphics(), 
    			PledgePane.this.getFont(), 
    			text, d, maxFontSize);

		StyledDocument doc = new DefaultStyledDocument();
		StyleConstants.setFontSize(center, fontSize);
        
		try {
			doc.insertString(0, text, center);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return;
		}
		
		doc.setParagraphAttributes(0, doc.getLength(), center, true);
		setDocument(doc);
	}
	
	// Should be called on the EDT thread
	public void nextText() {
		if (textQueue.isEmpty()) {
			setVisible(false);
			return;
		}

		currentPledgeText = textQueue.remove(0);
		//updateAndScaleText(currentPledgeText);
		stop();
		setVisible(true);
		alpha = alphaStart;
		textFadeInTimer.start();
	}
	
	public void setFont(String font) {
		StyleConstants.setFontFamily(center, font);
	}
	
	public void setFontColor(Color c) {
		StyleConstants.setForeground(center, c);
	}
	
	public void setFontSize(int size) {
		maxFontSize = size;
	}
	
	public void addText(String text) {
		
		/* System.out.printf("PledgePane: Add text '%s' queue size=%d\n",
				text, textQueue.size()); */
		
		textQueue.add(text);
		
		if (textQueue.size() == 1 && !isTextShowing()) {
			nextText();
		}
	}
	
	private AlphaComposite makeComposite(float alpha) {
	    int type = AlphaComposite.SRC_OVER;
	    return(AlphaComposite.getInstance(type, alpha));
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setComposite(makeComposite(alpha));
		updateAndScaleText(currentPledgeText);
		super.paintComponent(g);
	}

	static void createAndShowGUI() {
		final PledgePane pp = new PledgePane();
		pp.setOpaque(true);
		JFrame f = new JFrame("PledgePane Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(pp);
		f.setSize(400, 100);
		f.setVisible(true);
		pp.addText("Hello world!");
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
