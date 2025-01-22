/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import jakarta.annotation.PostConstruct;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.prism.panel.MetadataContainerPanel;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 */
@Component
public class PrismContainerWrapperFactoryImpl<C extends Containerable> extends ItemWrapperFactoryImpl<PrismContainerWrapper<C>, PrismContainerValue<C>, PrismContainer<C>, PrismContainerValueWrapper<C>> implements PrismContainerWrapperFactory<C> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperFactoryImpl.class);

    private static final QName VIRTUAL_CONTAINER_COMPLEX_TYPE = new QName("VirtualContainerType");
    public static final QName VIRTUAL_CONTAINER = new QName("virtualContainer");

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public PrismContainerValueWrapper<C> createValueWrapper(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, ValueStatus status, WrapperContext context)
            throws SchemaException {
        PrismContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(parent, value, status, context);
        containerValueWrapper.setExpanded(shouldBeExpanded(parent, value, context));
        containerValueWrapper.setShowEmpty(context.isShowEmpty());

        List<ItemWrapper<?, ?>> children = createChildren(parent, value, containerValueWrapper, context);

        if (context.isForceCreateVirtualContainers() && (parent == null || parent.getParent() == null)) {
            containerValueWrapper.addItems(createVirtualWrappers(containerValueWrapper, context));
        }
        VirtualContainersSpecificationType virtualContainerSpec = null;
        if (parent != null) {
            virtualContainerSpec = context.findVirtualContainerConfiguration(parent.getPath());
        }
        if (virtualContainerSpec != null) {
            //override default expanded settings
            if (virtualContainerSpec.isExpanded() != null) {
                containerValueWrapper.setExpanded(virtualContainerSpec.isExpanded());
            }
            for (ItemWrapper<?, ?> child : children) {
                if (childNotDefined(virtualContainerSpec, child)) {
                    continue;
                }
                containerValueWrapper.addItem(child);
            }
        } else {
            containerValueWrapper.addItems(children);
        }

        containerValueWrapper.setVirtualContainerItems(determineVirtualContainerItems(parent, context));
        if (parent != null && context.getVirtualItemSpecification() != null) {
            parent.setVirtual(true);
            parent.setShowInVirtualContainer(true);
        }

        return containerValueWrapper;
    }

    private List<VirtualContainerItemSpecificationType> determineVirtualContainerItems(PrismContainerWrapper<C> parent, WrapperContext context) {
        if (context.getVirtualItemSpecification() != null) {
            return context.getVirtualItemSpecification();
        }
        for (VirtualContainersSpecificationType virtualContainer : context.getVirtualContainers()) {
            if (virtualContainer.getPath() == null) {
                continue;
            }
            if (parent.getPath().namedSegmentsOnly().equivalent(virtualContainer.getPath().getItemPath())) {
                return virtualContainer.getItem();
            }
        }
        return null;
    }

    private boolean childNotDefined(VirtualContainersSpecificationType virtualContainerSpec, ItemWrapper<?, ?> child) {
        if (virtualContainerSpec.getItem().isEmpty()) {
            return false;
        }
        for (VirtualContainerItemSpecificationType item : virtualContainerSpec.getItem()) {
            if (child.getPath().equivalent(item.getPath().getItemPath())) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    protected List<ItemWrapper<?, ?>> createChildren(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, PrismContainerValueWrapper<C> containerValueWrapper, WrapperContext context) throws SchemaException {
        List<ItemWrapper<?, ?>> wrappers = new ArrayList<>();
        for (ItemDefinition<?> def : getItemDefinitions(parent, value)) {
            addItemWrapper(def, containerValueWrapper, context, wrappers);
        }
        return wrappers;
    }

    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<C> parent, PrismContainerValue<C> value) {
        if (parent == null) {
            Class<C> compileTimeClass = value.getCompileTimeClass();
            if (compileTimeClass == null) {
                return new ArrayList<>();
            }
            PrismContainerDefinition<C> containerDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(compileTimeClass);
            if (containerDef == null) {
                return new ArrayList<>();
            }
            return containerDef.getDefinitions();
        }
        return parent.getDefinitions();
    }

    protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper,
            WrapperContext context, List<ItemWrapper<?, ?>> wrappers) throws SchemaException {

        ItemWrapper<?, ?> wrapper = createChildWrapper(def, containerValueWrapper, context);

        if (wrapper != null) {
            wrappers.add(wrapper);
        }
    }

    protected ItemWrapper<?, ?> createChildWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context) throws SchemaException {
        ItemWrapperFactory<?, ?, ?> factory = getChildWrapperFactory(def, containerValueWrapper.getNewValue());
        ItemWrapper<?, ?> child = factory.createWrapper(containerValueWrapper, def, context);
        if (context.isMetadata() && ItemStatus.ADDED == child.getStatus()) {
            return null;
        }
        return child;
    }

    private ItemWrapperFactory<?, ?, ?> getChildWrapperFactory(ItemDefinition def, PrismContainerValue<?> parentValue) throws SchemaException {
        ItemWrapperFactory<?, ?, ?> factory = getRegistry().findWrapperFactory(def, parentValue);
        if (factory == null) {
            LOGGER.error("Cannot find factory for {}", def);
            throw new SchemaException("Cannot find factory for " + def);
        }

        LOGGER.trace("Found factory {} for {}", factory, def);
        return factory;
    }

    @Override
    protected PrismContainerValue<C> createNewValue(PrismContainer<C> item) {
        return item.createNewValue();
    }

    @Override
    protected PrismContainerWrapper<C> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
            ItemStatus status, WrapperContext ctx) {

        status = recomputeStatus(childContainer, status, ctx);

        PrismContainerWrapper<C> containerWrapper = createWrapper(parent, childContainer, status);
        VirtualContainersSpecificationType virtualContainerSpec = ctx.findVirtualContainerConfiguration(containerWrapper.getPath());
        if (virtualContainerSpec != null) {
            containerWrapper.setVirtual(true);
            containerWrapper.setShowInVirtualContainer(true);
        }

        return containerWrapper;
    }

    protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer, ItemStatus status) {
        return new PrismContainerWrapperImpl<>(parent, childContainer, status);
    }

    protected ItemStatus recomputeStatus(PrismContainer<C> containerWrapper, ItemStatus defaultStatus, WrapperContext ctx) {
        if (isShadowCredentialsOrPassword(containerWrapper.getDefinition(), ctx)) {
            return ItemStatus.NOT_CHANGED;
        }
        return defaultStatus;
    }

    private boolean isShadowCredentialsOrPassword(PrismContainerDefinition<C> childItemDef, WrapperContext ctx) {
        PrismObject<?> object = ctx.getObject();
        if (object == null || !ShadowType.class.equals(object.getCompileTimeClass())) {
            return false;
        }
        QName typeName = childItemDef.getTypeName();
        return QNameUtil.match(typeName, CredentialsType.COMPLEX_TYPE) || QNameUtil.match(typeName, PasswordType.COMPLEX_TYPE);
    }

    @Override
    public void registerWrapperPanel(PrismContainerWrapper<C> wrapper) {
        if (wrapper.isMetadata()) {
            getRegistry().registerWrapperPanel(wrapper.getTypeName(), MetadataContainerPanel.class);
        } else {
            getRegistry().registerWrapperPanel(wrapper.getTypeName(), PrismContainerPanel.class);
        }
    }

    @Override
    public PrismContainerValueWrapper<C> createContainerValueWrapper(PrismContainerWrapper<C> objectWrapper, PrismContainerValue<C> objectValue, ValueStatus status, WrapperContext context) {
        if (isShadowCredentialsOrPassword(objectValue.getDefinition(), context)) {
            status = ValueStatus.NOT_CHANGED;
        }
        return new PrismContainerValueWrapperImpl<>(objectWrapper, objectValue, status);
    }

    protected boolean shouldBeExpanded(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, WrapperContext context) {

        if (context.getVirtualItemSpecification() != null) {
            return true;
        }

        if (value.isEmpty()) {
            return context.isShowEmpty() || containsEmphasizedItems(parent.getDefinitions());
        }

        return true;
    }

    private boolean containsEmphasizedItems(List<? extends ItemDefinition> definitions) {
        for (ItemDefinition def : definitions) {
            if (def.isEmphasized()) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void setupWrapper(PrismContainerWrapper<C> wrapper) {
        boolean expanded = false;
        for (PrismContainerValueWrapper<C> valueWrapper : wrapper.getValues()) {
            if (valueWrapper.isExpanded()) {
                expanded = true;
            }
        }

        wrapper.setExpanded(expanded || wrapper.isSingleValue());
    }

    protected List<ItemWrapper<?, ?>> createVirtualWrappers(PrismContainerValueWrapper<C> objectValueWrapper, WrapperContext context) throws SchemaException {
        List<ItemWrapper<?, ?>> virtualWrappers = new ArrayList<>();
        for (VirtualContainersSpecificationType virtualContainer : context.getVirtualContainers()) {

            if (virtualContainer.getPath() != null) {
                if (virtualContainer.isExpanded() != null) {
                    try {
                        PrismContainerWrapper<Containerable> container =
                                objectValueWrapper.findContainer(virtualContainer.getPath().getItemPath());
                        if (container != null) {
                            container.getValues().forEach(vw -> vw.setExpanded(virtualContainer.isExpanded()));
                            container.setExpanded(virtualContainer.isExpanded());
                        }
                    } catch (Exception e) {
                        //ignore exception
                    }
                }
                continue;
            }

            ComplexTypeDefinition mCtd = getPrismContext().definitionFactory().newComplexTypeDefinition(VIRTUAL_CONTAINER_COMPLEX_TYPE);
            DisplayType display = virtualContainer.getDisplay();

            //TODO: support full polystring -> translations could be defined directly there.
            if (display == null || display.getLabel() == null) {
                mCtd.mutator().setDisplayName("N/A");
            } else {
                mCtd.mutator().setDisplayName(WebComponentUtil.getOrigStringFromPoly(display.getLabel()));
                mCtd.mutator().setHelp(WebComponentUtil.getOrigStringFromPoly(display.getHelp()));
            }

            mCtd.mutator().setRuntimeSchema(true);

            PrismContainerDefinition<?> def =
                    getPrismContext().definitionFactory().newContainerDefinition(VIRTUAL_CONTAINER, mCtd);
            def.mutator().setMaxOccurs(1);
            if (display != null && display.getLabel() != null) {
                if (display.getLabel().getTranslation() != null && StringUtils.isNotEmpty(display.getLabel().getTranslation().getKey())) {
                    def.mutator().setDisplayName(display.getLabel().getTranslation().getKey());
                } else {
                    def.mutator().setDisplayName(WebComponentUtil.getTranslatedPolyString(display.getLabel()));
                }
            }
            def.mutator().setDynamic(true);
            def.mutator().setRuntimeSchema(true);

            ItemWrapperFactory<?, ?, ?> factory = getRegistry().findWrapperFactory(def, null);
            if (factory == null) {
                LOGGER.warn("Cannot find factory for {}. Skipping wrapper creation.", def);
                continue;
            }

            WrapperContext ctx = context.clone();
            ctx.setVirtualItemSpecification(virtualContainer.getItem());

            PrismContainer<?> virtualPrismContainer = def.instantiate();
            ItemStatus virtualContainerStatus = context.getObjectStatus() != null ? context.getObjectStatus() : ItemStatus.NOT_CHANGED;

            ItemWrapper<?, ?> iw = factory.createWrapper(objectValueWrapper, virtualPrismContainer, virtualContainerStatus, ctx);
            if (iw == null) {
                continue;
            }

            if (iw instanceof PrismContainerWrapper<?> cw) {
                cw.setIdentifier(virtualContainer.getIdentifier());
                cw.setVirtual(true);
                if (virtualContainer.isExpanded() != null) {
                    ((PrismContainerWrapper<?>) iw).setExpanded(virtualContainer.isExpanded());
                    cw.getValues().forEach(vw -> vw.setExpanded(virtualContainer.isExpanded()));
                }
            }
            iw.setVisibleOverwrite(virtualContainer.getVisibility());
            virtualWrappers.add(iw);
        }
        return virtualWrappers;
    }

}
