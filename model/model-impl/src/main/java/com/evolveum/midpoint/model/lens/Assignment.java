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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

/**
 * @author semancik
 *
 */
public class Assignment implements DebugDumpable, Dumpable {

	private Collection<AccountConstruction> accountConstructions;
	private Collection<PrismObject<OrgType>> orgs;

	public Assignment() {
		accountConstructions = new ArrayList<AccountConstruction>();
		orgs = new ArrayList<PrismObject<OrgType>>();
	}
	
	public Collection<AccountConstruction> getAccountConstructions() {
		return accountConstructions;
	}

	public void addAccountConstruction(AccountConstruction accpuntContruction) {
		accountConstructions.add(accpuntContruction);
	}
	
	public Collection<PrismObject<OrgType>> getOrgs() {
		return orgs;
	}

	public void addOrg(PrismObject<OrgType> org) {
		orgs.add(org);
	}

	public Collection<ResourceType> getResources(OperationResult result) throws ObjectNotFoundException, SchemaException {
		Collection<ResourceType> resources = new ArrayList<ResourceType>();
		for (AccountConstruction acctConstr: accountConstructions) {
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
		if (!accountConstructions.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Accounts", indent+1);
			for (AccountConstruction ac: accountConstructions) {
				sb.append("\n");
				sb.append(ac.debugDump(indent+2));
			}
		}
		if (!orgs.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Orgs", indent+1);
			for (PrismObject<OrgType> org: orgs) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(org.toString());
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Assignment(acc=" + accountConstructions + "; org="+orgs+")";
	}
	
}
