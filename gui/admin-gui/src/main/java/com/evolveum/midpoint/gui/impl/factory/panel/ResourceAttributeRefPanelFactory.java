/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ConstructionValueWrapper;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

//FIXME serializable?
@Component
public class ResourceAttributeRefPanelFactory
        extends AbstractInputGuiComponentFactory<ItemPathType> implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAttributeRefPanelFactory.class);

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {

        AutoCompleteQNamePanel<ItemName> autoCompleteTextPanel = new AutoCompleteQNamePanel<ItemName>(
                panelCtx.getComponentId(), new AttributeRefModel(panelCtx.getRealValueModel())) {

            @Override
            public Collection<ItemName> loadChoices() {
                return getChoicesList(panelCtx);
            }
        };

        autoCompleteTextPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

        return autoCompleteTextPanel;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        ItemPath wrapperPath = wrapper.getPath().removeIds();
        return isAssignmentAttributeOrAssociation(wrapperPath) || isInducementAttributeOrAssociation(wrapper);
    }

    private boolean isAssignmentAttributeOrAssociation(ItemPath wrapperPath) {
        ItemPath assignmentAttributePath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF);
        ItemPath assignmentAssociationPath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION, ResourceAttributeDefinitionType.F_REF);

        return assignmentAttributePath.equivalent(wrapperPath) || assignmentAssociationPath.equivalent(wrapperPath);
    }

    private <IW extends ItemWrapper<?, ?>> boolean isInducementAttributeOrAssociation(IW wrapper) {
        ItemPath wrapperPath = wrapper.getPath().removeIds();
        ItemPath inducementAttributePath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF);
        ItemPath inducementAssociationPath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION, ResourceAttributeDefinitionType.F_REF);

        return inducementAttributePath.equivalent(wrapperPath) || inducementAssociationPath.equivalent(wrapperPath) || isVirtualPropertyOfMapping(wrapper);
    }

    private <IW extends ItemWrapper<?, ?>> boolean isVirtualPropertyOfMapping(IW wrapper) {
        return QNameUtil.match(wrapper.getItemName(), ResourceAttributeDefinitionType.F_REF)
                && wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                AbstractRoleType.F_INDUCEMENT,
                AssignmentType.F_CONSTRUCTION,
                ConstructionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_OUTBOUND));
    }

    private List<ItemName> getChoicesList(PrismPropertyPanelContext<ItemPathType> ctx) {

        PrismPropertyWrapper<?> wrapper = ctx.unwrapWrapperModel();
        //attribute/ref
        if (wrapper == null) {
            return Collections.emptyList();
        }

        //attribute value
        PrismContainerValueWrapper<ResourceAttributeDefinitionType> attributeValueWrapper =
                wrapper.getParentContainerValue(ResourceAttributeDefinitionType.class);
        if (attributeValueWrapper == null) {
            return Collections.emptyList();
        }

        //attribute
        ItemWrapper<?, ?> attributeWrapper = attributeValueWrapper.getParent();
        if (attributeWrapper == null) {
            return Collections.emptyList();
        }

        PrismContainerValueWrapper<ConstructionType> itemWrapper = wrapper.getParentContainerValue(ConstructionType.class);

        if (itemWrapper == null) {
            return Collections.emptyList();
        }

        if (!(itemWrapper instanceof ConstructionValueWrapper)) {
            return Collections.emptyList();
        }

        ConstructionValueWrapper constructionWrapper = (ConstructionValueWrapper) itemWrapper;

        try {
            Task task = ctx.getPageBase().createSimpleTask("Load resource");
            OperationResult result = task.getResult();
            PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class,
                    constructionWrapper.getResourceOid(), SelectorOptions.createCollection(GetOperationOptions.createNoFetch()),
                    ctx.getPageBase(), task, result);
            ResourceObjectDefinition rOcd = constructionWrapper.getResourceObjectDefinition(resource);
            if (rOcd == null) {
                return Collections.emptyList();
            }

            if (ConstructionType.F_ASSOCIATION.equivalent(attributeWrapper.getItemName())) {
                Collection<ResourceAssociationDefinition> associationDefs = rOcd.getAssociationDefinitions();
                return associationDefs.stream()
                        .map(ResourceAssociationDefinition::getName)
                        .collect(Collectors.toList());
            }

            Collection<? extends ResourceAttributeDefinition<?>> attrDefs = rOcd.getAttributeDefinitions();
            return attrDefs.stream().map(a -> a.getItemName()).collect(Collectors.toList());

        } catch (SchemaException | ConfigurationException e) {
            LOGGER.warn("Cannot get resource attribute definitions");
        }

        return Collections.emptyList();

    }

    @Override
    public Integer getOrder() {
        return 9999;
    }

    static class AttributeRefModel implements IModel<ItemName> {

        private final IModel<ItemPathType> itemPath;

        public AttributeRefModel(IModel<ItemPathType> itemPath) {
            this.itemPath = itemPath;
        }

        @Override
        public ItemName getObject() {
            ItemPathType itemPathType = itemPath.getObject();
            if (itemPathType == null) {
                return null;
            }
            ItemPath path = itemPathType.getItemPath();
            if (path.size() > 1) {
                return new ItemName("failure");
            }

            if (ItemPath.isName(path.first())) {
                return path.firstToName();
            }

            return null;
        }

        @Override
        public void setObject(ItemName object) {
            itemPath.setObject(new ItemPathType(object));
        }
    }
}
