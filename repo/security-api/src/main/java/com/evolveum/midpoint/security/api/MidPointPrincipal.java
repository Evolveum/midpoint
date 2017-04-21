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
package com.evolveum.midpoint.security.api;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class MidPointPrincipal implements UserDetails,  DebugDumpable {
	
	private static final long serialVersionUID = 8299738301872077768L;
    @NotNull private final UserType user;
    private Collection<Authorization> authorizations = new ArrayList<>();
    private ActivationStatusType effectiveActivationStatus;
    private AdminGuiConfigurationType adminGuiConfiguration;
    private SecurityPolicyType applicableSecurityPolicy;
    // TODO: or a set?
    @NotNull private final Collection<DelegatorWithOtherPrivilegesLimitations> delegatorWithOtherPrivilegesLimitationsCollection = new ArrayList<>();

    public MidPointPrincipal(@NotNull UserType user) {
        Validate.notNull(user, "User must not be null.");
        this.user = user;
    }

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getAuthorities()
	 */
	@Override
	public Collection<Authorization> getAuthorities() {
		return authorizations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getPassword()
	 */
	@Override
	public String getPassword() {
		// We won't return password
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getUsername()
	 */
	@Override
	public String getUsername() {
		return getUser().getName().getOrig();
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonExpired()
	 */
	@Override
	public boolean isAccountNonExpired() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonLocked()
	 */
	@Override
	public boolean isAccountNonLocked() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isCredentialsNonExpired()
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
        if (effectiveActivationStatus == null) {
        	ActivationType activation = user.getActivation();
            if (activation == null) {
            	effectiveActivationStatus = ActivationStatusType.ENABLED;
            } else {
            	effectiveActivationStatus = activation.getEffectiveStatus();
	            if (effectiveActivationStatus == null) {
	            	throw new IllegalArgumentException("Null effective activation status in "+user);
	            }
            }
        }
		return effectiveActivationStatus == ActivationStatusType.ENABLED;
	}

	@NotNull
	public UserType getUser() {
        return user;
    }

    public PolyStringType getName() {
        return getUser().getName();
    }

    public String getFamilyName() {
        PolyStringType string = getUser().getFamilyName();
        return string != null ? string.getOrig() : null;
    }

    public String getFullName() {
        PolyStringType string = getUser().getFullName();
        return string != null ? string.getOrig() : null;
    }

    public String getGivenName() {
        PolyStringType string = getUser().getGivenName();
        return string != null ? string.getOrig() : null;
    }

    public String getOid() {
        return getUser().getOid();
    }

	public AdminGuiConfigurationType getAdminGuiConfiguration() {
		return adminGuiConfiguration;
	}

	public void setAdminGuiConfiguration(AdminGuiConfigurationType adminGuiConfiguration) {
		this.adminGuiConfiguration = adminGuiConfiguration;
	}

	public SecurityPolicyType getApplicableSecurityPolicy() {
		return applicableSecurityPolicy;
	}

	public void setApplicableSecurityPolicy(SecurityPolicyType applicableSecurityPolicy) {
		this.applicableSecurityPolicy = applicableSecurityPolicy;
	}

	@NotNull
	public Collection<DelegatorWithOtherPrivilegesLimitations> getDelegatorWithOtherPrivilegesLimitationsCollection() {
		return delegatorWithOtherPrivilegesLimitationsCollection;
	}

	public void addDelegatorWithOtherPrivilegesLimitations(DelegatorWithOtherPrivilegesLimitations value) {
		delegatorWithOtherPrivilegesLimitationsCollection.add(value);
	}

	/**
	 * Semi-shallow clone.
	 */
	public MidPointPrincipal clone() {
		MidPointPrincipal clone = new MidPointPrincipal(this.user);
		clone.adminGuiConfiguration = this.adminGuiConfiguration;
		clone.applicableSecurityPolicy = this.applicableSecurityPolicy;
		clone.authorizations = cloneAuthorities();
		clone.effectiveActivationStatus = this.effectiveActivationStatus;
		clone.delegatorWithOtherPrivilegesLimitationsCollection.addAll(delegatorWithOtherPrivilegesLimitationsCollection);
		return clone;
	}

	private Collection<Authorization> cloneAuthorities() {
		Collection<Authorization> clone = new ArrayList<>(authorizations.size());
		clone.addAll(authorizations);
		return clone;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "MidPointPrincipal", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "User", user.asPrismObject(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "Authorizations", authorizations, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "Delegators with other privilege limitations", delegatorWithOtherPrivilegesLimitationsCollection, indent + 1);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "MidPointPrincipal(" + user + ", autz=" + authorizations + ")";
	}

    public ObjectReferenceType toObjectReference() {
        if (user.getOid() == null) {
            return null;
        }
        ObjectReferenceType rv = new ObjectReferenceType();
        rv.setType(UserType.COMPLEX_TYPE);
        rv.setOid(user.getOid());
        return rv;
    }
}
