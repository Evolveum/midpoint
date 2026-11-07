/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConfirmationPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Base step for reviewing attributes of an object retrieved from the testing resource.
 *
 * @param <T> table row type
 */
public abstract class AbstractObjectVerificationConnectorStepPanel<T extends RetrievedAttributeDto>
        extends ScriptConfirmationPanel {

    private static final String ID_FILTER = "filter";
    private static final String ID_TABLE = "table";

    private final IModel<ShadowType> retrievedObjectModel;

    private final IModel<String> searchTextModel = Model.of();
    private final IModel<Boolean> showMarkedOnlyModel = Model.of(false);

    private final Set<String> markedAttributes = new HashSet<>();

    private String markedObjectOid;

    public AbstractObjectVerificationConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel,
            IModel<ShadowType> retrievedObjectModel) {
        super(helper, valueModel);
        this.retrievedObjectModel = retrievedObjectModel;
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

        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        BoxedTablePanel<T> table = createTable();
        add(table);
        add(createFilterPanel(table));
    }

    private BoxedTablePanel<T> createTable() {
        ListDataProvider<T> provider = new ListDataProvider<>(this, this::getVisibleRows, true);
        provider.setSort(null);

        BoxedTablePanel<T> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns()) {

            @Override
            protected Item<T> customizeNewRowItem(Item<T> item, IModel<T> model) {
                Item<T> row = super.customizeNewRowItem(item, model);
                row.add(AttributeAppender.append("class", () -> isMarked(model.getObject()) ? "table-danger" : null));
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

    private AttributesFilterPanel createFilterPanel(BoxedTablePanel<T> table) {
        return new AttributesFilterPanel(
                ID_FILTER, searchTextModel, showMarkedOnlyModel, getFilterLabelModel(), target -> target.add(table));
    }

    @SuppressWarnings("unchecked")
    protected final BoxedTablePanel<T> getTable() {
        return (BoxedTablePanel<T>) get(ID_TABLE);
    }

    private List<T> getVisibleRows() {
        String searchText = StringUtils.trim(searchTextModel.getObject());

        return getAttributeRows().stream()
                .filter(row -> StringUtils.isEmpty(searchText) || StringUtils.containsIgnoreCase(row.getName(), searchText))
                .filter(row -> !isShowMarkedOnly() || isMarked(row))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    protected final boolean isShowMarkedOnly() {
        return Boolean.TRUE.equals(showMarkedOnlyModel.getObject());
    }

    protected final ShadowType getRetrievedObject() {
        return retrievedObjectModel.getObject();
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

    protected final Set<String> getMarkedAttributes() {
        ShadowType retrievedObject = getRetrievedObject();
        String oid = retrievedObject != null ? retrievedObject.getOid() : null;
        if (!Objects.equals(oid, markedObjectOid)) {
            markedObjectOid = oid;
            resetMarks();
        }
        return markedAttributes;
    }

    protected final void resetMarks() {
        markedAttributes.clear();
        markedAttributes.addAll(getInitialMarks());
    }

    protected final boolean isMarked(RetrievedAttributeDto row) {
        return row != null && getMarkedAttributes().contains(row.getName());
    }

    protected final void setMarked(RetrievedAttributeDto row, boolean marked) {
        if (row == null) {
            return;
        }
        if (marked) {
            getMarkedAttributes().add(row.getName());
        } else {
            getMarkedAttributes().remove(row.getName());
        }
    }

    protected final boolean isAnyAttributeMarked() {
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
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        if (isAnyAttributeMarked()) {
            navigateToScriptPerformed(target);
        } else {
            onNextPerformed(target);
        }
    }

    private void navigateToScriptPerformed(AjaxRequestTarget target) {
        if (!(getWizard() instanceof WizardModelWithParentSteps wizardModel)) {
            return;
        }
        retrievedObjectModel.setObject(null);
        wizardModel.setActiveStepWithinActivePart(getScriptPanelType());
        wizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }

    @Override
    public boolean onBackPerformed(AjaxRequestTarget target) {
        retrievedObjectModel.setObject(null);
        return super.onBackPerformed(target);
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
