/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.dto.ObjectDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.Validate;

public class ObjectTypeCatalogImpl implements ObjectTypeCatalog {

	private static final long serialVersionUID = -8993202087773409957L;
	private Map<Class<? extends ObjectType>, ObjectManager> managers = new HashMap<Class<? extends ObjectType>, ObjectManager>();

	@Override
	public Set<Class<? extends ObjectType>> listSupportedObjectTypes() {
		Set<Class<? extends ObjectType>> supportedObjectTypes = managers.keySet();
		return supportedObjectTypes;
	}

	public <T extends ObjectType> void add(Class<T> type, ObjectManager<?> objectManager) {
		managers.put(type, objectManager);
	}

	public void setSupportedObjectManagers(Map<Class<? extends ObjectType>, ObjectManager> objectManagers) {
		Validate.notNull(objectManagers);
		managers = objectManagers;
	}

	@Override
	public <T extends ObjectDto, C extends T> ObjectManager<T> getObjectManager(Class<T> managerType,
			Class<C> dtoType) {
		return managers.get(dtoType);
	}
}
