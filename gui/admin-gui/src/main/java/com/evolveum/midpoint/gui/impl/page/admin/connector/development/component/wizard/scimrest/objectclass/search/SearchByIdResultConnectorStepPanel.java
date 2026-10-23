/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractObjectVerificationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.RetrievedAttributeDto;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for reviewing the object returned by the GET (search by ID) operation. The user either
 * confirms the result, or marks the incorrect attributes and goes back to fix the GET script.
 */
@PanelType(name = "cdw-search-by-id-verification")
@PanelInstance(identifier = "cdw-search-by-id-verification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.searchByIdVerification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchByIdResultConnectorStepPanel extends AbstractObjectVerificationConnectorStepPanel<RetrievedAttributeDto> {

    private static final String PANEL_TYPE = "cdw-search-by-id-verification";

    public SearchByIdResultConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel,
            IModel<ShadowType> retrievedObjectModel) {
        super(helper, valueModel, retrievedObjectModel);
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_BY_ID_DEFINITION);
    }

    @Override
    protected List<IColumn<RetrievedAttributeDto, String>> createColumns() {
        List<IColumn<RetrievedAttributeDto, String>> columns = new ArrayList<>();
        columns.add(createMarkColumn());
        columns.add(new PropertyColumn<>(createStringResource("SearchByIdResultConnectorStepPanel.attribute"),
                RetrievedAttributeDto.F_NAME, RetrievedAttributeDto.F_NAME));
        columns.add(new PropertyColumn<>(createStringResource("SearchByIdResultConnectorStepPanel.returnedValue"),
                RetrievedAttributeDto.F_VALUE, RetrievedAttributeDto.F_VALUE));
        return columns;
    }

    private IColumn<RetrievedAttributeDto, String> createMarkColumn() {
        return new CheckBoxHeaderColumn<>() {

            @Override
            protected IModel<Boolean> getCheckBoxValueModel(IModel<RetrievedAttributeDto> rowModel) {
                return new IModel<>() {

                    @Override
                    public Boolean getObject() {
                        return isMarked(rowModel.getObject());
                    }

                    @Override
                    public void setObject(Boolean marked) {
                        setMarked(rowModel.getObject(), Boolean.TRUE.equals(marked));
                    }
                };
            }

            @Override
            protected boolean isTableRowSelected(IModel<RetrievedAttributeDto> model) {
                return isMarked(model.getObject());
            }

            @Override
            protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                TableUtil.<RetrievedAttributeDto>getAvailableData(table)
                        .forEach(row -> setMarked(row.getObject(), selected));
                refreshAfterMarking(target, table);
            }

            @Override
            protected void onUpdateRow(Item<ICellPopulator<RetrievedAttributeDto>> cellItem, AjaxRequestTarget target,
                    DataTable table, IModel<RetrievedAttributeDto> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(cellItem, target, table, rowModel, selected);
                if (isShowMarkedOnly()) {
                    target.add(getTable());
                } else {
                    target.add(cellItem.findParent(SelectableDataTable.SelectableRowItem.class));
                }
                target.add(getSubmit());
            }
        };
    }

    private void refreshAfterMarking(AjaxRequestTarget target, DataTable<?, ?> table) {
        if (isShowMarkedOnly()) {
            target.add(getTable());
        } else {
            TableUtil.updateRows(table, target);
        }
        target.add(getSubmit());
    }

    @Override
    protected List<RetrievedAttributeDto> getAttributeRows() {
        ShadowType retrievedObject = getRetrievedObject();
        if (retrievedObject == null) {
            return List.of();
        }

        return getSimpleAttributeNamesWithIdentifiersFirst(retrievedObject).stream()
                .map(name -> new RetrievedAttributeDto(
                        name.getLocalPart(),
                        ConnectorDevelopmentWizardUtil.formatValues(
                                ShadowUtil.getAttributeValues(retrievedObject, name))))
                .toList();
    }

    @Override
    protected IModel<String> getFilterLabelModel() {
        return createStringResource("SearchByIdResultConnectorStepPanel.showMarkedOnly");
    }

    @Override
    protected String getScriptPanelType() {
        return SearchByIdScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> getString(isAnyAttributeMarked()
                ? "SearchByIdResultConnectorStepPanel.tryToFixScript"
                : "SearchByIdResultConnectorStepPanel.confirmAndSave");
    }

    @Override
    protected IModel<String> getBackLabelModel() {
        return createStringResource("SearchByIdResultConnectorStepPanel.changeTestObject");
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchByIdVerification");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchByIdVerification.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchByIdVerification.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
