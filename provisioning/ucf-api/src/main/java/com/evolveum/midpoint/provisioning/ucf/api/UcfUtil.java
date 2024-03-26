/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
public class UcfUtil {

    public static void addConnectorNames(ConnectorType connectorType, String frameworkPrefix, String type, String version, ConnectorHostType hostType) {
        StringBuilder connectorName = new StringBuilder();
        connectorName.append(frameworkPrefix).append(" ");
        connectorName.append(type);
        connectorName.append(" v");
        connectorName.append(version);
        StringBuilder displayName = new StringBuilder();
        if (type.contains(".")) {
            displayName.append(StringUtils.substringAfterLast(type, "."));
        } else {
            displayName.append(type);
        }
        if (hostType != null) {
            connectorName.append(" @");
            connectorName.append(hostType.getName());
            displayName.append(" @");
            displayName.append(hostType.getName());
        }
        connectorType.setName(new PolyStringType(connectorName.toString()));
        connectorType.setDisplayName(new PolyStringType(displayName.toString()));
    }

    public static void setConnectorSchema(ConnectorType connectorType, PrismSchema connectorSchema) throws SchemaException {
        Document xsdDoc = connectorSchema.serializeToXsd();
        Element xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
        ConnectorTypeUtil.setConnectorXsdSchema(connectorType, xsdElement);
    }

    public static PropertyDescriptor findAnnotatedProperty(Class<?> connectorClass, Class<? extends Annotation> annotationClass) {
        BeanWrapper connectorBean = new BeanWrapperImpl(connectorClass);
        return findAnnotatedProperty(connectorBean, annotationClass);
    }

    public static PropertyDescriptor findAnnotatedProperty(BeanWrapper connectorBean, Class<? extends Annotation> annotationClass) {
        for (PropertyDescriptor prop: connectorBean.getPropertyDescriptors()) {
            if (hasAnnotation(prop, annotationClass)) {
                return prop;
            }
        }
        return null;
    }

    public static boolean hasAnnotation(PropertyDescriptor prop, Class<? extends Annotation> annotationClass) {
        return getAnnotation(prop, annotationClass) != null;
    }

    private static <T extends Annotation> T getAnnotation(PropertyDescriptor prop, Class<T> annotationClass) {
        Method readMethod = prop.getReadMethod();
        if (readMethod != null) {
            T annotation = readMethod.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        Method writeMethod = prop.getWriteMethod();
        if (writeMethod != null) {
            T annotation = writeMethod.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return prop.getPropertyType().getAnnotation(annotationClass);
    }
}
