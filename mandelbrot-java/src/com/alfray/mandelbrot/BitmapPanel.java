/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alfray.mandelbrot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import javax.swing.JPanel;

/**
 *
 * @author ralf
 */
public class BitmapPanel extends JPanel {
    private BufferedImage mImage;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();
        
        g.setColor(Color.red);
        g.drawOval(0, 0, w, h);
        
        if (mImage == null || mImage.getWidth() != w || mImage.getHeight() != h) {
            mImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            fill(mImage);
        }
        g.drawImage(mImage, 0, 0, null);
    }

    private void fill(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        WritableRaster raster = image.getRaster();
        ColorModel model = image.getColorModel();
        
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int a = (i+j) & 0x0FF;
                a |= (a<<8) | (a<<16);
                raster.setPixel(i, j, new int[] { a, a, a } );
                // raster.setDataElements(i, j, model.getDataElements(a, null));
            }
        }
    }

}
