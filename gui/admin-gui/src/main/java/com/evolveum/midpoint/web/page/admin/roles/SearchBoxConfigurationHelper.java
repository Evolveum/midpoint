/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class SearchBoxConfigurationHelper implements Serializable {

    private SearchBoxConfigurationType searchBoxConfigurationType;

    private ScopeSearchItemConfigurationType defaultScopeConfiguration;
    private ObjectTypeSearchItemConfigurationType defaultObjectTypeConfiguration;
    private RelationSearchItemConfigurationType defaultRelationConfiguration = null;
    private IndirectSearchItemConfigurationType defaultIndirectConfiguration = null;
    private UserInterfaceFeatureType defaultTenantConfiguration = null;
    private UserInterfaceFeatureType defaultProjectConfiguration = null;

    private List<QName> supportedRelations;
    private List<QName> supportedObjectTypes;
    private QName defaultRelation;
    private QName defaultObjectType;


    public SearchBoxConfigurationHelper(GuiObjectListPanelConfigurationType additionalPanel) {
        this.searchBoxConfigurationType = additionalPanel == null ? new SearchBoxConfigurationType() : additionalPanel.getSearchBoxConfiguration();
    }

    public void setDefaultSupportedRelations(List<QName> defaultSupportedRelations) {
        this.supportedRelations = defaultSupportedRelations;
    }

    public void setDefaultSupportedObjectTypes(List<QName> defaultSupportedObjectTypes) {
        this.supportedObjectTypes = defaultSupportedObjectTypes;
    }

    public void setDefaultRelation(QName defaultRelation) {
        this.defaultRelation = defaultRelation;
    }

    public void setDefaultObjectType(QName defaultObjectType) {
        this.defaultObjectType = defaultObjectType;
    }

    public ScopeSearchItemConfigurationType getDefaultSearchScopeConfiguration() {
        if (defaultScopeConfiguration == null) {
            defaultScopeConfiguration = searchBoxConfigurationType.getScopeConfiguration();
            if (defaultScopeConfiguration == null) {
                defaultScopeConfiguration = new ScopeSearchItemConfigurationType();
            }
            if (defaultScopeConfiguration.getDefaultValue() == null) {
                defaultScopeConfiguration.setDefaultValue(searchBoxConfigurationType.getDefaultScope());
            }

            if (defaultScopeConfiguration == null) {
                defaultScopeConfiguration = new ScopeSearchItemConfigurationType();
            }
            if (defaultScopeConfiguration.getDefaultValue() == null) {
                defaultScopeConfiguration.setDefaultValue(SearchBoxScopeType.ONE_LEVEL);
            }
            setDisplay(defaultScopeConfiguration, "scope", "abstractRoleMemberPanel.searchScope", "abstractRoleMemberPanel.searchScope.tooltip");

        }
        return defaultScopeConfiguration;
    }

    public ObjectTypeSearchItemConfigurationType getDefaultObjectTypeConfiguration() {
        if (defaultObjectTypeConfiguration == null) {
            defaultObjectTypeConfiguration = searchBoxConfigurationType.getObjectTypeConfiguration();

            if (defaultObjectTypeConfiguration == null) {
                defaultObjectTypeConfiguration = new ObjectTypeSearchItemConfigurationType();
            }
            if (defaultObjectTypeConfiguration.getVisibility() == null) {
                defaultObjectTypeConfiguration.setVisibility(UserInterfaceElementVisibilityType.AUTOMATIC);
            }
        }
        if (defaultObjectTypeConfiguration.getDefaultValue() == null) {
            defaultObjectTypeConfiguration.setDefaultValue(defaultObjectType);
        }

        if (CollectionUtils.isEmpty(defaultObjectTypeConfiguration.getSupportedTypes())) {
            defaultObjectTypeConfiguration.getSupportedTypes().addAll(supportedObjectTypes);
        }
        return defaultObjectTypeConfiguration;
    }

    public RelationSearchItemConfigurationType getDefaultRelationConfiguration() {
        if (defaultRelationConfiguration == null) {
            defaultRelationConfiguration = searchBoxConfigurationType.getRelationConfiguration();
            if (defaultRelationConfiguration == null) {
                defaultRelationConfiguration = new RelationSearchItemConfigurationType();
            }
            setDisplay(defaultRelationConfiguration, "relation","relationDropDownChoicePanel.relation", "relationDropDownChoicePanel.tooltip.relation");

        }
        if (CollectionUtils.isEmpty(defaultRelationConfiguration.getSupportedRelations())) {
            defaultRelationConfiguration.getSupportedRelations().addAll(supportedRelations);
        }

        if (defaultRelationConfiguration.getDefaultValue() == null) {
            QName defaultRelation = getDefaultRelationAllowAny(defaultRelationConfiguration.getSupportedRelations());
            defaultRelationConfiguration.setDefaultValue(defaultRelation);
        }

        return defaultRelationConfiguration;
    }

    private QName getDefaultRelationAllowAny(List<QName> availableRelationList) {
        if (availableRelationList != null && availableRelationList.size() == 1) {
            return availableRelationList.get(0);
        }
        return PrismConstants.Q_ANY;
    }


    public IndirectSearchItemConfigurationType getDefaultIndirectConfiguration() {
        if (defaultIndirectConfiguration == null) {
            defaultIndirectConfiguration = searchBoxConfigurationType.getIndirectConfiguration();
            if (defaultIndirectConfiguration == null) {
                defaultIndirectConfiguration = new IndirectSearchItemConfigurationType();
            }
            if (defaultIndirectConfiguration.isIndirect() == null) {
                defaultIndirectConfiguration.setIndirect(false);
            }
            setDisplay(defaultIndirectConfiguration, "indirect", "abstractRoleMemberPanel.indirectMembers", "abstractRoleMemberPanel.indirectMembers.tooltip");
        }
        return defaultIndirectConfiguration;
    }

    public UserInterfaceFeatureType getDefaultTenantConfiguration() {
        if (defaultTenantConfiguration == null) {
            defaultTenantConfiguration = searchBoxConfigurationType.getTenantConfiguration();
            if (defaultTenantConfiguration == null) {
                defaultTenantConfiguration = new UserInterfaceFeatureType();
            }
            setDisplay(defaultTenantConfiguration, "tenant","abstractRoleMemberPanel.tenant", null);
        }
        return defaultTenantConfiguration;
    }

    public UserInterfaceFeatureType getDefaultProjectConfiguration() {
        if (defaultProjectConfiguration == null) {
            defaultProjectConfiguration = searchBoxConfigurationType.getProjectConfiguration();
            if (defaultProjectConfiguration == null) {
                defaultProjectConfiguration = new UserInterfaceFeatureType();
            }
            setDisplay(defaultProjectConfiguration, "project/org", "abstractRoleMemberPanel.project", null);
        }
        return defaultProjectConfiguration;
    }

    private void setDisplay(UserInterfaceFeatureType configuration, String orig, String labelKey, String helpKey) {
        if (configuration.getDisplay() == null) {
            DisplayType display = new DisplayType();
            configuration.setDisplay(display);
        }
        if (configuration.getDisplay().getLabel() == null) {
            DisplayType display = configuration.getDisplay();
            PolyStringType label = new PolyStringType(orig);
            PolyStringTranslationType translationLabel = new PolyStringTranslationType();
            translationLabel.setKey(labelKey);
            label.setTranslation(translationLabel);
            display.setLabel(label);
            configuration.setDisplay(display);
        }
        if (helpKey != null && configuration.getDisplay().getHelp() == null) {
            DisplayType display = configuration.getDisplay();
            PolyStringType help = new PolyStringType("");
            PolyStringTranslationType translationHelp = new PolyStringTranslationType();
            translationHelp.setKey(helpKey);
            help.setTranslation(translationHelp);
            display.setHelp(help);
            configuration.setDisplay(display);
        }
        if (configuration.getVisibility() == null) {
            configuration.setVisibility(UserInterfaceElementVisibilityType.AUTOMATIC);
        }
    }


}
