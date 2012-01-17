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
package com.isencia.passerelle.actor.gui;

import java.net.MalformedURLException;
import java.net.URL;

import ptolemy.actor.gui.Configuration;
import ptolemy.actor.gui.Effigy;
import ptolemy.actor.gui.MoMLApplication;
import ptolemy.actor.gui.Tableau;
import ptolemy.actor.gui.TableauFactory;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.actor.DocEffigy;
import ptolemy.vergil.actor.DocViewer;
import ptolemy.vergil.basic.DocAttribute;

//////////////////////////////////////////////////////////////////////////
//// DocTableau
// Modified for Passerelle : hide menu of the doc viewer
// as it includes all kinds of Vergil-stuff that we don't want.

/**
 A tableau representing a documentation view in a toplevel window.
 The URL that is viewed is given by the <i>url</i> parameter, and
 can be either an absolute URL, a system fileName, or a resource that
 can be loaded relative to the classpath.  For more information about how
 the URL is specified, see MoMLApplication.specToURL().
 <p>
 The constructor of this
 class creates the window. The text window itself is an instance
 of DocViewer, and can be accessed using the getFrame() method.
 As with other tableaux, this is an entity that is contained by
 an effigy of a model.
 There can be any number of instances of this class in an effigy.

 @author  Edward A. Lee
 @version $Id: DocTableau.java,v 1.7 2006/02/12 17:43:38 cxh Exp $
 @since Ptolemy II 5.2
 @Pt.ProposedRating Yellow (eal)
 @Pt.AcceptedRating Red (cxh)
 @see Effigy
 @see DocViewer
 @see MoMLApplication#specToURL(String)
 */
public class DocTableau extends Tableau {

    /** Construct a new tableau for the model represented by the given effigy.
     *  This creates an instance of DocViewer.  It does not make the frame
     *  visible.  To do that, call show().
     *  @param container The container.
     *  @param name The name.
     *  @exception IllegalActionException If the container does not accept
     *   this entity (this should not occur).
     *  @exception NameDuplicationException If the name coincides with an
     *   attribute already in the container.
     */
    public DocTableau(Effigy container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
        if (!(container instanceof DocEffigy)) {
            throw new IllegalActionException(container,
                    "Needs to be an instance of DocEffigy to contain a DocTableau.");
        }
        DocAttribute docAttribute = ((DocEffigy) container).getDocAttribute();
        if (docAttribute != null) {
            // Have a doc attribute.
            DocViewer frame = new DocViewer(docAttribute.getContainer(),
                    (Configuration) container.toplevel());
            frame.hideMenuBar();
            setFrame(frame);
            frame.setTableau(this);
        } else {
            // No doc attribute. Find the URL of the enclosing effigy.
            try {
                URL effigyURL = container.uri.getURL();
                DocViewer frame = new DocViewer(effigyURL,
                        (Configuration) container.toplevel());
                frame.hideMenuBar();
                setFrame(frame);
                frame.setTableau(this);
            } catch (MalformedURLException e) {
                throw new IllegalActionException(this, container, e,
                        "Malformed URL");
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         inner classes                     ////

    /** A factory that creates Doc viewer tableaux for Ptolemy models.
     */
    public static class Factory extends TableauFactory {
        /** Create a factory with the given name and container.
         *  @param container The container.
         *  @param name The name.
         *  @exception IllegalActionException If the container is incompatible
         *   with this attribute.
         *  @exception NameDuplicationException If the name coincides with
         *   an attribute already in the container.
         */
        public Factory(NamedObj container, String name)
                throws IllegalActionException, NameDuplicationException {
            super(container, name);
        }

        ///////////////////////////////////////////////////////////////////
        ////                         public methods                    ////

        /** If the specified effigy already contains a tableau named
         *  "DocTableau", then return that tableau; otherwise, create
         *  a new instance of DocTableau in the specified
         *  effigy, and name it "DocTableau".  If the specified
         *  effigy is not an instance of DocEffigy, then do not
         *  create a tableau and return null.  It is the
         *  responsibility of callers of this method to check the
         *  return value and call show().
         *
         *  @param effigy The effigy.
         *  @return A Doc viewer tableau, or null if one cannot be
         *    found or created.
         *  @exception Exception If the factory should be able to create a
         *   tableau for the effigy, but something goes wrong.
         */
        public Tableau createTableau(Effigy effigy) throws Exception {
            if (effigy instanceof DocEffigy) {
                // Indicate to the effigy that this factory contains effigies
                // offering multiple views of the effigy data.
                effigy.setTableauFactory(this);

                // First see whether the effigy already contains an
                // DocTableau.
                DocTableau tableau = (DocTableau) effigy
                        .getEntity("DocTableau");

                if (tableau == null) {
                    tableau = new DocTableau(effigy, "DocTableau");
                }
                // Don't call show() here.  If show() is called here,
                // then you can't set the size of the window after
                // createTableau() returns.  This will affect how
                // centering works.
                return tableau;
            } else {
                return null;
            }
        }
    }
}
