package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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
                if (desc == null || desc.identifier() == null) {
                    continue;
                }
                if (identifier.equals(desc.identifier())) {
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
                    panels.add(config);
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
                if (desc == null || desc.applicableFor() == null) {
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
