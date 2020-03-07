/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ShadowAssociationReferenceWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ShadowAssociationWrapperImpl;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
@Component
public class ShadowAssociationWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C>{

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAssociationWrapperFactoryImpl.class);

    @Autowired private GuiComponentRegistry registry;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;
    @Autowired private TaskManager taskManager;

    private static final String CREATE_ASSOCIATION_WRAPPER = "createAssociationWrapper";

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), ShadowAssociationType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def,
            WrapperContext context) throws SchemaException {
        ItemName name = def.getItemName();

        PrismContainer<C> childItem = (PrismContainer) parent.getNewValue().findItem(name);
        ItemStatus status = ItemStatus.NOT_CHANGED;
        if (childItem == null) {
            childItem = (PrismContainer) parent.getNewValue().findOrCreateItem(name);
            status = ItemStatus.ADDED;
        }

        PrismContainerWrapper<C> itemWrapper = createWrapper(parent, childItem, status);
        itemWrapper.setShowEmpty(context.isCreateIfEmpty(), false);
        return itemWrapper;
    }


    protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
            ItemStatus status) {

        try {
            ObjectType objectType = (ObjectType) parent.getRealValue();
            ShadowType shadow;
            if (objectType instanceof ShadowType) {
                shadow = (ShadowType) objectType;
            } else {
                throw new SchemaException("Something very strange happenned. Association contianer in the " + objectType.getClass().getSimpleName() + "?");
            }

            if(shadow.getResourceRef().getOid() == null) {
                return super.createWrapper(parent, childContainer, status);
            }
            Task task = taskManager.createTaskInstance("Load resource ref");
            OperationResult result = task.getResult();
            PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, shadow.getResourceRef().getOid(), null, task, result);

            result.computeStatusIfUnknown();
            if (!result.isAcceptable()) {
                LOGGER.error("Cannot find resource referenced from shadow. {}", result.getMessage());
                result.recordPartialError("Could not find resource referenced from shadow.");
                return super.createWrapper(parent, childContainer, status);
            }

            ShadowKindType kind = shadow.getKind();
            String shadowIntent = shadow.getIntent();
            PrismContainer<C> association = childContainer;

            if (association == null || association.getDefinition() == null
                    || (!(association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class))
                    && !(association.getDefinition().getCompileTimeClass().equals(ResourceObjectAssociationType.class)))){
                LOGGER.debug("Association for {} is not supported", association.getComplexTypeDefinition().getTypeClass());
                return super.createWrapper(parent, childContainer, status);
            }
            result = new OperationResult(CREATE_ASSOCIATION_WRAPPER);
            //we need to switch association wrapper to single value
            //the transformation will be as following:
            // we have single value ShadowAssociationType || ResourceObjectAssociationType, and from each shadowAssociationType we will create
            // property - name of the property will be association type(QName) and the value will be shadowRef
            PrismContainerDefinition<C> associationDefinition = association.getDefinition().clone();
            associationDefinition.toMutable().setMaxOccurs(1);

            RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
            RefinedObjectClassDefinition oc = refinedResourceSchema.getRefinedDefinition(kind, shadowIntent);
            if (oc == null) {
                LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
                return super.createWrapper(parent, childContainer, status);
            }
            Collection<RefinedAssociationDefinition> refinedAssociationDefinitions = oc.getAssociationDefinitions();

            if (CollectionUtils.isEmpty(refinedAssociationDefinitions)) {
                LOGGER.debug("Association for {}/{} not supported by resource {}", kind, shadowIntent, resource);
                return super.createWrapper(parent, childContainer, status);
            }

            PrismContainer associationTransformed = associationDefinition.instantiate();
            PrismContainerWrapper associationWrapper;
            if (association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class)) {
                registry.registerWrapperPanel(associationTransformed.getDefinition().getTypeName(), PrismContainerPanel.class);
                associationWrapper = new ShadowAssociationWrapperImpl((PrismContainerValueWrapper<C>) parent, associationTransformed, status);
            } else {
                return super.createWrapper(parent, childContainer, status);
            }

            WrapperContext context = new WrapperContext(task, result);
            context.setShowEmpty(ItemStatus.ADDED == status);
            PrismContainerValueWrapper<ShadowAssociationType> shadowValueWrapper = createContainerValueWrapper(associationWrapper,
                    associationTransformed.createNewValue(),
                    ItemStatus.ADDED == status ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, context);

            List<ItemWrapper<?,?,?,?>> items = new ArrayList<>();
            for (RefinedAssociationDefinition refinedAssociationDefinition: refinedAssociationDefinitions) {
                MutablePrismReferenceDefinition shadowRefDef = prismContext
                        .definitionFactory().createReferenceDefinition(refinedAssociationDefinition.getName(), ObjectReferenceType.COMPLEX_TYPE);
                shadowRefDef.toMutable().setMaxOccurs(-1);
                shadowRefDef.setTargetTypeName(ShadowType.COMPLEX_TYPE);
                PrismReference shadowAss = shadowRefDef.instantiate();
                ItemPath itemPath = null;
                for (PrismContainerValue<C> associationValue : association.getValues()) {
                    if (association.getDefinition().getCompileTimeClass().equals(ShadowAssociationType.class)) {
                        ShadowAssociationType shadowAssociation = (ShadowAssociationType)associationValue.asContainerable();
                        if (shadowAssociation.getName().equals(refinedAssociationDefinition.getName())) {
                            itemPath = associationValue.getPath();
                            shadowAss.add(associationValue.findReference(ShadowAssociationType.F_SHADOW_REF).getValue().clone());
                        }
                    }
                }

                if (itemPath == null) {
                    itemPath = ShadowType.F_ASSOCIATION;
                }

                String displayName = refinedAssociationDefinition.getDisplayName();
                if (StringUtils.isBlank(displayName)) {
                    displayName = refinedAssociationDefinition.getName().getLocalPart();
                }

                ShadowAssociationReferenceWrapperImpl item = new ShadowAssociationReferenceWrapperImpl(shadowValueWrapper, shadowAss,
                        shadowAss.isEmpty() ? ItemStatus.ADDED : ItemStatus.NOT_CHANGED);
                item.setDisplayName(displayName);
                List<PrismReferenceValueWrapperImpl> refValues = new ArrayList<PrismReferenceValueWrapperImpl>();
                for(PrismReferenceValue prismValue : shadowAss.getValues()) {
                    PrismReferenceValueWrapperImpl refValue = new PrismReferenceValueWrapperImpl(item, prismValue,
                            prismValue.isEmpty() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED);
                    refValue.setEditEnabled(isEmpty(prismValue));
                    refValues.add(refValue);
                }
                if (shadowAss.getValues().isEmpty()) {
                    PrismReferenceValue prismReferenceValue = getPrismContext().itemFactory().createReferenceValue();
                    shadowAss.add(prismReferenceValue);
                    PrismReferenceValueWrapperImpl refValue = new PrismReferenceValueWrapperImpl(item, prismReferenceValue,
                            shadowAss.getValue().isEmpty() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED);
                    refValue.setEditEnabled(true);
                    refValues.add(refValue);
                }
                item.getValues().addAll((Collection)refValues);
                item.setFilter(WebComponentUtil.createAssociationShadowRefFilter(refinedAssociationDefinition,
                        prismContext, resource.getOid()));
//                item.setReadOnly(true);

                items.add(item);
            }
            shadowValueWrapper.setExpanded(true);
            shadowValueWrapper.getItems().addAll((Collection)items);
            associationWrapper.getValues().addAll(Arrays.asList(shadowValueWrapper));
            return associationWrapper;
        } catch (Exception e) {
            LOGGER.error("Couldn't create container for associations. ", e);
        }
        return null;
    }

    private boolean isEmpty(PrismReferenceValue prismValue) {
        if (prismValue == null) {
            return true;
        }

        return prismValue.isEmpty();

    }

    private <C extends Containerable> boolean isItemReadOnly(ItemDefinition def, PrismContainerValueWrapper<C> cWrapper) {
        if (cWrapper == null || cWrapper.getStatus() == ValueStatus.NOT_CHANGED) {

            return cWrapper.isReadOnly() || !def.canModify();
        }

        return cWrapper.isReadOnly() || !def.canAdd();

    }
}
