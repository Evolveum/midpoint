/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import java.io.Serializable;
import java.util.List;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.ItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

        @NotNull
        public QName getName() {
            return name;
        }

        @NotNull
        public Class<?> getType() {
            return type;
        }
    }

    public static ExtensionItem extensionItem(QName name, Class<?> type) {
        return new ExtensionItem(name, type);
    }

    private PrismContainerWrapper containerWrapper = null;

    public GenericHandlerDto(@NotNull List<ExtensionItem> extensionItems, PageBase pageBase) {
        PrismContext prismContext = pageBase.getPrismContext();

        PrismContainer container = prismContext.itemFactory().createContainer(new QName("test"));
        ComplexTypeDefinition ctd = prismContext.definitionFactory().createComplexTypeDefinition(new QName("Test"));
        int displayOrder = 1;
        for (ExtensionItem extensionItem : extensionItems) {
            Item<?,?> item = null;//taskDto.getExtensionItem(extensionItem.name);
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
        ItemWrapperFactory factory = pageBase.findWrapperFactory(containerDefinition);
        if (factory instanceof PrismContainerWrapperFactory) {

            PrismContainerWrapperFactory containerF = (PrismContainerWrapperFactory) factory;

            WrapperContext ctx = new WrapperContext(task, task.getResult());
        try {
            containerWrapper = (PrismContainerWrapper) containerF.createWrapper(container, ItemStatus.NOT_CHANGED, ctx);
        } catch (SchemaException e) {
            LOGGER.error("Error creating wrapper for {}", container);
        }
        }
    }

    public PrismContainerWrapper getContainer() {
        return containerWrapper;
    }
}
