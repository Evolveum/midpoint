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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.evolveum.midpoint.schema.xjc.PrismForJAXBUtil;
import com.evolveum.midpoint.schema.xjc.Processor;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSType;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;

import static com.evolveum.midpoint.schema.xjc.util.ProcessorUtils.*;

/**
 * Simple proof of concept for our custom XJC plugin.
 *
 * @author lazyman
 */
public class SchemaProcessor implements Processor {

    //qname for object reference type
    private static final QName OBJECT_REFERENCE_TYPE = new QName(PrefixMapper.C.getNamespace(), "ObjectReferenceType");
    private static final String REFERENCE_FIELD_NAME = "reference";
    private static final String METHOD_GET_REFERENCE = "getReference";
    private static final String METHOD_SET_REFERENCE = "setReference";
    //annotations for schema processor
    private static final QName PROPERTY_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "propertyContainer");
    private static final QName MIDPOINT_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "midPointContainer");
    //fields and methods for prism containers/prism objects
    private static final String COMPLEX_TYPE_FIELD = "COMPLEX_TYPE";
    private static final String CONTAINER_FIELD_NAME = "container";
    private static final String METHOD_GET_CONTAINER = "getContainer";
    private static final String METHOD_SET_CONTAINER = "setContainer";
    private static final String METHOD_GET_CONTAINER_NAME = "getContainerName";
    //methods in PrismForJAXBUtil
    private static final String METHOD_GET_PROPERTY_VALUE = "getPropertyValue";
    private static final String METHOD_GET_PROPERTY_VALUES = "getPropertyValues";
    private static final String METHOD_SET_PROPERTY_VALUE = "setPropertyValue";
    //equals, toString, hashCode methods
    private static final String METHOD_TO_STRING = "toString";
    private static final String METHOD_DEBUG_DUMP = "debugDump";
    private static final int METHOD_DEBUG_DUMP_INDENT = 3;
    private static final String METHOD_EQUALS = "equals";
    private static final String METHOD_EQUIVALENT = "equivalent";
    private static final String METHOD_HASH_CODE = "hashCode";
    //prism container handling
    private static final String METHOD_ADD_REPLACE_EXISTING = "addReplaceExisting";
    //map which contains mapping from complex type qnames to element names
    private Map<QName, List<QName>> complexTypeToElementName;

    //todo change annotation on ObjectType in common-1.xsd to a:midPointContainer

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        try {
            StepSchemaConstants stepSchemaConstants = new StepSchemaConstants();
            stepSchemaConstants.run(outline, options, errorHandler);

            Map<String, JFieldVar> namespaceFields = stepSchemaConstants.getNamespaceFields();
            addComplextType(outline, namespaceFields);
            addContainerName(outline, namespaceFields);
            addFieldQNames(outline, namespaceFields);

            updateObjectReferenceType(outline);

            Set<JDefinedClass> containers = updateMidPointContainer(outline);
            containers.addAll(updatePropertyContainer(outline));

            updateFields(outline, containers);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Couldn't process MidPoint JAXB customisation, reason: "
                    + ex.getMessage() + ", " + ex.getClass(), ex);
        }

        return true;
    }

    private void updateObjectReferenceType(Outline outline) {
        ClassOutline objectReferenceOutline = null;
        for (Map.Entry<NClass, CClassInfo> entry : outline.getModel().beans().entrySet()) {
            QName qname = entry.getValue().getTypeName();
            if (qname == null || !OBJECT_REFERENCE_TYPE.equals(qname)) {
                continue;
            }
            objectReferenceOutline = outline.getClazz(entry.getValue());
            break;
        }

        if (objectReferenceOutline == null) {
            //object reference type class not found
            return;
        }

        JDefinedClass definedClass = objectReferenceOutline.implClass;
        //add prism reference and get/set method for it
        JVar reference = definedClass.field(JMod.PRIVATE, PrismReferenceValue.class, REFERENCE_FIELD_NAME);
        JMethod getReference = definedClass.method(JMod.PUBLIC, PrismReferenceValue.class, METHOD_GET_REFERENCE);
        JBlock body = getReference.body();
        JBlock then = body._if(reference.eq(JExpr._null()))._then();
        JClass clazz = (JClass) objectReferenceOutline.parent().getModel().codeModel._ref(PrismReferenceValue.class);
        JInvocation newReference = (JInvocation) JExpr._new(clazz);
        then.assign(reference, newReference);
        body._return(reference);

        JMethod setReference = definedClass.method(JMod.PUBLIC, void.class, METHOD_SET_REFERENCE);
        JVar value = setReference.param(PrismReferenceValue.class, "value");
        body.assign(reference, value);

        //update for oid methods
        JFieldVar oidField = definedClass.fields().get("oid");
        JMethod getOid = recreateMethod(findMethod(definedClass, "getOid"), definedClass);
        copyAnnotations(getOid, oidField);
        definedClass.removeField(oidField);
        body = getOid.body();
        body._return(JExpr.invoke(JExpr.invoke(getReference), getOid.name()));

        JMethod setOid = recreateMethod(findMethod(definedClass, "setOid"), definedClass);
        body = setOid.body();
        JInvocation invocation = body.invoke(JExpr.invoke(getReference), setOid.name());
        invocation.arg(setOid.listParams()[0]);
        //update for type methods
        JFieldVar typeField = definedClass.fields().get("type");
        JMethod getType = recreateMethod(findMethod(definedClass, "getType"), definedClass);
        copyAnnotations(getType, typeField);
        body = getType.body();
        body._return(JExpr.invoke(JExpr.invoke(getReference), "getTargetType"));

        definedClass.removeField(typeField);
        JMethod setType = recreateMethod(findMethod(definedClass, "setType"), definedClass);
        body = setType.body();
        invocation = body.invoke(JExpr.invoke(getReference), "setTargetType");
        invocation.arg(setType.listParams()[0]);
    }

    private JMethod findMethod(JDefinedClass definedClass, String methodName) {
        for (JMethod method : definedClass.methods()) {
            if (method.name().equals(methodName)) {
                return method;
            }
        }

        throw new IllegalArgumentException("Couldn't find method '" + methodName
                + "' in defined class '" + definedClass.name() + "'");
    }

    private Set<JDefinedClass> updatePropertyContainer(Outline outline) {
        return updateContainer(outline, PROPERTY_CONTAINER, PrismContainer.class);
    }

    private Set<JDefinedClass> updateMidPointContainer(Outline outline) {
        return updateContainer(outline, MIDPOINT_CONTAINER, PrismObject.class);
    }

    private Set<JDefinedClass> updateContainer(Outline outline, QName annotation,
            Class<? extends PrismContainer> containerClass) {

        Set<JDefinedClass> containers = new HashSet<JDefinedClass>();
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null || !hasAnnotation(classOutline, annotation)) {
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
            createGetContainerMethod(classOutline, container, containerClass);
            //create setContainer
            createSetContainerMethod(definedClass, container, containerClass, outline);

            System.out.println("Creating toString, equals, hashCode methods.");
            //create toString, equals, hashCode
            createToStringMethod(definedClass, outline);
            createEqualsMethod(definedClass, outline);
            createHashCodeMethod(definedClass, outline);
        }

        return containers;
    }

    private void createHashCodeMethod(JDefinedClass definedClass, Outline outline) {
        JMethod hashCode = definedClass.method(JMod.PUBLIC, int.class, METHOD_HASH_CODE);
        hashCode.annotate((JClass) outline.getModel().codeModel._ref(Override.class));
        JBlock body = hashCode.body();
        body._return(JExpr.invoke(METHOD_GET_CONTAINER).invoke(METHOD_HASH_CODE));
    }

    private void createEqualsMethod(JDefinedClass definedClass, Outline outline) {
        JClass object = (JClass) outline.getModel().codeModel._ref(Object.class);

        JMethod equals = definedClass.method(JMod.PUBLIC, boolean.class, METHOD_EQUALS);
        JVar obj = equals.param(object, "obj");
        equals.annotate((JClass) outline.getModel().codeModel._ref(Override.class));

        JBlock body = equals.body();
        JBlock ifNull = body._if(obj._instanceof(definedClass).not())._then();
        ifNull._return(JExpr.lit(false));

        JVar other = body.decl(definedClass, "other", JExpr.cast(definedClass, obj));

        JInvocation invocation = JExpr.invoke(METHOD_GET_CONTAINER).invoke(METHOD_EQUIVALENT);
        invocation.arg(other.invoke(METHOD_GET_CONTAINER));
        body._return(invocation);
    }

    private void createToStringMethod(JDefinedClass definedClass, Outline outline) {
        JClass clazz = (JClass) outline.getModel().codeModel._ref(String.class);

        JMethod toString = definedClass.method(JMod.PUBLIC, clazz, METHOD_TO_STRING);
        toString.annotate((JClass) outline.getModel().codeModel._ref(Override.class));

        JBlock body = toString.body();
        JInvocation invocation = JExpr.invoke(METHOD_GET_CONTAINER).invoke(METHOD_DEBUG_DUMP);
        invocation.arg(JExpr.lit(METHOD_DEBUG_DUMP_INDENT));
        body._return(invocation);
    }

    private void createGetContainerMethod(ClassOutline classOutline, JVar container,
            Class<? extends PrismContainer> containerClass) {
        JDefinedClass definedClass = classOutline.implClass;
        JClass clazz = (JClass) classOutline.parent().getModel().codeModel._ref(containerClass);
        JMethod getContainer = definedClass.method(JMod.PUBLIC, clazz, METHOD_GET_CONTAINER);

        //create method body
        JBlock body = getContainer.body();
        JBlock then = body._if(container.eq(JExpr._null()))._then();

        JInvocation newContainer = (JInvocation) JExpr._new(clazz);
        newContainer.arg(JExpr.invoke(METHOD_GET_CONTAINER_NAME));
        then.assign(container, newContainer);

        body._return(container);
    }

    private void createSetContainerMethod(JDefinedClass definedClass, JVar container,
            Class<? extends PrismContainer> containerClass, Outline outline) {

        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, METHOD_SET_CONTAINER);
        JVar methodContainer = setContainer.param(containerClass, "container");
        //create method body
        JBlock body = setContainer.body();
        JBlock then = body._if(methodContainer.eq(JExpr._null()))._then();
        then.assign(JExpr._this().ref(container), JExpr._null());
        then._return();

        JInvocation equals = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER_NAME), "equals");
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
    
    private QName getCClassInfoQName(CClassInfo info) {
        QName qname = info.getTypeName();
        if (qname == null) {
            qname = info.getElementName();
        }
        
        return qname;
    }

    private void addContainerName(Outline outline, Map<String, JFieldVar> namespaceFields) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null || (!hasAnnotation(classOutline, PROPERTY_CONTAINER)
                    && !hasAnnotation(classOutline, MIDPOINT_CONTAINER))) {
                continue;
            }

            //element name
            List<QName> qnames = getComplexTypeToElementName(classOutline).get(qname);
            if (qnames == null || qnames.size() != 1) {
                System.out.println("Found zero or more than one element names for type '"
                        + qname + "', " + qnames + ".");
                continue;
            }
            qname = qnames.get(0);

            JDefinedClass definedClass = classOutline.implClass;
            JMethod getContainerName = definedClass.method(JMod.NONE, QName.class, METHOD_GET_CONTAINER_NAME);
            JBlock body = getContainerName.body();

            JFieldVar var = namespaceFields.get(qname.getNamespaceURI());
            JClass clazz = (JClass) outline.getModel().codeModel._ref(QName.class);
            JInvocation invocation = (JInvocation) JExpr._new(clazz);
            if (var != null) {
                JClass schemaClass = (JClass) outline.getModel().codeModel._getClass(StepSchemaConstants.CLASS_NAME);
                invocation.arg(schemaClass.staticRef(var));
                invocation.arg(qname.getLocalPart());
            } else {
                invocation.arg(qname.getNamespaceURI());
                invocation.arg(qname.getLocalPart());

            }
            body._return(invocation);
        }
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

    private Map<QName, List<QName>> getComplexTypeToElementName(ClassOutline classOutline) {
        if (complexTypeToElementName != null) {
            return complexTypeToElementName;
        } else {
            complexTypeToElementName = new HashMap<QName, List<QName>>();
        }

        XSSchemaSet schemaSet = classOutline.target.getSchemaComponent().getRoot();
        for (XSSchema schema : schemaSet.getSchemas()) {
            Map<String, XSElementDecl> elemDecls = schema.getElementDecls();
            for (Entry<String, XSElementDecl> entry : elemDecls.entrySet()) {
                XSElementDecl decl = entry.getValue();
                XSType xsType = decl.getType();

                if (xsType.getName() == null) {
                    continue;
                }
                QName type = new QName(xsType.getTargetNamespace(), xsType.getName());

                List<QName> qnames = complexTypeToElementName.get(type);
                if (qnames == null) {
                    qnames = new ArrayList<QName>();
                    complexTypeToElementName.put(type, qnames);
                }
                qnames.add(new QName(decl.getTargetNamespace(), decl.getName()));
            }
        }

        return complexTypeToElementName;
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

            if (fields == null || !isPropertyContainer(classOutline.implClass, outline)) {
                //it's PropertyContainer, MidPointObject class or doesn't have fields
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
        body._return(JExpr.invoke(METHOD_GET_CONTAINER).invoke("getOid"));
        copyAnnotations(method, field, oldMethod);

        method = definedClass.getMethod("setOid", new JType[]{string});
        method = recreateMethod(method, definedClass);
        body = method.body();
        JInvocation invocation = body.invoke(JExpr.invoke(METHOD_GET_CONTAINER), method.name());
        invocation.arg(method.listParams()[0]);

        return true;
    }

//    PrismContainer container = getContainer().findContainer(F_EXTENSION);
//    if (container != null) {
//        getContainer().removeContainer(container);
//    }
//
//    if (value != null) {
//        getContainer().addContainer(value.getContainer());
//    }
    private boolean updateContainerFieldType(JFieldVar field, ClassOutline classOutline) {
        //getter method update
        JDefinedClass definedClass = classOutline.implClass;
        String methodName = getGetterMethod(classOutline, field);
        JMethod method = definedClass.getMethod(methodName, new JType[]{});
        JMethod getMethod = recreateMethod(method, definedClass);
        copyAnnotations(getMethod, field);

        JBlock body = getMethod.body();
        JClass clazz = (JClass) classOutline.parent().getModel().codeModel._ref(PrismContainer.class);
        JInvocation invocation = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "findContainer");
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        JVar container = body.decl(clazz, CONTAINER_FIELD_NAME, invocation);
        JBlock then = body._if(container.eq(JExpr._null()))._then();
        then._return(JExpr._null());
        JVar wrapper = body.decl(field.type(), field.name(), JExpr._new(field.type()));
        invocation = body.invoke(wrapper, METHOD_SET_CONTAINER);
        invocation.arg(container);
        body._return(wrapper);

        //setter method update
        methodName = getSetterMethod(classOutline, field);
        method = definedClass.getMethod(methodName, new JType[]{field.type()});
        method = recreateMethod(method, definedClass);
        JVar param = method.listParams()[0];
        body = method.body();
        
        invocation = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "findContainer");
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        container = body.decl(clazz, CONTAINER_FIELD_NAME, invocation);
        then = body._if(container.eq(JExpr._null()).not())._then();
        invocation = then.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "removeContainer");
        invocation.arg(container);

        then = body._if(param.eq(JExpr._null()).not())._then();
        invocation = then.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "addContainer");
        invocation.arg(param.invoke(METHOD_GET_CONTAINER));

        return true;
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

        JClass prismUtil = (JClass) classOutline.parent().getModel().codeModel._ref(PrismForJAXBUtil.class);
        JInvocation invocation = body.staticInvoke(prismUtil, METHOD_SET_PROPERTY_VALUE);
        //push arguments
        invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(method.listParams()[0]);
    }

    private void createFieldGetterBody(JMethod method, JFieldVar field, ClassOutline classOutline, boolean isList) {
        JBlock body = method.body();

        JClass prismUtil = (JClass) classOutline.parent().getModel().codeModel._ref(PrismForJAXBUtil.class);

        JInvocation invocation;
        if (isList) {
            invocation = prismUtil.staticInvoke(METHOD_GET_PROPERTY_VALUES);
        } else {
            invocation = prismUtil.staticInvoke(METHOD_GET_PROPERTY_VALUE);
        }
        //push arguments
        invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
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
