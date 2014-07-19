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
package com.evolveum.midpoint.common.refinery;

import java.io.Serializable;

import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Aggregate bean containing resource OID, intent and thombstone flag.
 * It uniquely identifies an shadow projection (usually account) for a specific user regardless whether it has OID, does not have
 * OID yet, it exists of was deleted.
 * 
 * This is used mostly as a key in hashes and for searches.
 * 
 * @author Radovan Semancik
 */
public class ResourceShadowDiscriminator implements Serializable {
	private static final long serialVersionUID = 346600684011645741L;
	
	private String resourceOid;
	private ShadowKindType kind = ShadowKindType.ACCOUNT;
	private String intent;
	private boolean thombstone;
	private int order = 0;
	
	public ResourceShadowDiscriminator(String resourceOid, ShadowKindType kind, String intent, boolean thombstone) {
		this.resourceOid = resourceOid;
		this.thombstone = thombstone;
		setIntent(intent);
		setKind(kind);
	}
	
	public ResourceShadowDiscriminator(String resourceOid, ShadowKindType kind, String intent) {
		this(resourceOid, kind, intent, false);
	}

	
	public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType) {
		this(accRefType.getResourceRef().getOid(), accRefType.getKind(), accRefType.getIntent());
	}
	
	public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType, String defaultResourceOid, ShadowKindType defaultKind) {
		ShadowKindType kind = accRefType.getKind();
		if (kind == null) {
			kind = defaultKind;
		}
		if (accRefType.getResourceRef() == null) {
			this.resourceOid = defaultResourceOid;
		} else {
			this.resourceOid = accRefType.getResourceRef().getOid();
		}
		this.thombstone = false;
		setIntent(accRefType.getIntent());
		setKind(kind);
	}

	public String getResourceOid() {
		return resourceOid;
	}
	
	public void setResourceOid(String resourceOid) {
		this.resourceOid = resourceOid;
	}
	
	public ShadowKindType getKind() {
		return kind;
	}

	public void setKind(ShadowKindType kind) {
		if (kind == null) {
			throw new IllegalArgumentException("Kind cannot be null");
		}
		this.kind = kind;
	}

	public String getIntent() {
		return intent;
	}
	
	public void setIntent(String intent) {
		if (intent == null) {
			intent = SchemaConstants.INTENT_DEFAULT;
		}
		this.intent = intent;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Thumbstone flag is true: the account no longer exists. The data we have are the latest metadata we were able to get. 
	 */
	public boolean isThombstone() {
		return thombstone;
	}

	public void setThombstone(boolean thombstone) {
		this.thombstone = thombstone;
	}
	
    public ShadowDiscriminatorType toResourceShadowDiscriminatorType() {
        ShadowDiscriminatorType rsdt = new ShadowDiscriminatorType();
        rsdt.setIntent(intent);
        rsdt.setKind(kind);
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        resourceRef.setType(ResourceType.COMPLEX_TYPE);
        rsdt.setResourceRef(resourceRef);
        return rsdt;
    }

    public static ResourceShadowDiscriminator fromResourceShadowDiscriminatorType(ShadowDiscriminatorType resourceShadowDiscriminatorType) {
        if (resourceShadowDiscriminatorType == null) {
            return null;
        }
        
        // For compatibility. Otherwise the kind should be explicitly serialized.
        ShadowKindType kind = resourceShadowDiscriminatorType.getKind();
        if (kind == null) {
        	kind = ShadowKindType.ACCOUNT;
        }
        
        return new ResourceShadowDiscriminator(
                resourceShadowDiscriminatorType.getResourceRef() != null ? resourceShadowDiscriminatorType.getResourceRef().getOid() : null,
                kind,
                resourceShadowDiscriminatorType.getIntent());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((intent == null) ? 0 : intent.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + order;
		result = prime * result
				+ ((resourceOid == null) ? 0 : resourceOid.hashCode());
		result = prime * result + (thombstone ? 1231 : 1237);
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
		ResourceShadowDiscriminator other = (ResourceShadowDiscriminator) obj;
		if (intent == null) {
			if (other.intent != null)
				return false;
		} else if (!intent.equals(other.intent))
			return false;
		if (kind != other.kind)
			return false;
		if (order != other.order)
			return false;
		if (resourceOid == null) {
			if (other.resourceOid != null)
				return false;
		} else if (!resourceOid.equals(other.resourceOid))
			return false;
		if (thombstone != other.thombstone)
			return false;
		return true;
	}

	/**
	 * Similar to equals but ignores the order. 
	 */
	public boolean equivalent(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceShadowDiscriminator other = (ResourceShadowDiscriminator) obj;
		if (intent == null) {
			if (other.intent != null)
				return false;
		} else if (!equalsAccountType(this.intent, other.intent))
			return false;
		if (resourceOid == null) {
			if (other.resourceOid != null)
				return false;
		} else if (!resourceOid.equals(other.resourceOid))
			return false;
		if (thombstone != other.thombstone)
			return false;
		return true;
	}
	
	public static boolean equalsAccountType(String a, String b) {
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
	
    @Override
	public String toString() {
    	return "Discr("+toHumanReadableString()+")";
	}
    
    public String toHumanReadableString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(kind.value());
    	sb.append(" (").append(intent).append(") on ");
    	sb.append(resourceOid);
    	if (order != 0) {
    		sb.append(" order=");
    		sb.append(order);
    	}
    	if (thombstone) {
    		sb.append(" THOMBSTONE");
    	}
    	return sb.toString();
    }
}
