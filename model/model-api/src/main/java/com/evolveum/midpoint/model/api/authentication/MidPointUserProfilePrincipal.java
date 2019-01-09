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
package com.evolveum.midpoint.model.api.authentication;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_4.PolyStringType;

/**
 * Principal that extends simple MidPointPrincipal with user interface concepts (user profile).
 * 
 * @since 4.0
 * @author Radovan Semancik
 */
public class MidPointUserProfilePrincipal extends MidPointPrincipal {
	private static final long serialVersionUID = 1L;
	
	private CompiledUserProfile compiledUserProfile;	

    public MidPointUserProfilePrincipal(@NotNull UserType user) {
    	super(user);
    }
    
    @NotNull
	public CompiledUserProfile getCompiledUserProfile() {
		if (compiledUserProfile == null) {
			compiledUserProfile = new CompiledUserProfile();
		}
		return compiledUserProfile;
	}

	public void setCompiledUserProfile(CompiledUserProfile compiledUserProfile) {
		this.compiledUserProfile = compiledUserProfile;
	}

	/**
	 * Semi-shallow clone.
	 */
	public MidPointUserProfilePrincipal clone() {
		MidPointUserProfilePrincipal clone = new MidPointUserProfilePrincipal(this.getUser());
		copyValues(clone);
		return clone;
	}
	
	protected void copyValues(MidPointUserProfilePrincipal clone) {
		super.copyValues(clone);
		// No need to clone user profile here. It is essentially read-only.
		clone.compiledUserProfile = this.compiledUserProfile;
	}
	
	@Override
	protected void debugDumpInternal(StringBuilder sb, int indent) {
		super.debugDumpInternal(sb, indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "compiledUserProfile", compiledUserProfile, indent + 1);
	}

}
