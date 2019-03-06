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
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public class GenericHandlerDto extends HandlerDto {

	private static final Trace LOGGER = TraceManager.getTrace(GenericHandlerDto.class);

	public static final String F_CONTAINER = "container";

	public static class ExtensionItem implements Serializable {
		@NotNull private final QName name;
		@NotNull private final Class<?> type;

		public ExtensionItem(@NotNull QName name, @NotNull Class<?> type) {
			this.name = name;
			this.type = type;
		}
	}

	public static ExtensionItem extensionItem(QName name, Class<?> type) {
		return new ExtensionItem(name, type);
	}

	private final ContainerWrapperImpl containerWrapper;

	public GenericHandlerDto(TaskDto taskDto, @NotNull List<ExtensionItem> extensionItems, PageBase pageBase) {
		super(taskDto);
		PrismContext prismContext = pageBase.getPrismContext();
		ContainerWrapperFactory cwf = new ContainerWrapperFactory(pageBase);

		PrismContainer container = prismContext.itemFactory().createContainer(new QName("test"));
		ComplexTypeDefinition ctd = prismContext.definitionFactory().createComplexTypeDefinition(new QName("Test"));
		int displayOrder = 1;
		for (ExtensionItem extensionItem : extensionItems) {
			Item<?,?> item = taskDto.getExtensionItem(extensionItem.name);
			MutableItemDefinition<?> clonedDefinition = null;
			if (item != null) {
				try {
					Item<?,?> clonedItem = item.clone();
					//noinspection unchecked
					container.add(clonedItem);
					if (clonedItem.getDefinition() != null) {
						clonedDefinition = clonedItem.getDefinition().clone().toMutable();
						//noinspection unchecked
						((Item) clonedItem).setDefinition(clonedDefinition);
					}
				} catch (SchemaException e) {
					throw new SystemException(e);
				}
			}
			if (clonedDefinition == null) {
				ItemDefinition definition = prismContext.getSchemaRegistry().findItemDefinitionByElementName(extensionItem.name);
				if (definition != null) {
					clonedDefinition = CloneUtil.clone(definition).toMutable();
				}
			}
			if (clonedDefinition == null) {
				LOGGER.warn("Extension item without definition: {} of {}", extensionItem.name, extensionItem.type);
			} else {
				clonedDefinition.setCanAdd(false);
				clonedDefinition.setCanModify(false);
				clonedDefinition.setDisplayOrder(displayOrder);
				ctd.toMutable().add(clonedDefinition);
			}
			displayOrder++;
		}
		MutablePrismContainerDefinition<?> containerDefinition = prismContext.definitionFactory().createContainerDefinition(new QName("Handler data"), ctd);
		//noinspection unchecked
		container.setDefinition(containerDefinition);
		Task task = pageBase.createSimpleTask("Adding new container wrapper");
		//noinspection unchecked
		containerWrapper = cwf.createContainerWrapper(null, container, ContainerStatus.MODIFYING, ItemPath.EMPTY_PATH, true, task);
	}

	public ContainerWrapperImpl getContainer() {
		return containerWrapper;
	}
}
