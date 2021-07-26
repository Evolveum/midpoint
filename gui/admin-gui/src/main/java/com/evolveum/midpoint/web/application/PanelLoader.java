package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.*;

public class PanelLoader {

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.component.objectdetails",
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
                if (applicableFor.isAssignableFrom(objectType)) {
                    ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
                    config.setIdentifier(desc.identifier());
                    config.setDisplay(createDisplayType(desc));
                    panels.add(config);
                }
            }
        }
        return panels;
    }

    private static DisplayType createDisplayType(PanelDescription desc) {
        DisplayType displayType = new DisplayType();
        displayType.setLabel(WebComponentUtil.createPolyFromOrigString(desc.label()));
        displayType.setCssClass(desc.icon());
        return displayType;
    }
}
