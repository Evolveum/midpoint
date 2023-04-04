/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueObjectChoosePanel;
import com.evolveum.midpoint.web.component.input.AssociationExpressionValuePanel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

/**
 * Created by honchar.
 */
public class ConstructionAssociationPanel extends BasePanel<PrismContainerWrapper<ConstructionType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ASSOCIATION_REFERENCE_PANEL = "associationReferencePanel";
    private static final String ID_ASSOCIATION_REFERENCE_LABEL = "associationReferenceLabel";
    private static final String ID_ASSOCIATION_REFERENCE_ADD_BUTTON = "associationReferenceAddButton";
    private static final String ID_ASSOCIATION_REFERENCE_REMOVE_BUTTON = "associationReferenceRemoveButton";
    private static final Trace LOGGER = TraceManager.getTrace(ConstructionAssociationPanel.class);
    private static final String DOT_CLASS = ConstructionAssociationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";


    private LoadableDetachableModel<PrismObject<ResourceType>> resourceModel;
    private LoadableDetachableModel<List<ResourceAssociationDefinition>> refinedAssociationDefinitionsModel;

    public ConstructionAssociationPanel(String id, IModel<PrismContainerWrapper<ConstructionType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        resourceModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<ResourceType> load() {
                ConstructionType construction = getModelObject().getItem().getRealValue();
                ObjectReferenceType resourceRef = construction.getResourceRef();
                Task loadResourceTask = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
                OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), loadResourceTask, result);
                result.computeStatusIfUnknown();
                if (!result.isAcceptable()) {
                    LOGGER.error("Cannot find resource {} referenced from construction {}.", construction, result.getMessage());
                    result.recordPartialError("Could not find resource referenced from construction.");
                    return null;
                }
                return resource;
            }
        };

        refinedAssociationDefinitionsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<ResourceAssociationDefinition> load() {
                ConstructionType construction = getModelObject().getItem().getRealValue();

                return WebComponentUtil.getRefinedAssociationDefinition(resourceModel.getObject().asObjectable(), construction.getKind(),
                        construction.getIntent());
            }
        };
    }

    protected void initLayout() {
        ListView<ResourceAssociationDefinition> associationsPanel = new ListView<>(ID_ASSOCIATIONS, refinedAssociationDefinitionsModel) {
            @Override
            protected void populateItem(ListItem<ResourceAssociationDefinition> item) {

                item.setOutputMarkupId(true);

                Label label = new Label(
                        ID_ASSOCIATION_REFERENCE_LABEL,
                        Model.of(WebComponentUtil.getAssociationDisplayName(item.getModelObject())));
                item.add(label);

                IModel<ExpressionType> expressionModel = getExpressionModel(item.getModelObject());
                AjaxLink<Void> removeButton = new AjaxLink<>(ID_ASSOCIATION_REFERENCE_REMOVE_BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        removeValuePerformed(item.getModelObject());
                        expressionModel.detach();
                        target.add(ConstructionAssociationPanel.this);
                    }
                };
                removeButton.setOutputMarkupId(true);
                removeButton.add(new VisibleBehaviour(() -> expressionModel.getObject() != null));
                item.add(removeButton);

                AssociationExpressionValuePanel associationPanel = new AssociationExpressionValuePanel(
                        ID_ASSOCIATION_REFERENCE_PANEL,
                        expressionModel,
                        ConstructionAssociationPanel.this.getModelObject().getItem().getRealValue()) {
                    @Override
                    protected boolean showShadowRefPanel() {
                        return true;
                    }
                };
                associationPanel.setOutputMarkupId(true);
                associationPanel.add(new VisibleBehaviour(() -> expressionModel.getObject() != null));
                item.add(associationPanel);

                AjaxButton addButton = new AjaxButton(ID_ASSOCIATION_REFERENCE_ADD_BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addNewShadowRefValuePerformed(item.getModelObject(), target);
                        expressionModel.detach();
                        target.add(ConstructionAssociationPanel.this);
                    }
                };
                addButton.setOutputMarkupId(true);
                addButton.add(new VisibleBehaviour(() -> expressionModel.getObject() == null));
                item.add(addButton);
            }
        };

        associationsPanel.setOutputMarkupId(true);
        add(associationsPanel);
    }

    private void removeValuePerformed(ResourceAssociationDefinition def) {
        try {
            QName defName = def.getName();
            PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper =
                    getModelObject().findContainer(ConstructionType.F_ASSOCIATION);
            Iterator<PrismContainerValueWrapper<ResourceObjectAssociationType>> iterator = associationWrapper.getValues().iterator();
            while (iterator.hasNext()) {
                PrismContainerValueWrapper<ResourceObjectAssociationType> value = iterator.next();
                if (ValueStatus.DELETED.equals(value.getStatus())) {
                    continue;
                }
                PrismContainerValue associationValue = ((PrismContainerValueWrapper) value).getNewValue();
                ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) associationValue.asContainerable();
                QName assocRef = ItemPathTypeUtil.asSingleNameOrFailNullSafe(assoc.getRef());
                if (defName != null && defName.equals(assocRef)) {
                    if (ValueStatus.ADDED.equals(value.getStatus())) {
                        iterator.remove();
                    } else {
                        value.setStatus(ValueStatus.DELETED);
                    }
                }
            }
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't remove association value: {}", ex.getLocalizedMessage());
        }
    }

    private IModel<ExpressionType> getExpressionModel(ResourceAssociationDefinition def) {
        return new LoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public ExpressionType load() {
                try {
                    QName defName = def.getName();
                    PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper =
                            getModelObject().findContainer(ConstructionType.F_ASSOCIATION);
                    for (PrismContainerValueWrapper<ResourceObjectAssociationType> value : associationWrapper.getValues()) {
                        if (ValueStatus.DELETED.equals(value.getStatus())) {
                            return null;
                        }
                        PrismContainerValue associationValue = ((PrismContainerValueWrapper) value).getNewValue();
                        ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) associationValue.asContainerable();
                        if (assoc.getOutbound() == null) {
                            return null;
                        }
                        QName assocRef = ItemPathTypeUtil.asSingleNameOrFailNullSafe(assoc.getRef());
                        if (defName != null && defName.equals(assocRef)) {
                            return assoc.getOutbound().getExpression();
                        }
                    }
                } catch (SchemaException ex) {
                    LOGGER.error("Couldn't find container for association in " + getModelObject());
                }
                return null;
            }
        };
    }

    private void addNewShadowRefValuePerformed(ResourceAssociationDefinition def, AjaxRequestTarget target) {
        try {
            @NotNull ItemName defName = def.getName();
            PrismContainerWrapper<ConstructionType> constructionContainerWrapper = ConstructionAssociationPanel.this.getModelObject();
            PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionContainerWrapper.findContainer(ConstructionType.F_ASSOCIATION);
            PrismContainerValueWrapper<ResourceObjectAssociationType> valueWrapper =
                    associationWrapper.getValues().stream()
                            .filter(value -> defName.equals(
                                    ItemPathTypeUtil.asSingleNameOrFailNullSafe(value.getRealValue().getRef())))
                            .findFirst().orElse(null);
            if (valueWrapper == null) {
                valueWrapper = WebPrismUtil.createNewValueWrapper(
                        associationWrapper, associationWrapper.getItem().createNewValue(), getPageBase(), target);
            } else if (ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
                valueWrapper.setStatus(ValueStatus.NOT_CHANGED);
                valueWrapper
                        .findItem(ItemPath.create(ResourceObjectAssociationType.F_OUTBOUND, MappingType.F_EXPRESSION))
                        .getValue()
                        .setRealValue(null);
            }

            ResourceObjectAssociationType association = valueWrapper.getRealValue();

            ItemName associationRefPath = def.getName();
            association.ref(new ItemPathType(associationRefPath))
                    .getOutbound().beginExpression();
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't find association container: {}", ex.getLocalizedMessage());
        }
    }
}
