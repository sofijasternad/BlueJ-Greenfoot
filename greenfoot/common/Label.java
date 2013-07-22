import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextLayout;

/**
 * A Label class that allows you to display a textual value on screen.
 * 
 * The Label is an actor, so you will need to create it, and then add it to the world
 * in Greenfoot.  If you keep a reference to the Counter then you can adjust its value.  
 *
 * @author Amjad Altadmri 
 * @version 1.0
 */
public class Label extends Actor
{
    private String text;
    private int fontSize;
    private String fontFamilyName = "Arial";
    private Color lineColor = Color.BLACK;
    private Color fillColor = Color.WHITE;
    
    private GreenfootImage background;
    private static final Color transparent = new Color(0,0,0,0);

    
    /**
     * Create a new label, initialise it with the int value to be shown and the font size 
     */
    public Label(int value, int fontSize)
    {
        this(Integer.toString(value), fontSize);
    }
    
    /**
     * Create a new label, initialise it with the needed text and the font size 
     */
    public Label(String text, int fontSize)
    {
        this.text = text;
        this.fontSize = fontSize;
        updateImage();
    }
    
    /**
     * Animate will just refresh the label display.
     */
    public void act() 
    {
        updateImage();
    }

    /**
     * Sets the font family
     * 
     * @param fontFamilyName the name of the font family only
     */
    public void setFont(String fontFamilyName)
    {
        this.fontFamilyName = fontFamilyName;
        updateImage();
    }
    
    /**
     * Sets the line color of the text
     * 
     * @param lineColor the line color of the text
     */
    public void setLineColor(Color lineColor)
    {
        this.lineColor = lineColor;
        updateImage();
    }
    
    /**
     * Sets the fill color of the text
     * 
     * @param fillColor the fill color of the text
     */
    public void setFillColor(Color fillColor)
    {
        this.fillColor = fillColor;
        updateImage();
    }
    

    /**
     * Update the image on screen to show the current value.
     */
    private void updateImage()
    {
        GreenfootImage backgroundImage = new GreenfootImage(text, fontSize, transparent, transparent);
        Graphics2D g = (Graphics2D) backgroundImage.getAwtImage().getGraphics();

        Font font = new Font(fontFamilyName, Font.PLAIN, fontSize);
        g.setFont(font);
        
        // If you ask for a height of size 40, you may well get a font of size 48
        // (I did on my Ubuntu system).  So we compensate if that happens by
        // scaling down our request to one that we expect should get the right size.
        // We don't loop because it may not converge.
        if (g.getFontMetrics().getHeight() != fontSize) {
            font = g.getFont().deriveFont((float)fontSize * (float)fontSize / (float)g.getFontMetrics().getHeight());
            g.setFont(font);
        }
        
        TextLayout textLayout = new TextLayout(text, font, g.getFontRenderContext());
        Shape outline = textLayout.getOutline(null);
        
        GreenfootImage textImage = drawOutlinedText(outline, textLayout);
        
        if (textImage.getWidth() > backgroundImage.getWidth())
        {
            backgroundImage.scale(textImage.getWidth(), textImage.getHeight());
        }
        
        backgroundImage.drawImage(textImage, (backgroundImage.getWidth()-textImage.getWidth())/2, 
                        (backgroundImage.getHeight()-textImage.getHeight())/2);
        
        setImage(backgroundImage);
    }
    
    private GreenfootImage drawOutlinedText(Shape outline, TextLayout textLayout)
    {
        
        GreenfootImage textImage = new GreenfootImage(outline.getBounds().width + 10,
                                   outline.getBounds().height + 10);
        Graphics2D g = (Graphics2D) textImage.getAwtImage().getGraphics();

        g.translate(0, outline.getBounds().height);
     
        g.setColor(fillColor);
        textLayout.draw(g, 0, 0);
        
        g.setColor(lineColor);
        g.draw(outline);
        
        return textImage;
    }
}