/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

/**
 * Created by honchar.
 */
public class ConstructionAssociationPanel extends BasePanel<PrismContainerWrapper<ConstructionType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ASSOCIATION_NAME = "associationName";
    private static final String ID_ASSOCIATION_REFERENCE_PANEL = "associationReferencePanel";

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionAssociationPanel.class);
    private static final String DOT_CLASS = ConstructionAssociationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_SHADOW_DISPLAY_NAME = DOT_CLASS + "loadShadowReferenceDisplayName";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

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
                GenericMultiValueLabelEditPanel associationReferencesPanel = new GenericMultiValueLabelEditPanel<>(ID_ASSOCIATION_REFERENCE_PANEL,
                        getShadowReferencesModel(item.getModelObject()),
                        Model.of(WebComponentUtil.getAssociationDisplayName(item.getModelObject())),
                        ID_LABEL_SIZE, ID_INPUT_SIZE, true) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isEditButtonEnabled() {
                        return false;
                    }

                    @Override
                    protected void addValuePerformed(AjaxRequestTarget target) {
                        addNewShadowRefValuePerformed(target, item.getModelObject());
                    }

                    protected void addFirstPerformed(AjaxRequestTarget target) {
                        addNewShadowRefValuePerformed(target, item.getModelObject());
                    }

                    @Override
                    protected IModel<String> createTextModel(final IModel<ObjectReferenceType> model) {
                        return () -> {
                            ObjectReferenceType obj = model.getObject();
                            if (obj == null) {
                                return "";
                            }
                            return WebComponentUtil.getDisplayNameOrName(obj, getPageBase(), OPERATION_LOAD_SHADOW_DISPLAY_NAME);
                        };
                    }

                    @Override
                    protected void removeValuePerformed(AjaxRequestTarget target, ListItem<ObjectReferenceType> item) {
                        try {
                            ObjectReferenceType removedShadowRef = item.getModelObject();
                            PrismContainerWrapper<ConstructionType> constructionContainerWrapper = ConstructionAssociationPanel.this.getModelObject();
                            PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionContainerWrapper
                                    .findContainer(ConstructionType.F_ASSOCIATION);
                            associationWrapper.getValues().forEach(associationValueWrapper -> {
                                if (ValueStatus.DELETED.equals(associationValueWrapper.getStatus())) {
                                    return;
                                }
                                ResourceObjectAssociationType associationValue = associationValueWrapper.getRealValue();
                                if (associationValue == null || associationValue.getOutbound() == null || associationValue.getOutbound().getExpression() == null ||
                                        ExpressionUtil.getShadowRefValue(associationValue.getOutbound().getExpression(),
                                                ConstructionAssociationPanel.this.getPageBase().getPrismContext()) == null) {
                                    return;
                                }
                                List<ObjectReferenceType> shadowRefList = ExpressionUtil.getShadowRefValue(associationValue.getOutbound().getExpression(),
                                        ConstructionAssociationPanel.this.getPageBase().getPrismContext());
                                shadowRefList.forEach(shadowRef -> {
                                    if (shadowRef.equals(removedShadowRef)) {
                                        associationValueWrapper.setStatus(ValueStatus.DELETED);
                                    }
                                });
                            });
                        } catch (SchemaException ex) {
                            LOGGER.error("Couldn't remove association value: {}", ex.getLocalizedMessage());
                        }
                        super.removeValuePerformed(target, item);
                    }
                };
                associationReferencesPanel.setOutputMarkupId(true);
                item.add(associationReferencesPanel);
            }
        };

        associationsPanel.setOutputMarkupId(true);
        add(associationsPanel);
    }

    private IModel<List<ObjectReferenceType>> getShadowReferencesModel(ResourceAssociationDefinition def) {
        return new LoadableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<ObjectReferenceType> load() {
                try {
                    QName defName = def.getName();
                    List<ObjectReferenceType> shadowsList = new ArrayList<>();
                    PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper =
                            getModelObject().findContainer(ConstructionType.F_ASSOCIATION);
                    associationWrapper.getValues().forEach(associationValueWrapper -> {
                        if (ValueStatus.DELETED.equals(associationValueWrapper.getStatus())) {
                            return;
                        }
                        PrismContainerValue associationValue = ((PrismContainerValueWrapper) associationValueWrapper).getNewValue();
                        ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) associationValue.asContainerable();
                        if (assoc.getOutbound() == null || assoc.getOutbound().getExpression() == null
                                || ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(),
                                ConstructionAssociationPanel.this.getPageBase().getPrismContext()) == null && !ValueStatus.ADDED
                                .equals(associationValueWrapper.getStatus())) {
                            return;
                        }
                        QName assocRef = ItemPathTypeUtil.asSingleNameOrFailNullSafe(assoc.getRef());
                        if ((defName != null && defName.equals(assocRef))
                                || (assocRef == null && ValueStatus.ADDED.equals(associationValueWrapper.getStatus()))) {
                            shadowsList.addAll(ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(),
                                    ConstructionAssociationPanel.this.getPageBase().getPrismContext()));
                        }
                    });
                    return shadowsList;
                } catch (SchemaException ex) {
                    // nothing?
                }
                return null;
            }
        };
    }

    private void addNewShadowRefValuePerformed(AjaxRequestTarget target, ResourceAssociationDefinition def) {
        ObjectFilter filter = WebComponentUtil.createAssociationShadowRefFilter(def,
                getPageBase().getPrismContext(), resourceModel.getObject().getOid());
        ObjectBrowserPanel<ShadowType> objectBrowserPanel = new ObjectBrowserPanel<ShadowType>(
                getPageBase().getMainPopupBodyId(), ShadowType.class,
                Collections.singletonList(ShadowType.COMPLEX_TYPE),
                false, getPageBase(), filter) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, ShadowType object) {
                getPageBase().hideMainPopup(target);
                try {
                    PrismContainerWrapper<ConstructionType> constructionContainerWrapper = ConstructionAssociationPanel.this.getModelObject();
                    PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionContainerWrapper.findContainer(ConstructionType.F_ASSOCIATION);
                    List<PrismContainerValue<ResourceObjectAssociationType>> associationValueList = associationWrapper.getItem().getValues();
                    PrismContainerValue<ResourceObjectAssociationType> associationValue;
                    if (CollectionUtils.isEmpty(associationValueList)) {
                        associationValue = associationWrapper.getItem().createNewValue();
                    } else {
                        associationValue = associationValueList.get(0);
                    }
                    ItemName associationRefPath = def.getName();
                    associationValue.asContainerable()
                            .setRef(new ItemPathType(associationRefPath));
                    ExpressionType newAssociationExpression =
                            associationValue.asContainerable().beginOutbound().beginExpression();
                    ExpressionUtil.addShadowRefEvaluatorValue(
                            newAssociationExpression, object.getOid(), getPageBase().getPrismContext());
                } catch (SchemaException ex) {
                    LOGGER.error("Couldn't find association container: {}", ex.getLocalizedMessage());
                }
                target.add(ConstructionAssociationPanel.this);
            }

        };

        getPageBase().showMainPopup(objectBrowserPanel, target);
    }
}
