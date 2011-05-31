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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

public class SlidePane extends JComponent {
	private static final long serialVersionUID = 1L;
	private Image slide = null;
	//private Image scaledSlide = null;
	private Thread slideShowThread = null;
	private ImageLoader imgLoader = new ImageLoader();
	private int slideIndex = 0;
	public int NEXT_SLIDE_TIMEOUT = 10000;
	Dimension oldSize;
	boolean keepAspect = false;
	
	class ImageFilter implements FilenameFilter {
	    public boolean accept(File dir, String name) {
	        return name.endsWith(".png") || name.endsWith(".jpg");
	    }
	}
	
	SlidePane() {
		setSize(800, 600);
		oldSize = getSize();
	}

	public void setNextSlideTimeout(int milliseconds) {
		NEXT_SLIDE_TIMEOUT = milliseconds;
	}
	
	class SlideShowTimeout implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			nextSlide();
		}
	}

	public void nextSlide() {
		if (slideShowThread != null)
			slideShowThread.interrupt();
	}
	
	class ImageUpdater implements Runnable {
		Image img, imgScaled;
		
		ImageUpdater(Image img, Image imgScaled) {
			this.img = img;
			this.imgScaled = imgScaled;
		}
		@Override
		public void run() {
			updateSlide(img, imgScaled);
		}	
	}
	
	private Image scaleImage(Image img, boolean keepAspect) {
		int width = keepAspect ? -1 : getWidth();
		int height = getHeight();
		return img.getScaledInstance(width, height, Image.SCALE_DEFAULT);
	}
	
	class ImageLoader implements Runnable {
		boolean shouldExit = false;
		
		@Override
		public void run() {
			shouldExit = false;
			CodeSource codeSource = PledgeDisplayPanel.class.getProtectionDomain().getCodeSource();
			File jarFile = null;
			try {
				jarFile = new File(codeSource.getLocation().toURI().getPath());
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}

			if (jarFile == null) {
				return;
			}

			String slidesPath = jarFile.getParentFile().getAbsolutePath()
					+ "/slides";
			System.out.println("dir is " + slidesPath);

			File slidesFile = new File(slidesPath);
			File[] images = slidesFile.listFiles(new ImageFilter());

			if (images == null)
				return;
			
			System.out.println("loading " + images[slideIndex].getName());
			Image img, imgScaled = null;
			
			try {
				img = ImageIO.read(images[slideIndex]);
				//System.out.println("loading - width=" + getWidth());
				imgScaled = scaleImage(img, keepAspect);	
			} catch (IOException e2) {
				e2.printStackTrace();
				return;
			}
			
			while (!shouldExit) {
				slideIndex = (slideIndex + 1) % images.length;
				try {
					long startTime = System.currentTimeMillis();
					javax.swing.SwingUtilities.invokeLater(new ImageUpdater(img, imgScaled));
					//System.out.println("loading " + images[slideIndex].getName());
					img = ImageIO.read(images[slideIndex]);
					//System.out.println("loading - width=" + getWidth())
					imgScaled = scaleImage(img, keepAspect);
					long timeDiff = System.currentTimeMillis() - startTime;
					Thread.sleep(NEXT_SLIDE_TIMEOUT - timeDiff);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e) {
					//System.err.println("Slideshow interrupted");
				}
			}
		}
		
		public void stop() {
			shouldExit = true;
		}
	}

	private void updateSlide(Image img, Image imgScaled) {
		slide = img;
		//scaledSlide = imgScaled;
		repaint();
	}
	public final void start() {
		if (slideShowThread != null && slideShowThread.isAlive())
			return;

		slideIndex = 0;
		slideShowThread = new Thread(imgLoader);
		slideShowThread.start();
	}
	
	public void stop() {
		if (slideShowThread == null || !slideShowThread.isAlive())
			return;
		
		imgLoader.stop();
		slideShowThread.interrupt();
		slideShowThread = null;
	}
	
	/*
	private void checkImageScale() {
		if (slide == null)
			return;
		if (scaledSlide.getHeight(this) != getSize().height) {
			//System.out.println("Scaling image");
			scaledSlide = scaleImage(slide, keepAspect);
		}
	}
	*/

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		//checkImageScale();
		//g.drawImage(scaledSlide, 0, 0, this);
		int width = keepAspect ? -1 : getWidth();
		g.drawImage(slide, 0, 0, width, getHeight(), this);
		g.dispose();
		oldSize = getSize();
	}
	
	private static void createAndShowGUI() {
		final SlidePane sp = new SlidePane();
		JFrame f = new JFrame("SlidePane Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(sp);
		f.setSize(800, 600);
		f.setVisible(true);
		sp.start();
	}
	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
