/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * Created by honchar.
 */
public class AssignmentPopup extends BasePanel<AssignmentPopupDto> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPopup.class);

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_ASSIGN_BUTTON = "assignButton";
    private static final String ID_BACK_BUTTON = "backButton";
    private static final String ID_COMPOSITED_BUTTONS = "compositedButtons";
    private static final String ID_FORM = "form";

    private final List<OrgType> selectedOrgsList = new ArrayList<>();
    private IModel<QName> orgTabsRelationModel;

    private static final String DOT_CLASS = AssignmentPopup.class.getName() + ".";
    protected static final String OPERATION_LOAD_ASSIGNMENT_HOLDER_SPECIFICATION = DOT_CLASS + "loadAssignmentHolderSpecification";

    public AssignmentPopup(String id, @NotNull IModel<AssignmentPopupDto> model) {
        super(id, model);
    }

    private IModel<List<CompositedIconButtonDto>> createNewButtonDescriptionModel() {
        return new LoadableModel<>(false) {
            @Override
            protected List<CompositedIconButtonDto> load() {
                return newButtonDescription();
            }
        };
    }

    private List<CompositedIconButtonDto> newButtonDescription() {
        if (getModelObject() == null) {
            return null;
        }
        List<AssignmentObjectRelation> relations = getModelObject().getAssignmentObjectRelation();
        if (relations == null) {
            return null;
        }

        List<CompositedIconButtonDto> buttonDtoList = new ArrayList<>();
        relations.forEach(relation -> buttonDtoList.add(createCompositedButtonForAssignmentRelation(relation)));

        if (isGenericNewObjectButtonVisible()) {
            DisplayType defaultButtonDisplayType = GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.EVO_ASSIGNMENT_ICON,
                    "green",
                    createStringResource("AssignmentPanel.defaultAssignment").getString(),
                    createStringResource("AssignmentPanel.newAssignmentTitle", "", "").getString());
            CompositedIconButtonDto defaultButton = new CompositedIconButtonDto();
            CompositedIconBuilder builder = new CompositedIconBuilder();
            builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(defaultButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(defaultButtonDisplayType))
                    .appendLayerIcon(IconAndStylesUtil.createIconType(GuiStyleConstants.CLASS_PLUS_CIRCLE, "green"), IconCssStyle.BOTTOM_RIGHT_STYLE);

            defaultButton.setAdditionalButtonDisplayType(defaultButtonDisplayType);
            defaultButton.setCompositedIcon(builder.build());
            buttonDtoList.add(defaultButton);
        }
        return buttonDtoList;
    }

    private boolean isGenericNewObjectButtonVisible() {
        AssignmentCandidatesSpecification spec = loadAssignmentHolderSpecification();
        return spec == null || spec.isSupportGenericAssignment();
    }

    private AssignmentCandidatesSpecification loadAssignmentHolderSpecification() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_HOLDER_SPECIFICATION);
        PrismObject obj = getFocusObject();
        AssignmentCandidatesSpecification spec = null;
        try {
            spec = getPageBase().getModelInteractionService()
                    .determineAssignmentHolderSpecification(obj, result);
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment holder specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return spec;
    }

    protected <F extends AssignmentHolderType> PrismObject<F> getFocusObject() {
        return null;
    }

    private CompositedIconButtonDto createCompositedButtonForAssignmentRelation(AssignmentObjectRelation relation) {
        CompositedIconButtonDto buttonDto = new CompositedIconButtonDto();
        buttonDto.setAssignmentObjectRelation(relation);

        DisplayType additionalButtonDisplayType = GuiDisplayTypeUtil.getAssignmentObjectRelationDisplayType(getPageBase(), relation, "AssignmentPanel.newAssignmentTitle");
        buttonDto.setAdditionalButtonDisplayType(additionalButtonDisplayType);

        CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(getPageBase(), relation,
                additionalButtonDisplayType.getIcon(), IconAndStylesUtil.createIconType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green"));
        CompositedIcon icon = null;
        if (builder != null) {
            icon = builder.build();
        }
        buttonDto.setCompositedIcon(icon);
        return buttonDto;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form form = new Form(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        MultiCompositedButtonPanel newObjectIcon =
                new MultiCompositedButtonPanel(ID_COMPOSITED_BUTTONS, createNewButtonDescriptionModel()) {

                    @Override
                    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                        Form form = (Form) AssignmentPopup.this.get(ID_FORM);
                        AssignmentPopup.this.getModelObject().setSelectionVisible(false);
                        addOrReplaceTabPanels(form, relationSpec);
                        target.add(form);
                    }
                };
        form.add(newObjectIcon);
        newObjectIcon.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().isSelectionVisible()));

        addOrReplaceTabPanels(form, null);

        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);

        form.add(createCancelButton());
        form.add(createAddButton());
        form.add(createBackButton());

    }

    private void addOrReplaceTabPanels(Form form, AssignmentObjectRelation relationSpec) {
        List<ITab> tabs = createAssignmentTabs(relationSpec);
        TabbedPanel<ITab> tabPanel = WebComponentUtil.createTabPanel(ID_TABS_PANEL, getPageBase(), tabs, null);
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(new VisibleBehaviour(() -> getModelObject() != null && !getModelObject().isSelectionVisible()));
        form.addOrReplace(tabPanel);
    }

    private AjaxButton createCancelButton() {
        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("userBrowserDialog.button.cancelButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        return cancelButton;
    }

    private AjaxButton createAddButton() {
        AjaxButton addButton = new AjaxButton(ID_ASSIGN_BUTTON,
                createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Map<String, AssignmentType> selectedAssignmentsMap = new HashMap<>();

                TabbedPanel<ITab> panel = (TabbedPanel) AssignmentPopup.this.get(createComponentPath(ID_FORM, ID_TABS_PANEL));

                panel.getTabs().getObject().forEach(panelTab -> {
                    WebMarkupContainer assignmentPanel = ((PanelTab) panelTab).getPanel();
                    if (assignmentPanel == null) {
                        return;
                    }
                    if (AbstractAssignmentPopupTabPanel.class.isAssignableFrom(assignmentPanel.getClass())) {
                        Map<String, AssignmentType> map = (((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedAssignmentsMap());
                        map.forEach(selectedAssignmentsMap::putIfAbsent);
                    }
                });
                List<AssignmentType> assignments = new ArrayList<>(selectedAssignmentsMap.values());
                getPageBase().hideMainPopup(target);
                addPerformed(target, assignments);
            }
        };
        addButton.add(AttributeAppender.append("title", getAddButtonTitleModel()));
        addButton.add(new EnableBehaviour(this::isAssignButtonEnabled));
        addButton.setOutputMarkupId(true);
        return addButton;
    }

    private AjaxButton createBackButton() {
        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON,
                createStringResource("userBrowserDialog.button.backButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPopup.this.getModelObject().setSelectionVisible(true);
                target.add(AssignmentPopup.this.get(ID_FORM));
            }
        };
        backButton.setOutputMarkupId(true);
        backButton.add(new VisibleBehaviour(() -> !getModelObject().isSelectionVisible() && getModelObject().hasSelectionEnabled()));
        return backButton;
    }

    protected List<ITab> createAssignmentTabs(AssignmentObjectRelation relationSpec) {
        List<ITab> tabs = new ArrayList<>();

        if (isTabVisible(ObjectTypes.ROLE, relationSpec)) {
            tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"),
                    new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ROLE, relationSpec))) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new FocusTypeAssignmentPopupTabPanel<RoleType>(panelId, ObjectTypes.ROLE, relationSpec) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<RoleType>>> rowModelList, DataTable dataTable) {
                            tabLabelPanelUpdate(target);
                        }

                        @Override
                        protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                            return AssignmentPopup.this.getAssignmentWrapperModel();
                        }
                    };
                }

                @Override
                public String getCount() {
                    return Integer.toString(getTabPanelSelectedCount(getPanel()));
                }
            });
        }

        if (isTabVisible(ObjectTypes.ORG, relationSpec)) {
            tabs.add(
                    new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"),
                            new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ORG, relationSpec))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer createPanel(String panelId) {
                            return new FocusTypeAssignmentPopupTabPanel<OrgType>(panelId, ObjectTypes.ORG, relationSpec) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<OrgType>>> rowModelList, DataTable dataTable) {
                                    selectedOrgsListUpdate(rowModelList);
                                    tabLabelPanelUpdate(target);
                                }

                                @Override
                                protected List<OrgType> getPreselectedObjects() {
                                    return selectedOrgsList;
                                }

                                @Override
                                protected IModel<QName> createQNameModel(QName defaultRelation) {
                                    return getOrgRelationModel(defaultRelation);
                                }

                                @Override
                                protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                                    return AssignmentPopup.this.getAssignmentWrapperModel();
                                }

                                @Override
                                protected ObjectFilter getSubtypeFilter() {
                                    return AssignmentPopup.this.getSubtypeFilter();
                                }
                            };
                        }

                        public String getCount() {
                            return Integer.toString(selectedOrgsList.size());
                        }
                    });
        }

        if (isTabVisible(ObjectTypes.ORG, relationSpec) && isOrgTreeTabVisible(relationSpec)) {
            tabs.add(new CountablePanelTab(createStringResource("TypedAssignablePanel.orgTreeView"),
                    new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ORG, relationSpec) && isOrgTreeTabVisible(relationSpec))) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new OrgTreeAssignmentPopupTabPanel(panelId, relationSpec) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<OrgType>>> rowModelList, DataTable dataTable) {
                            selectedOrgsListUpdate(rowModelList);
                            tabLabelPanelUpdate(target);
                        }

                        @Override
                        protected List<OrgType> getPreselectedObjects() {
                            return selectedOrgsList;
                        }

                        @Override
                        protected IModel<QName> createQNameModel(QName defaultRelation) {
                            return getOrgRelationModel(defaultRelation);
                        }

                        @Override
                        protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                            return AssignmentPopup.this.getAssignmentWrapperModel();
                        }

                        @Override
                        protected ObjectFilter getSubtypeFilter() {
                            return AssignmentPopup.this.getSubtypeFilter();
                        }
                    };
                }

                @Override
                public String getCount() {
                    return Integer.toString(selectedOrgsList.size());
                }
            });
        }

        if (isTabVisible(ObjectTypes.SERVICE, relationSpec)) {
            tabs.add(
                    new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"),
                            new VisibleBehaviour(() -> isTabVisible(ObjectTypes.SERVICE, relationSpec))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer createPanel(String panelId) {
                            return new FocusTypeAssignmentPopupTabPanel<ServiceType>(panelId, ObjectTypes.SERVICE, relationSpec) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<ServiceType>>> rowModelList, DataTable dataTable) {
                                    tabLabelPanelUpdate(target);
                                }

                                @Override
                                protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                                    return AssignmentPopup.this.getAssignmentWrapperModel();
                                }

                            };
                        }

                        @Override
                        public String getCount() {
                            return Integer.toString(getTabPanelSelectedCount(getPanel()));
                        }
                    });
        }

        if (isTabVisible(ObjectTypes.RESOURCE, relationSpec)) {
            tabs.add(
                    new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.RESOURCE"),
                            new VisibleBehaviour(() -> isTabVisible(ObjectTypes.RESOURCE, relationSpec))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer createPanel(String panelId) {
                            return new ResourceTypeAssignmentPopupTabPanel(panelId, relationSpec) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<ResourceType>>> rowModelList, DataTable dataTable) {
                                    super.onSelectionPerformed(target, rowModelList, dataTable);
                                    tabLabelPanelUpdate(target);
                                }

                                @Override
                                protected boolean isEntitlementAssignment() {
                                    return AssignmentPopup.this.isEntitlementAssignment();
                                }
                            };
                        }

                        @Override
                        public String getCount() {
                            return Integer.toString(getTabPanelSelectedCount(getPanel()));
                        }
                    });
        }

        return tabs;
    }

    private IModel<QName> getOrgRelationModel(QName defaultRelation) {
        if (orgTabsRelationModel == null) {
            orgTabsRelationModel = Model.of(defaultRelation);
        }
        return orgTabsRelationModel;
    }

    protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
        return null;
    }

    protected ObjectFilter getSubtypeFilter() {
        return null;
    }

    private boolean isTabVisible(ObjectTypes objectType, AssignmentObjectRelation relationSpec) {
        List<ObjectTypes> availableObjectTypesList = getAvailableObjectTypesList(relationSpec);
        return availableObjectTypesList == null || availableObjectTypesList.size() == 0 || availableObjectTypesList.contains(objectType);
    }

    protected boolean isOrgTreeTabVisible(AssignmentObjectRelation relationSpec) {
        return relationSpec == null;
    }

    private List<ObjectTypes> getAvailableObjectTypesList(AssignmentObjectRelation relationSpec) {
        if (relationSpec == null || CollectionUtils.isEmpty(relationSpec.getObjectTypes())) {
            return getObjectTypesList();
        } else {
            return mergeNewAssignmentTargetTypeLists(relationSpec.getObjectTypes(), getObjectTypesList());
        }
    }

    private List<ObjectTypes> mergeNewAssignmentTargetTypeLists(List<QName> allowedByAssignmentTargetSpecification, List<ObjectTypes> availableTypesList) {
        if (CollectionUtils.isEmpty(allowedByAssignmentTargetSpecification)) {
            return availableTypesList;
        }
        if (CollectionUtils.isEmpty(availableTypesList)) {
            return allowedByAssignmentTargetSpecification.stream().map(spec -> ObjectTypes.getObjectTypeFromTypeQName(spec)).collect(Collectors.toList());
        }
        List<ObjectTypes> mergedList = new ArrayList<>();
        allowedByAssignmentTargetSpecification.forEach(qnameValue -> {
            ObjectTypes objectTypes = ObjectTypes.getObjectTypeFromTypeQName(qnameValue);
            for (ObjectTypes availableObjectTypes : availableTypesList) {
                if (availableObjectTypes.getClassDefinition().equals(objectTypes.getClassDefinition())) {
                    mergedList.add(objectTypes);
                    break;
                }
            }
        });
        return mergedList;
    }

    protected List<ObjectTypes> getObjectTypesList() {
        return ObjectTypeListUtil.createAssignableTypesList();
    }

    protected boolean isEntitlementAssignment() {
        return false;
    }

    private int getTabPanelSelectedCount(WebMarkupContainer panel) {
        if (panel instanceof AbstractAssignmentPopupTabPanel) {
            return ((AbstractAssignmentPopupTabPanel) panel).getPreselectedObjects().size();
        }
        return 0;
    }

    protected void tabLabelPanelUpdate(AjaxRequestTarget target) {
        getTabbedPanel().reloadCountLabels(target);
        target.add(get(ID_FORM).get(ID_ASSIGN_BUTTON));
    }

    private void selectedOrgsListUpdate(List<IModel<SelectableBean<OrgType>>> selectedOrgs){
        if (CollectionUtils.isEmpty(selectedOrgs)){
            return;
        }
        selectedOrgs.forEach(selectedOrg -> {
            if (selectedOrg.getObject().isSelected()){
                selectedOrgsList.add(selectedOrg.getObject().getValue());
            } else {
                selectedOrgsList.removeIf((OrgType org) -> org.getOid().equals(selectedOrg.getObject().getValue().getOid()));
            }
        });
    }

    private TabbedPanel getTabbedPanel() {
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected void addPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
    }

    private IModel<String> getAddButtonTitleModel() {
        return new LoadableModel<>(true) {
            @Override
            protected String load() {
                return !isAssignButtonEnabled() ? createStringResource("AssignmentPopup.addButtonTitle").getString() : "";
            }
        };
    }

    private boolean isAssignButtonEnabled() {
        TabbedPanel<ITab> tabbedPanel = getTabbedPanel();
        List<ITab> tabs = tabbedPanel.getTabs().getObject();
        for (ITab tab : tabs) {
            WebMarkupContainer assignmentPanel = ((PanelTab) tab).getPanel();
            if (assignmentPanel == null) {
                continue;
            }
            if (((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedObjectsList().size() > 0) {
                return true;
            }
        }
        return false;
    }

    protected IModel<String> getWarningMessageModel() {
        return null;
    }

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    public StringResourceModel getTitle() {
        return createStringResource("TypedAssignablePanel.selectObjects");
    }

    public Component getContent() {
        return this;
    }
}
