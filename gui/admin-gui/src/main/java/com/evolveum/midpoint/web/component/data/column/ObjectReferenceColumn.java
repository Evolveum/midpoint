/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;


import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ObjectReferenceColumn<T> extends PropertyColumn<T, String> {


    public ObjectReferenceColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    public ObjectReferenceColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel) {
        IModel<List<ObjectReferenceType>> dataModel = extractDataModel(rowModel);
        RepeatingView view = new RepeatingView(componentId);

        for (ObjectReferenceType ref : dataModel.getObject()) {
            view.add(new ObjectReferenceColumnPanel(view.newChildId(), Model.of(ref)) {

                @Serial
                private static final long serialVersionUID = 1L;

                @Override
                protected Collection<SelectorOptions<GetOperationOptions>> getOptions() {
                    return ObjectReferenceColumn.this.getOptions(ref);
                }

                @Override
                protected boolean useNameAsLabel() {
                    return ObjectReferenceColumn.this.useNameAsLabel();
                }
            });
        }
        item.add(view);

    }

    protected boolean useNameAsLabel() {
        return false;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions(ObjectReferenceType ref) {
        return null;
    }

    @Override
    public IModel<?> getDataModel(IModel<T> rowModel) {
        return () -> {
            IModel<List<ObjectReferenceType>> extractedRefs = extractDataModel(rowModel);
            List<ObjectReferenceType> referenceTypes = extractedRefs.getObject();

            return referenceTypes.stream()
                    .map(r -> WebComponentUtil.getOrigStringFromPoly(r.getTargetName()))
                    .collect(Collectors.joining(" -> "));
        };
    }

    public abstract IModel<List<ObjectReferenceType>> extractDataModel(IModel<T> rowModel);




}
