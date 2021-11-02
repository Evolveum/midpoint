/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilable;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilerRegistry;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Component
public class DefaultGuiConfigurationCompiler implements GuiProfileCompilable {

    @Autowired private GuiProfileCompilerRegistry registry;
    @Autowired private PrismContext prismContext;
    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    @SuppressWarnings("SpellCheckingInspection")
//    private static final String[] PANEL_PACKAGES_TO_SCAN = {
//            "com.evolveum.midpoint.gui.impl.page.admin",
//            "com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.archetype.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment",
//            "com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement",
//            "com.evolveum.midpoint.gui.impl.page.admin.cases.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.focus.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.objectcollection.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.objecttemplate.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.org.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.report.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.resource.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.task.component",
//            "com.evolveum.midpoint.gui.impl.page.admin.user.component",
//            "com.evolveum.midpoint.web.component.assignment",  //Assignments
//            "com.evolveum.midpoint.web.component.objectdetails" //Old panels
//    };

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
            "com.evolveum.midpoint.web.page.admin.users"
    };

    private final Map<String, Class<? extends Panel>> panelsMap = new HashMap<>();

    private final Map<String, SimpleCounter> countersMap = new HashMap<>();

    private Boolean experimentalFeaturesEnabled = false;
    private Set<Class<?>> panelInstanceClasses;
    private Set<Class<?>> panelTypeClasses;
    private Set<Class<?>> collectionClasses;

    @Override
    @PostConstruct
    public void register() {
        registry.registerCompiler(this);
    }

    public Class<? extends Panel> findPanel(String identifier) {
        return panelsMap.get(identifier);
    }

    public SimpleCounter findCounter(String idenifier) {
        return countersMap.get(idenifier);
    }

    @Override
    public void postProcess(CompiledGuiProfile compiledGuiProfile) {
        experimentalFeaturesEnabled = compiledGuiProfile.isEnableExperimentalFeatures();

        collectPanelInstanceClasses();
        collectPanelTypeClasses();

        fillInPanelsMap();
        fillInCountersMap();

        compileDefaultDetailsPages(compiledGuiProfile);
        mergeCollectionViewsWithDefault(compiledGuiProfile);
        processShadowPanels(compiledGuiProfile);

    }

    private void compileDefaultDetailsPages(CompiledGuiProfile compiledGuiProfile) {
        GuiObjectDetailsSetType defaultDetailsPages = compileDefaultGuiObjectDetailsSetType();
        List<GuiObjectDetailsPageType> detailsPages = defaultDetailsPages.getObjectDetailsPage();
        for (GuiObjectDetailsPageType defaultDetailsPage : detailsPages) {

            //objects
            GuiObjectDetailsPageType compiledPageType = compiledGuiProfile.findObjectDetailsConfiguration(defaultDetailsPage.getType());
            GuiObjectDetailsPageType mergedDetailsPage = adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(defaultDetailsPage, compiledPageType);

            if (compiledGuiProfile.getObjectDetails() == null) {
                compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType(prismContext));
            }
            compiledGuiProfile.getObjectDetails().getObjectDetailsPage().removeIf(p -> QNameUtil.match(p.getType(), defaultDetailsPage.getType()));
            compiledGuiProfile.getObjectDetails().getObjectDetailsPage().add(mergedDetailsPage.cloneWithoutId());
        }
    }

    private void mergeCollectionViewsWithDefault(CompiledGuiProfile compiledGuiProfile) {
        Set<Class<?>> classes = collectCollectionClasses();
        List<CompiledObjectCollectionView> defaultCollectionViews = compileDefaultCollectionViews(classes);

        for (CompiledObjectCollectionView defaultCollectionView : defaultCollectionViews) {
            CompiledObjectCollectionView compiledObjectCollectionView = compiledGuiProfile.findObjectCollectionView(defaultCollectionView.getContainerType(), defaultCollectionView.getViewIdentifier());
            if (compiledObjectCollectionView == null) {
                compiledGuiProfile.getObjectCollectionViews().add(defaultCollectionView);
                continue;
            }
            mergeCollectionViews(compiledObjectCollectionView, defaultCollectionView);
        }

    }

    private void mergeCollectionViews(CompiledObjectCollectionView compiledObjectCollectionView, CompiledObjectCollectionView defaulCollectionView) {
        DisplayType displayType = adminGuiConfigurationMergeManager.mergeDisplayType(compiledObjectCollectionView.getDisplay(), defaulCollectionView.getDisplay());
        compiledObjectCollectionView.setDisplay(displayType);

        if (compiledObjectCollectionView.getApplicableForOperation() == null) {
            compiledObjectCollectionView.setApplicableForOperation(defaulCollectionView.getApplicableForOperation());
        }
    }

    private List<CompiledObjectCollectionView> compileDefaultCollectionViews(Set<Class<?>> classes) {
        List<CompiledObjectCollectionView> compiledObjectCollectionViews = new ArrayList<>();
        for (Class<?> clazz : classes) {
            CollectionInstance collectionInstance = clazz.getAnnotation(CollectionInstance.class);
            if (collectionInstance == null) {
                continue;
            }
            ObjectTypes objectType = ObjectTypes.getObjectType(collectionInstance.applicableForType());
            CompiledObjectCollectionView defaultCollectionView = new CompiledObjectCollectionView(objectType.getTypeQName(), collectionInstance.identifier());
            defaultCollectionView.setDisplay(createDisplayType(collectionInstance.display()));
            compiledObjectCollectionViews.add(defaultCollectionView);
            defaultCollectionView.setDefaultView(true);

            if (collectionInstance.applicableForOperation().length == 1) {
                defaultCollectionView.setApplicableForOperation(collectionInstance.applicableForOperation()[0]);
            }
        }
        return compiledObjectCollectionViews;
    }

    private void processShadowPanels(CompiledGuiProfile compiledGuiProfile) {
        List<ContainerPanelConfigurationType> shadowPanels = new ArrayList<>();
        for (Class<?> clazz : panelInstanceClasses) {
            PanelInstance instance = clazz.getAnnotation(PanelInstance.class);
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

        if (compiledGuiProfile.getObjectDetails() == null) {
            compiledGuiProfile.setObjectDetails(new GuiObjectDetailsSetType(prismContext));
        }

        if (compiledGuiProfile.getObjectDetails().getShadowDetailsPage().isEmpty()) {
            compiledGuiProfile.getObjectDetails().getShadowDetailsPage().add(new GuiShadowDetailsPageType());
        }

        for (GuiShadowDetailsPageType shadowDetailsPage : compiledGuiProfile.getObjectDetails().getShadowDetailsPage()) {
            List<ContainerPanelConfigurationType> mergedPanels = adminGuiConfigurationMergeManager.mergeContainerPanelConfigurationType(shadowPanels, shadowDetailsPage.getPanel());
            shadowDetailsPage.getPanel().clear();
            shadowDetailsPage.getPanel().addAll(mergedPanels);
        }
    }

    private void fillInPanelsMap() {
        if (!panelsMap.isEmpty()) {
            return;
        }
        for (Class<?> clazz : panelTypeClasses) {
            PanelType panelType = clazz.getAnnotation(PanelType.class);
            if (isNotPanelTypeDefinition(clazz, panelType)) {
                continue;
            }
            //noinspection unchecked
            panelsMap.put(panelType.name(), (Class<? extends Panel>) clazz);
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

    private void fillInCountersMap() {
        if (!countersMap.isEmpty()) {
            return;
        }
        for (Class<?> clazz : panelInstanceClasses) {
            Counter counterDefinition = clazz.getAnnotation(Counter.class);
            if (counterDefinition != null) {
                Class<? extends SimpleCounter> counterProvider = counterDefinition.provider();
                try {
                    PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
                    if (panelInstance != null) {
                        countersMap.put(panelInstance.identifier(), counterProvider.getDeclaredConstructor().newInstance());
                    }
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    //TODO log at least
                }
            }
        }

    }

    private GuiObjectDetailsSetType compileDefaultGuiObjectDetailsSetType() {
        GuiObjectDetailsSetType guiObjectDetailsSetType = new GuiObjectDetailsSetType();
        for (ObjectTypes objectType : ObjectTypes.values()) {
            GuiObjectDetailsPageType detailsPageType = compileDefaultGuiObjectDetailsPage(objectType);
            if (QNameUtil.match(detailsPageType.getType(), ShadowType.COMPLEX_TYPE)) {
                continue;
            }
            guiObjectDetailsSetType.getObjectDetailsPage().add(detailsPageType);
        }
        return guiObjectDetailsSetType;
    }

    private GuiObjectDetailsPageType compileDefaultGuiObjectDetailsPage(ObjectTypes objectType) {
        GuiObjectDetailsPageType detailsPageType = new GuiObjectDetailsPageType();
        detailsPageType.setType(objectType.getTypeQName());
        detailsPageType.getPanel().addAll(getPanelsFor(objectType.getClassDefinition()));
        return detailsPageType;
    }

    private List<ContainerPanelConfigurationType> getPanelsFor(Class<? extends ObjectType> objectType) {
        List<ContainerPanelConfigurationType> panels = new ArrayList<>();
        for (Class<?> clazz : panelInstanceClasses) {
            PanelInstances panelInstances = clazz.getAnnotation(PanelInstances.class);
            if (panelInstances != null) {
                for (PanelInstance panelInstance : panelInstances.instances()) {
                    if (isNotApplicableFor(objectType, panelInstance)) {
                        continue;
                    }

                    if (isSubPanel(panelInstance)) {
                        continue;
                    }
                    ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, objectType, panelInstance);
                    panels.add(config);
                }
            } else {
                PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
                if (isNotApplicableFor(objectType, panelInstance)) {
                    continue;
                }

                if (isSubPanel(panelInstance)) {
                    continue;
                }
                ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, objectType, panelInstance);
                panels.add(config);
            }
        }

        MiscSchemaUtil.sortDetailsPanels(panels);
        return panels;
    }

    private Set<Class<?>> collectPanelInstanceClasses() {
        if (panelInstanceClasses == null) {
            panelInstanceClasses = collectClasses(PanelInstance.class);
            panelInstanceClasses.addAll(collectClasses(PanelInstances.class));
        }
        return panelInstanceClasses;
    }

    private Set<Class<?>> collectPanelTypeClasses() {
        if (panelTypeClasses == null) {
            panelTypeClasses = collectClasses(PanelType.class);
        }
        return panelTypeClasses;
    }

    private Set<Class<?>> collectClasses(Class annotationClass) {
        return ClassPathUtil.scanClasses(annotationClass);
    }

    private Set<Class<?>> collectCollectionClasses() {
        if (collectionClasses == null) {
            collectionClasses = collectClasses(COLLECTION_PACKAGES_TO_SCAN);
        }
        return collectionClasses;
    }

    private Set<Class<?>> collectClasses(String[] packagesToScan) {
        Set<Class<?>> allClasses = new HashSet<>();
        for (String packageToScan : packagesToScan) {
            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
            allClasses.addAll(classes);
        }
        return allClasses;
    }

    private boolean isNotApplicableFor(Class<? extends ObjectType> objectType, PanelInstance panelInstance) {
        if (panelInstance == null) {
            return true;
        }
        if (panelInstance.applicableForType() == null) {
            return true;
        }

        if (ObjectType.class.equals(panelInstance.applicableForType())) {
            return true;
        }

        if (panelInstance.excludeTypes() != null && panelInstance.excludeTypes().length > 0) {
            return Arrays.asList(panelInstance.excludeTypes()).contains(objectType);
        }

        return !panelInstance.applicableForType().isAssignableFrom(objectType);
    }

    private boolean isSubPanel(PanelInstance panelInstance) {
        return !panelInstance.childOf().equals(Panel.class);
    }

    private ContainerPanelConfigurationType compileContainerPanelConfiguration(Class<?> clazz, Class<? extends ObjectType> objectType, PanelInstance panelInstance) {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setIdentifier(panelInstance.identifier());

        addPanelTypeConfiguration(clazz, config);
        compileDisplay(panelInstance, config);

        List<ContainerPanelConfigurationType> children = processChildren(objectType, clazz);
        config.getPanel().addAll(children);

        if (panelInstance.defaultPanel()) {
            config.setDefault(true);
        }

        if (panelInstance.applicableForOperation().length == 1) {
            config.setApplicableForOperation(panelInstance.applicableForOperation()[0]);
        }

        return config;
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
        compileDefaultContainerSpecification(panelType, config);

        if (panelType.experimental() && BooleanUtils.isNotTrue(experimentalFeaturesEnabled)) {
            config.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
        }
    }

    private void compileDefaultContainerSpecification(PanelType panelType, ContainerPanelConfigurationType config) {
        if (panelType.defaultContainerPath().isBlank()) {
            return;
        }
        VirtualContainersSpecificationType defaultContainer = new VirtualContainersSpecificationType();
        if ("empty".equals(panelType.defaultContainerPath())) {
            defaultContainer.setPath(new ItemPathType(ItemPath.EMPTY_PATH));
        } else {
            ItemPathType path = prismContext.itemPathParser().asItemPathType(panelType.defaultContainerPath());
            defaultContainer.setPath(path);
        }
        defaultContainer.setDisplayOrder(10);
        config.getContainer().add(defaultContainer);
    }

    private void compileDisplay(PanelInstance panelInstance, ContainerPanelConfigurationType config) {
        PanelDisplay display = panelInstance.display();
        if (display != null) {
            config.setDisplay(createDisplayType(display));
            config.setDisplayOrder(display.order());
        }
    }

    private List<ContainerPanelConfigurationType> processChildren(Class<? extends ObjectType> objectType, Class<?> parentClass) {
        List<ContainerPanelConfigurationType> configs = new ArrayList<>();
//        Set<Class<?>> classes = collectClasses();
        for (Class<?> clazz : panelInstanceClasses) {
            PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
            if (isNotApplicableFor(objectType, panelInstance)) {
                continue;
            }
            if (!isSubPanel(panelInstance)) {
                continue;
            }

            if (!panelInstance.childOf().equals(parentClass)) {
                continue;
            }

            ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, objectType, panelInstance);
            configs.add(config);
        }
        MiscSchemaUtil.sortDetailsPanels(configs);
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
        PolyStringTranslationType translationType = new PolyStringTranslationType();
        translationType.setKey(key);
        PolyString polyString = new PolyString(null, null, translationType);
        return new PolyStringType(polyString);
    }
}
