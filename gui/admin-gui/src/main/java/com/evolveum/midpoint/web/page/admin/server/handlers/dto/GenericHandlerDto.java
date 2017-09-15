/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class GenericHandlerDto extends HandlerDto {

	public static final String F_CONTAINER = "container";

	public static class Item implements Serializable {
		@NotNull private QName name;
		@NotNull private Class<?> type;

		public Item(@NotNull QName name, @NotNull Class<?> type) {
			this.name = name;
			this.type = type;
		}
	}

	public static Item item(QName name, Class<?> type) {
		return new Item(name, type);
	}

	@NotNull private List<Item> items;
	private final List<ItemWrapper> propertyWrappers = new ArrayList<>();
	private final ContainerWrapper containerWrapper;

	public GenericHandlerDto(TaskDto taskDto, @NotNull List<Item> items, PageBase pageBase) {
		super(taskDto);
		this.items = items;
		for (Item item : items) {
			PrismProperty<?> property = taskDto.getExtensionProperty(item.name);
			if (property != null) {
				PropertyWrapper<?, ?> propertyWrapper = new PropertyWrapper<>(null, property, true, ValueStatus.NOT_CHANGED);
				propertyWrappers.add(propertyWrapper);
			} else {
				// TODO create empty property?
			}
		}
		ContainerWrapperFactory cwf = new ContainerWrapperFactory(pageBase);

		final PrismContext prismContext = pageBase.getPrismContext();
		PrismContainer container = new PrismContainer(new QName("test"), prismContext);
		ComplexTypeDefinitionImpl ctd = new ComplexTypeDefinitionImpl(new QName("Test"), prismContext);
		int displayOrder = 1;
		for (Item item : items) {
			PrismProperty<?> property = taskDto.getExtensionProperty(item.name);
			PrismPropertyDefinitionImpl<?> clonedDefinition = null;
			if (property != null) {
				try {
					PrismProperty<?> clonedProperty = property.clone();
					container.add(clonedProperty);
					if (clonedProperty.getDefinition() != null) {
						clonedDefinition = (PrismPropertyDefinitionImpl) clonedProperty.getDefinition().clone();
						clonedProperty.setDefinition((PrismPropertyDefinition) clonedDefinition);
					}
				} catch (SchemaException e) {
					throw new SystemException(e);
				}
			}
			if (clonedDefinition == null) {
				clonedDefinition = CloneUtil.clone((PrismPropertyDefinitionImpl) prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(item.name));
			}
			if (clonedDefinition == null) {
				System.out.println("Definition-less property " + item.name);        // TODO
			} else {
				clonedDefinition.setCanAdd(false);
				clonedDefinition.setCanModify(false);
				clonedDefinition.setDisplayOrder(displayOrder);
				ctd.add(clonedDefinition);
			}
			displayOrder++;
		}
		PrismContainerDefinition<?> containerDefinition = new PrismContainerDefinitionImpl<>(new QName("Handler data"), ctd, prismContext);
		container.setDefinition(containerDefinition);
		containerWrapper = cwf.createContainerWrapper(container, ContainerStatus.MODIFYING, ItemPath.EMPTY_PATH, true);
	}

	public ContainerWrapper getContainer() {
		return containerWrapper;
	}
}
