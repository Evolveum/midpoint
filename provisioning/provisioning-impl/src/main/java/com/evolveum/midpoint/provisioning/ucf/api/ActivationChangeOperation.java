/**
 * 
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;

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
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		SchemaDebugUtil.indentDebugDump(sb, indent);
		sb.append("Activation change: enabled=");
		sb.append(enabled);
		return sb.toString();
	}
	
}
