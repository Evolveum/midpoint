package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.panel.Panel;

import java.util.*;

public class PanelLoader {

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.component.objectdetails", //Old panels
            "com.evolveum.midpoint.web.component.assignment",  //Assignments
            "com.evolveum.midpoint.gui.impl.page.admin"
    };

    public static Class<?> findPanel(String identifier) {
        for (String packageToScan : PACKAGES_TO_SCAN) {
            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
            for (Class<?> clazz : classes) {
                PanelDescription desc = clazz.getAnnotation(PanelDescription.class);
                if (desc == null || desc.panelIdentifier() == null) {
                    continue;
                }
                if (identifier.equals(desc.panelIdentifier())) {
                    return clazz;
                }
            }
        }
        return null;
    }

    public static List<ContainerPanelConfigurationType> getPanelsFor(Class<? extends ObjectType> objectType) {
        List<ContainerPanelConfigurationType> panels = new ArrayList<>();
        for (String packageToScan : PACKAGES_TO_SCAN) {
            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
            for (Class<?> clazz : classes) {
                PanelDescription desc = clazz.getAnnotation(PanelDescription.class);
                if (desc == null || desc.applicableFor() == null || desc.generic()) {
                    continue;
                }
                Class<? extends ObjectType> applicableFor = desc.applicableFor();
                if (applicableFor.isAssignableFrom(objectType) && desc.childOf().equals(Panel.class)) {
                    ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
                    config.setIdentifier(desc.identifier());
                    config.setPanelIdentifier(desc.panelIdentifier());
                    PanelDisplay display = clazz.getAnnotation(PanelDisplay.class);
                    if (display != null) {
                        config.setDisplay(createDisplayType(display));
                    }
//                    if (!desc.path().isBlank()) {
//                        config.setPath(new ItemPathType(ItemPath.create(desc.path())));
//                    }
                    processChildren(objectType, config, clazz);
                    panels.add(config);
                }
            }
        }
        return panels;
    }

    public static Map<String, ContainerPanelConfigurationType> getPanelMapFor(Class<? extends ObjectType> objectType) {
        Map<String, ContainerPanelConfigurationType> panels = new HashMap<>();
        for (String packageToScan : PACKAGES_TO_SCAN) {
            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
            for (Class<?> clazz : classes) {
                PanelDescription desc = clazz.getAnnotation(PanelDescription.class);
                if (desc == null || desc.applicableFor() == null) {
                    continue;
                }
                Class<? extends ObjectType> applicableFor = desc.applicableFor();
                if (applicableFor.isAssignableFrom(objectType) && desc.childOf().equals(Panel.class)) {
                    ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
                    config.setIdentifier(desc.identifier());
                    config.setPanelIdentifier(desc.panelIdentifier());

                    PanelDisplay display = clazz.getAnnotation(PanelDisplay.class);
                    if (display != null) {
                        config.setDisplay(createDisplayType(display));
                    }
                    processChildren(objectType, config, clazz);
                    panels.put(desc.identifier(), config);
                }
            }
        }
        return panels;
    }

    private static void processChildren(Class<? extends ObjectType> objectType, ContainerPanelConfigurationType parent, Class<?> parentClass) {
        for (String packageToScan : PACKAGES_TO_SCAN) {
            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
            for (Class<?> clazz : classes) {
                PanelDescription desc = clazz.getAnnotation(PanelDescription.class);
                if (desc == null || desc.applicableFor() == null || desc.generic()) {
                    continue;
                }
                if (Panel.class.equals(desc.childOf())) {
                    continue;
                }

                if (!desc.childOf().equals(parentClass)) {
                    continue;
                }

                Class<? extends ObjectType> applicableFor = desc.applicableFor();
                if (applicableFor.isAssignableFrom(objectType)) {
                    ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
                    config.setIdentifier(desc.identifier());
                    config.setPanelIdentifier(desc.panelIdentifier());
//                    if (parent.getPath() != null) {
//                        config.setPath(parent.getPath());
//                    }
//                    if (!desc.path().isBlank()) {  //TODO append to parent? consider only absolutePaths?
//                        config.setPath(new ItemPathType(ItemPath.create(desc.path())));
//                    }
                    PanelDisplay display = clazz.getAnnotation(PanelDisplay.class);
                    if (display != null) {
                        config.setDisplay(createDisplayType(display));
                    }
                    parent.getPanel().add(config);
                }
            }
        }
    }

    private static DisplayType createDisplayType(PanelDisplay display) {
        DisplayType displayType = new DisplayType();
        displayType.setLabel(WebComponentUtil.createPolyFromOrigString(display.label()));
        displayType.setCssClass(display.icon());
        return displayType;
    }

    private static DisplayType createDisplayType(String display) {
        DisplayType displayType = new DisplayType();
        displayType.setLabel(WebComponentUtil.createPolyFromOrigString(display));
        displayType.setCssClass(GuiStyleConstants.EVO_ASSIGNMENT_ICON);
        return displayType;
    }

    public static <O extends ObjectType> List<ContainerPanelConfigurationType> getAssignmentPanelsFor(Class<O> clazz) {
        List<ContainerPanelConfigurationType> panels = new ArrayList<>();
        panels.add(createAssignmentPanelConfiguration("allAssignments", "All"));
        panels.add(createAssignmentPanelConfiguration("roleAssignments", "Role"));
        panels.add(createAssignmentPanelConfiguration("resourceAssignments", "Resource"));
        panels.add(createAssignmentPanelConfiguration("orgAssignments", "Organization"));
        panels.add(createAssignmentPanelConfiguration("serviceAssignments", "Service"));
        panels.add(createAssignmentPanelConfiguration("indirectAssignments", "Direct + Indirect"));
        return panels;
    }

    private static ContainerPanelConfigurationType createAssignmentPanelConfiguration(String identifier, String display) {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setPanelIdentifier(identifier);
        config.setIdentifier(identifier);
        config.setDisplay(createDisplayType(display));
        return config;
    }
}
