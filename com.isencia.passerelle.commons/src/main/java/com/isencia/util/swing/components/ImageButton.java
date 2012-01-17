/* Copyright 2011 - iSencia Belgium NV

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.isencia.util.swing.components;

import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

public class ImageButton extends JButton {

  public ImageButton() {

  }

  public ImageButton(String text, String image, String toolTip) {

    if (image != null) {
      try {
        ImageIcon img = getImageIcon(image);
        this.setIcon(grayed(img.getImage()));
        this.setRolloverIcon(img);
        this.setRolloverEnabled(true);
      } catch (Exception e) {
        // System.out.println("Can not load Image " + image);
      }
    }
    if (text != null) this.setText(text);
    if (toolTip != null) this.setToolTipText(toolTip);

  }

  public ImageIcon getImageIcon(String image) {

    ImageIcon img = new ImageIcon();

    if (image != null) {
      img = LoadImage.getImage(image);
    }
    return img;

  }

  public ImageIcon grayed(Image orig) {
    ImageFilter filter = new GrayFilter();
    ImageProducer producer = new FilteredImageSource(orig.getSource(), filter);
    ImageIcon imgIcon = new ImageIcon(createImage(producer));
    return imgIcon;
  }

  public static void main(String args[]) {

    System.out.println("Fredy's ImageButton\n" + "is based on JButton and does a Rollover-Image\n"
        + "use it as follows: java -D admin.image=<path-to-be.isencia.util.swing.images> gpl.fredy.ui.ImageButton <text> <image> <tooltip>\n");
    if (args.length != 3) System.exit(0);
    JFrame frame = new JFrame("TEST");
    ImageButton imgB = new ImageButton(args[0], args[1], args[2]);
    frame.getContentPane().add(imgB);
    frame.pack();
    frame.setVisible(true);
    frame.addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
      }

      public void windowClosed(WindowEvent e) {
      }

      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }

      public void windowDeactivated(WindowEvent e) {
      }

      public void windowDeiconified(WindowEvent e) {
      }

      public void windowIconified(WindowEvent e) {
      }

      public void windowOpened(WindowEvent e) {
      }
    });

  }

}

class GrayFilter extends RGBImageFilter {
  public GrayFilter() {
    canFilterIndexColorModel = true;
  }

  public int filterRGB(int x, int y, int rgb) {
    int a = rgb & 0xff000000;
    int r = (((rgb & 0xff0000) + 0x1800000) / 3) & 0xff0000;
    int g = (((rgb & 0x00ff00) + 0x018000) / 3) & 0x00ff00;
    int b = (((rgb & 0x0000ff) + 0x000180) / 3) & 0x0000ff;
    return a | r | g | b;
  }
}
