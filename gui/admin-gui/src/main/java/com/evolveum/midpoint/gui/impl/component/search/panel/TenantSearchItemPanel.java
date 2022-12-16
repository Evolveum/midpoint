/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.TenantSearchItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.web.component.search.ReferenceValueSearchPanel;

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
        return null; //this part is taken from ProjectSearchItem, it is not clear why we return null here
    }

}
