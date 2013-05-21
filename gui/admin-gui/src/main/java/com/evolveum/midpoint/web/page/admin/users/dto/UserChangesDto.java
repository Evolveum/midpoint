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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author mserbak
 */
public class UserChangesDto implements Serializable {
	private List<SubmitDeltaObjectDto> assignmentsList = new ArrayList<SubmitDeltaObjectDto>();
	private List<SubmitDeltaObjectDto> userPropertiesList = new ArrayList<SubmitDeltaObjectDto>();
	private PrismObject oldUserObject;

	public UserChangesDto(ModelElementContext userChanges) {
		this.oldUserObject = userChanges.getObjectOld();
		getChanges(userChanges.getPrimaryDelta(), false);
		getChanges(userChanges.getSecondaryDelta(), true);
	}

	private void getChanges(ObjectDelta delta, boolean secondaryValue) {
		if (delta == null) {
			return;
		}

		ItemPath account = new ItemPath(UserType.F_LINK_REF);

		if (delta.getChangeType().equals(ChangeType.DELETE)) {

			for (Object value : oldUserObject.getValues()) {

				PrismContainerValue items = (PrismContainerValue) value;

				for (Object item : items.getItems()) {
					Item prismItem = (Item) item;
					if (prismItem instanceof PrismProperty
							&& prismItem.getDefinition().getName().equals(ObjectType.F_NAME)) {

						PrismProperty property = (PrismProperty) prismItem;
						PropertyDelta propertyDelta = new PropertyDelta(property.getDefinition());
						propertyDelta.addValuesToDelete(PrismPropertyValue.cloneCollection(property
								.getValues()));
						userPropertiesList.add(new SubmitDeltaObjectDto(propertyDelta, secondaryValue));

					}

					if (prismItem instanceof PrismContainer
							&& prismItem.getDefinition().getTypeName().equals(AssignmentType.COMPLEX_TYPE)) {
						PrismContainer assign = (PrismContainer) item;
						ContainerDelta assignDelta = new ContainerDelta(assign.getDefinition());
						assignDelta
								.addValuesToDelete(PrismContainerValue.cloneCollection(assign.getValues()));
						assignmentsList.add(new SubmitDeltaObjectDto(assignDelta, secondaryValue));
					}
				}
			}

			return;
		} else if (delta.getChangeType().equals(ChangeType.MODIFY)) {
			for (Object item : delta.getModifications()) {
				ItemDelta itemDelta = (ItemDelta) item;
				if (itemDelta.getPath().equals(account)) {
					continue;
				} else if (itemDelta instanceof ContainerDelta) {
					assignmentsList.add(new SubmitDeltaObjectDto((ContainerDelta) itemDelta, secondaryValue));
				} else if (itemDelta instanceof ReferenceDelta) {
					assignmentsList.add(new SubmitDeltaObjectDto((ReferenceDelta) itemDelta, secondaryValue));
				} else {
					userPropertiesList
							.add(new SubmitDeltaObjectDto((PropertyDelta) itemDelta, secondaryValue));
				}
			}
		} else {
			for (Object value : delta.getObjectToAdd().getValues()) {
				PrismContainerValue prismValue = (PrismContainerValue) value;

				if (prismValue.getItems() == null) {
					continue;
				}

				for (Object itemObject : prismValue.getItems()) {
					Item item = (Item) itemObject;
					if (item instanceof PrismProperty) {
						PrismProperty property = (PrismProperty) item;
						PropertyDelta propertyDelta = new PropertyDelta(property.getDefinition());
						propertyDelta
								.addValuesToAdd(PrismPropertyValue.cloneCollection(property.getValues()));
						userPropertiesList.add(new SubmitDeltaObjectDto(propertyDelta, secondaryValue));
					} else if (item instanceof PrismContainer) {

						if (!item.getDefinition().getTypeName().equals(AssignmentType.COMPLEX_TYPE)) {
							PrismContainer property = (PrismContainer) item;
							PrismContainerDefinition def = property.getDefinition();

							PrismPropertyDefinition propertyDef = new PrismPropertyDefinition(def.getName(),
									def.getDefaultName(), def.getTypeName(), def.getPrismContext());
							PropertyDelta propertyDelta = new PropertyDelta(propertyDef);
							propertyDelta.addValuesToAdd(PrismContainerValue.cloneCollection(property
									.getValues()));
							userPropertiesList.add(new SubmitDeltaObjectDto(propertyDelta, secondaryValue));
							continue;
						}
						PrismContainer assign = (PrismContainer) item;
						ContainerDelta assignDelta = new ContainerDelta(assign.getDefinition());
						assignDelta.addValuesToAdd(PrismContainerValue.cloneCollection(assign.getValues()));
						assignmentsList.add(new SubmitDeltaObjectDto(assignDelta, secondaryValue));
					}
				}
			}
		}
	}

	public List<SubmitDeltaObjectDto> getAssignmentsList() {
		return assignmentsList;
	}

	public List<SubmitDeltaObjectDto> getUserPropertiesList() {
		return userPropertiesList;
	}
}
