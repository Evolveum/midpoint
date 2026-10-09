/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IsolatedRadioPanel;
import com.evolveum.midpoint.web.component.input.EnumCardChoicePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevTestingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for testing the GET (search by ID) operation. The user picks the test object either from
 * the search results or by entering its primary identifier manually.
 */
@PanelType(name = "cdw-show-search-by-id-result")
@PanelInstance(identifier = "cdw-show-search-by-id-result",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.showSearchByIdResult", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchByIdObjectConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchByIdObjectConnectorStepPanel.class);

    private static final String CLASS_DOT = SearchByIdObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_RETRIEVE_OBJECT = CLASS_DOT + "retrieveObject";

    private static final String PANEL_TYPE = "cdw-show-search-by-id-result";

    private static final String ID_OBJECT_SOURCE = "objectSource";
    private static final String ID_SEARCH_RESULTS = "searchResults";
    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_TABLE = "table";
    private static final String ID_MANUAL_IDENTIFIER = "manualIdentifier";
    private static final String ID_IDENTIFIER = "identifier";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;
    private final IModel<ShadowType> retrievedObjectModel;

    private final IModel<SearchByIdTestObjectSource> objectSourceModel = Model.of(SearchByIdTestObjectSource.SEARCH_RESULTS);
    private final IModel<String> selectedShadowOidModel = Model.of();
    private final IModel<String> primaryIdentifierModel = Model.of();

    private ResourceDetailsModel resourceDetailsModel;

    /**
     * @param retrievedObjectModel shared with {@link SearchByIdResultConnectorStepPanel}, which shows the object
     * retrieved here
     */
    public SearchByIdObjectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel,
            IModel<ShadowType> retrievedObjectModel) {
        super(helper);
        this.valueModel = valueModel;
        this.retrievedObjectModel = retrievedObjectModel;
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
        // replacing the class drops the "disabled" class added by the base panel, so keep it here
        getSubmit().add(AttributeAppender.replace("class",
                () -> isSubmitEnable() ? "btn btn-primary" : "btn btn-primary disabled"));

        resourceDetailsModel = createTestingResourceModel();

        WebMarkupContainer searchResults = new WebMarkupContainer(ID_SEARCH_RESULTS);
        searchResults.setOutputMarkupId(true);
        searchResults.setOutputMarkupPlaceholderTag(true);
        searchResults.add(new VisibleBehaviour(() -> objectSourceModel.getObject() == SearchByIdTestObjectSource.SEARCH_RESULTS));
        add(searchResults);

        WebMarkupContainer manualIdentifier = new WebMarkupContainer(ID_MANUAL_IDENTIFIER);
        manualIdentifier.setOutputMarkupId(true);
        manualIdentifier.setOutputMarkupPlaceholderTag(true);
        manualIdentifier.add(new VisibleBehaviour(() -> objectSourceModel.getObject() == SearchByIdTestObjectSource.MANUAL_IDENTIFIER));
        add(manualIdentifier);

        EnumCardChoicePanel<SearchByIdTestObjectSource> objectSource = new EnumCardChoicePanel<>(
                ID_OBJECT_SOURCE, objectSourceModel, createObjectSourceOptions(), true, false) {

            @Override
            protected void onSelectionChanged(AjaxRequestTarget target) {
                target.add(searchResults, manualIdentifier, getSubmit());
            }

            @Override
            protected String getSelectedCardCssClass() {
                return "border-primary text-primary";
            }
        };
        add(objectSource);

        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, selectedShadowOidModel);
        radioGroup.setOutputMarkupId(true);
        searchResults.add(radioGroup);

        BoxedTablePanel<SelectableBean<ShadowType>> table = new BoxedTablePanel<>(
                ID_TABLE, createProvider(), createColumns()) {

            @Override
            protected Item<SelectableBean<ShadowType>> customizeNewRowItem(Item<SelectableBean<ShadowType>> item,
                    IModel<SelectableBean<ShadowType>> model) {
                Item<SelectableBean<ShadowType>> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class",
                        () -> isSelected(model) ? "table-primary" : null));
                return row;
            }
        };
        table.setOutputMarkupId(true);
        radioGroup.add(table);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // only the rows, re-rendering the whole table would search the resource again
                TableUtil.updateRows(table.getDataTable(), target);
                target.add(getSubmit());
            }
        });

        TextField<String> identifier = new TextField<>(ID_IDENTIFIER, primaryIdentifierModel);
        identifier.setOutputMarkupId(true);
        identifier.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getSubmit());
            }
        });
        manualIdentifier.add(identifier);
    }

    private ResourceDetailsModel createTestingResourceModel() {
        try {
            PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));

            ObjectDetailsModels objectDetailsModel = resource.getValue().getNewObjectModel(
                    getContainerConfiguration(PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));
            return (ResourceDetailsModel) objectDetailsModel;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private List<EnumCardChoicePanel.CardOption<SearchByIdTestObjectSource>> createObjectSourceOptions() {
        return Arrays.stream(SearchByIdTestObjectSource.values())
                .map(source -> EnumCardChoicePanel.createLocalizedOption(source, this, ""))
                .toList();
    }

    private SelectableBeanObjectDataProvider<ShadowType> createProvider() {
        String resourceOid = resourceDetailsModel.getObjectWrapper().getOid();
        QName objectClass = getObjectClass();

        SelectableBeanObjectDataProvider<ShadowType> provider = new SelectableBeanObjectDataProvider<>(this, null) {

            @Override
            public Class<ShadowType> getType() {
                return ShadowType.class;
            }

            @Override
            public ObjectQuery getQuery() {
                return ObjectQueryUtil.createResourceAndObjectClassQuery(resourceOid, objectClass);
            }
        };
        provider.setOptions(getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATIONS).dontRetrieve()
                .build());
        provider.setEmptyListOnNullQuery(true);
        provider.setSort(null);
        provider.setDefaultCountIfNull(Integer.MAX_VALUE);
        provider.setTaskConsumer((Consumer<Task> & Serializable) ConnectorDevelopmentWizardUtil::enableConnectorLogCapture);
        return provider;
    }

    private List<IColumn<SelectableBean<ShadowType>, String>> createColumns() {
        List<IColumn<SelectableBean<ShadowType>, String>> columns = new ArrayList<>();
        columns.add(createRadioColumn());

        ResourceObjectDefinition objectClassDefinition = getObjectClassDefinition();
        if (objectClassDefinition == null) {
            return columns;
        }

        List<ShadowSimpleAttributeDefinition<?>> attributes = new ArrayList<>();
        attributes.addAll(objectClassDefinition.getPrimaryIdentifiers());
        attributes.addAll(objectClassDefinition.getSecondaryIdentifiers());
        objectClassDefinition.getSimpleAttributeDefinitions().stream()
                .filter(attribute -> !attributes.contains(attribute))
                .forEach(attributes::add);

        attributes.forEach(attribute -> columns.add(createAttributeColumn(attribute)));
        return columns;
    }

    private IColumn<SelectableBean<ShadowType>, String> createRadioColumn() {
        return new AbstractColumn<>(Model.of("")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem, String componentId,
                    IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(new IsolatedRadioPanel<>(componentId, () -> getShadowOid(rowModel), Model.of(true)));
            }

            @Override
            public String getCssClass() {
                return "icon align-middle";
            }
        };
    }

    private IColumn<SelectableBean<ShadowType>, String> createAttributeColumn(ShadowSimpleAttributeDefinition<?> attribute) {
        QName attributeName = attribute.getItemName();
        String header = StringUtils.defaultIfBlank(attribute.getDisplayName(), attributeName.getLocalPart());

        return new AbstractColumn<>(Model.of(header)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem, String componentId,
                    IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(new Label(componentId, () -> formatAttributeValues(rowModel, attributeName)));
            }
        };
    }

    private boolean isSelected(IModel<SelectableBean<ShadowType>> rowModel) {
        String oid = getShadowOid(rowModel);
        return oid != null && oid.equals(selectedShadowOidModel.getObject());
    }

    private static String getShadowOid(IModel<SelectableBean<ShadowType>> rowModel) {
        SelectableBean<ShadowType> row = rowModel.getObject();
        return row != null && row.getValue() != null ? row.getValue().getOid() : null;
    }

    private static String formatAttributeValues(IModel<SelectableBean<ShadowType>> rowModel, QName attributeName) {
        SelectableBean<ShadowType> row = rowModel.getObject();
        if (row == null || row.getValue() == null) {
            return null;
        }
        return formatValues(ShadowUtil.getAttributeValues(row.getValue(), attributeName));
    }

    static String formatValues(Collection<?> values) {
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    private ResourceObjectDefinition getObjectClassDefinition() {
        return resourceDetailsModel.findResourceObjectClassDefinition(getObjectClass());
    }

    private QName getObjectClass() {
        return new QName(getObjectClassName());
    }

    private String getObjectClassName() {
        return valueModel.getObject().getRealValue().getName();
    }

    private void retrieveObjectPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_RETRIEVE_OBJECT);
        ConnectorDevelopmentWizardUtil.enableConnectorLogCapture(task);
        OperationResult result = task.getResult();

        ShadowType retrievedObject = null;
        try {
            retrievedObject = objectSourceModel.getObject() == SearchByIdTestObjectSource.MANUAL_IDENTIFIER
                    ? retrieveObjectByPrimaryIdentifier(task, result)
                    : retrieveSelectedObject(task, result);
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve object for GET operation test", e);
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }

        reportConnectorResults(result, target);

        if (!result.isSuccess()) {
            retrievedObjectModel.setObject(null);
            getPageBase().showResult(result);
            target.add(getFeedback());
            return;
        }
        if (retrievedObject == null) {
            retrievedObjectModel.setObject(null);
            getPageBase().error(getString("SearchByIdObjectConnectorStepPanel.objectNotFound"));
            target.add(getFeedback());
            return;
        }

        retrievedObjectModel.setObject(retrievedObject);
        onNextPerformed(target);
    }

    private ShadowType retrieveSelectedObject(Task task, OperationResult result) {
        PrismObject<ShadowType> shadow = WebModelServiceUtils.loadObject(
                ShadowType.class, selectedShadowOidModel.getObject(), getPageBase(), task, result);
        return shadow != null ? shadow.asObjectable() : null;
    }

    private ShadowType retrieveObjectByPrimaryIdentifier(Task task, OperationResult result) throws CommonException {
        ResourceObjectDefinition objectClassDefinition = getObjectClassDefinition();
        if (objectClassDefinition == null) {
            throw new IllegalStateException("No definition of object class " + getObjectClassName()
                    + " in the testing resource schema");
        }

        ObjectQuery query = Resource.of(resourceDetailsModel.getObjectType()).queryFor(objectClassDefinition)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClassDefinition.getObjectClassName())
                .and().item(ItemPath.create(ShadowType.F_ATTRIBUTES,
                        objectClassDefinition.getPrimaryIdentifierRequired().getItemName()))
                .eq(primaryIdentifierModel.getObject().trim())
                .build();

        List<PrismObject<ShadowType>> shadows = getPageBase().getModelService()
                .searchObjects(ShadowType.class, query, null, task, result);
        return shadows.isEmpty() ? null : shadows.get(0).asObjectable();
    }

    private void reportConnectorResults(OperationResult result, AjaxRequestTarget target) {
        if (getWizard() instanceof WizardModelWithParentSteps wizardModel) {
            ConnectorDevelopmentWizardUtil.collectConnectorResults(result, connIdResult -> {
                ConnectorDevelopmentWizardUtil.appendLogsAsContext(connIdResult);
                wizardModel.addOperationResult(getStepId(), SearchByIdScriptConnectorStepPanel.PANEL_TYPE, connIdResult);
            });
            ConnectorDevelopmentWizardUtil.reportConnectorLogs(this, getStepId(), result, target);
        }
    }

    @Override
    public boolean isCompleted() {
        return retrievedObjectModel.getObject() != null
                || ConnectorDevelopmentWizardUtil.isScriptConfirmed(getDetailsModel(),
                ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_BY_ID_DEFINITION, getObjectClassName());
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
    protected boolean isSubmitEnable() {
        if (objectSourceModel.getObject() == SearchByIdTestObjectSource.MANUAL_IDENTIFIER) {
            return StringUtils.isNotBlank(primaryIdentifierModel.getObject());
        }
        return selectedShadowOidModel.getObject() != null;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchByIdResult");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchByIdResult.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchByIdResult.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("SearchByIdObjectConnectorStepPanel.retrieveObject");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        retrieveObjectPerformed(target);
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
