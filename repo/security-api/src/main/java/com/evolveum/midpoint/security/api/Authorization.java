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
package com.evolveum.midpoint.security.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.GrantedAuthority;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationEnforcementStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSelectorType;

/**
 * @author semancik
 *
 */
public class Authorization implements GrantedAuthority, DebugDumpable {
	private static final long serialVersionUID = 1L;

	private AuthorizationType authorizationType;
	private String sourceDescription;

	public Authorization(AuthorizationType authorizationType) {
		super();
		this.authorizationType = authorizationType;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.GrantedAuthority#getAuthority()
	 */
	@Override
	public String getAuthority() {
		// this is complex authority. Just return null
		return null;
	}

	public String getDescription() {
		return authorizationType.getDescription();
	}

	public String getSourceDescription() {
		return sourceDescription;
	}

	public void setSourceDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}

	public AuthorizationDecisionType getDecision() {
		 AuthorizationDecisionType decision = authorizationType.getDecision();
		 if (decision == null) {
			 return AuthorizationDecisionType.ALLOW;
		 }
		 return decision;
	}

	public List<String> getAction() {
		return authorizationType.getAction();
	}

	public AuthorizationPhaseType getPhase() {
		return authorizationType.getPhase();
	}

	public AuthorizationEnforcementStrategyType getEnforcementStrategy() {
		return authorizationType.getEnforcementStrategy();
	}

	public boolean maySkipOnSearch() {
		return getEnforcementStrategy() == AuthorizationEnforcementStrategyType.MAY_SKIP_ON_SEARCH;
	}

	public List<OwnedObjectSelectorType> getObject() {
		return authorizationType.getObject();
	}

	@NotNull
	public List<ItemPathType> getItem() {
		return authorizationType.getItem();
	}
	
	@NotNull
	public List<ItemPathType> getExceptItem() {
		return authorizationType.getExceptItem();
	}
	
	@NotNull
	public Collection<ItemPath> getItems() {
		List<ItemPathType> itemPaths = getItem();
		// TODO: maybe we can cache the itemPaths here?
		Collection<ItemPath> items = new ArrayList<>(itemPaths.size());
		for (ItemPathType itemPathType: itemPaths) {
			ItemPath itemPath = itemPathType.getItemPath();
			items.add(itemPath);
		}
		return items;
	}
	
	@NotNull
	public Collection<ItemPath> getExceptItems() {
		List<ItemPathType> itemPaths = getExceptItem();
		// TODO: maybe we can cache the itemPaths here?
		Collection<ItemPath> items = new ArrayList<>(itemPaths.size());
		for (ItemPathType itemPathType: itemPaths) {
			ItemPath itemPath = itemPathType.getItemPath();
			items.add(itemPath);
		}
		return items;
	}
	
	public boolean hasItemSpecification() {
		return !getItem().isEmpty() || !getExceptItem().isEmpty();
	}

	public List<OwnedObjectSelectorType> getTarget() {
		return authorizationType.getTarget();
	}
	
	public List<QName> getRelation() {
		return authorizationType.getRelation();
	}
	
	public AuthorizationLimitationsType getLimitations() {
		return authorizationType.getLimitations();
	}
	
	public Authorization clone() {
		AuthorizationType authorizationTypeClone = authorizationType.clone();
		Authorization clone = new Authorization(authorizationTypeClone);
		clone.sourceDescription = this.sourceDescription;
		return clone;
	}

	public String getHumanReadableDesc() {
		StringBuilder sb = new StringBuilder();
		if (authorizationType.getName() != null) {
			sb.append("authorization '").append(authorizationType.getName()).append("'");
		} else {
			sb.append("unnamed authorization");
		}
		if (sourceDescription != null) {
			sb.append(" in ");
			sb.append(sourceDescription);
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "Authorization", indent);
		if (authorizationType == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			authorizationType.asPrismContainerValue().debugDump(indent+1);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Authorization(" + (authorizationType == null ? "null" : authorizationType.getAction() + ")");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Authorization))
			return false;
		Authorization that = (Authorization) o;
		return Objects.equals(authorizationType, that.authorizationType) &&
				Objects.equals(sourceDescription, that.sourceDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(authorizationType, sourceDescription);
	}
}
