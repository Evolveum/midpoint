/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.xjc.util;

import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.sun.codemodel.*;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyman
 */
public final class ProcessorUtils {

    private ProcessorUtils() {
    }

    public static String fieldFPrefixUnderscoredUpperCase(String fieldName) {
        return "F_" + fieldUnderscoredUpperCase(fieldName);
    }

    public static String fieldPrefixedUnderscoredUpperCase(String fieldName, QName qname) {
        String prefix = PrefixMapper.getPrefix(qname.getNamespaceURI());

        return prefix + fieldUnderscoredUpperCase(fieldName);
    }

    public static String fieldUnderscoredUpperCase(String fieldName) {
        return fieldName.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                "_"
        ).toUpperCase();
    }

    public static ClassOutline findClassOutline(Outline outline, QName type) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (!type.equals(qname)) {
                continue;
            }

            return classOutline;
        }

        throw new IllegalStateException("Object type class defined by qname '" + type + "' outline was not found.");
    }

    public static JFieldVar createPSFField(Outline outline, JDefinedClass definedClass, String fieldName,
            QName reference) {
        JClass clazz = (JClass) outline.getModel().codeModel._ref(QName.class);

        JInvocation invocation = (JInvocation) JExpr._new(clazz);
        invocation.arg(reference.getNamespaceURI());
        invocation.arg(reference.getLocalPart());

        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        return definedClass.field(psf, QName.class, fieldName, invocation);
    }
}
