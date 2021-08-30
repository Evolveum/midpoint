/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.FocusTypeAssignmentPopupTabPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
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

    public AssignmentHolderOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> model) {
        super(id, model);
    }

    @Override
    protected void addButtons(RepeatingView repeatingView) {
        createChnageArchetypeButton(repeatingView);
    }

    //TODO move to focus??
    private void createChnageArchetypeButton(RepeatingView repeatingView) {
        IconType iconType = new IconType();
        iconType.setCssClass(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                .setBasicIcon(GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON, IconCssStyle.IN_ROW_STYLE)
                .appendLayerIcon(iconType, IconCssStyle.BOTTOM_RIGHT_STYLE);
        AjaxCompositedIconButton changeArchetype = new AjaxCompositedIconButton(repeatingView.newChildId(), iconBuilder.build(), createStringResource("PageAdminObjectDetails.button.changeArchetype")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                changeArchetypeButtonClicked(target);
            }
        };
        changeArchetype.add(new VisibleBehaviour(() -> !getModelObject().isReadOnly() && getObjectArchetypeRef() != null && CollectionUtils.isNotEmpty(getArchetypeOidsListToAssign())));
        changeArchetype.add(AttributeAppender.append("class", "btn-default btn-sm"));
        repeatingView.add(changeArchetype);
    }

    private void changeArchetypeButtonClicked(AjaxRequestTarget target) {

        AssignmentPopup changeArchetypePopup = new AssignmentPopup(getPageBase().getMainPopupBodyId(), null) {

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
                            protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<ArchetypeType>> rowModel) {
                                target.add(getObjectListPanel());
                                tabLabelPanelUpdate(target);
                            }

                            @Override
                            protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<ArchetypeType>> rowModel) {
                                if (rowModel == null) {
                                    return Model.of(false);
                                }
                                List selectedObjects = getSelectedObjectsList();
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
        List<String> archetypeOidsList = getFilteredArchetypeOidsList();

        ObjectReferenceType archetypeRef = getObjectArchetypeRef();
        if (archetypeRef != null && StringUtils.isNotEmpty(archetypeRef.getOid())) {
            if (archetypeOidsList.contains(archetypeRef.getOid())) {
                archetypeOidsList.remove(archetypeRef.getOid());
            }
        }
        return archetypeOidsList;
    }

    private List<String> getFilteredArchetypeOidsList() {
        OperationResult result = new OperationResult(OPERATION_LOAD_FILTERED_ARCHETYPES);
        PrismObject obj = getModelObject().getObject();
        List<String> oidsList = new ArrayList<>();
        try {
            List<ArchetypeType> filteredArchetypes = getPageBase().getModelInteractionService().getFilteredArchetypesByHolderType(obj, result);
            if (filteredArchetypes != null) {
                filteredArchetypes.forEach(archetype -> oidsList.add(archetype.getOid()));
            }
        } catch (SchemaException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment target specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return oidsList;
    }

    protected String getMainPopupBodyId() {
        return getPageBase().getMainPopupBodyId();
    }

    protected void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        getPageBase().showMainPopup(popupable, target);
    }

}
