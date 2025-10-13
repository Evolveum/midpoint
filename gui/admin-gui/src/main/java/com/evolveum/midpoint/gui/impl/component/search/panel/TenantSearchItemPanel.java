/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.TenantSearchItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.schema.constants.RelationTypes;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

public class TenantSearchItemPanel extends SingleSearchItemPanel<TenantSearchItemWrapper> {

    public TenantSearchItemPanel(String id, IModel<TenantSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(id,
                new PropertyModel<>(getModel(), TenantSearchItemWrapper.F_VALUE),
                getTenantDefinition()) {

            @Override
            protected List<QName> getAllowedRelations() {
                return Collections.singletonList(RelationTypes.MEMBER.getRelation());
            }
        };
        searchItemField.setOutputMarkupId(true);
        return searchItemField;
    }

    public PrismReferenceDefinition getTenantDefinition() {
        return getModelObject() != null ? getModelObject().getTenantDefinition() : null;
    }

}
