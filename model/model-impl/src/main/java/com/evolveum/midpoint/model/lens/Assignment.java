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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author semancik
 *
 */
public class Assignment implements DebugDumpable, Dumpable {

	private Collection<Construction> constructions;
	private Collection<PrismReferenceValue> orgRefVals;
	private Collection<Authorization> authorizations;

	public Assignment() {
		constructions = new ArrayList<Construction>();
		orgRefVals = new ArrayList<PrismReferenceValue>();
		authorizations = new ArrayList<Authorization>();
	}
	
	public Collection<Construction> getConstructions() {
		return constructions;
	}

	public void addConstruction(Construction accpuntContruction) {
		constructions.add(accpuntContruction);
	}
	
	public Collection<PrismReferenceValue> getOrgRefVals() {
		return orgRefVals;
	}

	public void addOrgRefVal(PrismReferenceValue org) {
		orgRefVals.add(org);
	}

	public Collection<Authorization> getAuthorizations() {
		return authorizations;
	}
	
	public void addAuthorization(Authorization authorization) {
		authorizations.add(authorization);
	}

	public Collection<ResourceType> getResources(OperationResult result) throws ObjectNotFoundException, SchemaException {
		Collection<ResourceType> resources = new ArrayList<ResourceType>();
		for (Construction acctConstr: constructions) {
			resources.add(acctConstr.getResource(result));
		}
		return resources;
	}

	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "Assignment", indent);
		if (!constructions.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Constructions", indent+1);
			for (Construction ac: constructions) {
				sb.append("\n");
				sb.append(ac.debugDump(indent+2));
			}
		}
		if (!orgRefVals.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Orgs", indent+1);
			for (PrismReferenceValue org: orgRefVals) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(org.toString());
			}
		}
		if (!authorizations.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Authorizations", indent+1);
			for (Authorization autz: authorizations) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(autz.toString());
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Assignment(acc=" + constructions + "; org="+orgRefVals+"; autz="+authorizations+")";
	}
	
}
