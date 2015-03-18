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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class GetOperationOptions implements Serializable, Cloneable {
	
	/**
	 * Specifies whether to return specific items. It is used for optimizations.
	 * Some requests only needs a subset of items therefore fetching them all is a waste
	 * of resources. Other requests may need expensive data that are not normally returned by default.
	 * <p>
	 * If no retrieve option is set in the entire options set then it
	 * means that the whole object with a default set of properties has to be
	 * returned. This is equivalent to specifying DEFAULT retrieve root option.
	 * <p>
	 * If there is at least one retrieve option in the set then the following rules apply:
	 * <ul>
	 *   <li>Items marked as INCLUDE will be returned.</li>
	 *   <li>Any item marked as EXCLUDE may not be returned. (Note: Excluded items may still be returned if their retrieval is cheap.)</li>
	 *   <li>Items marked as DEFAULT will be returned if they would also be returned without any options (by default).</li>
	 *   <li>Items that are not marked (have no option or have null retrieve option) but their superitem is marked (have retrieve option) 
	 *       behave in the same way as superitem. E.g. if a superitem is marked as
	 *       INCLUDE they will also be included in the result. This also applies transitively (e.g. superitem of superitem).
	 *   <li>If a superitem is marked as EXCLUDE and subitem is marked as INCLUDE then the behavior is undefined. Do not do this. Strange things may happen.</li>
	 *   <li>For items that are not marked in any way and for which the superitem is also not marked the "I do not care" behavior is assumed.
	 *       This means that they may be returned or they may be not. The implementation will return them if their retrieval is cheap
	 *       but they will be most likely omitted from the result.</li>
	 *  </ul>
	 */
	private RetrieveOption retrieve;
	
	/**
	 * Resolve the object reference. This only makes sense with a (path-based) selector.
	 */
	private Boolean resolve;

    /**
     * Resolve the object reference names. (Currently applicable only as a top-level option.)
     *
     * Names of referenced objects are provided as PrismValue userData entries.
     *
     * EXPERIMENTAL.
     */
	private Boolean resolveNames;

    /**
	 * No not fetch any information from external sources, e.g. do not fetch account data from resource,
	 * do not fetch resource schema, etc.
	 * Such operation returns only the data stored in midPoint repository.
	 */
	private Boolean noFetch;
	
	/**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	private Boolean raw;
	
	/**
	 * Force to get object from the resource even if some of the error occurred.
	 * If the any copy of the shadow is fetched, we can't delete this object
	 * from the gui, for example
	 */
	private Boolean doNotDiscovery;
	
	private RelationalValueSearchQuery relationalValueSearchQuery;
	
	/**
	 * This flag indicated if the "object not found" error is critical for
	 * processing the original request. If it is not, we just ignore it and
	 * don't log it. In other cases, error in logs may lead to misleading
	 * information..
	 */
	Boolean allowNotFound;

	public RetrieveOption getRetrieve() {
		return retrieve;
	}

	public void setRetrieve(RetrieveOption retrieve) {
		this.retrieve = retrieve;
	}
	
	public static RetrieveOption getRetrieve(GetOperationOptions options) {
		if (options == null) {
			return null;
		}
		return options.retrieve;
	}
	
	public static GetOperationOptions createRetrieve(RetrieveOption retrieve) {
		GetOperationOptions options = new GetOperationOptions();
		options.retrieve = retrieve;
		return options;
	}

    public static GetOperationOptions createRetrieve(RelationalValueSearchQuery query) {
        GetOperationOptions options = new GetOperationOptions();
        options.retrieve = RetrieveOption.INCLUDE;
        options.setRelationalValueSearchQuery(query);
        return options;
    }

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

    // todo maybe at wrong place, but this might be quite useful
    public static Collection<SelectorOptions<GetOperationOptions>> createRetrieveNameOnlyOptions() {
        return SelectorOptions.createCollection(new ItemPath(ObjectType.F_NAME), createRetrieve(RetrieveOption.INCLUDE));
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createRetrieveAttributesOptions(QName... properties) {
        Collection<SelectorOptions<GetOperationOptions>> optionsCollection = new ArrayList<>(properties.length);
        for (QName property : properties) {
            optionsCollection.add(SelectorOptions.create(new ItemPath(property), createRetrieve(RetrieveOption.INCLUDE)));
        }
        return optionsCollection;
    }

    public Boolean getResolveNames() {
		return resolveNames;
	}

	public void setResolveNames(Boolean resolveNames) {
		this.resolveNames = resolveNames;
	}
	
	public static boolean isResolveNames(GetOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.resolveNames == null) {
			return false;
		}
		return options.resolveNames;
	}
	
	public static GetOperationOptions createResolveNames() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setResolveNames(true);
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
	
	public static GetOperationOptions createAllowNotFound() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setAllowNotFound(true);
		return opts;
	}
	
	public Boolean getAllowNotFound() {
		return allowNotFound;
	}

	public void setAllowNotFound(Boolean allowNotFound) {
		this.allowNotFound = allowNotFound;
	}
	
	public static boolean isAllowNotFound(GetOperationOptions options) {
		if (options == null) {
			return false;
		}
		if (options.allowNotFound == null) {
			return false;
		}
		return options.allowNotFound;
	}
	
	public static GetOperationOptions createDoNotDiscovery() {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setDoNotDiscovery(true);
		return opts;
	}

	public RelationalValueSearchQuery getRelationalValueSearchQuery() {
		return relationalValueSearchQuery;
	}

	public void setRelationalValueSearchQuery(RelationalValueSearchQuery relationalValueSearchQuery) {
		this.relationalValueSearchQuery = relationalValueSearchQuery;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((allowNotFound == null) ? 0 : allowNotFound.hashCode());
		result = prime * result + ((doNotDiscovery == null) ? 0 : doNotDiscovery.hashCode());
		result = prime * result + ((noFetch == null) ? 0 : noFetch.hashCode());
		result = prime * result + ((raw == null) ? 0 : raw.hashCode());
		result = prime * result
				+ ((relationalValueSearchQuery == null) ? 0 : relationalValueSearchQuery.hashCode());
		result = prime * result + ((resolve == null) ? 0 : resolve.hashCode());
		result = prime * result + ((resolveNames == null) ? 0 : resolveNames.hashCode());
		result = prime * result + ((retrieve == null) ? 0 : retrieve.hashCode());
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
		if (allowNotFound == null) {
			if (other.allowNotFound != null)
				return false;
		} else if (!allowNotFound.equals(other.allowNotFound))
			return false;
		if (doNotDiscovery == null) {
			if (other.doNotDiscovery != null)
				return false;
		} else if (!doNotDiscovery.equals(other.doNotDiscovery))
			return false;
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
		if (relationalValueSearchQuery == null) {
			if (other.relationalValueSearchQuery != null)
				return false;
		} else if (!relationalValueSearchQuery.equals(other.relationalValueSearchQuery))
			return false;
		if (resolve == null) {
			if (other.resolve != null)
				return false;
		} else if (!resolve.equals(other.resolve))
			return false;
		if (resolveNames == null) {
			if (other.resolveNames != null)
				return false;
		} else if (!resolveNames.equals(other.resolveNames))
			return false;
		if (retrieve != other.retrieve)
			return false;
		return true;
	}

	public GetOperationOptions clone() {
        GetOperationOptions clone = new GetOperationOptions();
        clone.noFetch = this.noFetch;
        clone.doNotDiscovery = this.doNotDiscovery;
        clone.raw = this.raw;
        clone.resolve = this.resolve;
        clone.resolveNames = this.resolveNames;
        clone.retrieve = this.retrieve;
        clone.allowNotFound = this.allowNotFound;
        if (this.relationalValueSearchQuery != null) {
        	clone.relationalValueSearchQuery = this.relationalValueSearchQuery.clone();
        }
        return clone;
    }

	@Override
	public String toString() {
		return "GetOperationOptions(resolve=" + resolve + ", resolveNames=" + resolveNames + ",noFetch=" + noFetch
				+ ", raw=" + raw + ", doNotDiscovery="+doNotDiscovery+", retrieve="+retrieve+", allowNotFound="+ allowNotFound 
				+", relationalValueSearchQuery="+relationalValueSearchQuery+")";
	}

}
