/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.LambdaColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Base step for comparing the values sent by the test with the values read back afterwards.
 *
 * @param <T> table row type
 */
public abstract class AbstractSentValuesVerificationConnectorStepPanel<T extends VerifiedAttributeDto>
        extends AbstractObjectVerificationConnectorStepPanel<T> {

    private static final String ID_ALERT = "alert";
    private static final String ID_ALERT_TEXT = "alertText";

    /**
     * Alert shown when no attribute is marked as mismatching, refreshed when the marks change.
     */
    private WebMarkupContainer alert;

    public AbstractSentValuesVerificationConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> getRetrievedObject() != null;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        alert = createAlert();
        add(alert);
    }

    @Override
    protected List<IColumn<T, String>> createColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();
        columns.add(new LambdaColumn<>(createStringResource("AbstractSentValuesVerificationConnectorStepPanel.attribute"),
                VerifiedAttributeDto.F_NAME, VerifiedAttributeDto::getName));
        columns.addAll(createValueColumns());
        columns.add(createStatusColumn());
        columns.add(createMarkColumn());
        return columns;
    }

    @Override
    protected Set<String> getInitialMarks() {
        return getAttributeRows().stream()
                .filter(row -> row.getStatus() == AttributeVerificationStatus.MISMATCH)
                .map(VerifiedAttributeDto::getName)
                .collect(Collectors.toSet());
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createResetChangesButton(customButtons.newChildId()));
        customButtons.add(createSkipVerificationButton(customButtons.newChildId()));
    }

    @Override
    protected IModel<String> getFilterLabelModel() {
        return createStringResource("AbstractSentValuesVerificationConnectorStepPanel.showMismatchedOnly");
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> getMismatchState().isAnyMarked()
                ? createStringResource("AbstractSentValuesVerificationConnectorStepPanel.fixMismatched", getMismatchState().getMarkedAttributes().size()).getString()
                : getString("AbstractSentValuesVerificationConnectorStepPanel.confirmAndSave");
    }

    /**
     * Creates the columns showing the values of an attribute.
     *
     * @return the value columns
     */
    protected abstract List<IColumn<T, String>> createValueColumns();

    /**
     * Returns the key of the message shown when no attribute is marked.
     *
     * @return the localization key
     */
    protected abstract String getSuccessMessageKey();

    private WebMarkupContainer createAlert() {
        WebMarkupContainer container = new WebMarkupContainer(ID_ALERT);
        container.setOutputMarkupPlaceholderTag(true);
        container.add(new VisibleBehaviour(() -> !getMismatchState().isAnyMarked()));
        container.add(createAlertText());
        return container;
    }

    private Label createAlertText() {
        return new Label(ID_ALERT_TEXT, () -> getString(getSuccessMessageKey()));
    }

    private IColumn<T, String> createStatusColumn() {
        return new AbstractColumn<>(createStringResource("AbstractSentValuesVerificationConnectorStepPanel.status")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
                cellItem.add(createStatusLabel(componentId, rowModel));
            }
        };
    }

    private Label createStatusLabel(String id, IModel<T> rowModel) {
        Label label = new Label(id, () -> createStringResource(getDisplayedStatus(rowModel.getObject())).getString());
        label.add(AttributeAppender.append("class", () ->
                "badge " + getDisplayedStatus(rowModel.getObject()).getBadgeCssClass()));
        return label;
    }

    private IColumn<T, String> createMarkColumn() {
        return new AbstractColumn<>(null) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
                cellItem.add(createMarkLink(componentId, rowModel));
            }
        };
    }

    private AjaxLinkPanel createMarkLink(String id, IModel<T> rowModel) {
        return new AjaxLinkPanel(id, () -> getString(getMismatchState().isMarked(rowModel.getObject())
                ? "AbstractSentValuesVerificationConnectorStepPanel.markMatch"
                : "AbstractSentValuesVerificationConnectorStepPanel.markMismatch")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getMismatchState().setMarked(rowModel.getObject(), !getMismatchState().isMarked(rowModel.getObject()));
                refreshAfterMarkingChanged(target);
            }
        };
    }

    private void refreshAfterMarkingChanged(AjaxRequestTarget target) {
        target.add(getTable(), alert, getCustomButtonsContainer());
    }

    private AttributeVerificationStatus getDisplayedStatus(VerifiedAttributeDto row) {
        if (getMismatchState().isMarked(row)) {
            return AttributeVerificationStatus.MISMATCH;
        }
        return row.getStatus() == AttributeVerificationStatus.MISMATCH ? AttributeVerificationStatus.MATCH : row.getStatus();
    }

    private AjaxButton createResetChangesButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("AbstractSentValuesVerificationConnectorStepPanel.resetChanges")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getMismatchState().reset();
                refreshAfterMarkingChanged(target);
            }
        };
        button.add(AttributeModifier.replace("class", "btn btn-link"));
        button.add(new VisibleBehaviour(() -> !getMismatchState().getMarkedAttributes().equals(getInitialMarks())));
        return button;
    }

    private AjaxButton createSkipVerificationButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("AbstractSentValuesVerificationConnectorStepPanel.skipVerification")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        button.add(new VisibleBehaviour(getMismatchState()::isAnyMarked));
        return button;
    }
}
