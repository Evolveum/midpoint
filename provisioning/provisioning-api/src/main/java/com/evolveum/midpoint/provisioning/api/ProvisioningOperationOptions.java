/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.api;

import java.io.Serializable;

import com.evolveum.midpoint.util.ShortDumpable;

public class ProvisioningOperationOptions implements Serializable, ShortDumpable {
    private static final long serialVersionUID = -6960273605308871338L;

    /**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	private Boolean raw;
	
	private Boolean completePostponed;
	
	private Boolean force;
	
	private Boolean postpone;
	
	private Boolean doNotDiscovery;

	private Boolean overwrite;
	
	/**
	 * Run the operations on resource using the specified identity.
	 * The value is a OID of an account shadow.
	 * 
	 * This option should be considered a suggestion, not a command.
	 * Therefore if the connector can run operation as specified user
	 * the it should run it. But if it is not capable to run such
	 * operation as specified user but it can run it as administrator
	 * then it should run the operation as administrator.
	 * That case may happen e.g. if account cleartext password is not
	 * known at that time.
	 * 
	 * Note: maybe later we need some kind of flag that makes this
	 * option "critical", i.e. that an error is thrown if the
	 * operation cannot be executed as specified identity.
	 */
	private String runAsAccountOid;
	
	public Boolean getCompletePostponed() {
		return completePostponed;
	}

	public void setCompletePostponed(Boolean doDiscovery) {
		this.completePostponed = doDiscovery;
	}
	
	//by default we want to complete postponed operation, we skip only if the option is set to false..
	public static boolean isCompletePostponed(ProvisioningOperationOptions options){
		if (options == null) {
			return true;
		}
		if (options.completePostponed == null) {
			return true;
		}
		return options.completePostponed;
	}
	
	public static ProvisioningOperationOptions createCompletePostponed(boolean completePostponed) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setCompletePostponed(completePostponed);
		return opts;
	}

	public Boolean getForce() {
		return force;
	}

	public void setForce(Boolean force) {
		this.force = force;
	}
	
	public static boolean isForce(ProvisioningOperationOptions options){
		if (options == null) {
			return false;
		}
		if (options.force == null) {
			return false;
		}
		return options.force;
	}
	
	public static ProvisioningOperationOptions createForce(boolean force) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setForce(force);
		return opts;
	}

	public Boolean getPostpone() {
		return postpone;
	}

	public void setPostpone(Boolean postpone) {
		this.postpone = postpone;
	}
	
	public static boolean isPostpone(ProvisioningOperationOptions options){
		if (options == null) {
			return false;
		}
		if (options.postpone == null) {
			return false;
		}
		return options.postpone;
	}
	
	public static ProvisioningOperationOptions createPostpone(boolean postpone) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setPostpone(postpone);
		return opts;
	}

	public Boolean getDoNotDiscovery() {
		return doNotDiscovery;
	}

	public void setDoNotDiscovery(Boolean doDiscovery) {
		this.doNotDiscovery = doDiscovery;
	}
	
	public static boolean isDoNotDiscovery(ProvisioningOperationOptions options){
		if (options == null) {
			return false;
		}
		if (options.doNotDiscovery == null) {
			return false;
		}
		return options.doNotDiscovery;
	}
	
	public static ProvisioningOperationOptions createDoNotDiscovery(boolean doDiscovery) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setDoNotDiscovery(doDiscovery);
		return opts;
	}

	
	public Boolean getOverwrite() {
		return overwrite;
	}

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	public static boolean isOverwrite(ProvisioningOperationOptions options){
		if (options == null) {
			return false;
		}
		if (options.overwrite == null) {
			return false;
		}
		return options.overwrite;
	}
	
	public static ProvisioningOperationOptions createOverwrite(boolean overwrite) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setOverwrite(overwrite);
		return opts;
	}
	
	public Boolean getRaw() {
		return raw;
	}

	public void setRaw(Boolean raw) {
		this.raw = raw;
	}
	
	public static boolean isRaw(ProvisioningOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.raw == null) {
			return false;
		}
		return options.raw;
	}
	
	public static ProvisioningOperationOptions createRaw() {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setRaw(true);
		return opts;
	}
	
	public String getRunAsAccountOid() {
		return runAsAccountOid;
	}

	public void setRunAsAccountOid(String runAsAccountOid) {
		this.runAsAccountOid = runAsAccountOid;
	}
	
	public static ProvisioningOperationOptions createRunAsAccountOid(String runAsAccountOid) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setRunAsAccountOid(runAsAccountOid);
		return opts;
	}

	@Override
    public String toString() {
    	StringBuilder sb = new StringBuilder("ProvisioningOperationOptions(");
    	shortDump(sb);
		sb.append(")");
		return sb.toString();
    }
    
    private void appendFlag(StringBuilder sb, String name, Boolean val) {
		if (val == null) {
			return;
		} else if (val) {
			sb.append(name);
			sb.append(",");
		} else {
			sb.append(name);
			sb.append("=false,");
		}
	}

	@Override
	public void shortDump(StringBuilder sb) {
		appendFlag(sb, "raw", raw);
    	appendFlag(sb, "completePostponed", completePostponed);
    	appendFlag(sb, "force", force);
    	appendFlag(sb, "postpone", postpone);
    	appendFlag(sb, "doNotDiscovery", doNotDiscovery);
    	appendFlag(sb, "overwrite", overwrite);
    	if (runAsAccountOid != null) {
    		sb.append("runAsAccountOid=").append(runAsAccountOid).append(",");
    	}
    	if (sb.charAt(sb.length() - 1) == ',') {
			sb.deleteCharAt(sb.length() - 1);
		}
    	if (sb.length() == 0) {
    		sb.append("(empty)");
    	}
	}
	
}
