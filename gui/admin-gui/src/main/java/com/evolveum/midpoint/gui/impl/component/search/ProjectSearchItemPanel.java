/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.web.component.search.ReferenceValueSearchPanel;

public class ProjectSearchItemPanel extends AbstractSearchItemPanel<ProjectSearchItemWrapper> {

    public ProjectSearchItemPanel(String id, IModel<ProjectSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        PrismReferenceDefinition projectRefDef = getProjectRefDef();
        ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(ID_SEARCH_ITEM_FIELD,
                new PropertyModel<>(getModel(), ProjectSearchItemWrapper.F_SEARCH_CONFIG + "." + SearchConfigurationWrapper.F_PROJECT),
                projectRefDef) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getAllowedRelations() {
                return Collections.singletonList(RelationTypes.MEMBER.getRelation());
            }
        };
        return searchItemField;
    }

    protected PrismReferenceDefinition getProjectRefDef() {
        return null; //this part is taken from ProjectSearchItem, it is not clear why we return null here
    }

}
