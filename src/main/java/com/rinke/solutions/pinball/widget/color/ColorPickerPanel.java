/*
 * @(#)ColorPickerPanel.java
 *
 * $Date: 2014-06-06 14:04:49 -0400 (Fri, 06 Jun 2014) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood.
 * You may not use, copy or modify this software, except in 
 * accordance with the license agreement you entered into with 
 * Jeremy Wood. For details see accompanying license terms.
 *
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 *
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.rinke.solutions.pinball.widget.color;


import java.awt.Dimension;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.rinke.solutions.pinball.widget.ResourceManagedCanvas;
import com.rinke.solutions.pinball.widget.color.ColorPicker.Mode;


/** This is the large graphic element in the <code>ColorPicker</code>
 * that depicts a wide range of colors.
 * <P>This panel can operate in 6 different modes.  In each mode a different
 * property is held constant: hue, saturation, brightness, red, green, or blue.
 * (Each property is identified with a constant in the <code>ColorPicker</code> class,
 * such as: <code>ColorPicker.HUE</code> or <code>ColorPicker.GREEN</code>.)
 * <P>In saturation and brightness mode, a wheel is used.  Although it doesn't
 * use as many pixels as a square does: it is a very aesthetic model since the hue can
 * wrap around in a complete circle.  (Also, on top of looks, this is how most
 * people learn to think the color spectrum, so it has that advantage, too).
 * In all other modes a square is used.
 * <P>The user can click in this panel to select a new color.  The selected color is
 * highlighted with a circle drawn around it.  Also once this
 * component has the keyboard focus, the user can use the arrow keys to
 * traverse the available colors.
 * <P>Note this component is public and exists independently of the
 * <code>ColorPicker</code> class.  The only way this class is dependent
 * on the <code>ColorPicker</code> class is when the constants for the modes
 * are used.
 * <P>The graphic in this panel will be based on either the width or
 * the height of this component: depending on which is smaller.
 *
 *
 * @see com.rinke.solutions.pinball.widget.color.bric.swing.ColorPicker
 * @see com.bric.swing.ColorPickerDialog
 */
public class ColorPickerPanel extends ResourceManagedCanvas implements MouseListener, MouseMoveListener {
    private static final long serialVersionUID = 1L;
   
    /** The maximum size the graphic will be.  No matter
     *  how big the panel becomes, the graphic will not exceed
     *  this length.
     *  <P>(This is enforced because only 1 BufferedImage is used
     *  to render the graphic.  This image is created once at a fixed
     *  size and is never replaced.)
     */
    public static final int MAX_SIZE = 325;
   
    /** This controls how the colors are displayed. */
    private Mode mode = Mode.HUE;//ColorPicker.BRI;
   
    /** The point used to indicate the selected color. */
    private Point point = new Point(0,0);
   
    /* Floats from [0,1].  They must be kept distinct, because
     * when you convert them to RGB coordinates HSB(0,0,0) and HSB (.5,0,0)
     * and then convert them back to HSB coordinates, the hue always shifts back to zero.
     */
    float hue = 0.3f, sat = 0.9f, bri = 0.9f;
    int red = -1, green = -1, blue = -1;

    private Dimension preferredSize;

    private ColorPicker colorPicker;
   
    public ColorPickerPanel(Composite parent, ColorPicker cp, int style) {
        super(parent, SWT.NO_BACKGROUND);
        this.colorPicker = cp;
//        maximumSize = new Dimension(MAX_SIZE+imagePadding.left+imagePadding.right,
//                MAX_SIZE+imagePadding.top+imagePadding.bottom);
        preferredSize = new Dimension( (int)(MAX_SIZE*.75), (int)(MAX_SIZE*.75));
//        setBounds(0,0,preferredSize.width, preferredSize.height);
        setRGB(0,0,0);
        addMouseListener(this);
        addMouseMoveListener(this);
    }

    BufferedImage image = new BufferedImage(MAX_SIZE, MAX_SIZE, BufferedImage.TYPE_INT_ARGB);
    Image colorImage = null;
   
    Insets imagePadding = new Insets(6,6,6,6);

    private Display display = Display.getCurrent();
   
    private int getWidth() {
        return this.getBounds().width;
    }
   
    private int getHeight() {
        return this.getBounds().height;
    }
   
    public void paint(GC g2) {
        int size = Math.min(MAX_SIZE, Math.min(getWidth()-imagePadding.left-imagePadding.right,
                getHeight()-imagePadding.top-imagePadding.bottom));
       
        int xoff= getWidth()/2-size/2;
        int yoff= getHeight()/2-size/2;
       
        //g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           
//        if(hasFocus()) {
//            PlafPaintUtils.paintFocus(g2,shape,3);
//        }
       
//        if(!(shape instanceof Rectangle)) {
//            //paint a circular shadow
//            g2.translate(2,2);
//            g2.setColor(new Color(0,0,0,20));
//            g2.fill(new Ellipse2D.Float(-2,-2,size+4,size+4));
//            g2.setColor(new Color(0,0,0,40));
//            g2.fill(new Ellipse2D.Float(-1,-1,size+2,size+2));
//            g2.setColor(new Color(0,0,0,80));
//            g2.fill(new Ellipse2D.Float(0,0,size,size));
//            g2.translate(-2,-2);
//        }
        g2.drawImage(colorImage, 0, 0, size, size, 0, 0, size, size );
       
        g2.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
        if(mode.equals(Mode.SAT) || mode.equals(Mode.BRI)) {
            g2.drawOval(0,0,size,size);
        } else {
            g2.drawRectangle(0,0,size,size);
        }
       
        g2.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
        g2.drawOval(point.x-3,point.y-3,6,6);
        g2.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
        g2.drawOval(point.x-4,point.y-4,8,8);
       
        //g.translate(-imagePadding.left, -imagePadding.top);

    }
   
    ImageData convertToSWT(BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel = (DirectColorModel) bufferedImage
                    .getColorModel();
            PaletteData palette = new PaletteData(colorModel.getRedMask(),
                    colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
           
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[4];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0],
                            pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
                    data.setAlpha(x, y, pixelArray[3]);
                }
            }
            return data;
        } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel) bufferedImage
                    .getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                        blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }
        return null;
    }

   
    /** Set the mode of this panel.
     * @param mode This must be one of the following constants from the <code>ColorPicker</code> class:
     * <code>HUE</code>, <code>SAT</code>, <code>BRI</code>, <code>RED</code>, <code>GREEN</code>, or <code>BLUE</code>
     */
    public void setMode(Mode mode) {
        if(this.mode==mode)
            return;
        this.mode = mode;
        regenerateImage();
        regeneratePoint();
        redraw();
    }
   
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        regenerateImage();
        regeneratePoint();
    }

   
    /** Sets the selected color of this panel.
     * <P>If this panel is in HUE, SAT, or BRI mode, then
     * this method converts these values to HSB coordinates
     * and calls <code>setHSB</code>.
     * <P>This method may regenerate the graphic if necessary.
     *
     * @param r the red value of the selected color.
     * @param g the green value of the selected color.
     * @param b the blue value of the selected color.
     */
    public void setRGB(int r,int g,int b) {
        if(r<0 || r>255)
            throw new IllegalArgumentException("The red value ("+r+") must be between [0,255].");
        if(g<0 || g>255)
            throw new IllegalArgumentException("The green value ("+g+") must be between [0,255].");
        if(b<0 || b>255)
            throw new IllegalArgumentException("The blue value ("+b+") must be between [0,255].");
       
        if(red!=r || green!=g || blue!=b) {
            if(mode.equals(Mode.RED) ||
                    mode.equals(Mode.GREEN) ||
                    mode.equals(Mode.BLUE)) {
                int lastR = red;
                int lastG = green;
                int lastB = blue;
                red = r;
                green = g;
                blue = b;
               
                if (mode.equals(Mode.RED) && lastR != r) {
                    regenerateImage();
                } else if (mode.equals(Mode.GREEN) && lastG != g) {
                    regenerateImage();
                } else if (mode.equals(Mode.BLUE) && lastB != b) {
                    regenerateImage();
                }
            } else {
                float[] hsb = new float[3];
                java.awt.Color.RGBtoHSB(r, g, b, hsb);
                setHSB(hsb[0],hsb[1],hsb[2]);
                return;
            }
            regeneratePoint();
            redraw();
            // TODO fireChangeListeners();
        }
    }

   
    /** @return the HSB values of the selected color.
     * Each value is between [0,1].
     */
    public float[] getHSB() {
        return new float[] {hue, sat, bri};
    }
   
    /** @return the RGB values of the selected color.
     * Each value is between [0,255].
     */
    public int[] getRGB() {
        return new int[] {red, green, blue};
    }
   
    /** Returns the color at the indicated point in HSB values.
     *
     * @param p a point relative to this panel.
     * @return the HSB values at the point provided.
     */
    public float[] getHSB(Point p) {
        if(mode.is(Mode.RED) || mode.is(Mode.GREEN) ||
                mode.is(Mode.BLUE)) {
            int[] rgb = getRGB(p);
            float[] hsb = java.awt.Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], null);
            return hsb;
        }
       
        int size = Math.min(MAX_SIZE, Math.min(getWidth()-imagePadding.left-imagePadding.right,getHeight()-imagePadding.top-imagePadding.bottom));
       
        p = new Point(p.x-(getWidth()/2-size/2), p.y-(getHeight()/2-size/2));
       
        if(mode.is(Mode.BRI) || mode.is(Mode.SAT)) {
            //the two circular views:
            double radius = (size)/2.0;
            double x = p.x-size/2.0;
            double y = p.y-size/2.0;
            double r = Math.sqrt(x*x+y*y)/radius;
            double theta = Math.atan2(y,x)/(Math.PI*2.0);
           
            if(r>1) r = 1;
           
            if(mode.is(Mode.BRI)) {
                return new float[] {
                        (float)(theta+.25f),
                        (float)(r),
                        bri};
            } else {
                return new float[] {
                        (float)(theta+.25f),
                        sat,
                        (float)(r)
                };
            }
        } else {
            float s = ((float)p.x)/((float)size);
            float b = ((float)p.y)/((float)size);
            if(s<0) s = 0;
            if(s>1) s = 1;
            if(b<0) b = 0;
            if(b>1) b = 1;
            return new float[] {hue, s, b};
        }
    }

    /** Returns the color at the indicated point in RGB values.
     *
     * @param p a point relative to this panel.
     * @return the RGB values at the point provided.
     */
    public int[] getRGB(Point p) {
        if(mode.is(Mode.BRI) || mode.is(Mode.SAT) ||
                mode.is(Mode.HUE)) {
            float[] hsb = getHSB(p);
            int rgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            int r = (rgb & 0xff0000) >> 16;
            int g = (rgb & 0xff00) >> 8;
            int b = (rgb & 0xff);
            return new int[] {r, g, b};
        }
       
        int size = Math.min(MAX_SIZE, Math.min(getWidth()-imagePadding.left-imagePadding.right,getHeight()-imagePadding.top-imagePadding.bottom));
       
        p = new Point(p.x-(getWidth()/2-size/2), p.y-(getHeight()/2-size/2));
       
        int x2 = p.x*255/size;
        int y2 = p.y*255/size;
        if(x2<0) x2 = 0;
        if(x2>255) x2 = 255;
        if(y2<0) y2 = 0;
        if(y2>255) y2 = 255;
           
        if(mode.is(Mode.RED)) {
            return new int[] {red, x2, y2};
        } else if(mode.is(Mode.GREEN)) {
            return new int[] {x2, green, y2};
        } else {
            return new int[] {x2, y2, blue};
        }
    }

    /** Sets the selected color of this panel.
     * <P>If this panel is in RED, GREEN, or BLUE mode, then
     * this method converts these values to RGB coordinates
     * and calls <code>setRGB</code>.
     * <P>This method may regenerate the graphic if necessary.
     *
     * @param h the hue value of the selected color.
     * @param s the saturation value of the selected color.
     * @param b the brightness value of the selected color.
     */
    public void setHSB(float h,float s,float b) {
        //hue is cyclic: it can be any value
        h = (float)(h-Math.floor(h));
       
        if(s<0 || s>1)
            throw new IllegalArgumentException("The saturation value ("+s+") must be between [0,1]");
        if(b<0 || b>1)
            throw new IllegalArgumentException("The brightness value ("+b+") must be between [0,1]");
       
        if(hue!=h || sat!=s || bri!=b) {
            if(mode.is(Mode.HUE) ||
                    mode.is(Mode.BRI) ||
                    mode.is(Mode.SAT)) {
                float lastHue = hue;
                float lastBri = bri;
                float lastSat = sat;
                hue = h;
                sat = s;
                bri = b;
                if(mode.is(Mode.HUE)) {
                    if(lastHue!=hue) {
                        regenerateImage();
                    }
                } else if(mode.is(Mode.SAT)) {
                    if(lastSat!=sat) {
                        regenerateImage();
                    }
                } else if(mode.is(Mode.BRI)) {
                    if(lastBri!=bri) {
                        regenerateImage();
                    }
                }
            } else {

                java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(h, s, b));
                setRGB(c.getRed(), c.getGreen(), c.getBlue());
                return;
            }
           

            java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(hue, sat, bri));
            red = c.getRed();
            green = c.getGreen();
            blue = c.getBlue();
           
            regeneratePoint();
            redraw();
            // fireChangeListeners();
        }       
    }
   
    /** Recalculates the (x,y) point used to indicate the selected color. */
    private void regeneratePoint() {
        int size = Math.min(MAX_SIZE, Math.min(getWidth()-imagePadding.left-imagePadding.right,getHeight()-imagePadding.top-imagePadding.bottom));
        if(mode.is(Mode.HUE) || mode.is(Mode.SAT) || mode.is(Mode.BRI)) {
            if(mode.is(Mode.HUE)) {
                point = new Point((int)(sat*size+.5),(int)(bri*size+.5));
            } else if(mode.is(Mode.SAT)) {
                double theta = hue*2*Math.PI-Math.PI/2;
                if(theta<0) theta+=2*Math.PI;
               
                double r = bri*size/2;
                point = new Point((int)(r*Math.cos(theta)+.5+size/2.0),(int)(r*Math.sin(theta)+.5+size/2.0));
            } else if(mode.is(Mode.BRI)) {
                double theta = hue*2*Math.PI-Math.PI/2;
                if(theta<0) theta+=2*Math.PI;
                double r = sat*size/2;
                point = new Point((int)(r*Math.cos(theta)+.5+size/2.0),(int)(r*Math.sin(theta)+.5+size/2.0));
            }
        } else if(mode.is(Mode.RED)) {
            point = new Point((int)(green*size/255f+.49f),
                    (int)(blue*size/255f+.49f) );
        } else if(mode.is(Mode.GREEN)) {
            point = new Point((int)(red*size/255f+.49f),
                    (int)(blue*size/255f+.49f) );
        } else if(mode.is(Mode.BLUE)) {
            point = new Point((int)(red*size/255f+.49f),
                    (int)(green*size/255f+.49f) );
        }
    }
   
    /** A row of pixel data we recycle every time we regenerate this image. */
    private int[] row = new int[MAX_SIZE];

    private int but;
    /** Regenerates the image. */
    private synchronized void regenerateImage() {
        int size = Math.min(MAX_SIZE, Math.min(getWidth()-imagePadding.left-imagePadding.right,getHeight()-imagePadding.top-imagePadding.bottom));
        if( colorImage != null ) {
            colorImage.dispose();
        }

        if(mode.is(Mode.BRI) || mode.is(Mode.SAT)) {
            float bri2 = this.bri;
            float sat2 = this.sat;
            float radius = (size)/2f;
            float hue2;
            float k = 1.2f; //the number of pixels to antialias
            for(int y = 0; y<size; y++) {
                float y2 = (y-size/2f);
                for(int x = 0; x<size; x++) {
                    float x2 = (x-size/2f);
                    double theta = Math.atan2(y2,x2)-3*Math.PI/2.0;
                    if(theta<0) theta+=2*Math.PI;
                   
                    double r = Math.sqrt(x2*x2+y2*y2);
                    if(r<=radius) {
                        if(mode.is(Mode.BRI)) {
                            hue2 = (float)(theta/(2*Math.PI));
                            sat2 = (float)(r/radius);
                        } else { //SAT
                            hue2 = (float)(theta/(2*Math.PI));
                            bri2 = (float)(r/radius);
                        }
                        row[x] = java.awt.Color.HSBtoRGB(hue2, sat2, bri2);
                        if(r>radius-k) {
                            int alpha = (int)(255-255*(r-radius+k)/k);
                            if(alpha<0) alpha = 0;
                            if(alpha>255) alpha = 255;
                            row[x] = row[x] & 0xffffff+(alpha << 24);
                        }
                    } else {
                        row[x] = 0x00000000;
                    }
                }
                image.getRaster().setDataElements(0, y, size, 1, row);
            }
        } else if(mode.is(Mode.HUE)) {
            float hue2 = this.hue;
            for(int y = 0; y<size; y++) {
                float y2 = ((float)y)/((float)size);
                for(int x = 0; x<size; x++) {
                    float x2 = ((float)x)/((float)size);
                    row[x] = java.awt.Color.HSBtoRGB(hue2, x2, y2);
                }
                image.getRaster().setDataElements(0, y, image.getWidth(), 1, row);
            }
        } else { //mode is RED, GREEN, or BLUE
            int red2 = red;
            int green2 = green;
            int blue2 = blue;
            for(int y = 0; y<size; y++) {
                float y2 = ((float)y)/((float)size);
                for(int x = 0; x<size; x++) {
                    float x2 = ((float)x)/((float)size);
                    if(mode.is(Mode.RED)) {
                        green2 = (int)(x2*255+.49);
                        blue2 = (int)(y2*255+.49);
                    } else if(mode.is(Mode.GREEN)) {
                        red2 = (int)(x2*255+.49);
                        blue2 = (int)(y2*255+.49);
                    } else {
                        red2 = (int)(x2*255+.49);
                        green2 = (int)(y2*255+.49);
                    }
                    row[x] = 0xFF000000 + (red2 << 16) + (green2 << 8) + blue2;
                }
                image.getRaster().setDataElements(0, y, size, 1, row);
            }
        }
        colorImage = new Image(display, convertToSWT(image));
    }

    @Override
    protected void paintWidget(PaintEvent e) {
        paint(e.gc);
    }

    @Override
    public org.eclipse.swt.graphics.Point computeSize(int wHint, int hHint,
            boolean changed) {
        return new Point(wHint, hHint);
    }

    @Override
    public void mouseMove(org.eclipse.swt.events.MouseEvent e) {
        if( but == 1) {
            handleMouse(e);
        }
    }

    @Override
    public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {
    }

    @Override
    public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
        handleMouse(e);
        but = e.button;
    }

    private void handleMouse(org.eclipse.swt.events.MouseEvent e) {
        Point p = new Point(e.x+6, e.y+6);
        if(mode.is(Mode.BRI) || mode.is(Mode.SAT) ||
                mode.is(Mode.HUE)) {
            float[] hsb = getHSB(p);
            setHSB(hsb[0], hsb[1], hsb[2]);
        } else {
            int[] rgb = getRGB(p);
            setRGB(rgb[0], rgb[1], rgb[2]);
        }
        if( colorPicker != null ) {
            colorPicker.panelValueChanged(0);
        }
    }

    @Override
    public void mouseUp(org.eclipse.swt.events.MouseEvent e) {
        but = 0;
    }
}
