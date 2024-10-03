/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.component.FocusTypeAssignmentPopupTabPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AssignmentHolderOperationalButtonsPanel<AH extends AssignmentHolderType> extends OperationalButtonsPanel<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderOperationalButtonsPanel.class);

    private static final String DOT_CLASS = AssignmentHolderOperationalButtonsPanel.class.getName() + ".";
    protected static final String OPERATION_LOAD_FILTERED_ARCHETYPES = DOT_CLASS + "loadFilteredArchetypes";
    protected static final String OPERATION_EXECUTE_ARCHETYPE_CHANGES = DOT_CLASS + "executeArchetypeChanges";

    public AssignmentHolderOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> model) {
        super(id, model);
    }

    @Override
    protected void addButtons(RepeatingView repeatingView) {
        createChangeArchetypeButton(repeatingView);
    }

    //TODO move to focus??
    private void createChangeArchetypeButton(RepeatingView repeatingView) {
        IconType iconType = new IconType();
        iconType.setCssClass(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                .setBasicIcon(GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON, IconCssStyle.IN_ROW_STYLE)
                .appendLayerIcon(iconType, IconCssStyle.BOTTOM_RIGHT_STYLE);
        AjaxIconButton changeArchetype = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON), createStringResource("PageAdminObjectDetails.button.changeArchetype")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                changeArchetypeButtonClicked(target);
            }
        };
        changeArchetype.showTitleAsLabel(true);
        changeArchetype.add(new VisibleBehaviour(() -> isChangeArchetypeButtonVisible())); // && CollectionUtils.isNotEmpty(getArchetypeOidsListToAssign())));
        changeArchetype.add(AttributeAppender.append("class", "btn-default btn-sm"));
        repeatingView.add(changeArchetype);
    }

    protected boolean isChangeArchetypeButtonVisible() {
        return !getModelObject().isReadOnly() && isEditingObject()
                && getObjectArchetypeRef() != null;
    }

    private void changeArchetypeButtonClicked(AjaxRequestTarget target) {

        AssignmentPopup changeArchetypePopup = new AssignmentPopup(getPageBase().getMainPopupBodyId(), Model.of(new AssignmentPopupDto(null))) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
                addArchetypePerformed(target, newAssignmentsList);
            }

            @Override
            protected List<ITab> createAssignmentTabs(AssignmentObjectRelation assignmentObjectRelation) {
                List<ITab> tabs = new ArrayList<>();

                tabs.add(new PanelTab(getPageBase().createStringResource("ObjectTypes.ARCHETYPE"),
                        new VisibleBehaviour(() -> true)) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel<ArchetypeType>(panelId, ObjectTypes.ARCHETYPE, null) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                                PrismContainerWrapper<AssignmentType> assignmentsWrapper = null;
                                try {
                                    assignmentsWrapper = AssignmentHolderOperationalButtonsPanel.this.getModelObject().findContainer(FocusType.F_ASSIGNMENT);
                                } catch (SchemaException e) {
                                    LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage());
                                }
                                return assignmentsWrapper;
                            }

                            @Override
                            protected List<QName> getSupportedRelations() {
                                return Collections.singletonList(SchemaConstants.ORG_DEFAULT);
                            }

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<ArchetypeType>>> rowModelList, DataTable dataTable) {
                                target.add(getObjectListPanel());
                                tabLabelPanelUpdate(target);
                            }

                            @Override
                            protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<ArchetypeType>> rowModel) {
                                if (rowModel == null) {
                                    return Model.of(false);
                                }
                                List selectedObjects = getPreselectedObjects();
                                return Model.of(selectedObjects == null || selectedObjects.size() == 0
                                        || (rowModel.getObject() != null && rowModel.getObject().isSelected()));
                            }

                            @Override
                            protected ObjectTypes getObjectType() {
                                return ObjectTypes.ARCHETYPE;
                            }

                            @Override
                            protected ObjectQuery addFilterToContentQuery() {
                                ObjectQuery query = super.addFilterToContentQuery();
                                if (query == null) {
                                    query = getPrismContext().queryFactory().createQuery();
                                }
                                List<String> archetypeOidsList = getArchetypeOidsListToAssign();
                                ObjectFilter filter = getPrismContext().queryFor(ArchetypeType.class)
                                        .id(archetypeOidsList.toArray(new String[0]))
                                        .buildFilter();
                                query.addFilter(filter);
                                return query;
                            }
                        };
                    }
                });
                return tabs;
            }

            @Override
            protected IModel<String> getWarningMessageModel() {
                return createStringResource("PageAdminObjectDetails.button.changeArchetype.warningMessage");
            }
        };

        changeArchetypePopup.setOutputMarkupPlaceholderTag(true);
        getPageBase().showMainPopup(changeArchetypePopup, target);

    }

    //TODO make abstract
    protected void addArchetypePerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
        OperationResult result = new OperationResult(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
        if (newAssignmentsList.size() > 1) {
            result.recordWarning(getString("PageAdminObjectDetails.change.archetype.more.than.one.selected"));
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        AssignmentType oldArchetypAssignment = getOldArchetypeAssignment(result);
        if (oldArchetypAssignment == null) {
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        changeArchetype(oldArchetypAssignment, newAssignmentsList, result, target);
    }

    private void changeArchetype(AssignmentType oldArchetypAssignment, List<AssignmentType> newAssignmentsList, OperationResult result, AjaxRequestTarget target) {
        try {
            ObjectDelta<AH> delta = getPrismContext().deltaFor(getPrismObject().getCompileTimeClass())
                    .item(AssignmentHolderType.F_ASSIGNMENT)
                    .delete(oldArchetypAssignment.clone())
                    .asObjectDelta(getPrismObject().getOid());
            delta.addModificationAddContainer(AssignmentHolderType.F_ASSIGNMENT, newAssignmentsList.iterator().next());

            Task task = getPageBase().createSimpleTask(OPERATION_EXECUTE_ARCHETYPE_CHANGES);
            getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);

        } catch (Exception e) {
            LOGGER.error("Cannot find assignment wrapper: {}", e.getMessage(), e);
            result.recordFatalError(getString("PageAdminObjectDetails.change.archetype.failed", e.getMessage()), e);

        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        refresh(target);
    }

    protected void refresh(AjaxRequestTarget target) {

    }

    private AssignmentType getOldArchetypeAssignment(OperationResult result) {
        PrismContainer<AssignmentType> assignmentContainer = getModelObject().getObjectOld().findContainer(AssignmentHolderType.F_ASSIGNMENT);
        if (assignmentContainer == null) {
            //should not happen either
            result.recordWarning(getString("PageAdminObjectDetails.archetype.change.not.supported"));
            return null;
        }

        List<AssignmentType> oldAssignments = assignmentContainer.getRealValues().stream().filter(WebComponentUtil::isArchetypeAssignment).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(oldAssignments)) {
            result.recordWarning(getString("PageAdminObjectDetails.archetype.change.not.supported"));
            return null;
        }

        if (oldAssignments.size() > 1) {
            result.recordFatalError(getString("PageAdminObjectDetails.archetype.change.no.single.archetype"));
            return null;
        }
        return oldAssignments.iterator().next();
    }


    private ObjectReferenceType getObjectArchetypeRef() {
        PrismObjectWrapper<AH> objectWrapper = getModelObject();
        if (objectWrapper == null) {
            return null;
        }
        PrismObject<AH> prismObject = objectWrapper.getObject();
        AssignmentHolderType assignmentHolderObj = prismObject.asObjectable();
        for (AssignmentType assignment : assignmentHolderObj.getAssignment()) {
            if (isArchetypeAssignment(assignment)) {
                return assignment.getTargetRef();
            }
        }
        return null;
    }

    private boolean isArchetypeAssignment(AssignmentType assignment) {
        return assignment.getTargetRef() != null && assignment.getTargetRef().getType() != null
                && QNameUtil.match(assignment.getTargetRef().getType(), ArchetypeType.COMPLEX_TYPE);
    }

    private List<String> getArchetypeOidsListToAssign() {
        List<String> archetypeOidsList = WebComponentUtil.getArchetypeOidsListByHolderType(
                getModelObject().getObject().getCompileTimeClass(), getPageBase());

        ObjectReferenceType archetypeRef = getObjectArchetypeRef();
        if (archetypeRef != null && StringUtils.isNotEmpty(archetypeRef.getOid())) {
            if (archetypeOidsList.contains(archetypeRef.getOid())) {
                archetypeOidsList.remove(archetypeRef.getOid());
            }
        }
        return archetypeOidsList;
    }

    protected String getMainPopupBodyId() {
        return getPageBase().getMainPopupBodyId();
    }

    protected void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        getPageBase().showMainPopup(popupable, target);
    }

    @Override
    protected boolean isSaveButtonVisible() {
        // Note: when adding objects, the status below is "ADDED", so the first condition causes the button to be visible.
        // Hence, there's no need to ask for canAdd() here.
        return !isForcedPreview()
                && isObjectStatusAndAuthorizationVerifiedForModification();
    }

    /**
     * The same object status and authorization checks should be produced for
     * both save and preview buttons visibility. Therefore, this method should be used
     * as a part of the visibility check for both buttons.
     * @return
     */
    protected boolean isObjectStatusAndAuthorizationVerifiedForModification() {
        return getModelObject().getStatus() != ItemStatus.NOT_CHANGED
                || isEditingObject() && (getModelObject().canModify() || isAuthorizedToModify());
    }

    /**
     * This check was added due to MID-9380, MID-9898.
     *
     * It looks if there's an authorization to execute (any) modification.
     *
     * However, a better approach is probably to ask if there is a request authorization for operations that
     * are not covered by specific item-level modification rights: `#assign`, `#unassign`, `#recompute`.
     */
    protected boolean isAuthorizedToModify() {
        try {
            var object = getModelObject().getObject();
            return getPageBase().isAuthorized(
                    ModelAuthorizationAction.MODIFY.getUrl(),
                    AuthorizationPhaseType.EXECUTION,
                    object,
                    object.createModifyDelta(), // this is because the delta must not be null
                    null);
        } catch (Exception e) {
            return false;
        }
    }
}
