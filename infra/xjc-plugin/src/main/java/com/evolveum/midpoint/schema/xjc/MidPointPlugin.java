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

package com.evolveum.midpoint.schema.xjc;

import com.evolveum.midpoint.schema.processor.TestMidpointObject;
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * Simple proof of concept for our custom XJC plugin.
 *
 * @author lazyman
 */
public class MidPointPlugin {

    private static final QName QNAME_OBJECT_TYPE = new QName(PrefixMapper.C.getNamespace(), "ObjectType");
    private static final QName QNAME_USER_TYPE = new QName(PrefixMapper.C.getNamespace(), "UserType");
    private static final QName QNAME_FULL_NAME = new QName(PrefixMapper.C.getNamespace(), "fullName");

    public String getOptionName() {
        return "Xmidpoint";
    }

    public String getUsage() {
        return "-" + getOptionName();
    }

    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        try {
            addElementTypes(outline);

            createSchemaConstants(outline);

            updateObjectType(outline);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't process MidPoint JAXB customisation, reason: "
                    + ex.getMessage() + ", " + ex.getClass(), ex);
        }

        return true;
    }

    private void addElementTypes(Outline outline) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();

            if (qname != null) {
                createPSFField(outline, classOutline.implClass, "ELEMENT_TYPE", qname);
            }
        }
    }

    private JFieldVar createPSFField(Outline outline, JDefinedClass definedClass, String fieldName, QName reference) {
        JClass clazz = (JClass) outline.getModel().codeModel._ref(QName.class);

        JInvocation invocation = (JInvocation) JExpr._new(clazz);
        invocation.arg(reference.getNamespaceURI());
        invocation.arg(reference.getLocalPart());

        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        return definedClass.field(psf, QName.class, fieldName, invocation);
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

    private ClassOutline findClassOutline(Outline outline, QName type) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (!type.equals(qname)) {
                continue;
            }

            return classOutline;
        }

        throw new IllegalStateException("Object type class outline was not found.");
    }

    private void updateObjectType(Outline outline) {
        ClassOutline objectTypeClassOutline = findClassOutline(outline, QNAME_OBJECT_TYPE);

        JDefinedClass definedClass = objectTypeClassOutline.implClass;

        //inserting MidPointObject field into ObjectType class
        JClass clazz = (JClass) outline.getModel().codeModel._ref(TestMidpointObject.class);
        JVar field = definedClass.field(JMod.PRIVATE, TestMidpointObject.class, "container", JExpr._new(clazz));
        //adding XmlTransient annotation
        field.annotate((JClass) outline.getModel().codeModel._ref(XmlTransient.class));

        JMethod getMethod = definedClass.method(JMod.PUBLIC, clazz, "getContainer");
        //create body method
        JBlock body = getMethod.body();
        body._return(field);
        //adding Deprecation annotation and small comment to method
        getMethod.annotate((JClass) outline.getModel().codeModel._ref(Deprecated.class));
        JDocComment comment = getMethod.javadoc();
        comment.append("DO NOT USE! For testing purposes only.");

        //for example we update UserType and its fullName
        updateUserType(outline, getMethod);
    }

    private void updateUserType(Outline outline, JMethod getContainer) {
        ClassOutline userType = findClassOutline(outline, QNAME_USER_TYPE);
        JDefinedClass user = userType.implClass;

        //update field
        JFieldVar field = user.fields().get("fullName");
        user.removeField(field);

        //update get/set methods
        Iterator<JMethod> iterator = user.methods().iterator();
        while (iterator.hasNext()) {
            JMethod method = iterator.next();
            if ("getFullName".equals(method.name())) {
                iterator.remove();
            }
            if ("setFullName".equals(method.name())) {
                iterator.remove();
            }
        }
        updateGetFullName(outline, userType, getContainer);
        updateSetFullName(outline, userType, getContainer);
    }

    private void updateGetFullName(Outline outline, ClassOutline userType, JMethod getContainer) {
        JDefinedClass user = userType.implClass;
        JFieldVar field = createPSFField(outline, user, "FULL_NAME", QNAME_FULL_NAME);

        JMethod method = user.method(JMod.PUBLIC, String.class, "getFullName");
        JAnnotationUse annotation = method.annotate((JClass) outline.getModel().codeModel._ref(XmlElement.class));
        annotation.param("required", true);
        JBlock body = method.body();

        JInvocation returnExpr = JExpr.invoke(JExpr.invoke(getContainer), "getValue");
        returnExpr.arg(JExpr.ref("FULL_NAME"));

        JClass type = (JClass) outline.getModel().codeModel._ref(String.class);
        body._return(JExpr.cast(type, returnExpr));
    }

    private void updateSetFullName(Outline outline, ClassOutline userType, JMethod getContainer) {
        JDefinedClass user = userType.implClass;
        JMethod method = user.method(JMod.PUBLIC, void.class, "setFullName");
        method.param(String.class, "fullName");

        JBlock body = method.body();

        JExpression returnExpr = JExpr.invoke(getContainer);
        JInvocation set = body.invoke(returnExpr, "setValue");
        set.arg(JExpr.ref("FULL_NAME"));
        set.arg(JExpr.ref("fullName"));
    }
}
