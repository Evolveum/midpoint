/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractObjectVerificationConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for verifying the object created by the CREATE script.
 */
@PanelType(name = "cdw-create-verification")
@PanelInstance(identifier = "cdw-create-verification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.createVerification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CreateResultConnectorStepPanel extends AbstractObjectVerificationConnectorStepPanel<VerifiedAttributeDto> {

    private static final String PANEL_TYPE = "cdw-create-verification";

    private static final String ID_ALERT = "alert";
    private static final String ID_ALERT_TEXT = "alertText";

    private final IModel<ShadowType> createdObjectModel;

    private WebMarkupContainer alert;

    public CreateResultConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel,
            IModel<ShadowType> createdObjectModel, IModel<ShadowType> retrievedObjectModel) {
        super(helper, valueModel, retrievedObjectModel);
        this.createdObjectModel = createdObjectModel;
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        alert = createAlert();
        add(alert);
    }

    private WebMarkupContainer createAlert() {
        WebMarkupContainer container = new WebMarkupContainer(ID_ALERT);
        container.setOutputMarkupId(true);
        container.add(AttributeAppender.append("class", () -> isAnyAttributeMarked() ? "alert-warning" : "alert-success"));
        container.add(new Label(ID_ALERT_TEXT, () -> getString(isAnyAttributeMarked()
                ? "CreateResultConnectorStepPanel.mismatchFound"
                : "CreateResultConnectorStepPanel.retrievedSuccessfully")));
        return container;
    }

    @Override
    protected List<IColumn<VerifiedAttributeDto, String>> createColumns() {
        List<IColumn<VerifiedAttributeDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(createStringResource("SearchByIdResultConnectorStepPanel.attribute"),
                VerifiedAttributeDto.F_NAME, VerifiedAttributeDto.F_NAME));
        columns.add(new PropertyColumn<>(createStringResource("CreateResultConnectorStepPanel.expectedValue"),
                VerifiedAttributeDto.F_EXPECTED_VALUE, VerifiedAttributeDto.F_EXPECTED_VALUE));
        columns.add(new PropertyColumn<>(createStringResource("CreateResultConnectorStepPanel.retrievedValue"),
                VerifiedAttributeDto.F_VALUE, VerifiedAttributeDto.F_VALUE));
        columns.add(createStatusColumn());
        columns.add(createMarkColumn());
        return columns;
    }

    private IColumn<VerifiedAttributeDto, String> createStatusColumn() {
        return new AbstractColumn<>(createStringResource("CreateResultConnectorStepPanel.status")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<VerifiedAttributeDto>> cellItem, String componentId,
                    IModel<VerifiedAttributeDto> rowModel) {
                cellItem.add(new Label(componentId, () -> createStringResource(
                        getDisplayedStatus(rowModel.getObject())).getString()));
            }
        };
    }

    private IColumn<VerifiedAttributeDto, String> createMarkColumn() {
        return new AbstractColumn<>(null) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<VerifiedAttributeDto>> cellItem, String componentId,
                    IModel<VerifiedAttributeDto> rowModel) {
                cellItem.add(new AjaxLinkPanel(componentId, () -> getString(isMarked(rowModel.getObject())
                        ? "CreateResultConnectorStepPanel.markMatch"
                        : "CreateResultConnectorStepPanel.markMismatch")) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setMarked(rowModel.getObject(), !isMarked(rowModel.getObject()));
                        refreshAfterMarkingChanged(target);
                    }
                });
            }
        };
    }

    private void refreshAfterMarkingChanged(AjaxRequestTarget target) {
        target.add(getTable(), alert, getCustomButtonsContainer());
    }

    private AttributeVerificationStatus getDisplayedStatus(VerifiedAttributeDto row) {
        if (isMarked(row)) {
            return AttributeVerificationStatus.MISMATCH;
        }
        return row.getStatus() == AttributeVerificationStatus.MISMATCH ? AttributeVerificationStatus.MATCH : row.getStatus();
    }

    @Override
    protected List<VerifiedAttributeDto> getAttributeRows() {
        ShadowType createdObject = createdObjectModel.getObject();
        ShadowType retrievedObject = getRetrievedObject();
        if (createdObject == null || retrievedObject == null) {
            return List.of();
        }

        Set<QName> names = new LinkedHashSet<>(getSimpleAttributeNamesWithIdentifiersFirst(retrievedObject));
        ShadowUtil.getAttributesRaw(createdObject).stream()
                .filter(attribute -> attribute instanceof PrismProperty)
                .forEach(attribute -> names.add(attribute.getElementName()));

        return names.stream()
                .map(name -> createRow(name, ShadowUtil.getAttributeValues(createdObject, name),
                        ShadowUtil.getAttributeValues(retrievedObject, name)))
                .filter(row -> row.getStatus() != null)
                .toList();
    }

    private VerifiedAttributeDto createRow(QName name, Collection<?> expectedValues, Collection<?> retrievedValues) {
        AttributeVerificationStatus status = null;
        if (expectedValues.isEmpty() && !retrievedValues.isEmpty()) {
            status = AttributeVerificationStatus.GENERATED;
        } else if (!expectedValues.isEmpty() || !retrievedValues.isEmpty()) {
            status = toStrings(expectedValues).equals(toStrings(retrievedValues))
                    ? AttributeVerificationStatus.MATCH
                    : AttributeVerificationStatus.MISMATCH;
        }
        return new VerifiedAttributeDto(name.getLocalPart(),
                ConnectorDevelopmentWizardUtil.formatValues(expectedValues),
                ConnectorDevelopmentWizardUtil.formatValues(retrievedValues),
                status);
    }

    private static Set<String> toStrings(Collection<?> values) {
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
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

    private AjaxButton createResetChangesButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("CreateResultConnectorStepPanel.resetChanges")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                resetMarks();
                refreshAfterMarkingChanged(target);
            }
        };
        button.add(new VisibleBehaviour(() -> !getMarkedAttributes().equals(getInitialMarks())));
        return button;
    }

    private AjaxButton createSkipVerificationButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("CreateResultConnectorStepPanel.skipVerification")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        button.add(new VisibleBehaviour(this::isAnyAttributeMarked));
        return button;
    }

    @Override
    protected IModel<String> getFilterLabelModel() {
        return createStringResource("CreateResultConnectorStepPanel.showMismatchedOnly");
    }

    @Override
    protected String getScriptPanelType() {
        return CreateScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> isAnyAttributeMarked()
                ? createStringResource("CreateResultConnectorStepPanel.fixMismatched", getMarkedAttributes().size()).getString()
                : getString("SearchByIdResultConnectorStepPanel.confirmAndSave");
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> getRetrievedObject() != null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createVerification");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createVerification.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createVerification.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
