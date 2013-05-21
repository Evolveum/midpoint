/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class GetOperationOptions implements Serializable {
	
	/**
	 * Resolve the object reference. This only makes sense with a (path-based) selector.
	 */
	Boolean resolve;
	
	/**
	 * No not fetch any information from external sources, e.g. do not fetch account data from resource,
	 * do not fetch resource schema, etc.
	 * Such operation returns only the data stored in midPoint repository.
	 */
	Boolean noFetch;
	
	/**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	Boolean raw;
	
	/**
	 * Force to get object from the resource even if some of the error occurrd.
	 * If the any copy of the shadow is fetched, we can't delete this object
	 * from the gui, for example
	 */
	Boolean doNotDiscovery;
	

	public Boolean getResolve() {
		return resolve;
	}

	public void setResolve(Boolean resolve) {
		this.resolve = resolve;
	}
	
	public static boolean isResolve(GetOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.resolve == null) {
			return false;
		}
		return options.resolve;
	}
	
	public static GetOperationOptions createResolve() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setResolve(true);
		return opts;
	}

	public Boolean getNoFetch() {
		return noFetch;
	}

	public void setNoFetch(Boolean noFetch) {
		this.noFetch = noFetch;
	}
	
	public static boolean isNoFetch(GetOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.noFetch == null) {
			return false;
		}
		return options.noFetch;
	}
	
	public static GetOperationOptions createNoFetch() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setNoFetch(true);
		return opts;
	}

	public Boolean getRaw() {
		return raw;
	}

	public void setRaw(Boolean raw) {
		this.raw = raw;
	}
	
	public static boolean isRaw(GetOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.raw == null) {
			return false;
		}
		return options.raw;
	}
	
	public static GetOperationOptions createRaw() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setRaw(true);
		return opts;
	}
	
	public Boolean getDoNotDiscovery() {
		return doNotDiscovery;
	}

	public void setDoNotDiscovery(Boolean force) {
		this.doNotDiscovery = force;
	}
	
	public static boolean isDoNotDiscovery(GetOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.doNotDiscovery == null) {
			return false;
		}
		return options.doNotDiscovery;
	}
	
	public static GetOperationOptions createDoNotDiscovery() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setDoNotDiscovery(true);
		return opts;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((noFetch == null) ? 0 : noFetch.hashCode());
		result = prime * result + ((raw == null) ? 0 : raw.hashCode());
		result = prime * result + ((resolve == null) ? 0 : resolve.hashCode());
		result = prime * result + ((doNotDiscovery == null) ? 0 : doNotDiscovery.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GetOperationOptions other = (GetOperationOptions) obj;
		if (noFetch == null) {
			if (other.noFetch != null)
				return false;
		} else if (!noFetch.equals(other.noFetch))
			return false;
		if (raw == null) {
			if (other.raw != null)
				return false;
		} else if (!raw.equals(other.raw))
			return false;
		if (resolve == null) {
			if (other.resolve != null)
				return false;
		} else if (!resolve.equals(other.resolve))
			return false;
		if (doNotDiscovery == null) {
			if (other.doNotDiscovery != null)
				return false;
		} else if (!doNotDiscovery.equals(other.doNotDiscovery))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GetOperationOptions(resolve=" + resolve + ", noFetch=" + noFetch
				+ ", raw=" + raw + ", doNotDiscovery="+doNotDiscovery+")";
	}

}
