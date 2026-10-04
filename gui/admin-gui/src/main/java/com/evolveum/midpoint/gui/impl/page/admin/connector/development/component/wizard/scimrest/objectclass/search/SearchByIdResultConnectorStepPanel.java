/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConfirmationPanel;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
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
public class SearchByIdResultConnectorStepPanel extends ScriptConfirmationPanel {

    private static final String PANEL_TYPE = "cdw-search-by-id-verification";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SHOW_MARKED_ONLY = "showMarkedOnly";
    private static final String ID_TABLE = "table";

    private final IModel<ShadowType> retrievedObjectModel;

    private final IModel<String> searchTextModel = Model.of();
    private final IModel<Boolean> showMarkedOnlyModel = Model.of(false);

    /** Names of the attributes the user marked as incorrect. */
    private final Set<String> markedAttributes = new HashSet<>();

    /** OID of the object the marks belong to, so that marks of a previously retrieved object are not kept. */
    private String markedObjectOid;

    public SearchByIdResultConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel,
            IModel<ShadowType> retrievedObjectModel) {
        super(helper, valueModel);
        this.retrievedObjectModel = retrievedObjectModel;
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_BY_ID_DEFINITION);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        BoxedTablePanel<RetrievedAttributeDto> table = createTable();
        add(table);

        MidpointForm<?> searchForm = new MidpointForm<>(ID_SEARCH_FORM);
        add(searchForm);

        searchForm.add(new TextField<>(ID_SEARCH_TEXT, searchTextModel));

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                target.add(table);
            }
        };
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);

        CheckBox showMarkedOnly = new CheckBox(ID_SHOW_MARKED_ONLY, showMarkedOnlyModel);
        showMarkedOnly.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(table);
            }
        });
        add(showMarkedOnly);
    }

    private BoxedTablePanel<RetrievedAttributeDto> createTable() {
        ListDataProvider<RetrievedAttributeDto> provider = new ListDataProvider<>(this, this::getVisibleRows, true);
        provider.setSort(null);

        BoxedTablePanel<RetrievedAttributeDto> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected Item<RetrievedAttributeDto> customizeNewRowItem(Item<RetrievedAttributeDto> item,
                    IModel<RetrievedAttributeDto> model) {
                Item<RetrievedAttributeDto> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class", () -> isMarked(model.getObject()) ? "table-danger" : null));
                return row;
            }

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<RetrievedAttributeDto, String> provider, int pageSize) {
                // all attributes of one object are reviewed together, scrolled, without paging
                return false;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private List<IColumn<RetrievedAttributeDto, String>> createColumns() {
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
                if (Boolean.TRUE.equals(showMarkedOnlyModel.getObject())) {
                    target.add(getTable());
                } else {
                    target.add(cellItem.findParent(SelectableDataTable.SelectableRowItem.class));
                }
                target.add(getSubmit());
            }
        };
    }

    private void refreshAfterMarking(AjaxRequestTarget target, DataTable<?, ?> table) {
        if (Boolean.TRUE.equals(showMarkedOnlyModel.getObject())) {
            target.add(getTable());
        } else {
            TableUtil.updateRows(table, target);
        }
        target.add(getSubmit());
    }

    @SuppressWarnings("unchecked")
    private BoxedTablePanel<RetrievedAttributeDto> getTable() {
        return (BoxedTablePanel<RetrievedAttributeDto>) get(ID_TABLE);
    }

    private List<RetrievedAttributeDto> getVisibleRows() {
        String searchText = StringUtils.trim(searchTextModel.getObject());
        boolean showMarkedOnly = Boolean.TRUE.equals(showMarkedOnlyModel.getObject());

        return getAttributeRows().stream()
                .filter(row -> StringUtils.isEmpty(searchText) || StringUtils.containsIgnoreCase(row.getName(), searchText))
                .filter(row -> !showMarkedOnly || isMarked(row))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<RetrievedAttributeDto> getAttributeRows() {
        ShadowType retrievedObject = retrievedObjectModel.getObject();
        if (retrievedObject == null) {
            return List.of();
        }

        List<QName> identifiers = new ArrayList<>();
        addAttributeNames(identifiers, ShadowUtil.getPrimaryIdentifiers(retrievedObject));
        addAttributeNames(identifiers, ShadowUtil.getSecondaryIdentifiers(retrievedObject));

        return ShadowUtil.getAttributesRaw(retrievedObject).stream()
                .filter(attribute -> attribute instanceof PrismProperty)
                .sorted(Comparator.comparingInt(attribute -> identifierOrder(identifiers, attribute.getElementName())))
                .map(attribute -> new RetrievedAttributeDto(
                        attribute.getElementName().getLocalPart(),
                        SearchByIdObjectConnectorStepPanel.formatValues(attribute.getRealValues())))
                .toList();
    }

    private static void addAttributeNames(List<QName> names, Collection<ShadowSimpleAttribute<?>> attributes) {
        if (attributes != null) {
            attributes.forEach(attribute -> names.add(attribute.getElementName()));
        }
    }

    private static int identifierOrder(List<QName> identifiers, QName attributeName) {
        int index = identifiers.indexOf(attributeName);
        return index >= 0 ? index : identifiers.size();
    }

    private Set<String> getMarkedAttributes() {
        ShadowType retrievedObject = retrievedObjectModel.getObject();
        String oid = retrievedObject != null ? retrievedObject.getOid() : null;
        if (!Objects.equals(oid, markedObjectOid)) {
            markedAttributes.clear();
            markedObjectOid = oid;
        }
        return markedAttributes;
    }

    private boolean isMarked(RetrievedAttributeDto row) {
        return row != null && getMarkedAttributes().contains(row.getName());
    }

    private void setMarked(RetrievedAttributeDto row, boolean marked) {
        if (row == null) {
            return;
        }
        if (marked) {
            getMarkedAttributes().add(row.getName());
        } else {
            getMarkedAttributes().remove(row.getName());
        }
    }

    private boolean isAnyAttributeMarked() {
        return !getMarkedAttributes().isEmpty();
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return () -> getString(isAnyAttributeMarked()
                ? "SearchByIdResultConnectorStepPanel.tryToFixScript"
                : "SearchByIdResultConnectorStepPanel.confirmAndSave");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        if (isAnyAttributeMarked()) {
            tryToFixScriptPerformed(target);
        } else {
            onNextPerformed(target);
        }
    }
    
    private void tryToFixScriptPerformed(AjaxRequestTarget target) {
        if (!(getWizard() instanceof WizardModelWithParentSteps wizardModel)) {
            return;
        }
        retrievedObjectModel.setObject(null);
        wizardModel.setActiveStepWithinActivePart(SearchByIdScriptConnectorStepPanel.PANEL_TYPE);
        wizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }

    @Override
    protected IModel<String> getBackLabelModel() {
        return createStringResource("SearchByIdResultConnectorStepPanel.changeTestObject");
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        retrievedObjectModel.setObject(null);
        return super.onBackPerformed(target);
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
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

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
