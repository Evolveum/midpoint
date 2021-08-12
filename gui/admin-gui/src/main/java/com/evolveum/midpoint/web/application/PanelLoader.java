package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.panel.Panel;

import java.util.*;

public class PanelLoader {

    private static final Trace LOGGER = TraceManager.getTrace(PanelLoader.class);

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.component.objectdetails", //Old panels
            "com.evolveum.midpoint.web.component.assignment",  //Assignments
            "com.evolveum.midpoint.gui.impl.page.admin"
    };

    public static Class<?> findPanel(String identifier) {
        LOGGER.info("Start collecting classes");
        Set<Class<?>> classes = collectClasses();
        LOGGER.info("Finish collecting classes");
        for (Class<?> clazz : classes) {
            PanelType desc = clazz.getAnnotation(PanelType.class);
            if (desc == null || desc.name() == null) {
                continue;
            }
            if (identifier.equals(desc.name())) {
                return clazz;
            }
        }

        return null;
    }

    private static Set<Class<?>> collectClasses() {
        Set<Class<?>> classes = new HashSet<>();
        for (String packageToScan : PACKAGES_TO_SCAN) {
            classes.addAll(ClassPathUtil.listClasses(packageToScan));
        }
        return classes;
    }

//    public static List<ContainerPanelConfigurationType> getPanelsFor(Class<? extends ObjectType> objectType) {
//
//        List<ContainerPanelConfigurationType> panels = new ArrayList<>();
//        for (String packageToScan : PACKAGES_TO_SCAN) {
//            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
//            for (Class<?> clazz : classes) {
//                PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
//                if (panelInstance == null || panelInstance.applicableFor() == null) {
//                    continue;
//                }
//                Class<? extends ObjectType> applicableFor = panelInstance.applicableFor();
//                if (applicableFor.isAssignableFrom(objectType) && panelInstance.childOf().equals(Panel.class)) {
//                    ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
//                    config.setIdentifier(panelInstance.identifier());
//                    PanelType desc = clazz.getAnnotation(PanelType.class);
//                    if (desc.generic()) {
//                        continue;
//                    }
//                    config.setPanelType(desc.name());
//                    VirtualContainersSpecificationType container = new VirtualContainersSpecificationType();
//                    if (!desc.defaultContainerPath().isBlank()) {
//                        if ("empty".equals(desc.defaultContainerPath())) {
//                            container.setPath(new ItemPathType(ItemPath.EMPTY_PATH));
//                        } else {
//                            container.setPath(new ItemPathType(ItemPath.create(desc.defaultContainerPath())));
//                        }
//                    }
//                    config.getContainer().add(container);
//                    PanelDisplay display = clazz.getAnnotation(PanelDisplay.class);
//                    if (display != null) {
//                        config.setDisplay(createDisplayType(display));
//                        config.setDisplayOrder(display.order());
//                    }
////                    if (!desc.path().isBlank()) {
////                        config.setPath(new ItemPathType(ItemPath.create(desc.path())));
////                    }
//                    List<ContainerPanelConfigurationType> children = processChildren(objectType, clazz);
//                    config.getPanel().addAll(children);
//                    panels.add(config);
//                }
//
//
//
//            }
//        }
//        sort(panels);
//        return panels;
//    }
//
//    private static void sort(List<ContainerPanelConfigurationType> panels) {
//        panels.sort((p1, p2) -> {
//            int displayOrder1 = (p1 == null || p1.getDisplayOrder() == null) ? Integer.MAX_VALUE : p1.getDisplayOrder();
//            int displayOrder2 = (p2 == null || p2.getDisplayOrder() == null) ? Integer.MAX_VALUE : p2.getDisplayOrder();
//
//            return Integer.compare(displayOrder1, displayOrder2);
//        });
//    }
//
//    private static List<ContainerPanelConfigurationType> processChildren(Class<? extends ObjectType> objectType, Class<?> parentClass) {
//        List<ContainerPanelConfigurationType> configs = new ArrayList<>();
//        for (String packageToScan : PACKAGES_TO_SCAN) {
//            Set<Class<?>> classes = ClassPathUtil.listClasses(packageToScan);
//            for (Class<?> clazz : classes) {
//                PanelInstance panelInstance = clazz.getAnnotation(PanelInstance.class);
//                if (panelInstance == null || panelInstance.applicableFor() == null) {
//                    continue;
//                }
//                if (Panel.class.equals(panelInstance.childOf())) {
//                    continue;
//                }
//
//                if (!panelInstance.childOf().equals(parentClass)) {
//                    continue;
//                }
//
//                Class<? extends ObjectType> applicableFor = panelInstance.applicableFor();
//                if (applicableFor.isAssignableFrom(objectType)) {
//                    ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
//                    config.setIdentifier(panelInstance.identifier());
//                    PanelType desc = clazz.getAnnotation(PanelType.class);
//                    config.setPanelType(desc.name());
//
////                    if (parent.getPath() != null) {
////                        config.setPath(parent.getPath());
////                    }
////                    if (!desc.path().isBlank()) {  //TODO append to parent? consider only absolutePaths?
////                        config.setPath(new ItemPathType(ItemPath.create(desc.path())));
////                    }
//                    VirtualContainersSpecificationType container = new VirtualContainersSpecificationType();
//                    if (!desc.defaultContainerPath().isBlank()) {
//                        if ("empty".equals(desc.defaultContainerPath())) {
//                            container.setPath(new ItemPathType(ItemPath.EMPTY_PATH));
//                        } else {
//                            container.setPath(new ItemPathType(ItemPath.create(desc.defaultContainerPath())));
//                        }
//                    }
//                    config.getContainer().add(container);
//                    PanelDisplay display = clazz.getAnnotation(PanelDisplay.class);
//                    if (display != null) {
//                        config.setDisplay(createDisplayType(display));
//                        config.setDisplayOrder(display.order());
//                    }
//                    configs.add(config);
//                }
//            }
//        }
//        sort(configs);
//        return configs;
//    }
//
//    private static DisplayType createDisplayType(PanelDisplay display) {
//        DisplayType displayType = new DisplayType();
//        displayType.setLabel(WebComponentUtil.createPolyFromOrigString(display.label()));
//        displayType.setCssClass(display.icon());
//        return displayType;
//    }

//    private static DisplayType createDisplayType(String display) {
//        DisplayType displayType = new DisplayType();
//        displayType.setLabel(WebComponentUtil.createPolyFromOrigString(display));
//        displayType.setCssClass(GuiStyleConstants.EVO_ASSIGNMENT_ICON);
//        return displayType;
//    }

//    public static <O extends ObjectType> List<ContainerPanelConfigurationType> getAssignmentPanelsFor(Class<O> clazz) {
//        List<ContainerPanelConfigurationType> panels = new ArrayList<>();
//        panels.add(createAssignmentPanelConfiguration("allAssignments", "All"));
//        panels.add(createAssignmentPanelConfiguration("roleAssignments", "Role"));
//        panels.add(createAssignmentPanelConfiguration("resourceAssignments", "Resource"));
//        panels.add(createAssignmentPanelConfiguration("orgAssignments", "Organization"));
//        panels.add(createAssignmentPanelConfiguration("serviceAssignments", "Service"));
//        panels.add(createAssignmentPanelConfiguration("indirectAssignments", "Direct + Indirect"));
//        return panels;
//    }

//    private static ContainerPanelConfigurationType createAssignmentPanelConfiguration(String identifier, String display) {
//        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
//        config.setPanelType(identifier);
//        config.setIdentifier(identifier);
//        config.setDisplay(createDisplayType(display));
//        return config;
//    }
}
