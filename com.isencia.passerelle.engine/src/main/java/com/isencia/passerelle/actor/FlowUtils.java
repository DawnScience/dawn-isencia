package com.isencia.passerelle.actor;

import ptolemy.kernel.util.NamedObj;

public class FlowUtils {


	public static final String FLOW_SEPARATOR = "#sep";

	public static String extractFlowName(NamedObj actor) {
		String fullName = actor.getFullName();

		if (fullName.contains(FLOW_SEPARATOR)) {
			return fullName.split(FLOW_SEPARATOR)[0];
		}
		return actor.toplevel().getName();
	}

	public static String generateUniqueFlowName(String name, String uniqueIndex) {
		String fullName = null;
		if (name.contains(FLOW_SEPARATOR)) {
		  fullName =  name.split(FLOW_SEPARATOR)[0];
    }else{
      fullName = name;
    }
	  
	  StringBuffer sb = new StringBuffer(fullName);
		sb.append(FLOW_SEPARATOR);
		sb.append(uniqueIndex);
		return sb.toString();
	}

	public static String generateUniqueFlowName(String name) {

		return generateUniqueFlowName(name, Long.toString(System.currentTimeMillis()));
	}

	public static String getFullNameWithoutFlow(NamedObj no) {
		NamedObj container = no.toplevel();
		return no.getFullName().substring(container.getName().length() + 1);
	}

	public static String getOriginalFullName(NamedObj no) {
		String fullNameWithoutFlow = getFullNameWithoutFlow(no);
		StringBuffer sb = new StringBuffer(extractFlowName(no));
		sb.append(fullNameWithoutFlow);
		return sb.toString();
	}

}
