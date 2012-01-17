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

package com.isencia.passerelle.actor.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.Filter;
import com.isencia.passerelle.actor.FilterException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
 * RegexpFilter A filter that checks whether the textual data contains a region
 * that matches a given regular expression.
 * 
 * @author erwin
 */
public class RegExpFilter extends Filter {
  // ~ Static variables/initializers
  // __________________________________________________________________________________________________________________________

  private static Logger logger = LoggerFactory.getLogger(RegExpFilter.class);

  // ~ Instance variables
  // _____________________________________________________________________________________________________________________________________

  public Parameter expressionParam = null;
  private Pattern pattern = null;

  // ~ Constructors
  // ___________________________________________________________________________________________________________________________________________

  /**
   * Construct an actor in the specified container with the specified name.
   * 
   * @param container The container.
   * @param name The name of this actor within the container.
   * @exception IllegalActionException If the actor cannot be contained by the
   *              proposed container.
   * @exception NameDuplicationException If the name coincides with an actor
   *              already in the container.
   */
  public RegExpFilter(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    expressionParam = new StringParameter(this, "Expression");
    expressionParam.setExpression("");
  }

  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " :" + attribute);
    }

    if (attribute == expressionParam) {
      String tmp = ((StringToken) expressionParam.getToken()).stringValue();

      try {
        pattern = Pattern.compile(tmp);
        logger.debug("Expression set to : " + tmp);
      } catch (PatternSyntaxException e) {
        throw new IllegalActionException("Invalid RegExp expression : " + tmp + " : " + e.getMessage());
      }
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  /**
   * @param msg
   * @return boolean indicating whether the message matches the filter
   * @throws FilterException
   */
  protected boolean isMatchingFilter(Object msg) throws FilterException {
    boolean matchFound = false;
    if ((pattern == null)) {
      matchFound = true;
    } else if (msg instanceof ManagedMessage) {
      ManagedMessage message = (ManagedMessage) msg;
      Object[] inputs = MessageHelper.getFilteredContent(message, new String[] { "text/plain", "text/html", "text/xml" });

      if ((inputs != null) && (inputs.length > 0)) {
        for (int i = 0; (i < inputs.length) && !matchFound; ++i) {
          if (logger.isDebugEnabled()) {
            logger.debug("Content :" + inputs[i]);
          }

          if (inputs[i] instanceof String) {
            matchFound = pattern.matcher((String) inputs[i]).matches();
          }
        }
      } else if (logger.isDebugEnabled()) {
        logger.debug(getInfo() + " : no valid content in " + msg);
      }
    }
    return matchFound;
  }

  protected String getExtendedInfo() {
    return pattern.pattern();
  }
}