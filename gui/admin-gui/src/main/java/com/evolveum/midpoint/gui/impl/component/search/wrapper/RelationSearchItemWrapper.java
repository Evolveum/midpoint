/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.RelationSearchItemPanel;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationSearchItemConfigurationType;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collections;
import java.util.List;

public class RelationSearchItemWrapper extends AbstractSearchItemWrapper<QName> {

    public static final String F_SUPPORTED_RELATIONS = "supportedRelations";

    private RelationSearchItemConfigurationType relationSearchItemConfigurationType;
    private List<QName> supportedRelations;


    public RelationSearchItemWrapper(RelationSearchItemConfigurationType relationSearchItemConfigurationType) {
        super();
        this.relationSearchItemConfigurationType = relationSearchItemConfigurationType;
        this.supportedRelations = relationSearchItemConfigurationType.getSupportedRelations();
    }

    @Override
    public boolean isEnabled() {
        return CollectionUtils.isNotEmpty(relationSearchItemConfigurationType.getSupportedRelations());
    }

    @Override
    public boolean isVisible() {
        return relationSearchItemConfigurationType == null
                || WebComponentUtil.getElementVisibility(relationSearchItemConfigurationType.getVisibility());
    }

    @Override
    public Class<RelationSearchItemPanel> getSearchItemPanelClass() {
        return RelationSearchItemPanel.class;
    }

    @Override
    public IModel<String> getName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = relationSearchItemConfigurationType == null ? null : relationSearchItemConfigurationType.getDisplay();
                var name = GuiDisplayTypeUtil.getTranslatedLabel(display);
                return StringUtils.isEmpty(name) ? LocalizationUtil.translate("relationDropDownChoicePanel.relation") : name;
            }
        };
    }

    @Override
    public IModel<String> getHelp() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = relationSearchItemConfigurationType == null ? null : relationSearchItemConfigurationType.getDisplay();
                var help = GuiDisplayTypeUtil.getHelp(display);
                return StringUtils.isEmpty(help) ? ("relationDropDownChoicePanel.tooltip.relation") : help;
            }
        };
    }


    @Override
    public IModel<String> getTitle() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                var display = relationSearchItemConfigurationType == null ? null : relationSearchItemConfigurationType.getDisplay();
                return GuiDisplayTypeUtil.getTooltip(display);
            }
        };
    }

    @Override
    public DisplayableValue<QName> getDefaultValue() {
        return new SearchValue<>(relationSearchItemConfigurationType.getDefaultValue());
    }

    public List<QName> getRelationsForSearch() {
        QName relation = getValue().getValue();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
            return relationSearchItemConfigurationType.getSupportedRelations();
        }

        return Collections.singletonList(relation);
    }

    public List<QName> getSupportedRelations() {
        return supportedRelations;
    }

    public RelationSearchItemConfigurationType getRelationSearchItemConfigurationType() {
        return relationSearchItemConfigurationType;
    }

}
