/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class AbstractRoleMemberSearchConfiguration implements Serializable {

    private SearchBoxConfigurationType searchBoxConfigurationType;

    private ScopeSearchItemConfigurationType defaultScopeConfiguration;
    private ObjectTypeSearchItemConfigurationType defaultObjectTypeConfiguration;
    private RelationSearchItemConfigurationType defaultRelationConfiguration = null;
    private IndirectSearchItemConfigurationType defaultIndirectConfiguration = null;
    private UserInterfaceFeatureType defaultTenantConfiguration = null;
    private UserInterfaceFeatureType defaultProjectConfiguration = null;


    private Class<ObjectType> defaultObjectTypeClass = null;



    public AbstractRoleMemberSearchConfiguration(GuiObjectListPanelConfigurationType additionalPanel) {
        this.searchBoxConfigurationType = additionalPanel == null ? new SearchBoxConfigurationType() : additionalPanel.getSearchBoxConfiguration();
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

    public UserInterfaceElementVisibilityType getDefaultSearchScopeVisibility() {
        if (getDefaultSearchScopeConfiguration() == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }

        return getDefaultSearchScopeConfiguration().getVisibility();
    }

    public PolyStringType getDefaultSearchScopeLabel() {
        return getDefaultSearchScopeConfiguration().getDisplay().getLabel();
    }

    public PolyStringType getDefaultSearchScopeHelp() {
        return getDefaultSearchScopeConfiguration().getDisplay().getHelp();
    }

    private UserInterfaceElementVisibilityType getVisibility(UserInterfaceFeatureType feature) {
        if (feature == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }

        return feature.getVisibility();
    }

    public ObjectTypeSearchItemConfigurationType getDefaultObjectTypeConfiguration() {
        if (defaultObjectTypeConfiguration == null) {
            defaultObjectTypeConfiguration = searchBoxConfigurationType.getObjectTypeConfiguration();

//        if (defaultObjectTypeConfiguration.getDefaultValue() == null) {
//            if (searchBoxConfigurationType.getDefaultObjectType() != null) {
//                defaultObjectTypeClass = (Class<ObjectType>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
//                        searchBoxConfigurationType.getDefaultObjectType());
//            }
//        } else {
//            defaultObjectTypeClass = (Class<ObjectType>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
//                    defaultObjectTypeConfiguration.getDefaultValue());
//        }

//        if (defaultObjectTypeClass == null) {
//            defaultObjectTypeClass = getDefaultObjectType();
//        }
            if (defaultObjectTypeConfiguration == null) {
                defaultObjectTypeConfiguration = new ObjectTypeSearchItemConfigurationType();
            }
            if (defaultObjectTypeConfiguration.getVisibility() == null) {
                defaultObjectTypeConfiguration.setVisibility(UserInterfaceElementVisibilityType.AUTOMATIC);
            }
        }
        return defaultObjectTypeConfiguration;
    }

    public QName getDefaultObjectTypeQName() {
        ObjectTypeSearchItemConfigurationType defaultObjectClassConfig = getDefaultObjectTypeConfiguration();
        QName defaultObjectTypeQName = defaultObjectClassConfig.getDefaultValue();
        if (defaultObjectTypeQName == null) {
            defaultObjectTypeQName = searchBoxConfigurationType.getDefaultObjectType();
        }
        return defaultObjectTypeQName;
    }

    public RelationSearchItemConfigurationType getDefaultRelationConfiguration() {
        if (defaultRelationConfiguration == null) {
            defaultRelationConfiguration = searchBoxConfigurationType.getRelationConfiguration();
            if (defaultRelationConfiguration == null) {
                defaultRelationConfiguration = new RelationSearchItemConfigurationType();
            }
            setDisplay(defaultRelationConfiguration, "relation","relationDropDownChoicePanel.relation", "relationDropDownChoicePanel.tooltip.relation");

        }
        return defaultRelationConfiguration;
    }

    public UserInterfaceElementVisibilityType getDefaultRelationVisibility() {
        return getVisibility(getDefaultRelationConfiguration());
    }

    public PolyStringType getRelationConfigurationLabel() {
        return getDefaultRelationConfiguration().getDisplay().getLabel();
    }

    public PolyStringType getRelationConfigurationHelp() {
        return getDefaultRelationConfiguration().getDisplay().getHelp();
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

    public UserInterfaceElementVisibilityType getDefaultIndirecVisibility() {
        return getVisibility(getDefaultIndirectConfiguration());
    }

    public PolyStringType getDefaultIndirectLabel() {
        return getDefaultIndirectConfiguration().getDisplay().getLabel();
    }

    public PolyStringType getDefaultIndirectHelp() {
        return getDefaultIndirectConfiguration().getDisplay().getHelp();
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

    public UserInterfaceElementVisibilityType getDefaultTenantVisibility() {
        return getVisibility(getDefaultTenantConfiguration());
    }

    public PolyStringType getDefaultTenantLabel() {
        return getDefaultTenantConfiguration().getDisplay().getLabel();
    }

    public PolyStringType getDefaultTenantHelp() {
        return getDefaultTenantConfiguration().getDisplay().getHelp();
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

    public UserInterfaceElementVisibilityType getDefaultProjectVisibility() {
        return getVisibility(getDefaultProjectConfiguration());
    }

    public PolyStringType getDefaultProjectLabel() {
        return getDefaultProjectConfiguration().getDisplay().getLabel();
    }

    public PolyStringType getDefaultProjectHelp() {
        return getDefaultProjectConfiguration().getDisplay().getHelp();
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
