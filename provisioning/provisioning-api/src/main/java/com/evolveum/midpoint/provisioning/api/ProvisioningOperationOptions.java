package com.evolveum.midpoint.provisioning.api;


public class ProvisioningOperationOptions {
	
	Boolean completePostponed;
	
	Boolean force;
	
	Boolean postpone;
	
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


}
