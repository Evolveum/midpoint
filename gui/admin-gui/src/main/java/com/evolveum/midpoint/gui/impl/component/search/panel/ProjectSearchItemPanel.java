/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ProjectSearchItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.schema.constants.RelationTypes;

public class ProjectSearchItemPanel extends SingleSearchItemPanel<ProjectSearchItemWrapper> {

    public ProjectSearchItemPanel(String id, IModel<ProjectSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(id,
                new PropertyModel<>(getModel(), ProjectSearchItemWrapper.F_VALUE),
                getProjectDefinition()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getAllowedRelations() {
                return Collections.singletonList(RelationTypes.MEMBER.getRelation());
            }
        };
        searchItemField.setOutputMarkupId(true);
        return searchItemField;
    }

    private PrismReferenceDefinition getProjectDefinition() {
        return getModelObject() != null ? getModelObject().getProjectRefDef() : null;
    }


}
