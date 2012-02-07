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

import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.evolveum.midpoint.schema.xjc.Processor;
import static com.evolveum.midpoint.schema.xjc.util.ProcessorUtils.*;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
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
    private static final String METHOD_GET_PROPERTY_VALUE = "getPropertyValue";
    private static final String METHOD_GET_PROPERTY_VALUES = "getPropertyValues";
    private static final String METHOD_SET_PROPERTY_VALUE = "setPropertyValue";
    private static final String METHOD_GET_CONTAINER = "getContainer";
    private static final String METHOD_SET_CONTAINER = "setContainer";

    //todo change annotation on ObjectType in common-1.xsd to a:midPointContainer

    //todo implement equals and hash methods for objects with property containers

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        try {
            StepSchemaConstants stepSchemaConstants = new StepSchemaConstants();
            stepSchemaConstants.run(outline, options, errorHandler);

            Map<String, JFieldVar> namespaceFields = stepSchemaConstants.getNamespaceFields();
            addComplextType(outline, namespaceFields);
            addFieldQNames(outline, namespaceFields);

//            Set<JDefinedClass> containers = updateMidPointContainer(outline);
//            containers.addAll(updatePropertyContainer(outline));
//
//            addContainerUtilMethodsToObjectType(outline);
//
//            updateFields(outline, containers);
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
        return updateContainer(outline, MIDPOINT_CONTAINER, MidPointObject.class);
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

            //todo remove, only till propertyContainer annotation is on ObjectType
            if (hasAnnotation(classOutline, MIDPOINT_CONTAINER) && hasAnnotation(classOutline, PROPERTY_CONTAINER)
                    && annotation.equals(PROPERTY_CONTAINER)) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;
            containers.add(definedClass);

            //inserting MidPointObject field into ObjectType class
            JVar container = definedClass.field(JMod.PRIVATE, containerClass, CONTAINER_FIELD_NAME);
            //adding XmlTransient annotation
            container.annotate((JClass) outline.getModel().codeModel._ref(XmlTransient.class));

            //create getContainer
            createGetContainerMethod(definedClass, container, containerClass, outline);

            //create setContainer
            createSetContainerMethod(definedClass, container, containerClass, outline);
        }

        return containers;
    }

    private void createGetContainerMethod(JDefinedClass definedClass, JVar container,
            Class<? extends PropertyContainer> containerClass, Outline outline) {

        JClass clazz = (JClass) outline.getModel().codeModel._ref(containerClass);
        JMethod getContainer = definedClass.method(JMod.PUBLIC, clazz, METHOD_GET_CONTAINER);
        //add deprecation
        addDeprecation(outline, getContainer);

        //create method body
        JBlock body = getContainer.body();
        JBlock then = body._if(container.eq(JExpr._null()))._then();

        JInvocation newContainer = (JInvocation) JExpr._new(clazz);
        newContainer.arg(JExpr.ref(COMPLEX_TYPE_FIELD));
        then.assign(container, newContainer);

        body._return(container);
    }

    private void createSetContainerMethod(JDefinedClass definedClass, JVar container,
            Class<? extends PropertyContainer> containerClass, Outline outline) {

        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, METHOD_SET_CONTAINER);
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
                createPSFField(outline, classOutline.implClass, COMPLEX_TYPE_FIELD, qname);
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
                if ("serialVersionUID".equals(field) || "oid".equals(field) || COMPLEX_TYPE_FIELD.equals(field)) {
                    continue;
                }

                String fieldName = fieldFPrefixUnderscoredUpperCase(field);
                boxes.add(new FieldBox(fieldName, new QName(qname.getNamespaceURI(), field)));
            }

            for (FieldBox<QName> box : boxes) {
                JFieldVar var = namespaceFields.get(qname.getNamespaceURI());
                if (var != null) {
                    createQNameDefinition(outline, implClass, box.getFieldName(), var, box.getValue());
                } else {
                    createPSFField(outline, implClass, box.getFieldName(), box.getValue());
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

            if (!isPropertyContainer(classOutline.implClass, outline)) {
                //it's PropertyContainer, MidPointObject class
                continue;
            }

            System.out.println("Updating fields and get/set methods: " + classOutline.implClass.fullName());

            List<JFieldVar> fieldsToBeRemoved = new ArrayList<JFieldVar>();
            boolean remove;
            for (String field : fields.keySet()) {
                if ("serialVersionUID".equals(field) || COMPLEX_TYPE_FIELD.equals(field)
                        || CONTAINER_FIELD_NAME.equals(field)) {
                    continue;
                }

                JFieldVar fieldVar = fields.get(field);
                boolean isPublicStaticFinal = (fieldVar.mods().getValue() & (JMod.STATIC | JMod.FINAL)) != 0;
                if (field.startsWith("F_") && isPublicStaticFinal) {
                    //our QName constant fields
                    continue;
                }

                remove = false;
                if ("oid".equals(field)) {
                    System.out.println("Updating oid field: " + fieldVar.name());
                    remove = updateOidField(fieldVar, classOutline);
                } else if (isFieldTypeContainer(fieldVar, classOutline)) {
                    System.out.println("Updating container field: " + fieldVar.name());
                    remove = updateContainerFieldType(fieldVar, classOutline);
                } else {
                    System.out.println("Updating field: " + fieldVar.name());
                    remove = updateField(fieldVar, classOutline);
                }

                if (remove) {
                    fieldsToBeRemoved.add(fieldVar);
                }
            }

            for (JFieldVar field : fieldsToBeRemoved) {
                implClass.removeField(field);
            }
        }
    }

    private void addContainerUtilMethodsToObjectType(Outline outline) {
        QName objectType = new QName(PrefixMapper.C.getNamespace(), "ObjectType");
        ClassOutline classOutline = findClassOutline(outline, objectType);

        if (classOutline == null) {
            throw new IllegalStateException("Couldn't find class outline for " + objectType);
        }

        JDefinedClass implClass = classOutline.implClass;

        JMethod getPropertyValues = implClass.method(JMod.NONE, List.class, METHOD_GET_PROPERTY_VALUES);
        JTypeVar T = getPropertyValues.generify("T");
        JClass listClass = (JClass) outline.getModel().codeModel.ref(List.class).narrow(T);
        getPropertyValues.type(listClass);
        getPropertyValues.param(QName.class, "name");
        JClass clazz = (JClass) outline.getModel().codeModel.ref(Class.class).narrow(T);
        getPropertyValues.param(clazz, "clazz");
        createGetPropertiesBody(getPropertyValues, outline);

        JMethod getPropertyValue = implClass.method(JMod.NONE, Object.class, METHOD_GET_PROPERTY_VALUE);
        T = getPropertyValue.generify("T");
        getPropertyValue.type(T);
        getPropertyValue.param(QName.class, "name");
        clazz = (JClass) outline.getModel().codeModel.ref(Class.class).narrow(T);
        getPropertyValue.param(clazz, "clazz");
        createGetPropertyBody(getPropertyValue, outline);

        JMethod setPropertyValue = implClass.method(JMod.NONE, void.class, METHOD_SET_PROPERTY_VALUE);
        T = setPropertyValue.generify("T");
        setPropertyValue.param(QName.class, "name");
        setPropertyValue.param(T, "value");
        createSetPropertyBody(setPropertyValue, outline);
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

    private boolean isPropertyContainer(JDefinedClass definedClass, Outline outline) {
        if (definedClass == null) {
            return false;
        }

        ClassOutline classOutline = null;
        for (ClassOutline clazz : outline.getClasses()) {
            if (definedClass.equals(clazz.implClass)) {
                classOutline = clazz;
                break;
            }
        }

        if (classOutline == null) {
            return false;
        }

        boolean isContainer = hasAnnotation(classOutline, PROPERTY_CONTAINER)
                || hasAnnotation(classOutline, MIDPOINT_CONTAINER);

        if (isContainer) {
            return true;
        }

        if (!(definedClass._extends() instanceof JDefinedClass)) {
            return false;
        }

        return isPropertyContainer((JDefinedClass) definedClass._extends(), outline);
    }

    private boolean isFieldTypeContainer(JFieldVar field, ClassOutline classOutline) {
        Outline outline = classOutline.parent();

        JType type = field.type();
        if (type instanceof JDefinedClass) {
            return isPropertyContainer((JDefinedClass) type, outline);
        }

        return false;
    }

    private boolean updateOidField(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass definedClass = classOutline.implClass;

        Outline outline = classOutline.parent();
        JClass string = (JClass) outline.getModel().codeModel._ref(String.class);
        JMethod oldMethod = definedClass.getMethod("getOid", new JType[]{});
        JMethod method = recreateMethod(oldMethod, definedClass);
        JBlock body = method.body();
        body._return(JExpr.invoke("getContainer").invoke("getOid"));
        copyAnnotations(method, field, oldMethod);

        method = definedClass.getMethod("setOid", new JType[]{string});
        method = recreateMethod(method, definedClass);
        body = method.body();
        JInvocation invocation = body.invoke(JExpr.invoke("getContainer"), method.name());
        invocation.arg(method.listParams()[0]);

        return true;
    }

    private void createGetPropertiesBody(JMethod method, Outline outline) {
        //todo implement
        notYetImplementedException(outline, method);
    }

    private void createGetPropertyBody(JMethod method, Outline outline) {
        //todo implement
        notYetImplementedException(outline, method);
    }

    private void createSetPropertyBody(JMethod method, Outline outline) {
        //todo implement
        notYetImplementedException(outline, method);
    }

    private boolean updateContainerFieldType(JFieldVar field, ClassOutline classOutline) {
        //only setter method must be updated
        String methodName = getSetterMethod(classOutline, field);
        JDefinedClass definedClass = classOutline.implClass;
        JMethod method = definedClass.getMethod(methodName, new JType[]{field.type()});
        method = recreateMethod(method, definedClass);

        JBlock body = method.body();
        body.directStatement("//todo do not use setValue but remove container from parent " +
                "property container, update if user set/remove method");
        JVar param = method.listParams()[0];
        body.assign(JExpr._this().ref(field), param);
        JConditional condition = body._if(param.eq(JExpr._null()));
        JBlock thenBlock = condition._then();

        JInvocation invocation = thenBlock.invoke(METHOD_SET_PROPERTY_VALUE);
        //push arguments
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(JExpr._null());

        JBlock elseBlock = condition._else();
        invocation = elseBlock.invoke(METHOD_SET_PROPERTY_VALUE);
        //push arguments
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(JExpr.invoke(param, METHOD_GET_CONTAINER));

        return false;
    }

    private boolean updateField(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass definedClass = classOutline.implClass;
        //update getter
        String methodName = getGetterMethod(classOutline, field);
        JMethod oldMethod = definedClass.getMethod(methodName, new JType[]{});
        JMethod method = recreateMethod(oldMethod, definedClass);
        copyAnnotations(method, field, oldMethod);

        JClass list = (JClass) classOutline.parent().getModel().codeModel._ref(List.class);
        JType type = field.type();
        boolean isList = false;
        if (type instanceof JClass) {
            isList = list.equals(((JClass) type).erasure());
        }
        createFieldGetterBody(method, field, classOutline, isList);

        //update setter
        if (isList) {
            //setter for list field members was not created
            return true;
        }

        methodName = getSetterMethod(classOutline, field);
        method = definedClass.getMethod(methodName, new JType[]{field.type()});
        method = recreateMethod(method, definedClass);
        createFieldSetterBody(method, field, classOutline);

        return true;
    }

    private void createFieldSetterBody(JMethod method, JFieldVar field, ClassOutline classOutline) {
        JBlock body = method.body();

        JInvocation invocation = body.invoke(METHOD_SET_PROPERTY_VALUE);
        //push arguments
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(method.listParams()[0]);
    }

    private void createFieldGetterBody(JMethod method, JFieldVar field, ClassOutline classOutline, boolean isList) {
        JBlock body = method.body();

        JInvocation invocation;
        if (isList) {
            invocation = JExpr.invoke(METHOD_GET_PROPERTY_VALUES);
        } else {
            invocation = JExpr.invoke(METHOD_GET_PROPERTY_VALUE);
        }
        //push arguments
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        JType type = field.type();
        if (type.isPrimitive()) {
            JPrimitiveType primitive = (JPrimitiveType) type;
            invocation.arg(JExpr.dotclass(primitive.boxify()));
        } else {
            JClass clazz = (JClass) type;
            if (isList) {
                invocation.arg(JExpr.dotclass(clazz.getTypeParameters().get(0)));
            } else {
                invocation.arg(JExpr.dotclass(clazz));
            }
        }

        body._return(invocation);
    }
}
