package bluej;

import java.awt.*;
import javax.swing.*;

import java.net.URL;

/**
 * This class implements a splash window that can be displayed while BlueJ
 * is starting up.
 *
 * @author  Michael Kolling
 * @version $Id: SplashWindow.java 2595 2004-06-12 19:30:25Z mik $
 */

public class SplashWindow extends JFrame
{
	
	private class BlueJLabel extends JLabel {
		
		BlueJLabel(ImageIcon iconImage){
			super(iconImage);
		}
		
		public void paint(Graphics g){
			super.paint(g);
			g.setColor(new Color(31,70,110));
			g.setFont(new Font("SansSerif", Font.PLAIN, 16));
			g.drawString("Version " + Boot.BLUEJ_VERSION, 26, image.getHeight()-20);
		}
	}
	
    private BlueJLabel image;

    public SplashWindow()
    {
    		setUndecorated(true);
    		// must start with a forward slash or else Java converts the .
    		// to a /
    		URL iconURL = getClass().getResource("/bluej/splash.jpg");
    	
        ImageIcon icon = new ImageIcon(iconURL);

        image = new BlueJLabel(icon);
        image.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        getContentPane().add(image);
        pack();

        // centre on screen
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenDim.width - getSize().width)/2,
                    (screenDim.height - getSize().height)/2);
        setVisible(true);
    }

    
    /**
     * Remove this splash screen from screen. Since we never need it again,
     * throw it away completely.
     */
    public void remove()
    {
        dispose();
    }
}
