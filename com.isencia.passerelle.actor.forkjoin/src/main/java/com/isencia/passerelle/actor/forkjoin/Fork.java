/**
 * 
 */
package com.isencia.passerelle.actor.forkjoin;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.dynaport.OutputPortConfigurationExtender;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.internal.MessageContainer;

/**
 * <p>
 * An actor with configurable named output ports, that sends out copies of the
 * incoming Context on each outgoing port. The original Context is sent on the
 * default output port.
 * </p>
 * <p>
 * This is useful to ensure that parallel branches in a sequence don't see each
 * others intermediate results.
 * </p>
 * <p>
 * The <code>Fork</code> is typically used with the <code>Join</code>, to merge
 * the results of the parallel branches into one Context again.
 * </p>
 * 
 * @author erwin
 * 
 */
public class Fork extends AbstractMessageSequenceGenerator {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(Fork.class);

  public Port input;
  public OutputPortConfigurationExtender outputPortCfgExt;

  /**
   * @param container
   * @param name
   * @throws IllegalActionException
   * @throws NameDuplicationException
   */
  public Fork(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name, true);
    input = PortFactory.getInstance().createInputPort(this, null);
    outputPortCfgExt = new OutputPortConfigurationExtender(this, "output port configurer");
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  public void process(ActorContext ctx, ProcessRequest procRequest, ProcessResponse procResponse) throws ProcessingException {
    MessageContainer message = (MessageContainer) procRequest.getMessage(input);
    if (message != null) {
      try {
        Long scopeId = message.getID();
        registerSequenceScopeMessage(scopeId, message);

        try {
          getAuditLogger().info("Forking msg with scope " + scopeId + " : " + getAuditTrailMessage(message, input));
        } catch (Exception e) {
          getLogger().error("Error logging audit trail", e);
        }

        List<Port> outputPorts = outputPortCfgExt.getOutputPorts();
        for (int i = 0; i < outputPorts.size(); ++i) {
          MessageContainer outputMsg = (MessageContainer) MessageFactory.getInstance().createMessageCloneInSequence(
              message,
              scopeId,  // sequence ID
              new Long(i),                          // sequence position
              (i == (outputPorts.size() - 1)));     // end of sequence?
          // enforce single Fork name
          outputMsg.setHeader(HEADER_SEQ_SRC, getName());
          outputMsg.setBodyContent(cloneScopeMessageBodyContent(message.getBodyContent()), ManagedMessage.objectContentType);
          procResponse.addOutputMessage(outputPorts.get(i), outputMsg);
        }
      } catch (Exception e) {
        throw new ProcessingException(ErrorCode.ACTOR_EXECUTION_ERROR, "Error generating forked messages", this, message, e);
      }
    }
  }
  
  /**
   * override this if you need special msg copies/clones
   * 
   * default impl just returns the argument
   * 
   * @param scopeMessageBodyContent
   * @return
   * @throws MessageException
   */
  protected Object cloneScopeMessageBodyContent(Object scopeMessageBodyContent) throws MessageException {
    return scopeMessageBodyContent;
  }
}
