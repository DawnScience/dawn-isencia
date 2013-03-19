package com.isencia.passerelle.project.repository.api;

import com.isencia.passerelle.model.Flow;

/**
 * This interface of the root-level entries in the Passerelle Repository, i.e. Projects,
 * hides the generic Repository API, and just exposes the Passerelle/Drools-related concepts.
 * I.e. there is no way to navigate the Package/Asset tree, 
 * as there is no reason for it either in the given context.
 * 
 * The only things that are exposed, are Passerelle Flows and Drools Knowledgebases.
 * 
 * @author delerw
 *
 */
public interface Project {
	
  MetaData getMetaData();
  
	String getCode();
		
	/**
	 * @param seqCode the unique code identifying the flow in the project 
	 * (also unique within the complete repository??)
	 * @return the sequence in this project, for the given flowCode, or null if not found
	 */
	Flow getSubModel(String flowCode);
	/**
	 * @param flowCode the unique code identifying the flow in the project 
	 * (also unique within the complete repository??)
	 * @return the sequence in this project, for the given flowCode, or null if not found
	 */
	Flow getFlow(String flowCode);

	MetaData getFlowMetaData(String flowCode);
	
	/**
	 * @param seqCode the unique code identifying the flow in the project 
	 * (also unique within the complete repository??)
	 * @return the sequence in this project, for the given flowCode, or null if not found
	 */
	Long getFlowId(String flowCode);
	/**
	 * 
	 * @return an array of all flows defined within this project
	 */
	String[] getAllFlows();
	
	/**
	 * @return the identifiers of all the flows
	 */
	Long[] getAllFlowIds();
	
	/**
	 * 
	 * @param kbCode the unique code identifying the knowledgebase in the project
	 * (also unique within the complete repository??)
	 * @return the knowledgebase in this project, for the given kbCode, or null if not found
	 */
	Object getKnowledgeBase(String kbCode) throws Exception ;
	/**
	 * 
	 * @return array of all KB codes in this project. Since KBs can be quite large,
	 * the Project interface does not offer to return all of them in one shot, but returns just their codes.
	 * (contrary to the approach for the contained Passerelle Flows)
	 */
	String[] getAllKnowledgeBaseCodes();
	
	Long getId();

}
