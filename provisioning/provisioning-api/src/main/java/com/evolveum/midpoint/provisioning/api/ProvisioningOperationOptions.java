/*
 * Copyright (c) 2010-2016 Evolveum
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

public class ProvisioningOperationOptions implements Serializable {

    private static final long serialVersionUID = -6960273605308871338L;

    /**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	Boolean raw;
	
	Boolean completePostponed;
	
	Boolean force;
	
	Boolean postpone;
	
	// TODO: align with GetOperationOptions, the option there is doNotDiscovery
	Boolean doDiscovery;

	Boolean overwrite;
	
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

	public Boolean getDoDiscovery() {
		return doDiscovery;
	}

	public void setDoDiscovery(Boolean doDiscovery) {
		this.doDiscovery = doDiscovery;
	}
	
	public static boolean isDoDiscovery(ProvisioningOperationOptions options){
		if (options == null) {
			return false;
		}
		if (options.doDiscovery == null) {
			return false;
		}
		return options.doDiscovery;
	}
	
	public static ProvisioningOperationOptions createDoDiscovery(boolean doDiscovery) {
		ProvisioningOperationOptions opts = new ProvisioningOperationOptions();
		opts.setDoDiscovery(doDiscovery);
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


}
