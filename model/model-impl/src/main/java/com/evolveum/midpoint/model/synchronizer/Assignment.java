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
package com.evolveum.midpoint.model.synchronizer;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * @author semancik
 *
 */
public class Assignment implements DebugDumpable, Dumpable {

	private Collection<AccountConstruction> accountConstructions;

	public Assignment() {
		accountConstructions = new ArrayList<AccountConstruction>();
	}
	
	public Collection<AccountConstruction> getAccountConstructions() {
		return accountConstructions;
	}

	public void addAccountConstruction(AccountConstruction accpuntContruction) {
		accountConstructions.add(accpuntContruction);
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
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("Assignment");
		for (AccountConstruction ac: accountConstructions) {
			sb.append("\n");
			sb.append(ac.debugDump(indent+1));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Assignment(" + accountConstructions + ")";
	}
	
}
