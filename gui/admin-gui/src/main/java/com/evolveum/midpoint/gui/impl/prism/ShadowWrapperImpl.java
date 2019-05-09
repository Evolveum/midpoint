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
package com.evolveum.midpoint.gui.impl.prism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author katka
 *
 */
public class ShadowWrapperImpl<S extends ShadowType> extends PrismObjectWrapperImpl<S> implements ShadowWrapper<S> {

	private static final long serialVersionUID = 1L;
	
	UserDtoStatus status;

	public ShadowWrapperImpl(PrismObject<S> item, ItemStatus status) {
		super(item, status);
	}

	@Override
	public UserDtoStatus getProjectionStatus() {
		return status;
	}

	@Override
	public void setProjectionStatus(UserDtoStatus status) {
		this.status = status;
	}

//	@Override
//	public ObjectDelta<S> getObjectDelta() throws SchemaException {
//		ObjectDelta<S> objectDelta = getPrismContext().deltaFor(getObject().getCompileTimeClass())
//				.asObjectDelta(getObject().getOid());
//
//		Collection<ItemDelta> deltas = new ArrayList<>();
//		for (ItemWrapper<?, ?, ?, ?> itemWrapper : getValue().getItems()) {
//			Collection<ItemDelta> delta = itemWrapper.getDelta();
//			if (delta == null || delta.isEmpty()) {
//				continue;
//			}
//			// objectDelta.addModification(delta);
//			deltas.addAll(delta);
//		}
//
//		switch (getStatus()) {
//			case ADDED:
//				objectDelta.setChangeType(ChangeType.ADD);
//				PrismObject<S> clone = (PrismObject<S>) getOldItem().clone();
//				// cleanupEmptyContainers(clone);
//				for (ItemDelta d : deltas) {
//					d.applyTo(clone);
//				}
//				objectDelta.setObjectToAdd(clone);
//				break;
//			case NOT_CHANGED:
//				objectDelta.mergeModifications(deltas);
//				break;
//			case DELETED:
//				objectDelta.setChangeType(ChangeType.DELETE);
//				break;
//		}
//		// if (ItemStatus.ADDED == getStatus()) {
//		// objectDelta.setObjectToAdd(getObject());
//		// }
//
//		if (objectDelta.isEmpty()) {
//			return null;
//		}
//
//		return objectDelta;
//	}
}
