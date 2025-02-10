/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.Nullable;

/**
 * Compiled user profile. This class contains information about configuration and customization
 * of individual parts of user interface and user preferences. This class contains pre-processed
 * information in a form that is suitable to direct use by user interface code. The GUI should not
 * be required to do any complex processing on this.
 *
 * This idea is to compile the profile just once, on login time. Therefore only the authentication
 * code (GuiProfiledPrincipalManager) should modify this object. It should be considered read-only for all other
 * purposes.
 *
 * Later it may be split to interface and implementation parts.
 *
 * @since 4.0
 * @author Radovan Semancik
 */
@Experimental
public class CompiledGuiProfile implements DebugDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    private String defaultTimezone;
    private String preferredDataLanguage;
    private Boolean enableExperimentalFeatures;
    private Boolean useNewDesign = true; //default
    private List<RichHyperlinkType> additionalMenuLink = new ArrayList<>();
    private List<RichHyperlinkType> userDashboardLink = new ArrayList<>();
    private List<CompiledObjectCollectionView> objectCollectionViews = new ArrayList<>();
    private List<CompiledShadowCollectionView> shadowCollectionViews = new ArrayList<>();
    private CompiledObjectCollectionView defaultObjectCollectionView = null;
    private DefaultGuiObjectListPanelConfigurationType defaultObjectCollectionViewsSettings;
    private List<CompiledDashboardType> configurableDashboards = new ArrayList<>();
    private GuiExportSettingsType defaultExportSettings;
    private GuiObjectDetailsSetType objectDetails;
    private DefaultGuiObjectListPanelConfigurationType defaultObjectDetailsSettings;
    private FeedbackMessagesHookType feedbackMessagesHook;
    @Deprecated
    private AdminGuiConfigurationRoleManagementType roleManagement;
    private AccessRequestType accessRequest;
    private AdminGuiApprovalsConfigurationType approvals;
    private List<UserInterfaceFeatureType> features = new ArrayList<>();
    private AdminGuiConfigurationDisplayFormatsType displayFormats;
    private byte[] jpegPhoto;
    private Locale locale;
    private HomePageType homePage;
    private GuiObjectDetailsPageType selfProfilePage;

    private Set<String> dependencies = new HashSet<>();

    private boolean invalid = false;

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public String getPreferredDataLanguage() {
        return preferredDataLanguage;
    }

    public void setPreferredDataLanguage(String preferredDataLanguage) {
        this.preferredDataLanguage = preferredDataLanguage;
    }

    public Boolean isEnableExperimentalFeatures() {
        return enableExperimentalFeatures;
    }

    public void setEnableExperimentalFeatures(Boolean enableExperimentalFeatures) {
        this.enableExperimentalFeatures = enableExperimentalFeatures;
    }

    public void setUseNewDesign(Boolean useNewDesign) {
        this.useNewDesign = useNewDesign;
    }

    public Boolean isUseNewDesign() {
        return useNewDesign;
    }

    public GuiObjectDetailsPageType getSelfProfilePage() {
        return selfProfilePage;
    }

    public void setSelfProfilePage(GuiObjectDetailsPageType selfProfilePage) {
        this.selfProfilePage = selfProfilePage;
    }

    @NotNull
    public List<RichHyperlinkType> getAdditionalMenuLink() {
        return additionalMenuLink;
    }

    /**
     * Very likely to change in the future (for "flexible dashboards" feature).
     */
    @Experimental
    @NotNull
    public List<RichHyperlinkType> getUserDashboardLink() {
        return userDashboardLink;
    }

    public List<CompiledDashboardType> getConfigurableDashboards() {
        return configurableDashboards;
    }

    /**
     * Compiled information about all configured object list views.
     */
    @NotNull
    public List<CompiledObjectCollectionView> getObjectCollectionViews() {
        return objectCollectionViews;
    }

    @NotNull
    public List<CompiledShadowCollectionView> getShadowCollectionViews() {
        return shadowCollectionViews;
    }

    /**
     * Compiled information about object list view for a particular type.
     * If viewName is null then it returns view definition for "all objects" view,
     * e.g. "all users", "all roles".
     */
    public CompiledObjectCollectionView findObjectCollectionView(@NotNull QName objectType, String viewName) {
        for (CompiledObjectCollectionView objectCollectionView : objectCollectionViews) {
            if (objectCollectionView.match(objectType, viewName)) {
                return objectCollectionView;
            }
        }
        return defaultObjectCollectionView;
    }

    public CompiledShadowCollectionView findShadowCollectionView(@NotNull String resourceOid, ShadowKindType kindType, String intent) {
        for (CompiledShadowCollectionView shadowCollectionView : shadowCollectionViews) {
            if (shadowCollectionView.match(resourceOid, kindType, intent)) {
                return shadowCollectionView;
            }
        }
        return null;
    }

    /**
     * Find all views that are applicable for a particular object type. Returns views for all collections
     * and archetypes that are applicable for that type. Ideal to be used in constructing menus.
     */
    @NotNull
    public List<CompiledObjectCollectionView> findAllApplicableObjectCollectionViews(@NotNull QName objectType) {
        List<CompiledObjectCollectionView> applicableViews = new ArrayList<>();
        for (CompiledObjectCollectionView objectCollectionView : objectCollectionViews) {
            if (objectCollectionView.match(objectType)) {
                applicableViews.add(objectCollectionView);
            }
        }
        return applicableViews;
    }

    /**
     * Find all archetype views that are applicable for a particular object type. Returns views for
     * archetypes that are applicable for that type.
     */
    @NotNull
    public List<CompiledObjectCollectionView> findAllApplicableArchetypeViews(@NotNull QName objectType, OperationTypeType operationTypeType) {
        List<CompiledObjectCollectionView> applicableViews = findAllApplicableObjectCollectionViews(objectType);
        List<CompiledObjectCollectionView> archetypeViews = new ArrayList<>();
        for (CompiledObjectCollectionView objectCollectionView : applicableViews) {
            if (UserInterfaceElementVisibilityType.HIDDEN == objectCollectionView.getVisibility()) {
                continue;
            }
            if (!objectCollectionView.isApplicableForOperation(operationTypeType)) {
                continue;
            }
            ObjectReferenceType collectionRef = objectCollectionView.getCollection() != null ? objectCollectionView.getCollection().getCollectionRef() : null;
            if (collectionRef == null && objectCollectionView.isDefaultView()) { // e.g. All users, All roles, ...
                archetypeViews.add(objectCollectionView);
                continue;
            }

            QName collectionRefType = collectionRef != null ? collectionRef.getType() : null;
            if (collectionRefType != null && ArchetypeType.COMPLEX_TYPE.equals(collectionRefType)){
                archetypeViews.add(objectCollectionView);
            }
        }
        return archetypeViews;
    }

    @NotNull
    public <O extends ObjectType>List<CompiledObjectCollectionView> findAllApplicableArchetypeViews(@NotNull Class<O> objectType) {
        return findAllApplicableArchetypeViews(ObjectTypes.getObjectType(objectType).getTypeQName(), null);
    }

    @NotNull
    public <O extends ObjectType>List<CompiledObjectCollectionView> findAllApplicableArchetypeViews(@NotNull Class<O> objectType, OperationTypeType operationType) {
        return findAllApplicableArchetypeViews(ObjectTypes.getObjectType(objectType).getTypeQName(), operationType);
    }

    /**
     * Find archetype view for archetype defined by oid.
     */
    @Nullable
    public <O extends ObjectType> CompiledObjectCollectionView findApplicableArchetypeView(@NotNull String archetypeOid) {
        for (CompiledObjectCollectionView objectCollectionView : objectCollectionViews) {
            if (archetypeOid.equals(objectCollectionView.getArchetypeOid())) {
                return objectCollectionView;
            }
        }
        return null;
    }

    /**
     * Find all views that are applicable for a particular object type. Returns views for all collections
     * and archetypes that are applicable for that type. Ideal to be used in costructing menus.
     */
    @NotNull
    public <O extends ObjectType> List<CompiledObjectCollectionView> findAllApplicableObjectCollectionViews(Class<O> compileTimeClass) {
        return findAllApplicableObjectCollectionViews(ObjectTypes.getObjectType(compileTimeClass).getTypeQName());
    }

    /**
     * Default list view setting should never be needed publicly. Always check setting for specific
     * object type (and archetype).
     */
    public CompiledObjectCollectionView getDefaultObjectCollectionView() {
        return defaultObjectCollectionView;
    }

    public void setDefaultObjectCollectionView(CompiledObjectCollectionView defaultObjectCollectionView) {
        this.defaultObjectCollectionView = defaultObjectCollectionView;
    }

    public DefaultGuiObjectListPanelConfigurationType getDefaultObjectCollectionViewsSettings() {
        return defaultObjectCollectionViewsSettings;
    }

    public void setDefaultObjectCollectionViewsSettings(DefaultGuiObjectListPanelConfigurationType defaultObjectCollectionViewsSettings) {
        this.defaultObjectCollectionViewsSettings = defaultObjectCollectionViewsSettings;
    }

    public GuiExportSettingsType getDefaultExportSettings() {
        return defaultExportSettings;
    }

    public void setDefaultExportSettings(GuiExportSettingsType defaultExportSettings) {
        this.defaultExportSettings = defaultExportSettings;
    }

    /**
     * May change in the future.
     */
    @Experimental
    public GuiObjectDetailsSetType getObjectDetails() {
        return objectDetails;
    }

    @Experimental
    public void setObjectDetails(GuiObjectDetailsSetType objectDetails) {
        this.objectDetails = objectDetails;
    }

    public DefaultGuiObjectListPanelConfigurationType getDefaultObjectDetailsSettings() {
        return defaultObjectDetailsSettings;
    }

    public void setDefaultObjectDetailsSettings(DefaultGuiObjectListPanelConfigurationType defaultObjectDetailsSettings) {
        this.defaultObjectDetailsSettings = defaultObjectDetailsSettings;
    }

    public <O extends ObjectType> GuiObjectDetailsPageType findObjectDetailsConfiguration(Class<O> compileTimeClass) {
        if (objectDetails == null) {
            return null;
        }
        return findObjectConfiguration(objectDetails.getObjectDetailsPage(), compileTimeClass);
    }

    public <O extends ObjectType> GuiObjectDetailsPageType findObjectDetailsConfiguration(QName typeQName) {
        if (objectDetails == null) {
            return new GuiObjectDetailsPageType().type(typeQName);
        }
        GuiObjectDetailsPageType result = findObjectConfiguration(objectDetails.getObjectDetailsPage(), typeQName);

        return result != null ? result : new GuiObjectDetailsPageType().type(typeQName);
    }

    public <O extends ObjectType> GuiResourceDetailsPageType findResourceDetailsConfiguration(String connectorOid){
        if (objectDetails == null) {
            return new GuiResourceDetailsPageType();
        }

        GuiResourceDetailsPageType applicableForAll = new GuiResourceDetailsPageType();

        for (GuiResourceDetailsPageType resourceDetailsPageType : objectDetails.getResourceDetailsPage()) {
            if (resourceDetailsPageType.getConnectorRef() == null) {
                applicableForAll = resourceDetailsPageType;
                continue;
            }
            if (resourceDetailsPageType.getConnectorRef().getOid() != null
                    && resourceDetailsPageType.getConnectorRef().getOid().equals(connectorOid)) {
                return resourceDetailsPageType;
            }
        }

        return applicableForAll;
    }

    public <O extends ObjectType> GuiShadowDetailsPageType findShadowDetailsConfiguration(ResourceShadowCoordinates coordinates) {
        if (objectDetails == null) {
            return null;
        }

        for (GuiShadowDetailsPageType shadowDetailsPageType : objectDetails.getShadowDetailsPage()) {
            if (applicableForAll(shadowDetailsPageType)) {
                return shadowDetailsPageType;
            }
            if (shadowDetailsPageType.getResourceRef() == null) {
                continue;
            }
            if (!coordinates.getResourceOid().equals(shadowDetailsPageType.getResourceRef().getOid())) {
                continue;
            }
            if (coordinates.getKind() != shadowDetailsPageType.getKind()) {
                continue;
            }
            if (!coordinates.getIntent().equals(shadowDetailsPageType.getIntent())) {
                continue;
            }
            return shadowDetailsPageType;
        }

        return null;
    }

    private boolean applicableForAll(GuiShadowDetailsPageType shadowDetailsPageType) {
        return shadowDetailsPageType.getResourceRef() == null && shadowDetailsPageType.getKind() == null && shadowDetailsPageType.getIntent() == null;
    }

    private <T extends AbstractObjectTypeConfigurationType, O extends ObjectType> T findObjectConfiguration(
            List<T> list, Class<O> type) {
        QName typeQName = ObjectTypes.getObjectType(type).getTypeQName();
        return findObjectConfiguration(list, typeQName);
    }

    private <T extends AbstractObjectTypeConfigurationType> T findObjectConfiguration(
            List<T> list, QName typeQName) {
        if (list == null) {
            return null;
        }
        for (T item: list) {
            if (QNameUtil.match(item.getType(), typeQName)) {
                return item;
            }
        }
        for (T item: list) {
            if (item.getType() == null) {
                return item;
            }
        }
        return null;
    }


    public FeedbackMessagesHookType getFeedbackMessagesHook() {
        return feedbackMessagesHook;
    }

    public void setFeedbackMessagesHook(FeedbackMessagesHookType feedbackMessagesHook) {
        this.feedbackMessagesHook = feedbackMessagesHook;
    }

    public AdminGuiConfigurationRoleManagementType getRoleManagement() {
        return roleManagement;
    }

    public void setRoleManagement(AdminGuiConfigurationRoleManagementType roleManagement) {
        this.roleManagement = roleManagement;
    }

    public AdminGuiApprovalsConfigurationType getApprovals() {
        return approvals;
    }

    public void setApprovals(AdminGuiApprovalsConfigurationType approvals) {
        this.approvals = approvals;
    }

    public Boolean isExpandRolesOnApprovalPreview() {
        return approvals != null ? approvals.isExpandRolesOnPreview() : null;
    }

    public List<UserInterfaceFeatureType> getFeatures() {
        return features;
    }

    public UserInterfaceFeatureType findFeature(String identifier) {
        return findFeature(features, identifier);
    }

    public static <T extends UserInterfaceFeatureType> T findFeature(List<T> features, String identifier) {
        for (T feature: features) {
            if (feature.getIdentifier().equals(identifier)) {
                return feature;
            }
        }
        return null;
    }

    public AdminGuiConfigurationDisplayFormatsType getDisplayFormats() {
        return displayFormats;
    }

    public void setDisplayFormats(AdminGuiConfigurationDisplayFormatsType displayFormats) {
        this.displayFormats = displayFormats;
    }

    public byte[] getJpegPhoto() {
        return jpegPhoto;
    }

    public void setJpegPhoto(byte[] jpegPhoto) {
        this.jpegPhoto = jpegPhoto;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public UserInterfaceElementVisibilityType getFeatureVisibility(String identifier) {
        UserInterfaceFeatureType feature = findFeature(identifier);
        if (feature == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        UserInterfaceElementVisibilityType visibility = feature.getVisibility();
        if (visibility == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        return visibility;
    }

    public boolean isFeatureVisible(String identifier) {
        return isFeatureVisible(identifier, null);
    }

    public boolean isFeatureVisible(String identifier, BooleanSupplier automaticPredicate) {
        UserInterfaceElementVisibilityType visibility = getFeatureVisibility(identifier);
        return isVisible(visibility, automaticPredicate);
    }

    public static boolean isVisible(UserInterfaceElementVisibilityType visibility, BooleanSupplier automaticPredicate) {
        if (visibility == UserInterfaceElementVisibilityType.HIDDEN) {
            return false;
        }
        if (visibility == UserInterfaceElementVisibilityType.VISIBLE) {
            return true;
        }
        if (visibility == UserInterfaceElementVisibilityType.AUTOMATIC) {
            if (automaticPredicate == null) {
                return true;
            } else {
                return automaticPredicate.getAsBoolean();
            }
        }
        return false;
    }

    // TODO: later: information about menu structure

    public AccessRequestType getAccessRequest() {
        return accessRequest;
    }

    public void setAccessRequest(AccessRequestType accessRequest) {
        this.accessRequest = accessRequest;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CompiledGuiProfile.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "defaultTimezone", defaultTimezone, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "preferredDataLanguage", preferredDataLanguage, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "enableExperimentalFeatures", enableExperimentalFeatures, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "enableExperimentalFeatures", useNewDesign, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "additionalMenuLink", additionalMenuLink, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "userDashboardLink", userDashboardLink, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectCollectionViews", objectCollectionViews, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "defaultObjectCollectionView", defaultObjectCollectionView, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "homePage", homePage, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "defaultExportSettings", defaultExportSettings, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectDetails", objectDetails, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "feedbackMessagesHook", feedbackMessagesHook, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "roleManagement", roleManagement, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "approvals", approvals, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "features", features, indent + 1);
        return sb.toString();
    }

    /**
     * Returns true if compiled profile is derived from provided OID
     */
    public boolean derivedFrom(String oid) {
        return dependencies.contains(oid);
    }

    public void markInvalid() {
        invalid  = true;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setDependencies(Set<String> value) {
        dependencies = value;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public HomePageType getHomePage() {
        return homePage;
    }

    public void setHomePage(HomePageType homePage) {
        this.homePage = homePage;
    }

    public ContainerPanelConfigurationType findPrincipalFocusDetailsPanel(@NotNull QName focusType, @NotNull String panelType) {
        if (getObjectDetails() == null || CollectionUtils.isEmpty(getObjectDetails().getObjectDetailsPage())) {
            return null;
        }
        List<GuiObjectDetailsPageType> focusDetailsList = getObjectDetails().getObjectDetailsPage();
        GuiObjectDetailsPageType focusDetailsPage = null;
        for (GuiObjectDetailsPageType detailsPage : focusDetailsList) {
            if (QNameUtil.match(detailsPage.getType(), focusType)) {
                focusDetailsPage = detailsPage;
                break;
            }
        }
        if (focusDetailsPage != null && CollectionUtils.isNotEmpty(focusDetailsPage.getPanel())) {
            for (ContainerPanelConfigurationType panel : focusDetailsPage.getPanel()) {
                if (panelType.equals(panel.getPanelType())) {
                    return panel;
                }
            }
        }
        return null;
    }
}
