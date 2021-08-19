/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api;

import java.util.*;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilable;
import com.evolveum.midpoint.model.api.authentication.GuiProfileCompilerRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Component
public class DefaultGuiConfigurationCompiler implements GuiProfileCompilable {

    @Autowired private GuiProfileCompilerRegistry registry;

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.component.objectdetails", //Old panels
            "com.evolveum.midpoint.web.component.assignment",  //Assignments
            "com.evolveum.midpoint.gui.impl.page.admin",
            "com.evolveum.midpoint.web.page.admin.resources",
            "com.evolveum.midpoint.gui.impl.component.assignment",
            "com.evolveum.midpoint.gui.impl.component.assignmentType.assignment",
            "com.evolveum.midpoint.gui.impl.component.assignmentType.inducement"
    };

    private Map<String, Class<? extends Panel>> panelsMap = new HashMap<>();

    @Override
    @PostConstruct
    public void register() {
        registry.registerCompiler(this);
    }

    public Class<? extends Panel> findPanel(String identifier) {
        return panelsMap.get(identifier);
    }

    @Override
    public void postProcess(CompiledGuiProfile compiledGuiProfile) {
        fillInPanelsMap();

        GuiObjectDetailsSetType defaultDetailsPages = compileDefaultGuiObjectDetailsSetType();
        List<GuiObjectDetailsPageType> detailsPages = defaultDetailsPages.getObjectDetailsPage();
        for (GuiObjectDetailsPageType defaultDetailsPage : detailsPages) {
            GuiObjectDetailsPageType compiledPageType = compiledGuiProfile.findObjectDetailsConfiguration(defaultDetailsPage.getType());
            if (compiledPageType == null) {
                compiledGuiProfile.getObjectDetails().getObjectDetailsPage().add(defaultDetailsPage.cloneWithoutId());
                continue;
            }
            List<ContainerPanelConfigurationType> mergedPanels = mergeConfigurations(defaultDetailsPage.getPanel(), compiledPageType.getPanel());
            setupDefaultPanel(ObjectTypes.getObjectTypeClass(compiledPageType.getType()), mergedPanels);
            compiledPageType.getPanel().clear();
            compiledPageType.getPanel().addAll(CloneUtil.cloneCollectionMembersWithoutIds(mergedPanels));
        }
    }

    private void setupDefaultPanel(Class<? extends ObjectType> objectType, List<ContainerPanelConfigurationType> mergedPanels) {
        long defaultPanelsCount = mergedPanels.stream().filter(p -> BooleanUtils.isTrue(p.isDefault())).count();
        if (defaultPanelsCount >= 1) {
            return;
        }

        ContainerPanelConfigurationType systemDefault = defaultContainerPanelConfigurationMap.get(objectType);
        for (ContainerPanelConfigurationType mergedPanel : mergedPanels) {
            if (systemDefault.getIdentifier().equals(mergedPanel.getIdentifier())) {
                mergedPanel.setDefault(true);
            }
        }
    }

    private void fillInPanelsMap() {
        Set<Class<?>> classes = collectClasses();
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
    private GuiObjectDetailsSetType compileDefaultGuiObjectDetailsSetType() {
        GuiObjectDetailsSetType guiObjectDetailsSetType = new GuiObjectDetailsSetType();
        Set<Class<?>> scannedClasses = collectClasses();
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
            PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
            if (isNotApplicableFor(objectType, panelInstance)) {
                continue;
            }

            if (isSubPanel(panelInstance)) {
                continue;
            }
            ContainerPanelConfigurationType config = compileContainerPanelConfiguration(panelInstance.identifier(), panelInstance.defaultPanel(), clazz, objectType, allClasses);
            panels.add(config);
        }

        sort(panels);
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

        if (panelInstance.notApplicableFor() != null && !panelInstance.notApplicableFor().equals(SystemConfigurationType.class)) {
            return panelInstance.notApplicableFor().isAssignableFrom(objectType);
        }

        return !panelInstance.applicableFor().isAssignableFrom(objectType);
    }

    private boolean isSubPanel(PanelInstance panelInstance) {
        return !panelInstance.childOf().equals(Panel.class);
    }

    Map<Class<? extends ObjectType>, ContainerPanelConfigurationType> defaultContainerPanelConfigurationMap = new HashMap<>();

    private ContainerPanelConfigurationType compileContainerPanelConfiguration(String identifier, boolean isDefault, Class<?> clazz, Class<? extends ObjectType> objectType, Set<Class<?>> classes) {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setIdentifier(identifier);

        addPanelTypeConfiguration(clazz, config);
        compileDisplay(clazz, config);

        List<ContainerPanelConfigurationType> children = processChildren(classes, objectType, clazz);
        config.getPanel().addAll(children);

        if (isDefault) {
            config.setDefault(true);
//            defaultContainerPanelConfigurationMap.put(objectType, config);
        }
        return config;
    }

    private void addPanelTypeConfiguration(Class<?> clazz, ContainerPanelConfigurationType config) {
        PanelType panelType = clazz.getAnnotation(PanelType.class);
        if (panelType == null) {
            return;
        }
        config.setPanelType(panelType.name());
        compileDefaultContainerSpecification(panelType, config);
    }

    private void compileDefaultContainerSpecification(PanelType panelType, ContainerPanelConfigurationType config) {
        if (panelType.defaultContainerPath().isBlank()) {
            return;
        }
        VirtualContainersSpecificationType defaultContainer = new VirtualContainersSpecificationType();
        if ("empty".equals(panelType.defaultContainerPath())) {
            defaultContainer.setPath(new ItemPathType(ItemPath.EMPTY_PATH));
        } else {
            defaultContainer.setPath(new ItemPathType(ItemPath.create(panelType.defaultContainerPath())));
        }
        config.getContainer().add(defaultContainer);
    }

    private void compileDisplay(Class<?> clazz, ContainerPanelConfigurationType config) {
        PanelDisplay display = clazz.getAnnotation(PanelDisplay.class);
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

            ContainerPanelConfigurationType config = compileContainerPanelConfiguration(panelInstance.identifier(), panelInstance.defaultPanel(), clazz, objectType, classes);
            configs.add(config);
        }

        sort(configs);
        return configs;
    }

    private DisplayType createDisplayType(PanelDisplay display) {
        DisplayType displayType = new DisplayType();
        displayType.setLabel(WebComponentUtil.createPolyFromOrigString(display.label()));
        displayType.setCssClass(display.icon());
        return displayType;
    }

    private void sort(List<ContainerPanelConfigurationType> panels) {
        panels.sort((p1, p2) -> {
            int displayOrder1 = (p1 == null || p1.getDisplayOrder() == null) ? Integer.MAX_VALUE : p1.getDisplayOrder();
            int displayOrder2 = (p2 == null || p2.getDisplayOrder() == null) ? Integer.MAX_VALUE : p2.getDisplayOrder();

            return Integer.compare(displayOrder1, displayOrder2);
        });
    }

    private List<ContainerPanelConfigurationType> mergeConfigurations(List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> configuredPanels) {
        List<ContainerPanelConfigurationType> mergedPanels = new ArrayList<>(defaultPanels);
        for (ContainerPanelConfigurationType configuredPanel : configuredPanels) {
            mergePanelConfigurations(configuredPanel, defaultPanels, mergedPanels);
        }
        return mergedPanels;
    }

    private void mergePanelConfigurations(ContainerPanelConfigurationType configuredPanel, List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> mergedPanels) {
        for (ContainerPanelConfigurationType defaultPanel : defaultPanels) {
            if (defaultPanel.getIdentifier().equals(configuredPanel.getIdentifier())) {
                mergePanels(defaultPanel, configuredPanel);
                return;
            }
        }
        mergedPanels.add(configuredPanel.cloneWithoutId());
    }

    private void mergePanels(ContainerPanelConfigurationType mergedPanel, ContainerPanelConfigurationType configuredPanel) {
        if (configuredPanel.getPanelType() != null) {
            mergedPanel.setPanelType(configuredPanel.getPanelType());
        }

        if (configuredPanel.getPath() != null) {
            mergedPanel.setPath(configuredPanel.getPath());
        }

        if (configuredPanel.getListView() != null) {
            mergedPanel.setListView(configuredPanel.getListView().cloneWithoutId());
        }

        if (!configuredPanel.getContainer().isEmpty()) {
            mergedPanel.getContainer().addAll(CloneUtil.cloneCollectionMembersWithoutIds(configuredPanel.getContainer()));
        }

        if (configuredPanel.getType() != null) {
            mergedPanel.setType(configuredPanel.getType());
        }

        if (configuredPanel.getVisibility() != null) {
            mergedPanel.setVisibility(configuredPanel.getVisibility());
        }

        if (!configuredPanel.getPanel().isEmpty()) {
            List<ContainerPanelConfigurationType> mergedConfigs = mergeConfigurations(mergedPanel.getPanel(), configuredPanel.getPanel());
            mergedPanel.getPanel().clear();
            mergedPanel.getPanel().addAll(mergedConfigs);
        }
    }

}
