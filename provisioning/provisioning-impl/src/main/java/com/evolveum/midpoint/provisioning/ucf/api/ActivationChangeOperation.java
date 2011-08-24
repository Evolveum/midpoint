/**
 * 
 */
package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * Used for enable/disable of accounts and other resource objects.
 * 
 * TODO: may need refactoring later on to accommodate from and to dates.
 * 
 * @author Radovan Semancik
 *
 */
public class ActivationChangeOperation extends Operation {

	private boolean enabled;

	/**
	 * @param enabled
	 */
	public ActivationChangeOperation(boolean enabled) {
		super();
		this.enabled = enabled;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	
}
