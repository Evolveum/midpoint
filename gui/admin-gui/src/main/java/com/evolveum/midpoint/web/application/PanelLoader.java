package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.util.ClassPathUtil;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class PanelLoader {

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.component.objectdetails"
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
}
