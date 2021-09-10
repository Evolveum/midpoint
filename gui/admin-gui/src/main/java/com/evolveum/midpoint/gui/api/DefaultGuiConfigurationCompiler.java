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

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilable;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilerRegistry;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Component
public class DefaultGuiConfigurationCompiler implements GuiProfileCompilable {

    @Autowired private GuiProfileCompilerRegistry registry;
    @Autowired private PrismContext prismContext;
    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.component.objectdetails", //Old panels
            "com.evolveum.midpoint.web.component.assignment",  //Assignments
            "com.evolveum.midpoint.gui.impl.page.admin",
            "com.evolveum.midpoint.gui.impl.page.admin.component",
            "com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component",
            "com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component",
            "com.evolveum.midpoint.gui.impl.page.admin.focus.component",
            "com.evolveum.midpoint.gui.impl.page.admin.resource.component",
            "com.evolveum.midpoint.gui.impl.page.admin.task.component",
            "com.evolveum.midpoint.gui.impl.component.assignment",
            "com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment",
            "com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement",
            "com.evolveum.midpoint.gui.impl.page.admin.org.component",
            "com.evolveum.midpoint.gui.impl.page.admin.cases.component",
            "com.evolveum.midpoint.gui.impl.page.admin.user.component",
            "com.evolveum.midpoint.gui.impl.page.admin.report.component"
    };

    private final Map<String, Class<? extends Panel>> panelsMap = new HashMap<>();

    private final Map<String, SimpleCounter> countersMap = new HashMap<>();

    private Boolean experimentalFeaturesEnabled = false;

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

        Set<Class<?>> classes = collectClasses();

        fillInPanelsMap(classes);
        fillInCountersMap(classes);

        GuiObjectDetailsSetType defaultDetailsPages = compileDefaultGuiObjectDetailsSetType(classes);
        List<GuiObjectDetailsPageType> detailsPages = defaultDetailsPages.getObjectDetailsPage();
        for (GuiObjectDetailsPageType defaultDetailsPage : detailsPages) {
            GuiObjectDetailsPageType compiledPageType = compiledGuiProfile.findObjectDetailsConfiguration(defaultDetailsPage.getType());
            GuiObjectDetailsPageType mergedDetailsPage = adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(defaultDetailsPage, compiledPageType);

            compiledGuiProfile.getObjectDetails().getObjectDetailsPage().removeIf(p -> QNameUtil.match(p.getType(), defaultDetailsPage.getType()));
            compiledGuiProfile.getObjectDetails().getObjectDetailsPage().add(mergedDetailsPage.cloneWithoutId());
        }
    }

    private void fillInPanelsMap(Set<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            PanelType panelType = clazz.getAnnotation(PanelType.class);
            if (isNotPanelTypeDefinition(clazz, panelType)) {
                continue;
            }
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

    private void fillInCountersMap(Set<Class<?>> scannedClasses) {
        for (Class<?> clazz : scannedClasses) {
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

    private GuiObjectDetailsSetType compileDefaultGuiObjectDetailsSetType(Set<Class<?>> scannedClasses) {
        GuiObjectDetailsSetType guiObjectDetailsSetType = new GuiObjectDetailsSetType();
        for (ObjectTypes objectType : ObjectTypes.values()) {
            GuiObjectDetailsPageType detailsPageType = compileDefaultGuiObjectDetailsPage(objectType, scannedClasses);
            guiObjectDetailsSetType.getObjectDetailsPage().add(detailsPageType);
        }
        return guiObjectDetailsSetType;
    }

    private GuiObjectDetailsPageType compileDefaultGuiObjectDetailsPage(ObjectTypes objectType, Set<Class<?>> scannedClasses) {
        GuiObjectDetailsPageType detailsPageType = new GuiObjectDetailsPageType();
        detailsPageType.setType(objectType.getTypeQName());
        detailsPageType.getPanel().addAll(getPanelsFor(objectType.getClassDefinition(), scannedClasses));
        return detailsPageType;
    }

    private List<ContainerPanelConfigurationType> getPanelsFor(Class<? extends ObjectType> objectType, Set<Class<?>> allClasses) {
        List<ContainerPanelConfigurationType> panels = new ArrayList<>();
        for (Class<?> clazz : allClasses) {
            PanelInstances panelInstances = clazz.getAnnotation(PanelInstances.class);
            if (panelInstances != null) {
                for (PanelInstance panelInstance : panelInstances.instances()) {
                    if (isNotApplicableFor(objectType, panelInstance)) {
                        continue;
                    }

                    if (isSubPanel(panelInstance)) {
                        continue;
                    }
                    ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, objectType, allClasses, panelInstance);
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
                ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, objectType, allClasses, panelInstance);
                panels.add(config);
            }
        }

        MiscSchemaUtil.sortDetailsPanels(panels);
        return panels;
    }

    private Set<Class<?>> collectClasses() {
        Set<Class<?>> allClasses = new HashSet<>();
        for (String packageToScan : PACKAGES_TO_SCAN) {
            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
            allClasses.addAll(classes);
        }
        return allClasses;
    }

    private boolean isNotApplicableFor(Class<? extends ObjectType> objectType, PanelInstance panelInstance) {
        if (panelInstance == null) {
            return true;
        }
        if (panelInstance.applicableFor() == null) {
            return true;
        }

        if (ObjectType.class.equals(panelInstance.applicableFor())) {
            return true;
        }

        if (panelInstance.notApplicableFor() != null && !panelInstance.notApplicableFor().equals(SystemConfigurationType.class)) {
            return panelInstance.notApplicableFor().isAssignableFrom(objectType);
        }

        return !panelInstance.applicableFor().isAssignableFrom(objectType);
    }

    private boolean isSubPanel(PanelInstance panelInstance) {
        return !panelInstance.childOf().equals(Panel.class);
    }

    private ContainerPanelConfigurationType compileContainerPanelConfiguration(Class<?> clazz, Class<? extends ObjectType> objectType, Set<Class<?>> classes, PanelInstance panelInstance) {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setIdentifier(panelInstance.identifier());

        addPanelTypeConfiguration(clazz, config);
        compileDisplay(panelInstance, config);

        List<ContainerPanelConfigurationType> children = processChildren(classes, objectType, clazz);
        config.getPanel().addAll(children);

        if (panelInstance.defaultPanel()) {
            config.setDefault(true);
        }
        if (Arrays.stream(panelInstance.status()).filter(s -> ItemStatus.ADDED == s).count() == 1) {
            config.setVisibleForAdd(true);
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
        config.getContainer().add(defaultContainer);
    }

    private void compileDisplay(PanelInstance panelInstance, ContainerPanelConfigurationType config) {
        PanelDisplay display = panelInstance.display();
        if (display != null) {
            config.setDisplay(createDisplayType(display));
            config.setDisplayOrder(display.order());
        }
    }

    private List<ContainerPanelConfigurationType> processChildren(Set<Class<?>> classes, Class<? extends ObjectType> objectType, Class<?> parentClass) {
        List<ContainerPanelConfigurationType> configs = new ArrayList<>();
//        Set<Class<?>> classes = collectClasses();
        for (Class<?> clazz : classes) {
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

            ContainerPanelConfigurationType config = compileContainerPanelConfiguration(clazz, objectType, classes, panelInstance);
            configs.add(config);
        }
        MiscSchemaUtil.sortDetailsPanels(configs);
        return configs;
    }

    private DisplayType createDisplayType(PanelDisplay display) {
        DisplayType displayType = new DisplayType();
        PolyStringTranslationType translationType = new PolyStringTranslationType();
        translationType.setKey(display.label());
        PolyString polyString = new PolyString(null, null, translationType);
        displayType.setLabel(new PolyStringType(polyString));
        displayType.setIcon(new IconType().cssClass(display.icon()));
        return displayType;
    }
}
