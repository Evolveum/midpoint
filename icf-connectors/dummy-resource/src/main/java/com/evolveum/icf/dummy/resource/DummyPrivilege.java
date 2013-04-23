/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.icf.dummy.resource;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

/**
 * @author Radovan Semancik
 *
 */
public class DummyPrivilege extends DummyObject {
	
	public DummyPrivilege() {
		super();
	}

	public DummyPrivilege(String username) {
		super(username);
	}
		
	@Override
	protected DummyObjectClass getObjectClass() {
		return resource.getGroupObjectClass();
	}

	@Override
	public String getShortTypeName() {
		return "priv";
	}

	@Override
	public String toStringContent() {
		return super.toStringContent(); 
	}

	
}
