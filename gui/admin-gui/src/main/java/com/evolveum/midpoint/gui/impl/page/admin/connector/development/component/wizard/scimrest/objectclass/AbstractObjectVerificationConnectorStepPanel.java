/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Base step for reviewing attributes of an object retrieved from the external system.
 *
 * @param <T> table row type
 */
public abstract class AbstractObjectVerificationConnectorStepPanel<T extends RetrievedAttributeDto>
        extends AbstractCapabilityTestStepPanel {

    private static final String ID_RETRIEVED_OBJECT = "retrievedObject";
    private static final String ID_FILTER = "filter";
    private static final String ID_TABLE = "table";

    /**
     * Attributes the user marked as mismatching.
     */
    private final ObjectVerificationMismatchState mismatchState = new ObjectVerificationMismatchState(this::getInitialMarks);

    /**
     * Search text and the filter of the marked attributes of the table.
     */
    private final AttributesFilterState<T> filterState = new AttributesFilterState<>(
            new AttributesFilter<>(() -> getFilterLabelModel().getObject(), row -> mismatchState.isMarked(row)));

    /**
     * Table of the attributes, refreshed when the marks or the filter change.
     */
    private BoxedTablePanel<T> table;

    public AbstractObjectVerificationConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> getRetrievedObject() != null;
    }

    protected abstract List<IColumn<T, String>> createColumns();

    protected abstract List<T> getAttributeRows();

    protected abstract IModel<String> getFilterLabelModel();

    protected abstract String getScriptPanelType();

    protected Set<String> getInitialMarks() {
        return Set.of();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        table = createTable();
        add(table);
        add(createRetrievedObjectCard());
        add(createFilterPanel());
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        mismatchState.resetIfObjectChanged(getRetrievedObject());
    }

    private TestObjectCardPanel createRetrievedObjectCard() {
        return new TestObjectCardPanel(ID_RETRIEVED_OBJECT, getFlowState().getRetrievedObjectModel());
    }

    private BoxedTablePanel<T> createTable() {
        ListDataProvider<T> provider = new ListDataProvider<>(this, this::getVisibleRows, true);
        provider.setSort(null);

        BoxedTablePanel<T> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected Item<T> customizeNewRowItem(Item<T> item, IModel<T> model) {
                Item<T> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class", () -> mismatchState.isMarked(model.getObject()) ? "table-warning" : null));
                return row;
            }

            @Override
            protected boolean isFooterVisible(ISortableDataProvider<T, String> provider, int pageSize) {
                return false;
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private AttributesFilterPanel<T> createFilterPanel() {
        return new AttributesFilterPanel<>(ID_FILTER, () -> filterState, target -> target.add(table));
    }

    @SuppressWarnings("unchecked")
    protected final BoxedTablePanel<T> getTable() {
        return table;
    }

    private List<T> getVisibleRows() {
        Set<String> initialMarks = getInitialMarks();
        return getAttributeRows().stream()
                .filter(row -> filterState.matches(row.getName(), row))
                .sorted(Comparator.comparing(row -> !initialMarks.contains(row.getName())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    protected final boolean isShowMarkedOnly() {
        return filterState.isAnyFilterApplied();
    }

    protected final ShadowType getRetrievedObject() {
        return getFlowState().getRetrievedObjectModel().getObject();
    }

    protected static List<QName> getSimpleAttributeNamesWithIdentifiersFirst(ShadowType shadow) {
        List<QName> identifiers = new ArrayList<>();
        addAttributeNames(identifiers, ShadowUtil.getPrimaryIdentifiers(shadow));
        addAttributeNames(identifiers, ShadowUtil.getSecondaryIdentifiers(shadow));

        return ShadowUtil.getAttributesRaw(shadow).stream()
                .filter(attribute -> attribute instanceof PrismProperty)
                .<QName>map(attribute -> attribute.getElementName())
                .sorted(Comparator.comparingInt(name -> identifierOrder(identifiers, name)))
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

    /**
     * Returns the attributes the user marked as mismatching.
     *
     * @return the state of the marked attributes
     */
    protected final ObjectVerificationMismatchState getMismatchState() {
        return mismatchState;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        if (mismatchState.isAnyMarked()) {
            navigateToScriptPerformed(target);
        } else {
            onNextPerformed(target);
        }
    }

    private void navigateToScriptPerformed(AjaxRequestTarget target) {
        if (!(getWizard() instanceof WizardModelWithParentSteps wizardModel)) {
            return;
        }
        getFlowState().getRetrievedObjectModel().setObject(null);
        mismatchState.forget();
        wizardModel.setActiveStepWithinActivePart(getScriptPanelType());
        wizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        getFlowState().getRetrievedObjectModel().setObject(null);
        mismatchState.forget();
        return super.onBackPerformed(target);
    }

}
