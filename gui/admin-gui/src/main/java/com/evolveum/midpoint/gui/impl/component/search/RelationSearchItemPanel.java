/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationSearchItemConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.model.*;

public class RelationSearchItemPanel extends AbstractSearchItemPanel<RelationSearchItemWrapper> {

    public RelationSearchItemPanel(String id, IModel<RelationSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        ReadOnlyModel<List<QName>> availableRelations = new ReadOnlyModel<>(() -> {
            List<QName> choices = new ArrayList<>();
            List<QName> relations = getModelObject().getSearchConfig().getSupportedRelations();
            if (relations != null && relations.size() > 1) {
                choices.add(PrismConstants.Q_ANY);
            }
            choices.addAll(relations);
            return choices;
        });


        DropDownChoicePanel inputPanel = new DropDownChoicePanel(ID_SEARCH_ITEM_FIELD,
                new PropertyModel(getModel(), RelationSearchItemWrapper.F_SEARCH_CONFIG + "." + SearchConfigurationWrapper.F_RELATION),
                availableRelations, new QNameIChoiceRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(QName relation) {
                RelationDefinitionType relationDef = WebComponentUtil.getRelationDefinition(relation);
                if (relationDef != null) {
                    DisplayType display = relationDef.getDisplay();
                    if (display != null) {
                        PolyStringType label = display.getLabel();
                        if (PolyStringUtils.isNotEmpty(label)) {
                            return WebComponentUtil.getTranslatedPolyString(label);
                        }
                    }
                }
                if (QNameUtil.match(PrismConstants.Q_ANY, relation)) {
                    return new ResourceModel("RelationTypes.ANY", relation.getLocalPart()).getObject();
                }
                return super.getDisplayValue(relation);
            }
        }, false);
        return inputPanel;
    }

}
