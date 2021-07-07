package com.evolveum.midpoint.gui.impl.registry;

import java.util.*;

import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeAssignmentsTabPanel;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeDetailsTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;

public class ObjectDetailsMenuRegistry {

    private static Map<QName, List<String>> menuRegistry = new HashMap<>();

    static {
        menuRegistry.put(UserType.COMPLEX_TYPE, Arrays.asList("basic", "assignments"));
    }

    public static List<String> findPanelsFor(QName type) {
        return menuRegistry.get(type);
    }
}
