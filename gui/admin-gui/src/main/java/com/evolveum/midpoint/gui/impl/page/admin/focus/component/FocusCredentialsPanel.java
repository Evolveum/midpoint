/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.otp.OtpListPanel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.ITab;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.TabbedPanel;

import org.apache.wicket.markup.html.WebMarkupContainer;

@SuppressWarnings("unused")
@PanelType(name = "credentials")
@PanelInstance(
        identifier = "credentials",
        applicableForType = FocusType.class,
        display = @PanelDisplay(
                label = "FocusCredentialsPanel.panel.name",
                icon = GuiStyleConstants.CLASS_PASSWORD_ICON,
                order = 50
        ),
        containerPath = "credentials",
        type = "CredentialsType",
        expanded = true
)
public class FocusCredentialsPanel<F extends FocusType, FDM extends FocusDetailsModels<F>> extends AbstractObjectMainPanel<F, FDM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TAB_PANEL = "tabPanel";

    public FocusCredentialsPanel(
            String id,
            FDM objectModel,
            ContainerPanelConfigurationType configurationType) {
        super(id, objectModel, configurationType);
    }

    @Override
    protected void initLayout() {
        TabbedPanel<?> tabPanel = new TabbedPanel<>(ID_TAB_PANEL, createTabs());
        add(tabPanel);
    }

    private List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(createPasswordTab());
        tabs.add(createOtpsTab());

        return tabs;
    }

    private ITab createPasswordTab() {
        return new PanelTab(createStringResource("FocusCredentialsPanel.tab.password")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new WebMarkupContainer(panelId);
            }
        };
    }

    private ITab createOtpsTab() {
        return new PanelTab(createStringResource("FocusCredentialsPanel.tab.otp")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                PrismContainerWrapperModel<F, OtpCredentialType> model =
                        PrismContainerWrapperModel.fromContainerWrapper(
                                getObjectWrapperModel(),
                                ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_OTP),
                                () ->
                                getPageBase());

                return new OtpListPanel(panelId, () -> getObjectDetailsModels().getObjectType(), model, null);
            }
        };
    }

//    @Override
//    protected IColumn<PrismContainerValueWrapper<OtpCredentialType>, String> createCheckboxColumn() {
//        return new CheckBoxHeaderColumn<>();
//    }
//
//    @Override
//    protected List<IColumn<PrismContainerValueWrapper<OtpCredentialType>, String>> createDefaultColumns() {
//        List<IColumn<PrismContainerValueWrapper<OtpCredentialType>, String>> columns = new ArrayList<>();
//
//        columns.add(new PrismPropertyWrapperColumn<OtpCredentialType, String>(
//                getContainerModel(),
//                ItemPath.create(OtpCredentialType.F_NAME),
//                AbstractItemWrapperColumn.ColumnType.LINK,
//                getPageBase()) {
//
//            @Override
//            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
//                return super.createColumnPanel(componentId, rowModel);
//            }
//
//            @Override
//            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
//                FocusCredentialsPanel.this.itemDetailsPerformed(target, model);
//            }
//        });
//
//        columns.add(new PrismPropertyWrapperColumn<>(
//                getContainerModel(),
//                ItemPath.create(OtpCredentialType.F_CREATE_TIMESTAMP),
//                AbstractItemWrapperColumn.ColumnType.STRING,
//                getPageBase()));
//
//        columns.add(new PrismPropertyWrapperColumn<>(
//                getContainerModel(),
//                ItemPath.create(OtpCredentialType.F_VERIFIED),
//                AbstractItemWrapperColumn.ColumnType.STRING,
//                getPageBase()));
//
//        return columns;
//    }
//
//    @Override
//    protected boolean isCreateNewObjectVisible() {
//        return true;
//    }
//
//    @Override
//    protected IModel<PrismContainerWrapper<OtpCredentialType>> getContainerModel() {
//        return otpCredentialModel;
//    }
//
//    @Override
//    protected MultivalueContainerDetailsPanel<OtpCredentialType> getMultivalueContainerDetailsPanel(
//            ListItem<PrismContainerValueWrapper<OtpCredentialType>> item) {
//
//        return new OtpDetailsPanel(
//                MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS,
//                () -> objectModel.getObject().getObject().asObjectable(),
//                item.getModel());
//    }
//
//    @Override
//    protected void onDoneClicked(AjaxRequestTarget target) {
//        Set<OtpPanel> result = new HashSet<>();
//
//        WebMarkupContainer container = getDetailsPanelContainer();
//        container.visitChildren(FormComponent.class, (FormComponent<?> child, IVisit<FormComponent<?>> visit) -> {
//            if (child.isValid()) {
//                child.validate();
//            }
//            if (child.hasErrorMessage()) {
//                result.add(child.findParent(OtpPanel.class));
//            }
//        });
//
//        if (!result.isEmpty()) {
//            result.forEach(p -> target.add(p));
//            target.add(getPageBase().getFeedbackPanel());
//        } else {
//            super.onDoneClicked(target);
//        }
//    }
//
//    @Override
//    protected List<InlineMenuItem> createInlineMenu() {
//        return getDefaultMenuActions();
//    }
//
//    @Override
//    protected UserProfileStorage.TableId getTableId() {
//        return UserProfileStorage.TableId.PANEL_FOCUS_CREDENTIALS_OTP;
//    }
//
//    @Override
//    protected void newItemPerformed(
//            PrismContainerValue<OtpCredentialType> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {
//
//        if (value == null) {
//            Task task = getPageBase().createSimpleTask("createOtpCredential");
//            OperationResult result = task.getResult();
//
//            PrismObject obj = getContainerModel().getObject().findObjectWrapper().getObject();
//            OtpCredentialType credentialType = MidPointApplication.get().getOtpManager().createOtpCredential(obj, task, result);
//
//            value = credentialType.asPrismContainerValue();
//        }
//
//        super.newItemPerformed(value, target, relationSpec, isDuplicate);
//    }
//
//    private static class OtpDetailsPanel extends MultivalueContainerDetailsPanel<OtpCredentialType> {
//
//        private final IModel<FocusType> focusModel;
//
//        public OtpDetailsPanel(
//                String id, IModel<FocusType> focusModel, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
//
//            super(id, model, false);
//
//            this.focusModel = focusModel;
//        }
//
//        @Override
//        protected DisplayNamePanel<OtpCredentialType> createDisplayNamePanel(String displayNamePanelId) {
//            DisplayNamePanel<OtpCredentialType> panel =
//                    new DisplayNamePanel<>(displayNamePanelId, () -> getModelObject().getRealValue());
//            panel.add(VisibleBehaviour.ALWAYS_INVISIBLE);
//
//            return panel;
//        }
//
//        @Override
//        protected @NotNull List<ITab> createTabs() {
//            return List.of(createEditNewValueTab());
//        }
//
//        private ITab createEditNewValueTab() {
//            return new PanelTab(
//                    createStringResource("FocusOtpsPanel.tab.basic")) {
//
//                @Override
//                public WebMarkupContainer createPanel(String panelId) {
//                    OtpPanel panel = new OtpPanel(panelId, focusModel, () -> getModel().getObject().getRealValue());
//                    panel.add(AttributeAppender.append("style", "max-width: 600px;"));
//
//                    return panel;
//                }
//            };
//        }
//    }
}
