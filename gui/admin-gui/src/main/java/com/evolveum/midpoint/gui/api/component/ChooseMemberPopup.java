/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsHelper;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.*;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class ChooseMemberPopup<O extends ObjectType, T extends AbstractRoleType> extends BasePanel<O> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ChooseMemberPopup.class);
    private static final String DOT_CLASS = ChooseMemberPopup.class.getName() + ".";
    private static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_FORM = "form";
    private static final String ID_COMPOSITED_BUTTONS = "compositedButtons";
    private static final String ID_BUTTONS = "buttons";

    private final Fragment footer;

    private final List<OrgType> selectedOrgsList = new ArrayList<>();

    protected Search search;

    private boolean isCompositedButtonsPanelVisible;

    private List<ITab> tabs;

    public ChooseMemberPopup(String id, Search search,
            IModel<MultiFunctinalButtonDto> compositedButtonsModel) {
        super(id);
        this.search = search;
        isCompositedButtonsPanelVisible = compositedButtonsModel != null && compositedButtonsModel.getObject() != null &&
                !CollectionUtils.isEmpty(compositedButtonsModel.getObject().getAdditionalButtons());

        footer = initFooter();
    }

    private Fragment initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("userBrowserDialog.button.cancelButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ChooseMemberPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        footer.add(cancelButton);

        AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON,
                createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean orgPanelProcessed = false;
                for (ITab panelTab : tabs) {
                    WebMarkupContainer tabPanel = ((CountablePanelTab) panelTab).getPanel();
                    if (tabPanel == null) {
                        continue;
                    }
                    MemberPopupTabPanel memberPanel = (MemberPopupTabPanel) tabPanel;
                    if (memberPanel.getObjectType().equals(ObjectTypes.ORG) && orgPanelProcessed) {
                        continue;
                    }
                    List<ObjectType> selectedObjects = memberPanel.getPreselectedObjects();

                    if (selectedObjects == null || selectedObjects.size() == 0) {
                        continue;
                    }
                    executeMemberOperation(memberPanel.getAbstractRoleTypeObject(),
                            createInOidQuery(selectedObjects), memberPanel.getRelationValue(),
                            memberPanel.getObjectType().getTypeQName(), target, getPageBase());
                    if (memberPanel.getObjectType().equals(ObjectTypes.ORG)) {
                        orgPanelProcessed = true;
                    }
                }
                ChooseMemberPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        addButton.add(AttributeAppender.append("title", getAddButtonTitleModel()));
        addButton.add(new EnableBehaviour(() -> isAddButtonEnabled()));
        addButton.setOutputMarkupId(true);
        footer.add(addButton);

        return footer;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form form = new Form(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        IModel<List<CompositedIconButtonDto>> assignButtonDescriptionModel = createAssignButtonDescriptionModel();
        MultiCompositedButtonPanel assignDescriptionButtonsPanel =
                new MultiCompositedButtonPanel(ID_COMPOSITED_BUTTONS, assignButtonDescriptionModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                        Form form = (Form) ChooseMemberPopup.this.get(ID_FORM);
                        isCompositedButtonsPanelVisible = false;
                        addOrReplaceTabPanels(form, relationSpec);
                        target.add(form);
                    }
                };
        form.add(assignDescriptionButtonsPanel);
        assignDescriptionButtonsPanel.add(new VisibleBehaviour(() -> isCompositedButtonsPanelVisible));

        addOrReplaceTabPanels(form, null);
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private void addOrReplaceTabPanels(Form<?> form, AssignmentObjectRelation relationSpec) {
        tabs = createAssignmentTabs(relationSpec);
        TabCenterTabbedPanel<ITab> tabPanel = new TabCenterTabbedPanel(ID_TABS_PANEL, tabs);
        tabPanel.add(new VisibleBehaviour(() -> !isCompositedButtonsPanelVisible));
        tabPanel.setOutputMarkupId(true);
        form.addOrReplace(tabPanel);
    }

    protected QName getRelationIfIsStable() {
        return null;
    }

    protected List<ITab> createAssignmentTabs(AssignmentObjectRelation relationSpec) {
        List<ITab> tabs = new ArrayList<>();
        List<QName> objectTypes = relationSpec != null && CollectionUtils.isNotEmpty(relationSpec.getObjectTypes()) ?
                relationSpec.getObjectTypes() : getAvailableObjectTypes();
        List<ObjectReferenceType> archetypeRefList = relationSpec != null && !CollectionUtils.isEmpty(relationSpec.getArchetypeRefs()) ?
                relationSpec.getArchetypeRefs() : getArchetypeRefList();
        tabs.add(new CountablePanelTab(createStringResource(ObjectTypes.USER),
                new VisibleBehaviour(() -> objectTypes == null || QNameUtil.contains(objectTypes, UserType.COMPLEX_TYPE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createMemberPopup(panelId, ObjectTypes.USER, archetypeRefList);
            }

            @Override
            public String getCount() {
                return Integer.toString(getTabPanelSelectedCount(getPanel()));
            }
        });

        tabs.add(new CountablePanelTab(createStringResource(ObjectTypes.ROLE),
                new VisibleBehaviour(() -> objectTypes == null || QNameUtil.contains(objectTypes, RoleType.COMPLEX_TYPE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createMemberPopup(panelId, ObjectTypes.ROLE, archetypeRefList);
            }

            @Override
            public String getCount() {
                return Integer.toString(getTabPanelSelectedCount(getPanel()));
            }
        });

        tabs.add(new CountablePanelTab(createStringResource(ObjectTypes.ORG),
                new VisibleBehaviour(() -> objectTypes == null || QNameUtil.contains(objectTypes, OrgType.COMPLEX_TYPE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createMemberPopup(panelId, ObjectTypes.ORG, archetypeRefList);
            }

            @Override
            public String getCount() {
                return Integer.toString(selectedOrgsList.size());
            }
        });

        if (archetypeRefList == null || archetypeRefList.isEmpty()) {
            tabs.add(new CountablePanelTab(createStringResource("TypedAssignablePanel.orgTreeView"),
                    new VisibleBehaviour(() -> isOrgTreeVisible() && (objectTypes == null || QNameUtil.contains(objectTypes, OrgType.COMPLEX_TYPE)))) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new OrgTreeMemberPopupTabPanel(panelId, search, archetypeRefList) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected T getAbstractRoleTypeObject() {
                            return ChooseMemberPopup.this.getAssignmentTargetRefObject();
                        }

                        @Override
                        protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<OrgType>>> rowModelList, DataTable dataTable) {
                            selectedOrgsListUpdate(rowModelList);
                            tabLabelPanelUpdate(target);
                        }

                        @Override
                        protected List<OrgType> getPreselectedObjects() {
                            return selectedOrgsList;
                        }
                    };
                }

                @Override
                public String getCount() {
                    return Integer.toString(selectedOrgsList.size());
                }
            });
        }

        tabs.add(new CountablePanelTab(createStringResource(ObjectTypes.SERVICE),
                new VisibleBehaviour(() -> objectTypes == null || QNameUtil.contains(objectTypes, ServiceType.COMPLEX_TYPE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createMemberPopup(panelId, ObjectTypes.SERVICE, archetypeRefList);
            }

            @Override
            public String getCount() {
                return Integer.toString(getTabPanelSelectedCount(getPanel()));
            }
        });

        return tabs;
    }

    private WebMarkupContainer createMemberPopup(String panelId, ObjectTypes objectType, List<ObjectReferenceType> archetypeRefList) {
        return new MemberPopupTabPanel(panelId, search, archetypeRefList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectionPerformed(AjaxRequestTarget target, List rowModelList, DataTable dataTable) {
                if (ObjectTypes.ORG.equals(objectType)) {
                    selectedOrgsListUpdate(rowModelList);
                }
                tabLabelPanelUpdate(target);
            }

            @Override
            protected ObjectTypes getObjectType() {
                return objectType;
            }

            @Override
            protected T getAbstractRoleTypeObject() {
                return ChooseMemberPopup.this.getAssignmentTargetRefObject();
            }

            @Override
            protected QName getDefaultRelation() {
                return getRelationIfIsStable() != null ? getRelationIfIsStable() : super.getDefaultRelation();
            }

            @Override
            protected boolean isVisibleParameterPanel() {
                if (getRelationIfIsStable() == null) {
                    super.isVisibleParameterPanel();
                }
                return false;
            }
        };
    }

    protected final List<QName> getAvailableObjectTypes() {
        return search.getAllowedTypeList();
    }

    protected List<ObjectReferenceType> getArchetypeRefList() {
        return null;
    }

    protected int getTabPanelSelectedCount(WebMarkupContainer panel) {
        if (panel != null && panel instanceof MemberPopupTabPanel) {
            return ((MemberPopupTabPanel) panel).getPreselectedObjects().size();
        }
        return 0;
    }

    protected void tabLabelPanelUpdate(AjaxRequestTarget target) {
        getTabbedPanel().reloadCountLabels(target);
        target.add(footer);

    }

    private TabbedPanel getTabbedPanel() {
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected ObjectQuery createInOidQuery(List<ObjectType> selectedObjectsList) {
        List<String> oids = new ArrayList<>();
        for (Object selectable : selectedObjectsList) {
            oids.add(((ObjectType) selectable).getOid());
        }

        return getPrismContext().queryFactory().createQuery(getPrismContext().queryFactory().createInOid(oids));
    }

    private void selectedOrgsListUpdate(List<IModel<SelectableBean<OrgType>>> selectedOrgs) {
        if (CollectionUtils.isEmpty(selectedOrgs)) {
            return;
        }
        selectedOrgs.forEach(selectedOrg -> {
            if (selectedOrg.getObject().isSelected()) {
                selectedOrgsList.add(selectedOrg.getObject().getValue());
            } else {
                selectedOrgsList.removeIf((OrgType org) -> org.getOid().equals(selectedOrg.getObject().getValue().getOid()));
            }
        });
    }

    private IModel<String> getAddButtonTitleModel() {
        return new LoadableModel<String>(true) {
            @Override
            protected String load() {
                return !isAddButtonEnabled() ? createStringResource("AssignmentPopup.addButtonTitle").getString() : "";
            }
        };
    }

    private boolean isAddButtonEnabled() {
        TabbedPanel tabbedPanel = getTabbedPanel();
        List<ITab> tabs = (List<ITab>) tabbedPanel.getTabs().getObject();
        for (ITab tab : tabs) {
            WebMarkupContainer memberPanel = ((CountablePanelTab) tab).getPanel();
            if (memberPanel == null) {
                continue;
            }
            if (((MemberPopupTabPanel) memberPanel).getSelectedObjectsList().size() > 0) {
                return true;
            }
        }
        return false;
    }

    protected Task executeMemberOperation(AbstractRoleType targetObject, ObjectQuery query,
            @NotNull QName relation, QName type, AjaxRequestTarget target, PageBase pageBase) {
        return MemberOperationsHelper.createAndSubmitAssignMembersTask(targetObject, type, query,
                relation, target, pageBase);
    }

    private IModel<List<CompositedIconButtonDto>> createAssignButtonDescriptionModel() {
        return new LoadableModel<>(false) {
            @Override
            protected List<CompositedIconButtonDto> load() {
                return getAssignButtonDescription();
            }
        };
    }

    private List<CompositedIconButtonDto> getAssignButtonDescription() {
        List<CompositedIconButtonDto> buttons = new ArrayList<>();
        List<AssignmentObjectRelation> assignmentObjectRelations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadMemberRelationsList());
        if (assignmentObjectRelations != null) {
            assignmentObjectRelations.forEach(relation -> {
                DisplayType additionalDispayType = GuiDisplayTypeUtil.getAssignmentObjectRelationDisplayType(ChooseMemberPopup.this.getPageBase(),
                        relation, "abstractRoleMemberPanel.menu.assignMember");
                CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(ChooseMemberPopup.this.getPageBase(), relation,
                        additionalDispayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green"));
                CompositedIcon icon = builder.build();
                CompositedIconButtonDto buttonDto = createCompositedIconButtonDto(additionalDispayType, relation, icon);
                buttons.add(buttonDto);
            });
        }
        buttons.add(createCompositedIconButtonDto(getAssignMemberButtonDisplayType(), null, null));

        return buttons;
    }

    private DisplayType getAssignMemberButtonDisplayType() {
        String label = ChooseMemberPopup.this.createStringResource("abstractRoleMemberPanel.menu.assignMember", "", "").getString();
        return GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green", label, label);
    }

    private CompositedIconButtonDto createCompositedIconButtonDto(DisplayType buttonDisplayType, AssignmentObjectRelation relation, CompositedIcon icon) {
        CompositedIconButtonDto compositedIconButtonDto = new CompositedIconButtonDto();
        compositedIconButtonDto.setAdditionalButtonDisplayType(buttonDisplayType);
        if (icon != null) {
            compositedIconButtonDto.setCompositedIcon(icon);
        } else {
            CompositedIconBuilder mainButtonIconBuilder = new CompositedIconBuilder();
            mainButtonIconBuilder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(buttonDisplayType), IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(buttonDisplayType));
            compositedIconButtonDto.setCompositedIcon(mainButtonIconBuilder.build());
        }
        compositedIconButtonDto.setAssignmentObjectRelation(relation);
        return compositedIconButtonDto;
    }

    private List<AssignmentObjectRelation> loadMemberRelationsList() {
        AssignmentCandidatesSpecification spec = loadCandidateSpecification();
        return spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
    }

    private AssignmentCandidatesSpecification loadCandidateSpecification() {
        OperationResult result = new OperationResult(OPERATION_LOAD_MEMBER_RELATIONS);
        PrismObject obj = getAssignmentTargetRefObject().asPrismObject();
        AssignmentCandidatesSpecification spec = null;
        try {
            spec = getPageBase().getModelInteractionService()
                    .determineAssignmentHolderSpecification(obj, result);
        } catch (Throwable ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load member relations list for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return spec;
    }

    protected boolean isOrgTreeVisible() {
        return true;
    }

    protected abstract T getAssignmentTargetRefObject();

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    protected QName getDefaultTargetType() {
        return RoleType.COMPLEX_TYPE;
    }

    private List<QName> getSupportedRelations() {
        AbstractRoleSearchItemWrapper memberSearchItem = search.findMemberSearchItem();
        return memberSearchItem != null ? memberSearchItem.getSupportedRelations() : new ArrayList<>();
    }

    public StringResourceModel getTitle() {
        QName stableRelation = getRelationIfIsStable();
        if (stableRelation == null) {
            AbstractRoleSearchItemWrapper memberSearchItem = search.findMemberSearchItem();
            List<QName> relations = memberSearchItem != null ? memberSearchItem.getSupportedRelations() : new ArrayList<>();
            stableRelation = relations.stream().findFirst().orElse(null);
        }
        if (stableRelation != null) {
            RelationDefinitionType def = WebComponentUtil.getRelationDefinition(stableRelation);
            if (def != null) {
                String label = GuiDisplayTypeUtil.getTranslatedLabel(def.getDisplay());
                return createStringResource("ChooseMemberPopup.selectObjectWithRelation", label);
            }
        }
        return createStringResource("TypedAssignablePanel.selectObjects");
    }
}
