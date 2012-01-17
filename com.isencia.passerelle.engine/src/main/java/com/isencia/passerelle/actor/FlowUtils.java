package com.isencia.passerelle.actor;

import ptolemy.kernel.util.NamedObj;

public class FlowUtils {
	public static String getFullNameWithoutFlow(NamedObj no) {
		NamedObj container = getTopLevel(no);
		return no.getFullName().substring(container.getName().length() + 1);
	}

	public static NamedObj getTopLevel(NamedObj no) {
		if (no.getContainer() == null) {
			return no;
		}
		return getTopLevel(no.getContainer());
	}
}
