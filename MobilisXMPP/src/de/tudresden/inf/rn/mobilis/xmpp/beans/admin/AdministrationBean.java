package de.tudresden.inf.rn.mobilis.xmpp.beans.admin;

import de.tudresden.inf.rn.mobilis.xmpp.beans.XMPPBean;

public abstract class AdministrationBean extends XMPPBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String ServiceNamespace = null;
	public int ServiceVersion = -1;
	
	public AdministrationBean() {
		// TODO Auto-generated constructor stub
	}

	public AdministrationBean(String errorType, String errorCondition,
			String errorText) {
		super(errorType, errorCondition, errorText);
		// TODO Auto-generated constructor stub
	}

}