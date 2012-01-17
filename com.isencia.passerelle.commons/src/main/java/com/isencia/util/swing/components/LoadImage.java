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

/** 

 LoadImage is a part of Admin

 It tries to open a Image as a Icon from different locations:
 1) the directory given over with -Dadmin.image=[DIRECTORY]
 2) the directory ../be.isencia.util.swing.images
 3) the directory ./be.isencia.util.swing.images


 Admin is a Tool around mySQL to do basic jobs
 for DB-Administrations, like:
 - create/ drop tables
 - create  indices
 - perform sql-statements
 - simple form
 - a guided query
 and a other usefull things in DB-arena

 Admin Version see below
 Copyright (c) 1999 Fredy Fischer
 se-afs@dial.eunet.ch

 Fredy Fischer
 Hulmenweg 36
 8405 Winterthur
 Switzerland

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 **/

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;

public class LoadImage {

  String image = null;

  public void setImage(String v) {
    this.image = v;
  }

  public LoadImage() {
    image = null;
  }

  public LoadImage(String image) {
    this.setImage(image);
  }

  public static ImageIcon getImage(String image) {

    /**
     * we try to find the image a three places: 1) at the location defined by
     * the System-Property admin.image 2) at the directory
     * ../be.isencia.util.swing.images 3) at the directory
     * ./be.isencia.util.swing.images new for this Version is the use of a URL,
     * so be.isencia.util.swing.images can be loaded over the net
     **/

    String url1 = System.getProperty("admin.image") + File.separator + image;

    // did the user deliver a correct URL?
    if (System.getProperty("admin.image") != null) {
      if (!isImageLoadable(url1)) {
        try {
          File f = new File(url1);
          if (f.exists()) {
            url1 = f.toURL().toString();
          }
        } catch (Exception ioException) {
          url1 = null;
        }
      }
    }

    String url2 = null, url3 = null;

    if (System.getProperty("admin.image") == null) {
      try {
        url2 = LoadImage.class.getResource(".." + File.separator + "be.isencia.util.swing.images" + File.separator + image).toString();

        url3 = LoadImage.class.getResource(File.separator + "be.isencia.util.swing.images" + File.separator + image).toString();
      } catch (NullPointerException npe) {
        // not found
      }
    }

    String url = null;

    // now we go the order and find out, if the image exists
    if (isImageLoadable(url1)) {
      url = url1;
    } else {
      if (isImageLoadable(url2)) {
        url = url2;
      } else {
        if (isImageLoadable(url3)) {
          url = url3;
        }
      }
    }

    if (url != null) {

      try {
        URL u = new URL(url);
        ImageIcon img = new ImageIcon(u);
        // addImage(img);
        return img;
      } catch (Exception ecp1) {
        ecp1.printStackTrace();
      }

    }
    return null;
  }

  private static boolean isImageLoadable(String u) {
    boolean loadable = true;
    try {
      URL url = new URL(u);
      url.openStream();
    } catch (MalformedURLException mfue) {
      loadable = false;
    } catch (IOException ioex) {
      loadable = false;
    }
    return loadable;
  }
}
