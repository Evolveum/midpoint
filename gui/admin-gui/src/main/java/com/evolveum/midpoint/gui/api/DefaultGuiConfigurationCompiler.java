/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.certification.column.AbstractGuiColumn;
import com.evolveum.midpoint.web.application.ActionType;

import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilable;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilerRegistry;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Component
public class DefaultGuiConfigurationCompiler implements GuiProfileCompilable {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultGuiConfigurationCompiler.class);

    @Autowired private GuiProfileCompilerRegistry registry;
    @Autowired private PrismContext prismContext;
    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    @Value("${midpoint.additionalPackagesToScan:}") private String additionalPackagesToScan;

    private static final String COLLECTION_PACKAGES_KEY = "collectionPackages";

    private static final String[] COLLECTION_PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.page.admin.archetype",
            "com.evolveum.midpoint.web.page.admin.cases",
            "com.evolveum.midpoint.web.page.admin.objectCollection",
            "com.evolveum.midpoint.web.page.admin.objectTemplate",
            "com.evolveum.midpoint.web.page.admin.orgs",
            "com.evolveum.midpoint.web.page.admin.reports",
            "com.evolveum.midpoint.web.page.admin.resources",
            "com.evolveum.midpoint.web.page.admin.roles",
            "com.evolveum.midpoint.web.page.admin.services",
            "com.evolveum.midpoint.web.page.admin.users",
            "com.evolveum.midpoint.web.page.admin.server",
            "com.evolveum.midpoint.web.component.action",
            "com.evolveum.midpoint.gui.impl.page.admin.certification.column"
    };

    private static final Map<Object, Collection<Class<?>>> SCANNED_CLASSES_MAP = new HashMap<>();

    private static final Map<String, Class<? extends Panel>> PANELS_MAP = new HashMap<>();

    private static final Map<String, Class<? extends AbstractGuiAction<?>>> ACTIONS_MAP = new HashMap<>();

    private static final Map<String, Class<? extends AbstractGuiColumn<?, ?>>> COLUMNS_MAP = new HashMap<>();

    private static final Map<String, SimpleCounter<?, ?>> COUNTERS_MAP = new HashMap<>();

    private Boolean experimentalFeaturesEnabled = false;

    private static synchronized Collection<Class<?>> getClassesForAnnotation(
            Class<? extends Annotation> annotation, String additionalPackagesToScan) {
        Collection<Class<?>> result = SCANNED_CLASSES_MAP.get(annotation);
        if (result != null) {
            return result;
        }

        result = ClassPathUtil.scanClasses(annotation,
                StringUtils.joinWith(",", ClassPathUtil.DEFAULT_PACKAGE_TO_SCAN, additionalPackagesToScan));

        result = Collections.unmodifiableCollection(result);

        SCANNED_CLASSES_MAP.put(annotation, result);

        return result;
    }

    private static synchronized Collection<Class<?>> getCollectionClasses() {
        Collection<Class<?>> result = SCANNED_CLASSES_MAP.get(COLLECTION_PACKAGES_KEY);
        if (result != null) {
            return result;
        }

        result = ClassPathUtil.listClasses(COLLECTION_PACKAGES_TO_SCAN);

        result = Collections.unmodifiableCollection(result);

        SCANNED_CLASSES_MAP.put(COLLECTION_PACKAGES_KEY, result);

        return result;
    }

    private Collection<Class<?>> getPanelInstanceClasses() {
        Collection<Class<?>> result = new HashSet<>();
        result.addAll(getClassesForAnnotation(PanelInstance.class, additionalPackagesToScan));
        result.addAll(getClassesForAnnotation(PanelInstances.class, additionalPackagesToScan));

        return Collections.unmodifiableCollection(result);
    }

    private Collection<Class<?>> getPanelTypeClasses() {
        return getClassesForAnnotation(PanelType.class, additionalPackagesToScan);
    }

    private Collection<Class<?>> getActionTypeClasses() {
        return getClassesForAnnotation(ActionType.class, additionalPackagesToScan);
    }

    private Collection<Class<?>> getColumnTypeClasses() {
        return getClassesForAnnotation(ColumnType.class, additionalPackagesToScan);
    }

    @PostConstruct
    public void init() {
        fillInPanelsMap();
        fillInCountersMap();
        fillInActionsMap();
        fillInColumnsMap();

        registry.registerCompiler(this);
    }

    public Class<? extends Panel> findPanel(String identifier) {
        return PANELS_MAP.get(identifier);
    }

    public Class<? extends AbstractGuiAction<?>> findAction(String identifier) {
        return ACTIONS_MAP.get(identifier);
    }

    public Class<? extends AbstractGuiColumn<?, ?>> findColumn(String identifier) {
        return COLUMNS_MAP.get(identifier);
    }

    public List<Class<? extends AbstractGuiColumn<?, ?>>> findAllApplicableColumns(Class<? extends Containerable> clazz) {
        return COLUMNS_MAP
                .values()
                .stream()
                .filter(column -> column.getAnnotation(ColumnType.class).applicableForType().isAssignableFrom(clazz))
                .toList();
    }

    public SimpleCounter findCounter(String identifier) {
        return COUNTERS_MAP.get(identifier);
    }

    @Override
    public void postProcess(CompiledGuiProfile compiledGuiProfile) {
        experimentalFeaturesEnabled = compiledGuiProfile.isEnableExperimentalFeatures();

        compileDefaultDetailsPages(compiledGuiProfile);
        mergeCollectionViewsWithDefault(compiledGuiProfile);
        processShadowPanels(compiledGuiProfile);
        processResourcePanels(compiledGuiProfile);
        processSelfProfilePageConfig(compiledGuiProfile);
    }

    private void compileDefaultDetailsPages(CompiledGuiProfile compiledGuiProfile) {
        GuiObjectDetailsSetType defaultDetailsPages = compileDefaultGuiObjectDetailsSetType();
        List<GuiObjectDetailsPageType> detailsPages = defaultDetailsPages.getObjectDetailsPage();
        for (GuiObjectDetailsPageType defaultDetailsPage : detailsPages) {

            //objects
            GuiObjectDetailsPageType compiledPageType =
                    compiledGuiProfile.findObjectDetailsConfiguration(defaultDetailsPage.getType());
            GuiObjectDetailsPageType mergedDetailsPage =
                    adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(defaultDetailsPage, compiledPageType);

            if (compiledGuiProfile.getObjectDetails() == null) {
                compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType(prismContext));
            }
            compiledGuiProfile.getObjectDetails().getObjectDetailsPage()
                    .removeIf(p -> QNameUtil.match(p.getType(), defaultDetailsPage.getType()));
            compiledGuiProfile.getObjectDetails().getObjectDetailsPage().add(mergedDetailsPage.cloneWithoutId());
        }
    }

    private void mergeCollectionViewsWithDefault(CompiledGuiProfile compiledGuiProfile) {
        Collection<Class<?>> classes = getCollectionClasses();
        List<CompiledObjectCollectionView> defaultCollectionViews = compileDefaultCollectionViews(classes);

        for (CompiledObjectCollectionView defaultCollectionView : defaultCollectionViews) {
            CompiledObjectCollectionView compiledObjectCollectionView =
                    compiledGuiProfile.findObjectCollectionView(
                            defaultCollectionView.getContainerType(), defaultCollectionView.getViewIdentifier());
            if (compiledObjectCollectionView == null) {
                compiledGuiProfile.getObjectCollectionViews().add(defaultCollectionView);
                continue;
            }
            if (!compiledObjectCollectionView.isDefaultView()) {
                compiledObjectCollectionView.setDefaultView(true);
            }
            mergeCollectionViews(compiledObjectCollectionView, defaultCollectionView);
        }

    }

    private void processSelfProfilePageConfig(CompiledGuiProfile compiledGuiProfile) {
        if (compiledGuiProfile.getSelfProfilePage() == null || compiledGuiProfile.getSelfProfilePage().getType() == null) {
            return;
        }
        Class<? extends Containerable> principalFocusType = prismContext.getSchemaRegistry()
                .determineClassForType(compiledGuiProfile.getSelfProfilePage().getType());
        GuiObjectDetailsPageType defaultSelfProfilePage = compileDefaultGuiObjectDetailsPage(principalFocusType);
        compiledGuiProfile.setSelfProfilePage(adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(
                defaultSelfProfilePage, compiledGuiProfile.getSelfProfilePage()).cloneWithoutId());
    }

    private void mergeCollectionViews(CompiledObjectCollectionView compiledObjectCollectionView,
            CompiledObjectCollectionView defaultCollectionView) {
        DisplayType displayType = adminGuiConfigurationMergeManager.mergeDisplayType(
                compiledObjectCollectionView.getDisplay(), defaultCollectionView.getDisplay());
        compiledObjectCollectionView.setDisplay(displayType);

        if (compiledObjectCollectionView.getApplicableForOperation() == null) {
            compiledObjectCollectionView.setApplicableForOperation(defaultCollectionView.getApplicableForOperation());
        }
    }

    private List<CompiledObjectCollectionView> compileDefaultCollectionViews(Collection<Class<?>> classes) {
        List<CompiledObjectCollectionView> compiledObjectCollectionViews = new ArrayList<>();
        for (Class<?> clazz : classes) {
            CollectionInstance collectionInstance = clazz.getAnnotation(CollectionInstance.class);
            if (collectionInstance == null) {
                continue;
            }

            Class<? extends Containerable> applicableForType = collectionInstance.applicableForType();
            QName type;
            if (ObjectType.class.isAssignableFrom(applicableForType)) {
                ObjectTypes objectType = ObjectTypes.getObjectType((Class<? extends ObjectType>) applicableForType);
                type = objectType.getTypeQName();
            } else {
                type = prismContext.getSchemaRegistry().determineTypeForClass(applicableForType);
            }
            CompiledObjectCollectionView defaultCollectionView =
                    new CompiledObjectCollectionView(type, collectionInstance.identifier());
            defaultCollectionView.setDisplay(createDisplayType(collectionInstance.display()));
            compiledObjectCollectionViews.add(defaultCollectionView);
            defaultCollectionView.setDefaultView(true);

            if (collectionInstance.applicableForOperation().length == 1) {
                defaultCollectionView.setApplicableForOperation(collectionInstance.applicableForOperation()[0]);
            }
        }
        return compiledObjectCollectionViews;
    }

    private List<PanelInstance> getPanelInstancesAnnotations(Class<?> clazz) {
        List<PanelInstance> instances = new ArrayList<>();
        if (clazz == null) {
            return instances;
        }

        PanelInstance i = clazz.getAnnotation(PanelInstance.class);
        if (i != null) {
            instances.add(i);
        }

        PanelInstances pis = clazz.getAnnotation(PanelInstances.class);
        if (pis != null) {
            instances.addAll(Arrays.asList(pis.value()));
        }

        return instances;
    }

    private void processShadowPanels(CompiledGuiProfile compiledGuiProfile) {
        List<ContainerPanelConfigurationType> shadowPanels = new ArrayList<>();
        for (Class<?> clazz : getPanelInstanceClasses()) {
            for (PanelInstance instance : getPanelInstancesAnnotations(clazz)) {
                if (instance == null) {
                    continue;
                }
                if (!instance.applicableForType().equals(ShadowType.class)) {
                    continue;
                }

                if (compiledGuiProfile.getObjectDetails() == null) {
                    compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType());
                }
                ContainerPanelConfigurationType shadowPanel = compileContainerPanelConfiguration(clazz, ShadowType.class, instance);
                shadowPanels.add(shadowPanel);
            }
        }

        if (compiledGuiProfile.getObjectDetails() == null) {
            compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType(prismContext));
        }

        if (compiledGuiProfile.getObjectDetails().getShadowDetailsPage().isEmpty()) {
            compiledGuiProfile.getObjectDetails().getShadowDetailsPage().add(new GuiShadowDetailsPageType());
        }

        for (GuiShadowDetailsPageType shadowDetailsPage : compiledGuiProfile.getObjectDetails().getShadowDetailsPage()) {
            List<ContainerPanelConfigurationType> mergedPanels =
                    adminGuiConfigurationMergeManager.mergeContainerPanelConfigurationType(shadowPanels, shadowDetailsPage.getPanel());
            shadowDetailsPage.getPanel().clear();
            shadowDetailsPage.getPanel().addAll(mergedPanels);
        }
    }

    private void processResourcePanels(CompiledGuiProfile compiledGuiProfile) {
        List<ContainerPanelConfigurationType> resourcePanels = new ArrayList<>();
        for (Class<?> clazz : getPanelInstanceClasses()) {
            for (PanelInstance instance : getPanelInstancesAnnotations(clazz)) {
                if (instance == null) {
                    continue;
                }
                if (!instance.applicableForType().equals(ResourceType.class)) {
                    continue;
                }

                if (isSubPanel(instance)) {
                    continue;
                }

                if (compiledGuiProfile.getObjectDetails() == null) {
                    compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType());
                }
                ContainerPanelConfigurationType resourcePanel = compileContainerPanelConfiguration(clazz, ResourceType.class, instance);
                resourcePanels.add(resourcePanel);
            }
        }

        if (compiledGuiProfile.getObjectDetails() == null) {
            compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType());
        }

        if (compiledGuiProfile.getObjectDetails().getResourceDetailsPage().stream().noneMatch(d -> d.getConnectorRef() == null)) {
            compiledGuiProfile.getObjectDetails().getResourceDetailsPage().add(new GuiResourceDetailsPageType());
        }

        for (GuiResourceDetailsPageType resourceDetailsPage : compiledGuiProfile.getObjectDetails().getResourceDetailsPage()) {
            List<ContainerPanelConfigurationType> cloneResourcePanels = new ArrayList<>();
            resourcePanels.forEach(panel -> cloneResourcePanels.add(panel.clone()));
            List<ContainerPanelConfigurationType> mergedPanels =
                    adminGuiConfigurationMergeManager.mergeContainerPanelConfigurationType(cloneResourcePanels, resourceDetailsPage.getPanel());
            resourceDetailsPage.getPanel().clear();
            resourceDetailsPage.getPanel().addAll(mergedPanels);
        }
    }

    private synchronized void fillInPanelsMap() {
        if (!PANELS_MAP.isEmpty()) {
            return;
        }
        for (Class<?> clazz : getPanelTypeClasses()) {
            PanelType panelType = clazz.getAnnotation(PanelType.class);
            if (isNotPanelTypeDefinition(clazz, panelType)) {
                continue;
            }
            //noinspection unchecked
            PANELS_MAP.put(panelType.name(), (Class<? extends Panel>) clazz);
        }
    }

    private boolean isNotPanelTypeDefinition(Class<?> clazz, PanelType panelType) {
        if (panelType == null) {
            return true;
        }
        if (panelType.name() == null) {
            return true;
        }
        if (!Panel.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    private synchronized void fillInActionsMap() {
        if (!ACTIONS_MAP.isEmpty()) {
            return;
        }
        for (Class<?> clazz : getActionTypeClasses()) {
            ActionType actionType = clazz.getAnnotation(ActionType.class);
            if (actionType != null && StringUtils.isNotEmpty(actionType.identifier())) {
                ACTIONS_MAP.put(actionType.identifier(), (Class<? extends AbstractGuiAction<?>>) clazz);
            }
        }
    }

    private synchronized void fillInColumnsMap() {
        if (!COLUMNS_MAP.isEmpty()) {
            return;
        }
        for (Class<?> clazz : getColumnTypeClasses()) {
            ColumnType columnType = clazz.getAnnotation(ColumnType.class);
            if (columnType != null && StringUtils.isNotEmpty(columnType.identifier())) {
                COLUMNS_MAP.put(columnType.identifier(), (Class<? extends AbstractGuiColumn<?, ?>>) clazz);
            }
        }
    }

    private synchronized void fillInCountersMap() {
        if (!COUNTERS_MAP.isEmpty()) {
            return;
        }

        for (Class<?> clazz : getPanelInstanceClasses()) {
            Counter counterDefinition = clazz.getAnnotation(Counter.class);
            if (counterDefinition != null) {
                Class<? extends SimpleCounter> counterProvider = counterDefinition.provider();
                PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
                try {
                    if (panelInstance != null) {
                        COUNTERS_MAP.put(panelInstance.identifier(), counterProvider.getDeclaredConstructor().newInstance());
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    LOGGER.warn("Problem instantiating counter of type {} for panel identifier '{}'",
                            counterDefinition.getClass().getName(), panelInstance.identifier(), e);
                }
            }
        }
    }

    private GuiObjectDetailsSetType compileDefaultGuiObjectDetailsSetType() {
        GuiObjectDetailsSetType detailsSet = new GuiObjectDetailsSetType();

        // create defaults for all subclasses of ObjectType
        for (ObjectTypes type : ObjectTypes.values()) {
            addDetails(detailsSet, type.getClassDefinition());
        }

        // add all other Containerable types for which we found and cached panels on classpath
        Set<Class<? extends Containerable>> containerables = findSupportedContainerables();
        for (Class<? extends Containerable> clazz : containerables) {
            addDetails(detailsSet, clazz);
        }

        return detailsSet;
    }

    private Set<Class<? extends Containerable>> findSupportedContainerables() {
        Set<Class<? extends Containerable>> containerables = new HashSet<>();

        for (Class<?> clazz : getPanelInstanceClasses()) {
            List<PanelInstance> pis = getPanelInstancesAnnotations(clazz);
            pis.forEach(pi -> addSupportedContainerable(containerables, pi));
        }

        return containerables;
    }

    private void addSupportedContainerable(Set<Class<? extends Containerable>> containerables, PanelInstance pi) {
        if (pi == null || pi.applicableForType() == null) {
            return;
        }
        if (ObjectType.class.isAssignableFrom(pi.applicableForType())) {
            return;
        }

        containerables.add(pi.applicableForType());
    }

    private void addDetails(GuiObjectDetailsSetType detailsSet, Class<? extends Containerable> type) {
        GuiObjectDetailsPageType details = compileDefaultGuiObjectDetailsPage(type);
        if (details == null) {
            return;
        }

        if (QNameUtil.match(details.getType(), ShadowType.COMPLEX_TYPE)
                || QNameUtil.match(details.getType(), ResourceType.COMPLEX_TYPE)) {
            return;
        }

        detailsSet.getObjectDetailsPage().add(details);
    }

    private GuiObjectDetailsPageType compileDefaultGuiObjectDetailsPage(Class<? extends Containerable> containerable) {
        QName type;
        try {
            type = GuiImplUtil.getContainerableTypeName(containerable);
        } catch (Exception ex) {
            return null;
        }

        if (type == null) {
            return null;
        }

        GuiObjectDetailsPageType detailsPageType = new GuiObjectDetailsPageType();
        detailsPageType.setType(type);
        detailsPageType.getPanel().addAll(getPanelsFor(containerable));
        return detailsPageType;
    }

    private List<ContainerPanelConfigurationType> getPanelsFor(Class<? extends Containerable> containerable) {
        List<ContainerPanelConfigurationType> panels = new ArrayList<>();

        for (Class<?> clazz : getPanelInstanceClasses()) {
            List<PanelInstance> pis = getPanelInstancesAnnotations(clazz);
            pis.forEach(pi -> addPanelsFor(panels, containerable, clazz, pi));
        }

        MiscSchemaUtil.sortFeaturesPanels(panels);

        return panels;
    }

    private void addPanelsFor(List<ContainerPanelConfigurationType> panels, Class<? extends Containerable> c, Class<?> clazz, PanelInstance pi) {
        if (isNotApplicableFor(c, pi)) {
            return;
        }

        if (isSubPanel(pi)) {
            return;
        }

        ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, c, pi);

        panels.add(config);
    }

    private boolean isNotApplicableFor(Class<? extends Containerable> containerable, PanelInstance pi) {
        if (pi == null || Containerable.class.equals(pi.applicableForType())) {
            // if there's no applicableForType defined, it shouldn't be applicable
            return true;
        }

        boolean applicable = pi.applicableForType().isAssignableFrom(containerable);
        if (!applicable) {
            return true;
        }

        return Arrays.asList(pi.excludeTypes()).contains(containerable);
    }

    private boolean isSubPanel(PanelInstance panelInstance) {
        return !panelInstance.childOf().equals(Panel.class);
    }

    private ContainerPanelConfigurationType compileContainerPanelConfiguration(Class<?> clazz, Class<? extends Containerable> containerable, PanelInstance panelInstance) {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setIdentifier(panelInstance.identifier());

        addPanelTypeConfiguration(clazz, config);
        compileDisplay(panelInstance, config);

        List<ContainerPanelConfigurationType> children = processChildren(containerable, clazz);
        config.getPanel().addAll(children);

        if (panelInstance.defaultPanel()) {
            config.setDefault(true);
        }

        if (panelInstance.applicableForOperation().length == 1) {
            config.setApplicableForOperation(panelInstance.applicableForOperation()[0]);
        }

        createDefaultVirtualContainer(config, panelInstance.containerPath(), panelInstance.expanded());
        if (panelInstance.hiddenContainers().length > 0) {
            for (String path : panelInstance.hiddenContainers()) {
                VirtualContainersSpecificationType c = new VirtualContainersSpecificationType();
                c.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                ItemPathType itemPath = prismContext.itemPathParser().asItemPathType(path);
                c.setPath(itemPath);
                config.getContainer().add(c);
            }
        }

        if (StringUtils.isNotEmpty(panelInstance.type())) {
            config.setType(QNameUtil.uriToQName(panelInstance.type(), SchemaConstantsGenerated.NS_COMMON));
        }
        config.asPrismContainerValue().setParent(null);

        return config;
    }

    private void createDefaultVirtualContainer(ContainerPanelConfigurationType config, String path, Boolean expanded) {
        if (StringUtils.isEmpty(path)) {
            return;
        }

        VirtualContainersSpecificationType container = new VirtualContainersSpecificationType();
        if ("empty".equals(path)) {
            container.setPath(new ItemPathType(ItemPath.EMPTY_PATH));
        } else {
            ItemPathType itemPath = prismContext.itemPathParser().asItemPathType(path);
            container.setPath(itemPath);
        }
        container.setDisplayOrder(10);
        container.setExpanded(expanded);
        config.getContainer().add(container);
    }

    private void addPanelTypeConfiguration(Class<?> clazz, ContainerPanelConfigurationType config) {
        PanelType panelType = clazz.getAnnotation(PanelType.class);
        if (panelType == null) {
            return;
        }
        config.setPanelType(panelType.name());
        if (panelType.defaultType() != null && !Containerable.class.equals(panelType.defaultType())) {
            PrismContainerDefinition<?> def = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(panelType.defaultType());
            if (def != null) {
                config.setType(def.getTypeName());
            }
        }
        createDefaultVirtualContainer(config, panelType.defaultContainerPath(), null);

        if (panelType.experimental() && BooleanUtils.isNotTrue(experimentalFeaturesEnabled)) {
            config.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
        }
    }

    private void compileDisplay(PanelInstance panelInstance, ContainerPanelConfigurationType config) {
        PanelDisplay display = panelInstance.display();
        if (display != null) {
            config.setDisplay(createDisplayType(display));
            config.setDisplayOrder(display.order());
        }
    }

    private List<ContainerPanelConfigurationType> processChildren(Class<? extends Containerable> containerable, Class<?> parentClass) {
        List<ContainerPanelConfigurationType> configs = new ArrayList<>();
        for (Class<?> clazz : getPanelInstanceClasses()) {
            for (PanelInstance panelInstance : getPanelInstancesAnnotations(clazz)) {
                if (isNotApplicableFor(containerable, panelInstance)) {
                    continue;
                }
                if (!isSubPanel(panelInstance)) {
                    continue;
                }

                if (!panelInstance.childOf().equals(parentClass)) {
                    continue;
                }

                ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, containerable, panelInstance);
                configs.add(config);
            }
        }

        MiscSchemaUtil.sortFeaturesPanels(configs);

        return configs;
    }

    private DisplayType createDisplayType(PanelDisplay display) {
        DisplayType displayType = new DisplayType();
        displayType.setLabel(createPolyStringType(display.label()));
        displayType.setSingularLabel(createPolyStringType(display.singularLabel()));
        displayType.setIcon(new IconType().cssClass(display.icon()));
        return displayType;
    }

    private PolyStringType createPolyStringType(String key) {
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey(key);
        translation.setFallback(key);
        PolyString poly = new PolyString(null, null, translation);

        return new PolyStringType(poly);
    }
}
