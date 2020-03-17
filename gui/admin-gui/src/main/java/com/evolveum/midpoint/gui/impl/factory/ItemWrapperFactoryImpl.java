/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperFactoryImpl<IW extends ItemWrapper, PV extends PrismValue, I extends Item, VW extends PrismValueWrapper> implements ItemWrapperFactory<IW, VW, PV> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ItemWrapperFactoryImpl.class);

    @Autowired private GuiComponentRegistryImpl registry;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelInteractionService modelInteractionService;

    @Override
    public IW createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        ItemName name = def.getItemName();


        I childItem = null;
        ItemStatus status = null;
        if (CollectionUtils.isNotEmpty(context.getVirtualItemSpecification())) {
            childItem = (I) def.instantiate();
            status = ItemStatus.NOT_CHANGED;
        } else {
            childItem = (I) parent.getNewValue().findItem(name);
            status = getStatus(childItem);
        }

        if (!skipCreateWrapper(def, status, context, childItem == null || CollectionUtils.isEmpty(childItem.getValues()))) {
            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
            return null;
        }

        if (childItem == null) {
            childItem = (I) parent.getNewValue().findOrCreateItem(name);
            childItem.setDefinition(def);
        }

        return createWrapper(parent, childItem, status, context);
    }

    private ItemStatus getStatus(I childItem) {
        if (childItem == null) {
            return ItemStatus.ADDED;
        }

        return ItemStatus.NOT_CHANGED;

    }


    public IW createWrapper(Item childItem, ItemStatus status, WrapperContext context) throws SchemaException {
        return createWrapper(null, (I) childItem, status, context);

    };

    private IW createWrapper(PrismContainerValueWrapper<?> parent, I childItem, ItemStatus status, WrapperContext context) throws SchemaException {
        IW itemWrapper = createWrapper(parent, childItem, status);

        List<VW> valueWrappers  = createValuesWrapper(itemWrapper, (I) childItem, context);
        itemWrapper.getValues().addAll((Collection) valueWrappers);
        itemWrapper.setShowEmpty(context.isShowEmpty(), false);

        boolean readOnly = determineReadOnly(itemWrapper, context);
        itemWrapper.setReadOnly(readOnly);

        boolean showInVirtualContainer = determineShowInVirtualContainer(itemWrapper, context);
        itemWrapper.setShowInVirtualContainer(showInVirtualContainer);

        setupWrapper(itemWrapper);

        return itemWrapper;
    }

    protected abstract void setupWrapper(IW wrapper);

    protected <ID extends ItemDefinition<I>> List<VW> createValuesWrapper(IW itemWrapper, I item, WrapperContext context) throws SchemaException {
        List<VW> pvWrappers = new ArrayList<>();

        ID definition = (ID) item.getDefinition();

        //TODO : prismContainer.isEmpty() interates and check is all prismcontainervalues are empty.. isn't it confusing?
        if (item.isEmpty() && item.getValues().isEmpty()) {
            if (shouldCreateEmptyValue(item, context)) {
                PV prismValue = createNewValue(item);
                VW valueWrapper =  createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
                pvWrappers.add(valueWrapper);
            }
            return pvWrappers;
        }

        for (PV pcv : (List<PV>)item.getValues()) {
            if(canCreateValueWrapper(pcv)){
                VW valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
                pvWrappers.add(valueWrapper);
            }
        }

        return pvWrappers;

    }

    private boolean skipCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        if (QNameUtil.match(FocusType.F_LINK_REF, def.getItemName()) || QNameUtil.match(FocusType.F_PERSONA_REF, def.getItemName())) {
            LOGGER.trace("Skip creating wrapper for {}, it is not supported", def);
            return false;
        }

        if (ItemProcessing.IGNORE == def.getProcessing()) {
            LOGGER.trace("Skip creating wrapper for {}, because item processig is set to IGNORE.", def);
            return false;
        }

        if (def.isExperimental() && !WebModelServiceUtils.isEnableExperimentalFeature(modelInteractionService, context.getTask(), context.getResult())) {
            LOGGER.trace("Skipping creating wrapper for {}, because experimental GUI features are turned off.", def);
            return false;
        }


        if (ItemStatus.ADDED == status && def.isDeprecated()) {
            LOGGER.trace("Skipping creating wrapeer for {}, because item is deprecated and doesn't contain any value.", def);
            return false;
        }

        if (ItemStatus.ADDED == context.getObjectStatus() && !def.canAdd()) {
            LOGGER.trace("Skipping creating wrapper for {}, becasue ADD operation is not supported");
            return false;
        }

        if (ItemStatus.NOT_CHANGED == context.getObjectStatus()) {
            if (!def.canRead()) {
                LOGGER.trace("Skipping creating wrapper for {}, because read operation is not supported");
                return false;
            }

        }

        return canCreateWrapper(def, status, context, isEmptyValue);
    }

    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        if (!context.isCreateOperational() && def.isOperational()) {
            LOGGER.trace("Skipping creating wrapper for {}, because it is operational.", def.getItemName());
            return false;
        }

        return true;
    }

    private boolean determineReadOnly(IW itemWrapper, WrapperContext context) {

        Boolean readOnly = context.getReadOnly();
        if (readOnly != null) {
            LOGGER.trace("Setting {} as readonly because context said so.", itemWrapper);
            return readOnly.booleanValue();
        }

        ItemStatus objectStatus = context.getObjectStatus();

        if (ItemStatus.NOT_CHANGED == objectStatus) {
            if (!itemWrapper.canModify()) {
                LOGGER.trace("Setting {} as readonly because authZ said so");
                return true;
            }
        }

        return false;
    }

    private boolean determineShowInVirtualContainer(IW itemWrapper, WrapperContext context) {
        List<VirtualContainersSpecificationType> virtualContainers = context.getVirtualContainers();

        if (virtualContainers == null) {
            return false;
        }

        for (VirtualContainersSpecificationType virtualContainer : virtualContainers) {
            for (VirtualContainerItemSpecificationType item : virtualContainer.getItem()) {
                ItemPathType itemPathType = item.getPath();
                if (itemPathType == null) {
                    LOGGER.error("Bad virtual item specification, missing path. Skipping virtual item settings for {}", itemWrapper);
                    continue;
                }
                ItemPath itemPath = itemPathType.getItemPath();
                if (itemPath.equivalent(itemWrapper.getPath())) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean canCreateValueWrapper(PV pcv) {
        return true;
    }


    protected abstract PV createNewValue(I item) throws SchemaException;

    protected abstract IW createWrapper(PrismContainerValueWrapper<?> parent, I childContainer, ItemStatus status);

    protected boolean shouldCreateEmptyValue(I item, WrapperContext context) {
        if (item.getDefinition().isEmphasized()) {
            return true;
        }

        if (context.isCreateIfEmpty()) {
            return true;
        }

        return true;
    }

    /**
     * @return the registry
     */
    public GuiComponentRegistryImpl getRegistry() {
        return registry;
    }

    /**
     * @return the prismContext
     */
    public PrismContext getPrismContext() {
        return prismContext;
    }


//    @Override
//    public VW createValueWrapper(PV value, IW parent, ValueStatus status, WrapperContext context) throws SchemaException {
//        // TODO Auto-generated method stub
//        return null;
//    }

}
