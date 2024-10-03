/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.dummys;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import javax.xml.namespace.QName;

/**
 * These constants are not directly visible on the dummy resources here; but they are related to them.
 * For example, extension items are defined here.
 */
public class ScenariosConstants {

    private static final String NS_DMS = "http://midpoint.evolveum.com/xml/ns/samples/dms";
    private static final String NS_HR = "http://midpoint.evolveum.com/xml/ns/samples/hr";

    public static final ItemName HR_COST_CENTER = ItemName.from(NS_HR, "costCenter");
    public static final ItemName HR_RESPONSIBILITY = ItemName.from(NS_HR, "responsibility");
    public static final ItemPath HR_RESPONSIBILITY_PATH = AssignmentType.F_EXTENSION.append(HR_RESPONSIBILITY);

    public static final String LEVEL_READ = "read";
    public static final QName RELATION_READ = new QName(NS_DMS, LEVEL_READ);
    public static final String LEVEL_WRITE = "write";
    public static final QName RELATION_WRITE = new QName(NS_DMS, LEVEL_WRITE);
    public static final String LEVEL_ADMIN = "admin";
    public static final QName RELATION_ADMIN = new QName(NS_DMS, LEVEL_ADMIN);
}
