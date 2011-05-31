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
package text2pledge.server.gui;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import text2pledge.server.PledgeServer;

public class PledgeDisplayFrame extends JFrame 
	implements MouseListener, WindowListener, FocusListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean isFullScreen = false;
	private boolean isFullScreenHardware = false;
	private Dimension winDimension = new Dimension(800, 600);
	
	public PledgeDisplayFrame() {
		super("Text2Pledge");
		
		this.setFocusable(true);
		
		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				//System.out.println("Pressed key " + e.getKeyChar());
				
				if ((e.isAltDown() || e.isMetaDown()) && 
						e.getKeyChar() == 'g') {
					toggleFullScreen();
				} else if ((e.isAltDown() || e.isMetaDown()) 
						&& e.getKeyChar() == 'f') {
					toggleFullScreenHardware();
				}
			}

			@Override
			public void keyTyped(KeyEvent e) {
				
			}
		});
		
		addMouseListener(this);
	}
	
	private void toggleFullScreen() {
		if (isFullScreen && isFullScreenHardware)
			return;
		
		GraphicsDevice device = null;
		GraphicsDevice[] devices = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getScreenDevices();

		// This is a bit of a hack to support dual screen displays.
		// We want to find a better way to figure out which device we should
		// go full screen on, e.g., the one where the JFrame is currently on.
		if (devices.length == 2)
			device = devices[1];
		else
			device = devices[0];
		
		setVisible(false);
		dispose();
		
		if (!isFullScreen) {
			setUndecorated(true);
			Rectangle screenSize = device.getDefaultConfiguration().getBounds();
			setBounds(screenSize);
			setAlwaysOnTop(true);
			isFullScreen = true;
		} else {
			setUndecorated(false);
			setBounds(0, 0, winDimension.width, winDimension.height);
			setAlwaysOnTop(false);
			isFullScreen = false;
		}

		setVisible(true);
	}

	private void toggleFullScreenHardware() {
		if (isFullScreen && !isFullScreenHardware)
			return;
		
		GraphicsDevice device = null;
		GraphicsDevice[] devices = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getScreenDevices();

		// This is a bit of a hack to support dual screen displays.
		// We want to find a better way to figure out which device we should
		// go full screen on, e.g., the one where the JFrame is currently on.
		if (devices.length == 2)
			device = devices[1];
		else
			device = devices[0];
		
		if (!device.isFullScreenSupported()) {
			System.err.println("Full screen not supported on device");
			return;
		}

		setVisible(false);
		dispose();
		
		if (isFullScreen) {
			setUndecorated(false);
            device.setFullScreenWindow(null);
			isFullScreen = false;
			isFullScreenHardware = false;
		} else {
			setUndecorated(true);
            device.setFullScreenWindow(this);
			isFullScreen = true;
			isFullScreenHardware = true;
		}
		setVisible(true);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		//System.out.println("Frame: Mouse clicked. Count=" + e.getClickCount());
		if (e.getClickCount() == 2) {
			//System.out.println("Double click!");
			toggleFullScreen();
		} else if (e.getClickCount() == 3) {
			//System.out.println("Triple click!");
			toggleFullScreenHardware();
		} 
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		//System.out.println("Frame: Mouse entered");
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		//System.out.println("Frame: Mouse exited");
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		//System.out.println("Frame: Mouse pressed");
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		//System.out.println("Frame: Mouse released");
		this.requestFocus();
	}

	@Override
	public void windowActivated(WindowEvent e) {
		//System.out.println("Window activated");
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public static void createAndShowGUI(PledgeDisplayPanel newContentPane) {
		PledgeDisplayFrame frame = new PledgeDisplayFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		newContentPane.setOpaque(true); //content panes must be opaque
		//newContentPane.setSize(frame.winDimension);
		frame.setSize(frame.winDimension);
        frame.setContentPane(newContentPane);
		frame.setLocationRelativeTo(null);
		frame.setFocusable(true);
		frame.setFocusableWindowState(true);
		frame.setResizable(true);
        //Display the window.
        frame.setVisible(true);
	}

	public static void main(String[] args) {
		final PledgeDisplayPanel pdp = new PledgeDisplayPanel();
		final PledgeServer pServer = new PledgeServer(pdp);
		pServer.start();

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI(pdp);
				pdp.showWelcomePledge();
			}
		});
	}

	@Override
	public void focusGained(FocusEvent e) {
		//System.out.println("Frame: Focus gained");
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		//System.out.println("Frame: Focus lost");
		
	}
}
