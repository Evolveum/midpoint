/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.RelationSearchItemWrapper;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.model.*;

public class RelationSearchItemPanel extends SingleSearchItemPanel<RelationSearchItemWrapper> {

    public RelationSearchItemPanel(String id, IModel<RelationSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id,
                new PropertyModel(getModel(), RelationSearchItemWrapper.F_VALUE),
                new PropertyModel<>(getModel(), RelationSearchItemWrapper.F_SUPPORTED_RELATIONS), new RelationChoiceRenderer() , false);
        return inputPanel;
    }

    class RelationChoiceRenderer extends QNameIChoiceRenderer {

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

    }
}
