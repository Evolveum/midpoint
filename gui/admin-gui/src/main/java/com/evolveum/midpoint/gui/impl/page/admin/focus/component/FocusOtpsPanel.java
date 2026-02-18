/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisit;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.OtpPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
@PanelType(name = "otp")
@PanelInstance(
        identifier = "otp",
        applicableForType = FocusType.class,
        display = @PanelDisplay(
                label = "prismPropertyPanel.name.credentials.otp",
                icon = GuiStyleConstants.CLASS_PASSWORD_ICON,
                order = 55
        ),
        containerPath = "credentials/otp",
        type = "OtpCredentialsType",
        expanded = true
)
@Counter(provider = FocusOtpsMenuLinkCounter.class)
public class FocusOtpsPanel extends MultivalueContainerListPanelWithDetailsPanel<OtpCredentialType> {

    @Serial private static final long serialVersionUID = 1L;

    private IModel<PrismObjectWrapper<? extends FocusType>> objectModel;

    private final IModel<PrismContainerWrapper<OtpCredentialType>> otpCredentialModel;

    public FocusOtpsPanel(
            String id,
            AssignmentHolderDetailsModel<?> objectModel,
            ContainerPanelConfigurationType configurationType) {
        super(id, OtpCredentialType.class, configurationType);

        this.objectModel = (IModel) objectModel.getObjectWrapperModel();

        this.otpCredentialModel = PrismContainerWrapperModel.fromContainerWrapper(
                objectModel.getObjectWrapperModel(),
                ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_OTP),
                (SerializableSupplier<PageBase>) () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<OtpCredentialType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<OtpCredentialType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<OtpCredentialType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<OtpCredentialType, String>(
                getContainerModel(),
                ItemPath.create(OtpCredentialType.F_NAME),
                AbstractItemWrapperColumn.ColumnType.LINK,
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                return super.createColumnPanel(componentId, rowModel);
            }

            @Override
            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
                FocusOtpsPanel.this.itemDetailsPerformed(target, model);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getContainerModel(),
                ItemPath.create(OtpCredentialType.F_CREATE_TIMESTAMP),
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(
                getContainerModel(),
                ItemPath.create(OtpCredentialType.F_VERIFIED),
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()));

        return columns;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<OtpCredentialType>> getContainerModel() {
        return otpCredentialModel;
    }

    @Override
    protected MultivalueContainerDetailsPanel<OtpCredentialType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<OtpCredentialType>> item) {

        return new OtpDetailsPanel(
                MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS,
                () -> objectModel.getObject().getObject().asObjectable(),
                item.getModel());
    }

    @Override
    protected void onDoneClicked(AjaxRequestTarget target) {
        Set<OtpPanel> result = new HashSet<>();

        WebMarkupContainer container = getDetailsPanelContainer();
        container.visitChildren(FormComponent.class, (FormComponent<?> child, IVisit<FormComponent<?>> visit) -> {
            if (child.isValid()) {
                child.validate();
            }
            if (child.hasErrorMessage()) {
                result.add(child.findParent(OtpPanel.class));
            }
        });

        if (!result.isEmpty()) {
            result.forEach(p -> target.add(p));
            target.add(getPageBase().getFeedbackPanel());
        } else {
            super.onDoneClicked(target);
        }
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_FOCUS_CREDENTIALS_OTP;
    }

    @Override
    protected void newItemPerformed(
            PrismContainerValue<OtpCredentialType> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {

        if (value == null) {
            Task task = getPageBase().createSimpleTask("createOtpCredential");
            OperationResult result = task.getResult();

            PrismObject obj = getContainerModel().getObject().findObjectWrapper().getObject();
            OtpCredentialType credentialType = MidPointApplication.get().getOtpManager().createOtpCredential(obj, task, result);

            value = credentialType.asPrismContainerValue();
        }

        super.newItemPerformed(value, target, relationSpec, isDuplicate);
    }

    private static class OtpDetailsPanel extends MultivalueContainerDetailsPanel<OtpCredentialType> {

        private final IModel<FocusType> focusModel;

        public OtpDetailsPanel(
                String id, IModel<FocusType> focusModel, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {

            super(id, model, false);

            this.focusModel = focusModel;
        }

        @Override
        protected DisplayNamePanel<OtpCredentialType> createDisplayNamePanel(String displayNamePanelId) {
            DisplayNamePanel<OtpCredentialType> panel =
                    new DisplayNamePanel<>(displayNamePanelId, () -> getModelObject().getRealValue());
            panel.add(VisibleBehaviour.ALWAYS_INVISIBLE);

            return panel;
        }

        @Override
        protected @NotNull List<ITab> createTabs() {
            return List.of(createEditNewValueTab());
        }

        private ITab createEditNewValueTab() {
            return new PanelTab(
                    createStringResource("FocusOtpsPanel.tab.basic")) {

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new OtpPanel(panelId, focusModel, () -> getModel().getObject().getRealValue());
                }
            };
        }
    }
}
