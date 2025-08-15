/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.constants;

import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class MidPointConstants {

    public static final String NS_MIDPOINT_PUBLIC_PREFIX = "http://midpoint.evolveum.com/xml/ns/public";
    public static final String NS_MIDPOINT_TEST_PREFIX = "http://midpoint.evolveum.com/xml/ns/test";
    public static final String EXPRESSION_LANGUAGE_URL_BASE = NS_MIDPOINT_PUBLIC_PREFIX + "/expression/language#";

    public static final String NS_RA = NS_MIDPOINT_PUBLIC_PREFIX+"/resource/annotation-3";
    public static final String PREFIX_NS_RA = "ra";
    public static final QName RA_RESOURCE_OBJECT = new QName(NS_RA, "resourceObject");
    public static final QName RA_NATIVE_OBJECT_CLASS = new QName(NS_RA, "nativeObjectClass");
    public static final QName RA_NATIVE_ATTRIBUTE_NAME = new QName(NS_RA, "nativeAttributeName");
    public static final QName RA_FRAMEWORK_ATTRIBUTE_NAME = new QName(NS_RA, "frameworkAttributeName");
    public static final QName RA_RETURNED_BY_DEFAULT_NAME = new QName(NS_RA, "returnedByDefault");
    public static final QName RA_DISPLAY_NAME_ATTRIBUTE = new QName(NS_RA, "displayNameAttribute");
    public static final QName RA_NAMING_ATTRIBUTE = new QName(NS_RA, "namingAttribute");
    public static final QName RA_DESCRIPTION_ATTRIBUTE = new QName(NS_RA, "descriptionAttribute");
    public static final QName RA_IDENTIFIER = new QName(NS_RA, "identifier");
    public static final QName RA_SECONDARY_IDENTIFIER = new QName(NS_RA, "secondaryIdentifier");
    public static final QName RA_DEFAULT = new QName(NS_RA, "default");
    public static final QName RA_AUXILIARY = new QName(NS_RA, "auxiliary");
    public static final QName RA_EMBEDDED = new QName(NS_RA, "embedded");
    public static final QName RA_ROLE_IN_REFERENCE = new QName(NS_RA, "roleInReference");
    public static final QName RA_REFERENCED_OBJECT_CLASS_NAME = new QName(NS_RA, "referencedObjectClassName");

    public static final QName RA_DESCRIPTION = new QName(NS_RA, "description");

    public static final String NS_RI = NS_MIDPOINT_PUBLIC_PREFIX+"/resource/instance-3";
    public static final String PREFIX_NS_RI = "ri";

    public static final String FUNCTION_LIBRARY_BASIC_VARIABLE_NAME = "basic";
    public static final String FUNCTION_LIBRARY_LOG_VARIABLE_NAME = "log";
    public static final String FUNCTION_LIBRARY_REPORT_VARIABLE_NAME = "report";

    public static final String PROFILING_LOGGER_NAME = OperationExecutionLogger.PROFILING_LOGGER_NAME;
    public static final String JAVA_HOME_ENVIRONMENT_VARIABLE = "JAVA_HOME";

    public static final String NS_JAXB = "https://jakarta.ee/xml/ns/jaxb";
    public static final String PREFIX_NS_JAXB = "jaxb";
}
