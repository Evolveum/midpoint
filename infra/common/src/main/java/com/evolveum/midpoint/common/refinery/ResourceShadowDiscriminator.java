/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.refinery;

import java.io.Serializable;

import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

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
	
	public ResourceShadowDiscriminator(String resourceOid, String intent) {
		this(resourceOid, intent, false);
	}
	
	public ResourceShadowDiscriminator(String resourceOid, String intent, boolean thombstone) {
		this.resourceOid = resourceOid;
		this.thombstone = thombstone;
		setIntent(intent);
	}
	
	public ResourceShadowDiscriminator(ShadowDiscriminatorType accRefType) {
		this(accRefType.getResourceRef().getOid(), accRefType.getIntent());
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
        return new ResourceShadowDiscriminator(
                resourceShadowDiscriminatorType.getResourceRef() != null ? resourceShadowDiscriminatorType.getResourceRef().getOid() : null,
                resourceShadowDiscriminatorType.getIntent());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((intent == null) ? 0 : intent.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
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
		if (resourceOid == null) {
			if (other.resourceOid != null)
				return false;
		} else if (!resourceOid.equals(other.resourceOid))
			return false;
		if (thombstone != other.thombstone)
			return false;
		return true;
	}

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
		return "Discr(" + kind.value() + " ("+intent+") on "+ resourceOid + ( thombstone ? ", THOMBSTONE" : "" ) + ")";
	}
}
