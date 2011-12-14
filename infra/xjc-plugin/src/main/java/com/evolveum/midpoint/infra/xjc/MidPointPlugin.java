/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.infra.xjc;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Simple proof of concept for our custom XJC plugin.
 *
 * @author lazyman
 */
public class MidPointPlugin {

    public String getOptionName() {
        return "Xmidpoint";
    }

    public String getUsage() {
        return "-Xmidpoint";
    }

    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();

            if (qname != null) {
                createPSFField(outline, classOutline.implClass, "ELEMENT_TYPE", qname);
            }
        }

        try {
            createSchemaConstants(outline);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't process MidPoint JAXB customisation, reason: " + ex.getMessage(), ex);
        }

        return true;
    }

    private void createPSFField(Outline outline, JDefinedClass definedClass, String fieldName, QName reference) {
        JClass clazz = (JClass) outline.getModel().codeModel._ref(QName.class);

        JInvocation invocation = (JInvocation) JExpr._new(clazz);
        invocation.arg(reference.getNamespaceURI());
        invocation.arg(reference.getLocalPart());

        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        definedClass.field(psf, QName.class, fieldName, invocation);
    }

    private void createSchemaConstants(Outline outline) throws JClassAlreadyExistsException {
        Model model = outline.getModel();
        JDefinedClass schemaConstants = model.codeModel._class("com.evolveum.midpoint.schema.SchemaConstants");

        List<FieldBox> fields = new ArrayList<FieldBox>();

        Map<QName, CElementInfo> map = model.getElementMappings(null);
        Set<Map.Entry<QName, CElementInfo>> set = map.entrySet();
        for (Map.Entry<QName, CElementInfo> entry : set) {
            QName qname = entry.getKey();
            CElementInfo info = entry.getValue();
            String fieldName = transformFieldName(info.getSqueezedName(), qname);

            fields.add(new FieldBox(fieldName, qname));
        }

        //sort field by name and create field in class
        Collections.sort(fields);
        for (FieldBox field : fields) {
            createPSFField(outline, schemaConstants, field.getFieldName(), field.getQname());
        }
    }

    private String transformFieldName(String fieldName, QName qname) {
        String newName = fieldName.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                "_"
        ).toUpperCase();

        String prefix = PrefixMapper.getPrefix(qname.getNamespaceURI());

        return prefix + newName;
    }
}
