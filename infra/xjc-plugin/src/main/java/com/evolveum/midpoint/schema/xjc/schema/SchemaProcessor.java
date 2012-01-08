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

package com.evolveum.midpoint.schema.xjc.schema;

import com.evolveum.midpoint.schema.processorFake.MidpointObject;
import com.evolveum.midpoint.schema.processorFake.PropertyContainer;
import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.evolveum.midpoint.schema.xjc.Processor;
import com.evolveum.midpoint.schema.xjc.util.ProcessorUtils;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDeclaration;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComponent;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * Simple proof of concept for our custom XJC plugin.
 *
 * @author lazyman
 */
public class SchemaProcessor implements Processor {

    private static final String COMPLEX_TYPE_FIELD = "COMPLEX_TYPE";
    private static final String CONTAINER_FIELD_NAME = "container";
    private static final QName PROPERTY_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "propertyContainer");
    private static final QName MIDPOINT_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "midPointContainer");

    //todo change annotation on ObjectType in common-1.xsd to a:midPointContainer

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        try {
            StepSchemaConstants stepSchemaConstants = new StepSchemaConstants();
            stepSchemaConstants.run(outline, options, errorHandler);

            Map<String, JFieldVar> namespaceFields = stepSchemaConstants.getNamespaceFields();
            addComplextType(outline, namespaceFields);
            addFieldQNames(outline, namespaceFields);

            Set<JDefinedClass> containers = updateMidPointContainer(outline);
            containers.addAll(updatePropertyContainer(outline));

            addContainerUtilMethodsToObjectType(outline);

            updateFields(outline, containers);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Couldn't process MidPoint JAXB customisation, reason: "
                    + ex.getMessage() + ", " + ex.getClass(), ex);
        }

        return true;
    }

    private Set<JDefinedClass> updatePropertyContainer(Outline outline) {
        return updateContainer(outline, PROPERTY_CONTAINER, PropertyContainer.class);
    }

    private Set<JDefinedClass> updateMidPointContainer(Outline outline) {
        return updateContainer(outline, MIDPOINT_CONTAINER, MidpointObject.class);
    }

    private Set<JDefinedClass> updateContainer(Outline outline, QName annotation,
            Class<? extends PropertyContainer> containerClass) {

        Set<JDefinedClass> containers = new HashSet<JDefinedClass>();

        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());

            QName qname = entry.getValue().getTypeName();
            if (qname == null) {
                continue;
            }

            if (!hasAnnotation(classOutline, annotation)) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;
            containers.add(definedClass);

            //inserting MidPointObject field into ObjectType class
            JClass clazz = (JClass) outline.getModel().codeModel._ref(containerClass);
            JVar container = definedClass.field(JMod.PRIVATE, containerClass, CONTAINER_FIELD_NAME);
            //adding XmlTransient annotation
            container.annotate((JClass) outline.getModel().codeModel._ref(XmlTransient.class));

            //create getContainer
            JMethod getContainer = definedClass.method(JMod.PUBLIC, clazz, "getContainer");
            addDeprecation(outline, getContainer); //add deprecation
            //create method body
            JBlock body = getContainer.body();
            JBlock then = body._if(container.eq(JExpr._null()))._then();

            JInvocation newContainer = (JInvocation) JExpr._new(clazz);
            newContainer.arg(JExpr.ref(COMPLEX_TYPE_FIELD));
            then.assign(container, newContainer);

            body._return(container);

            //create setContainer
            createSetContainerMethod(definedClass, container, containerClass, outline);
        }

        return containers;
    }

    private void createSetContainerMethod(JDefinedClass definedClass, JVar container,
            Class<? extends PropertyContainer> containerClass, Outline outline) {

        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, "setContainer");
        addDeprecation(outline, setContainer); //add deprecation
        JVar methodContainer = setContainer.param(containerClass, "container");
        //create method body
        JBlock body = setContainer.body();
        JBlock then = body._if(methodContainer.eq(JExpr._null()))._then();
        then.assign(container, JExpr._null());
        then._return();

        JInvocation equals = JExpr.invoke(JExpr.ref(COMPLEX_TYPE_FIELD), "equals");
        equals.arg(methodContainer.invoke("getName"));

        then = body._if(equals.not())._then();
        JClass illegalArgumentClass = (JClass) outline.getModel().codeModel._ref(IllegalArgumentException.class);
        JInvocation exception = JExpr._new(illegalArgumentClass);

        JExpression message = JExpr.lit("Container qname '").plus(JExpr.invoke(methodContainer, "getName"))
                .plus(JExpr.lit("' doesn't equals to '")).plus(JExpr.ref(COMPLEX_TYPE_FIELD))
                .plus(JExpr.lit("'."));
        exception.arg(message);
        then._throw(exception);

        body.assign(JExpr._this().ref(container), methodContainer);
    }

    private boolean hasAnnotation(ClassOutline classOutline, QName qname) {
        XSComponent xsComponent = classOutline.target.getSchemaComponent();

        if (xsComponent == null) {
            return false;
        }
        XSAnnotation annotation = xsComponent.getAnnotation(false);
        if (annotation == null) {
            return false;
        }

        Object object = annotation.getAnnotation();
        if (!(object instanceof BindInfo)) {
            return false;
        }

        BindInfo info = (BindInfo) object;
        BIDeclaration[] declarations = info.getDecls();
        if (declarations == null) {
            return false;
        }

        for (BIDeclaration declaration : declarations) {
            if (qname.equals(declaration.getName())) {
                return true;
            }
        }

        return false;
    }

    private void addComplextType(Outline outline, Map<String, JFieldVar> namespaceFields) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (qname == null) {
                continue;
            }

            JFieldVar var = namespaceFields.get(qname.getNamespaceURI());
            if (var != null) {
                createQNameDefinition(outline, classOutline.implClass, COMPLEX_TYPE_FIELD, var, qname);
            } else {
                ProcessorUtils.createPSFField(outline, classOutline.implClass, COMPLEX_TYPE_FIELD, qname);
            }
        }
    }

    private JFieldVar createQNameDefinition(Outline outline, JDefinedClass definedClass, String fieldName,
            JFieldVar namespaceField, QName reference) {
        JClass clazz = (JClass) outline.getModel().codeModel._ref(QName.class);
        JClass schemaClass = (JClass) outline.getModel().codeModel._getClass(StepSchemaConstants.CLASS_NAME);

        JInvocation invocation = (JInvocation) JExpr._new(clazz);
        invocation.arg(schemaClass.staticRef(namespaceField));
        invocation.arg(reference.getLocalPart());

        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        return definedClass.field(psf, QName.class, fieldName, invocation);
    }

    private void addFieldQNames(Outline outline, Map<String, JFieldVar> namespaceFields) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (qname == null) {
                continue;
            }

            JDefinedClass implClass = classOutline.implClass;
            Map<String, JFieldVar> fields = implClass.fields();

            if (fields == null) {
                continue;
            }

            List<FieldBox<QName>> boxes = new ArrayList<FieldBox<QName>>();
            for (String field : fields.keySet()) {
                if ("serialVersionUID".equals(field) || COMPLEX_TYPE_FIELD.equals(field)) {
                    continue;
                }

                String fieldName = ProcessorUtils.fieldFPrefixUnderscoredUpperCase(field);
                boxes.add(new FieldBox(fieldName, new QName(qname.getNamespaceURI(), field)));
            }

            for (FieldBox<QName> box : boxes) {
                JFieldVar var = namespaceFields.get(qname.getNamespaceURI());
                if (var != null) {
                    createQNameDefinition(outline, implClass, box.getFieldName(), var, box.getValue());
                } else {
                    ProcessorUtils.createPSFField(outline, implClass, box.getFieldName(), box.getValue());
                }
            }
        }
    }

    private void updateFields(Outline outline, Set<JDefinedClass> containers) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (qname == null) {
                continue;
            }

            JDefinedClass implClass = classOutline.implClass;
            Map<String, JFieldVar> fields = implClass.fields();

            if (fields == null) {
                continue;
            }

            for (String field : fields.keySet()) {
                if ("serialVersionUID".equals(field) || COMPLEX_TYPE_FIELD.equals(field)) {
                    continue;
                }

                JFieldVar fieldVar = fields.get(field);
                boolean isPublicStaticFinal = (fieldVar.mods().getValue() & (JMod.STATIC | JMod.FINAL)) != 0;
                if (field.startsWith("F_") && isPublicStaticFinal) {
                    //our QName constant fields
                    continue;
                }

                if (isPropertyContainer(fieldVar, outline)) {
                    //it's PropertyContainer, MidPointObject
                    continue;
                }

                if ("oid".equals(field)) {
                    updateOidField(fieldVar, classOutline, outline);
                } else if (isFieldTypeContainer(fieldVar, outline)) {
                    updateContainerFieldType(fieldVar, outline);
                } else {
                    updateField(fieldVar, classOutline, outline);
                }
            }
        }
    }

    private void addContainerUtilMethodsToObjectType(Outline outline) {
        QName objectType = new QName(PrefixMapper.C.getNamespace(), "ObjectType");
        ClassOutline classOutline = ProcessorUtils.findClassOutline(outline, objectType);

        if (classOutline == null) {
            throw new IllegalStateException("Couldn't find class outline for " + objectType);
        }

        JDefinedClass implClass = classOutline.implClass;

        JMethod getPropertyValues = implClass.method(JMod.NONE, List.class, "getPropertyValues");
        JTypeVar T = getPropertyValues.generify("T");
        JClass listClass = (JClass) outline.getModel().codeModel.ref(List.class).narrow(T);
        getPropertyValues.type(listClass);
        getPropertyValues.param(QName.class, "name");
        JClass clazz = (JClass) outline.getModel().codeModel.ref(Class.class).narrow(T);
        getPropertyValues.param(clazz, "clazz");
        notYetImplementedException(outline, getPropertyValues);

        JMethod getPropertyValue = implClass.method(JMod.NONE, Object.class, "getPropertyValue");
        T = getPropertyValue.generify("T");
        getPropertyValue.type(T);
        getPropertyValue.param(QName.class, "name");
        notYetImplementedException(outline, getPropertyValue);

        JMethod setPropertyValue = implClass.method(JMod.NONE, void.class, "setPropertyValue");
        T = setPropertyValue.generify("T");
        setPropertyValue.param(QName.class, "name");
        setPropertyValue.param(T, "value");
        notYetImplementedException(outline, setPropertyValue);
    }

    /**
     * adding Deprecation annotation and small comment to method
     */
    @Deprecated
    private void addDeprecation(Outline outline, JMethod method) {
        method.annotate((JClass) outline.getModel().codeModel._ref(Deprecated.class));
        JDocComment comment = method.javadoc();
        comment.append("DO NOT USE! For testing purposes only.");
    }

    @Deprecated
    private void notYetImplementedException(Outline outline, JMethod method) {
        //adding deprecation
        addDeprecation(outline, method);

        //comment and not yet implemented exception
        JBlock body = method.body();
        body.directStatement("//todo implement in xjc processing with using XmlUtil");

        JClass illegalAccess = (JClass) outline.getModel().codeModel._ref(UnsupportedOperationException.class);
        JInvocation exception = JExpr._new(illegalAccess);
        exception.arg(JExpr.lit("Not yet implemented."));

        body._throw(exception);
    }

    private boolean isPropertyContainer(JFieldVar field, Outline outline) {
        if (!CONTAINER_FIELD_NAME.equals(field.name())) {
            return false;
        }

        JClass propertyContainer = (JClass) outline.getModel().codeModel._ref(PropertyContainer.class);
        JClass midpointObject = (JClass) outline.getModel().codeModel._ref(MidpointObject.class);

        JType type = field.type();
        if (type.equals(propertyContainer) || type.equals(midpointObject)) {
            return true;
        }

        return false;
    }

    private boolean isFieldTypeContainer(JFieldVar field, Outline outline) {
        JType type = field.type();

        if (!(type instanceof JDefinedClass)) {
            return false;
        }
        JDefinedClass clazz = (JDefinedClass) type;
//        System.out.println(">>> " + field.name() + ": " + clazz.fullName());

        return false;
    }

    private void updateOidField(JFieldVar field, ClassOutline classOutline, Outline outline) {

    }

    private void updateContainerFieldType(JFieldVar field, Outline outline) {

    }

    private void updateField(JFieldVar field, ClassOutline classOutline, Outline outline) {

    }
}
