/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping.FocusMappingMappingsTable;
import com.evolveum.midpoint.gui.impl.page.admin.shadow.ResourceAssociationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.shadow.ResourceAttributePanel;

import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ConstructionValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.assignment.ConstructionAssociationPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AssignmentsDetailsPanel extends MultivalueContainerDetailsPanel<AssignmentType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsDetailsPanel.class);
    private boolean isEntitledAssignment;

    public AssignmentsDetailsPanel(String id, IModel<PrismContainerValueWrapper<AssignmentType>> model, boolean isEntitledAssignment) {
        super(id, model, !isEntitledAssignment);
        this.isEntitledAssignment = isEntitledAssignment;
    }

    public AssignmentsDetailsPanel(String id, IModel<PrismContainerValueWrapper<AssignmentType>> model, boolean isEntitledAssignment, ContainerPanelConfigurationType config) {
        super(id, model, !isEntitledAssignment, config);
        this.isEntitledAssignment = isEntitledAssignment;
    }

    @NotNull
    @Override
    protected List<ITab> createTabs() {
        List<ITab> tabs = super.createTabs();
        if (isEntitledAssignment) {
            tabs.add(getConstructionAssociationPanel());
            return tabs;
        }

        AssignmentsUtil.AssignmentTypeType assignmentTypeType = AssignmentsUtil.getAssignmentType(getModelObject());
        switch (assignmentTypeType) {
            case CONSTRUCTION:
                tabs.addAll(createConstructionTabs());
                break;
            case POLICY_RULE:
                tabs.add(createTabs("AssignmentType.policyRule", AssignmentType.F_POLICY_RULE, PolicyRuleType.COMPLEX_TYPE));
                break;
            case FOCUS_MAPPING:
                tabs.add(createFocusMappingsTab());
                break;
            case PERSONA_CONSTRUCTION:
                tabs.add(createTabs("AssignmentType.personaConstruction", AssignmentType.F_PERSONA_CONSTRUCTION, PersonaConstructionType.COMPLEX_TYPE));
                break;
            case ASSIGNMENT_RELATION:
                tabs.add(createTabs("AssignmentType.assignmentRelation", AssignmentType.F_ASSIGNMENT_RELATION, AssignmentRelationType.COMPLEX_TYPE));
                break;
        }

        tabs.add(createActivationTab());
        tabs.add(createConditionTab());
        return tabs;
    }

    private PanelTab createFocusMappingsTab() {
        return new PanelTab(createStringResource("AssignmentType.focusMappings")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new FocusMappingMappingsTable(
                        panelId,
                        PrismContainerValueWrapperModel.fromContainerValueWrapper(getModel(), AssignmentType.F_FOCUS_MAPPINGS),
                        null){
                    @Override
                    public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<MappingType>> rowModel, List<PrismContainerValueWrapper<MappingType>> listItems) {
                        showDetailsPanel(target, rowModel, listItems);
                    }

                    @Override
                    protected WebMarkupContainer getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<MappingType>> item) {
                        return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {
                            @Override
                            protected DisplayNamePanel<MappingType> createDisplayNamePanel(String displayNamePanelId) {
                                return new DisplayNamePanel<>(displayNamePanelId, Model.of(item.getModelObject().getRealValue()));
                            }
                        };
                    }
                };
            }
        };
    }

    private PanelTab getConstructionAssociationPanel() {
        return new PanelTab(createStringResource("AssignmentPanel.inducedEntitlements")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                IModel<PrismContainerWrapper<ConstructionType>> constructionModel = PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), AssignmentType.F_CONSTRUCTION);
                ConstructionAssociationPanel constructionDetailsPanel = new ConstructionAssociationPanel(panelId, constructionModel);
                constructionDetailsPanel.setOutputMarkupId(true);
                return constructionDetailsPanel;
            }
        };
    }
    @Override
    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
        return getContainerVisibility(itemWrapper);
    }

    @Override
    protected boolean getBasicTabEditability(ItemWrapper<?, ?> itemWrapper) {
        return getContainerReadability(itemWrapper);
    }

    @Override
    protected DisplayNamePanel<AssignmentType> createDisplayNamePanel(String displayNamePanelId) {
        IModel<AssignmentType> displayNameModel = getDisplayModel(getModelObject().getRealValue());
        return new DisplayNamePanel<>(displayNamePanelId, displayNameModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected QName getRelation() {
                return getRelationForDisplayNamePanel(AssignmentsDetailsPanel.this.getModelObject());
            }

            @Override
            protected IModel<String> getKindIntentLabelModel() {
                return getKindIntentLabelModelForDisplayNamePanel(AssignmentsDetailsPanel.this.getModelObject());
            }

        };
    }

    @SuppressWarnings("unchecked")
    private <C extends Containerable> IModel<C> getDisplayModel(AssignmentType assignment) {
        return () -> {
            if (assignment.getTargetRef() != null && assignment.getTargetRef().getOid() != null) {
                Task task = getPageBase().createSimpleTask("Load target");
                OperationResult result = task.getResult();
                PrismObject<ObjectType> targetObject = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, result);
                return targetObject != null ? (C) targetObject.asObjectable() : null;
            }
            if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
                if (assignment.getConstruction().getResourceRef().getOid() != null) {
                    Task task = getPageBase().createSimpleTask("Load resource");
                    OperationResult result = task.getResult();
                    PrismObject<?> object = WebModelServiceUtils.loadObject(assignment.getConstruction().getResourceRef(), getPageBase(), task, result);
                    if (object != null) {
                        return (C) object.asObjectable();
                    }
                } else {
                    return (C) assignment.getConstruction();
                }
            } else if (assignment.getPersonaConstruction() != null) {
                return (C) assignment.getPersonaConstruction();
            } else if (assignment.getPolicyRule() != null) {
                return (C) assignment.getPolicyRule();
            }
            return null;
        };
    }

    private QName getRelationForDisplayNamePanel(PrismContainerValueWrapper<AssignmentType> modelObject) {
        AssignmentType assignment = modelObject.getRealValue();
        if (assignment.getTargetRef() != null) {
            return assignment.getTargetRef().getRelation();
        } else {
            return null;
        }
    }

    private IModel<String> getKindIntentLabelModelForDisplayNamePanel(PrismContainerValueWrapper<AssignmentType> modelObject) {
        AssignmentType assignment = modelObject.getRealValue();
        if (assignment.getConstruction() != null) {
            PrismContainerValueWrapper<ConstructionType> constructionValue = null;
            try {
                PrismContainerWrapper<ConstructionType> construction = modelObject.findContainer(AssignmentType.F_CONSTRUCTION);
                if (construction == null) {
                    return null;
                }
                constructionValue = construction.getValue();
            } catch (SchemaException e) {
                LOGGER.error("Unexpected problem during construction wrapper lookup, {}", e.getMessage(), e);
            }
            ShadowKindType kind;
            String intent;
            if (constructionValue instanceof ConstructionValueWrapper) {
                Task task = getPageBase().createSimpleTask("Load resource");
                OperationResult result = task.getResult();
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(ResourceType.class, ((ConstructionValueWrapper) constructionValue).getResourceOid(), getPageBase(), task, result);
                kind = ((ConstructionValueWrapper) constructionValue).getKind();
                intent = ((ConstructionValueWrapper) constructionValue).determineIntent(resource);
            } else {
                kind = assignment.getConstruction().getKind();
                intent = assignment.getConstruction().getIntent();
            }

            return createStringResource("DisplayNamePanel.kindIntentLabel", kind, intent);
        }
        return Model.of();
    }

    protected boolean getContainerReadability(ItemWrapper<?, ?> wrapper) {
        if (QNameUtil.match(ConstructionType.F_KIND, wrapper.getItemName())) {
            return false;
        }

        return !QNameUtil.match(ConstructionType.F_INTENT, wrapper.getItemName());
    }

    private ItemVisibility getContainerVisibility(ItemWrapper<?, ?> wrapper) {
        if (wrapper instanceof PrismContainerWrapper) {
            PrismContainerWrapper pcw = (PrismContainerWrapper) wrapper;
            if (pcw.isVirtual()) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        }
        return ItemVisibility.AUTO;
    }

    private PanelTab createTabs(String titleKey, ItemPath path, QName type) {
        return new PanelTab(createStringResource(titleKey)) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<>(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), path), type);
            }
        };

    }
    private List<PanelTab> createConstructionTabs() {
        List<PanelTab> constructionTabs = new ArrayList<>();

        constructionTabs.add(new PanelTab(createStringResource("AssignmentType.construction")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AssignmentConstructionPanel(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), AssignmentType.F_CONSTRUCTION), getConfig());
            }
        });

        if (isInducement()) {
            constructionTabs.add(new PanelTab(createStringResource("AssignmentDetailsPanel.construction.attribute")) {

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new ResourceAttributePanel(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE)), null);
                }
            });

            constructionTabs.add(new PanelTab(createStringResource("AssignmentDetailsPanel.construction.association")) {

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new ResourceAssociationPanel(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION)), null);
                }
            });
        }
        return constructionTabs;
//        return createTabs("AssignmentType.construction", AssignmentType.F_CONSTRUCTION, ConstructionType.COMPLEX_TYPE);
    }

    private boolean isInducement() {
        return AbstractRoleType.F_INDUCEMENT.equivalent(getModelObject().getPath().lastName());
    }

    private PanelTab createActivationTab() {
        return new PanelTab(createStringResource("AssignmentType.activation")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<>(panelId,
                        PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), AssignmentType.F_ACTIVATION), ActivationType.COMPLEX_TYPE) {

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        ItemPath itemPath = itemWrapper.getPath();
                        if (ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP).equivalent(itemPath.namedSegmentsOnly())) {
                            return ItemVisibility.HIDDEN;
                        }

                        if (ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).equivalent(itemPath.namedSegmentsOnly())) {
                            return ItemVisibility.HIDDEN;
                        }

                        if (ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP).equivalent(itemPath.namedSegmentsOnly())) {
                            return ItemVisibility.HIDDEN;
                        }

                        if (ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).equivalent(itemPath.namedSegmentsOnly())) {
                            return ItemVisibility.HIDDEN;
                        }

                        return super.getVisibility(itemWrapper);
                    }
                };
            }
        };
    }

    private PanelTab createConditionTab() {
        return new PanelTab(createStringResource("AssignmentType.condition")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<>(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), AssignmentType.F_CONDITION), MappingType.COMPLEX_TYPE) {

                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        ItemPath assignmentConditionExpressionPath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONDITION, MappingType.F_EXPRESSION);
                        ItemPath inducementConditionExpressionPath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONDITION, MappingType.F_EXPRESSION);
                        ItemPath itemPath = itemWrapper.getPath();
                        if (itemPath.namedSegmentsOnly().equivalent(assignmentConditionExpressionPath) || itemPath.namedSegmentsOnly().equivalent(inducementConditionExpressionPath)) {
                            return ItemVisibility.AUTO;
                        } else {
                            return ItemVisibility.HIDDEN;
                        }
                    }
                };
            }
        };
    }
}
