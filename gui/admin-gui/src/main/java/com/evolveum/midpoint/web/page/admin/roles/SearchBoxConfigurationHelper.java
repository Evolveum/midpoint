/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

public class SearchBoxConfigurationHelper implements Serializable {

    public static final String F_TENANT = "tenant";
    public static final String F_PROJECT = "project";

    public static final String F_RELATION_ITEM = "defaultRelationConfiguration";
    public static final String F_ORG_SEARCH_SCOPE_ITEM = "defaultScopeConfiguration";
    public static final String F_INDIRECT_ITEM = "defaultIndirectConfiguration";
    public static final String F_OBJECT_TYPE_ITEM = "defaultObjectTypeConfiguration";


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

    private ObjectReferenceType tenant = new ObjectReferenceType();
    private ObjectReferenceType project = new ObjectReferenceType();


    public SearchBoxConfigurationHelper(GuiObjectListPanelConfigurationType additionalPanel) {
        this.searchBoxConfigurationType = additionalPanel == null ? new SearchBoxConfigurationType() : additionalPanel.getSearchBoxConfiguration();
    }


    public SearchBoxConfigurationHelper(SearchBoxConfigurationType searchBoxConfigurationType) {
        this.searchBoxConfigurationType = searchBoxConfigurationType;
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
                defaultScopeConfiguration.setDefaultValue(SearchBoxScopeType.ONE_LEVEL);
            }
            setDisplay(defaultScopeConfiguration, "scope", "abstractRoleMemberPanel.searchScope", "abstractRoleMemberPanel.searchScope.tooltip");

        }
        return defaultScopeConfiguration;
    }

    @NotNull
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

    public boolean isRelationVisible() {
        return isSearchItemVisible(getDefaultRelationConfiguration());
    }

    public boolean isIndirectVisible() {
        return isSearchItemVisible(getDefaultIndirectConfiguration());
    }

    public boolean isIndirect() {
        return BooleanUtils.isTrue(getDefaultIndirectConfiguration().isIndirect());
    }

    public boolean isSearchScopeVisible() {
        return isSearchItemVisible(getDefaultSearchScopeConfiguration());
    }

    public boolean isSearchScope(SearchBoxScopeType scope) {
        return scope == getDefaultSearchScopeConfiguration().getDefaultValue();
    }

    public boolean isTenantVisible() {
        return isSearchItemVisible(getDefaultTenantConfiguration());
    }

    public boolean isProjectVisible() {
        return isSearchItemVisible(getDefaultProjectConfiguration());
    }

    private boolean isSearchItemVisible(UserInterfaceFeatureType feature) {
        if (feature == null) {
            return true;
        }
        return CompiledGuiProfile.isVisible(feature.getVisibility(), null);
    }

    public QName getDefaultRelation() {
        return getDefaultRelationConfiguration().getDefaultValue();
    }

    public @NotNull List<QName> getSupportedRelations() {
        return getDefaultRelationConfiguration().getSupportedRelations();
    }

    public List<QName> getSupportedObjectTypes() {
        return getDefaultObjectTypeConfiguration().getSupportedTypes();
    }

    public boolean isTenantEmpty() {
        return getTenant().getOid() == null || getTenant().getOid() == null || getTenant().asReferenceValue().isEmpty();
    }

    public boolean isProjectEmpty() {
        return getProject() == null || getProject().getOid() == null || getProject().asReferenceValue().isEmpty();
    }

    public ObjectReferenceType getTenant() {
        return tenant;
    }

    public ObjectReferenceType getProject() {
        return project;
    }

    public void resetTenantRef(){
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        this.tenant = ref;
    }

    public void resetProjectRef(){
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        this.project = ref;
    }
}
