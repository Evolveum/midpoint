/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.otp;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;

public class OtpListPanel<F extends FocusType> extends MultivalueContainerListPanel<OtpCredentialType> {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<F> focusModel;

    private final PrismContainerWrapperModel<F, OtpCredentialType> model;

    public OtpListPanel(
            String id, IModel<F> focusModel, PrismContainerWrapperModel<F, OtpCredentialType> model, ContainerPanelConfigurationType configuration) {

        super(id, OtpCredentialType.class, configuration);

        this.focusModel = focusModel;
        this.model = model;
    }

    @Override
    protected void editItemPerformed(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<OtpCredentialType>> rowModel,
            List<PrismContainerValueWrapper<OtpCredentialType>> listItems) {

    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<OtpCredentialType>> getContainerModel() {
        return model;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<OtpCredentialType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<OtpCredentialType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<>(
                model, OtpCredentialType.F_NAME, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<>(
                model, OtpCredentialType.F_CREATE_TIMESTAMP, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        columns.add(new AbstractColumn<>(

                createStringResource("OtpCredentialType.verified"), null) {

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<OtpCredentialType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {

                IModel<List<Badge>> badgesModel = () -> {
                    PrismContainerValueWrapper<OtpCredentialType> wrapper = model.getObject();
                    if (wrapper == null) {
                        return List.of();
                    }
                    OtpCredentialType credential = wrapper.getNewValue().asContainerable();
                    if (BooleanUtils.isNotTrue(credential.getVerified())) {
                        return List.of();
                    }

                    return List.of(
                            new Badge(
                                    "badge badge-success",
                                    "fa fa-shield-halved",
                                    LocalizationUtil.translate("OtpCredentialType.verified")));
                };

                item.add(new BadgeListPanel(componentId, badgesModel));
            }
        });

        columns.add(new AbstractColumn<>(Model.of()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<OtpCredentialType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {

                ActionPanel panel = new ActionPanel(id) {

                    @Override
                    protected void onDeletePerformed(AjaxRequestTarget target) {
                        OtpListPanel.this.onEditPerformed(target, model);
                    }

                    @Override
                    protected void onEditPerformed(AjaxRequestTarget target) {
                        OtpListPanel.this.onDeletePerformed(target, model);
                    }
                };
                item.add(panel);
            }
        });

        return columns;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_FOCUS_CREDENTIALS_OTP;
    }

    @Override
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected void newItemPerformed(
            PrismContainerValue<OtpCredentialType> value,
            AjaxRequestTarget target,
            AssignmentObjectRelation relationSpec,
            boolean isDuplicate) {

        PrismContainerWrapper<OtpCredentialType> wrapper = model.getObject();

        PrismContainerValue<OtpCredentialType> newValue = value;
        if (newValue == null) {
            Task task = getPageBase().createSimpleTask("createOtpCredential");
            OperationResult result = task.getResult();

            PrismObject<? extends FocusType> obj = focusModel.getObject().asPrismObject();
            OtpCredentialType credentialType = MidPointApplication.get().getOtpManager().createOtpCredential(obj, task, result);
            // noinspection unchecked
            newValue = credentialType.asPrismContainerValue();
        }

        PrismContainerValueWrapper<OtpCredentialType> newValueWrapper =
                createNewItemContainerValueWrapper(newValue, wrapper, target);

        IModel<OtpCredentialType> credentialModel = () -> newValueWrapper.getRealValue();

        OtpPanel<F> panel = new OtpPanel<>(getPageBase().getMainPopupBodyId(), focusModel, credentialModel) {

            @Override
            protected void onCancelPerformed(AjaxRequestTarget target) {
                OtpListPanel.this.onCancelPerformed(target);

                super.onCancelPerformed(target);
            }

            @Override
            protected void onConfirmPerformed(AjaxRequestTarget target) {
                OtpListPanel.this.onConfirmPerformed(target);

                super.onConfirmPerformed(target);
            }
        };

        getPageBase().showMainPopup(panel, target);
    }

    private void onDeletePerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
        // todo implement
    }

    private void onEditPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<OtpCredentialType>> model) {
        // todo implement
    }

    private void onCancelPerformed(AjaxRequestTarget target) {
        // todo implement
    }

    private void onConfirmPerformed(AjaxRequestTarget target) {
        // todo implement
    }
}
