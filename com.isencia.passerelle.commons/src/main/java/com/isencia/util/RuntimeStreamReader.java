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
package com.isencia.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * RuntimeStreamReader TODO: class comment
 * 
 * @author wim
 */
public class RuntimeStreamReader extends Thread {
  // ~ Instance variables
  // _____________________________________________________________________________________________________________________________________

  InputStream is = null;
  Object lock = null;
  Type type = null;
  Writer os1 = null;
  Writer os2 = null;

  PrintStream ps1 = null;
  PrintStream ps2 = null;

  // ~ Constructors
  // ___________________________________________________________________________________________________________________________________________

  public RuntimeStreamReader(Object lock, InputStream is, Type type, Writer htmlWriter, Writer asciiWriter) {
    this.is = is;
    this.type = type;
    this.os1 = htmlWriter;
    this.os2 = asciiWriter;
    this.lock = lock;
  }

  public RuntimeStreamReader(Object lock, InputStream is, Type type, PrintStream htmlStream, PrintStream asciiStream) {
    this.is = is;
    this.type = type;
    this.ps1 = htmlStream;
    this.ps2 = asciiStream;
    this.lock = lock;
  }

  /**
   * DOCUMENT ME!
   */
  public void run() {
    try {
      PrintWriter pw1 = null;

      if (null != os1) {
        pw1 = new PrintWriter(os1);
      }
      if (null != ps1) {
        pw1 = new PrintWriter(ps1);
      }

      PrintWriter pw2 = null;

      if (null != os2) {
        pw2 = new PrintWriter(os2);
      }

      if (null != ps2) {
        pw2 = new PrintWriter(ps2);
      }
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;

      while ((line = br.readLine()) != null) {
        if (pw1 != null) {
          if (type == Type.error) {
            pw1.println("<FONT style=\"font-size:10px;font-family:sans-serif;color:red;\"> &gt;&gt;&nbsp;" + line + "<BR></FONT>");
          } else {
            pw1.println("<FONT style=\"font-size:10px;font-family:sans-serif;\"> &gt;&gt;&nbsp;" + line + "<BR></FONT>");
          }
          pw1.flush();
        }

        if (pw2 != null) {
          pw2.println(line);
          pw2.flush();
        }

      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      synchronized (lock) {
        lock.notifyAll();
      }
    }
  }

  // ~ Classes
  // ________________________________________________________________________________________________________________________________________________

  public static final class Type {
    public static final Type output = new Type(1, "Output");
    public static final Type error = new Type(2, "Error");
    private String name = "";

    private Type(int type, String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }
}