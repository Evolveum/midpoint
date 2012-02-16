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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.evolveum.midpoint.schema.xjc.PrismForJAXBUtil;
import com.evolveum.midpoint.schema.xjc.PrismReferenceArrayList;
import com.evolveum.midpoint.schema.xjc.Processor;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDeclaration;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIXPluginCustomization;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSType;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
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
    private static final QName A_OBJECT_REFERENCE = new QName(PrefixMapper.A.getNamespace(), "objectReference");
    private static final String REFERENCE_FIELD_NAME = "reference";
    private static final String METHOD_GET_REFERENCE = "getReference";
    private static final String METHOD_SET_REFERENCE = "setReference";
    //annotations for schema processor
    private static final QName PROPERTY_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "container");
    private static final QName MIDPOINT_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "object");
    //fields and methods for prism containers/prism objects
    private static final String COMPLEX_TYPE_FIELD = "COMPLEX_TYPE";
    private static final String CONTAINER_FIELD_NAME = "container";
    private static final String METHOD_GET_CONTAINER = "getContainer";
    private static final String METHOD_SET_CONTAINER = "setContainer";
    private static final String METHOD_GET_CONTAINER_NAME = "getContainerName";
    //methods in PrismForJAXBUtil
    private static final String METHOD_PRISM_GET_PROPERTY_VALUE = "getPropertyValue";
    private static final String METHOD_PRISM_GET_PROPERTY_VALUES = "getPropertyValues";
    private static final String METHOD_PRISM_SET_PROPERTY_VALUE = "setPropertyValue";
    private static final String METHOD_PRISM_GET_CONTAINER = "getContainer";
    private static final String METHOD_PRISM_GET_CONTAINER_VALUE = "getContainerValue";
    private static final String METHOD_PRISM_SET_CONTAINER_VALUE = "setContainerValue";
    private static final String METHOD_PRISM_GET_REFERENCE_VALUE = "getReferenceValue";
    private static final String METHOD_PRISM_SET_REFERENCE_VALUE = "setReferenceValue";
    private static final String METHOD_PRISM_SET_REFERENCE_OBJECT = "setReferenceObject";
    //equals, toString, hashCode methods
    private static final String METHOD_TO_STRING = "toString";
    private static final String METHOD_DEBUG_DUMP = "debugDump";
    private static final int METHOD_DEBUG_DUMP_INDENT = 3;
    private static final String METHOD_EQUALS = "equals";
    private static final String METHOD_EQUIVALENT = "equivalent";
    private static final String METHOD_HASH_CODE = "hashCode";
    //referenced class map
    private static final Map<Class, JClass> CLASS_MAP = new HashMap<Class, JClass>() {

        @Override
        public JClass get(Object o) {
            JClass clazz = super.get(o);
            Validate.notNull(clazz, "Class '" + o + "' not registered.");
            return clazz;
        }
    };

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        try {
            createClassMap(CLASS_MAP, outline, PrismReferenceValue.class, PrismReference.class, PrismObject.class,
                    String.class, Object.class, XmlTransient.class, Override.class, IllegalArgumentException.class,
                    QName.class, PrismForJAXBUtil.class, PrismReferenceArrayList.class, PrismContainerValue.class,
                    List.class, Objectable.class, StringBuilder.class);

            StepSchemaConstants stepSchemaConstants = new StepSchemaConstants();
            stepSchemaConstants.run(outline, options, errorHandler);

            Map<String, JFieldVar> namespaceFields = stepSchemaConstants.getNamespaceFields();
            addComplextType(outline, namespaceFields);
            addContainerName(outline, namespaceFields);
            addFieldQNames(outline, namespaceFields);

            updateMidPointContainer(outline);
            updatePropertyContainer(outline);
            updateFields(outline);

            updateObjectReferenceType(outline);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Couldn't process MidPoint JAXB customisation, reason: "
                    + ex.getMessage() + ", " + ex.getClass(), ex);
        }

        return true;
    }

    private void createClassMap(Map<Class, JClass> classMap, Outline outline, Class... classes) {
        for (Class clazz : classes) {
            classMap.put(clazz, (JClass) outline.getModel().codeModel._ref(clazz));
        }
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
        JInvocation newReference = JExpr._new(CLASS_MAP.get(PrismReferenceValue.class));
        then.assign(reference, newReference);
        body._return(reference);

        JMethod setReference = definedClass.method(JMod.PUBLIC, void.class, METHOD_SET_REFERENCE);
        JVar value = setReference.param(PrismReferenceValue.class, "value");
        body = setReference.body();
        body.assign(reference, value);

        //update for oid methods
        updateObjectReferenceOid(definedClass, getReference);
        //update for type methods
        updateObjectReferenceType(definedClass, getReference);
    }

    private void updateObjectReferenceType(JDefinedClass definedClass, JMethod getReference) {
        JFieldVar typeField = definedClass.fields().get("type");
        JMethod getType = recreateMethod(findMethod(definedClass, "getType"), definedClass);
        copyAnnotations(getType, typeField);
        JBlock body = getType.body();
        body._return(JExpr.invoke(JExpr.invoke(getReference), "getTargetType"));

        definedClass.removeField(typeField);
        JMethod setType = recreateMethod(findMethod(definedClass, "setType"), definedClass);
        body = setType.body();
        JInvocation invocation = body.invoke(JExpr.invoke(getReference), "setTargetType");
        invocation.arg(setType.listParams()[0]);
    }

    private void updateObjectReferenceOid(JDefinedClass definedClass, JMethod getReference) {
        JFieldVar oidField = definedClass.fields().get("oid");
        JMethod getOid = recreateMethod(findMethod(definedClass, "getOid"), definedClass);
        copyAnnotations(getOid, oidField);
        definedClass.removeField(oidField);
        JBlock body = getOid.body();
        body._return(JExpr.invoke(JExpr.invoke(getReference), getOid.name()));

        JMethod setOid = recreateMethod(findMethod(definedClass, "setOid"), definedClass);
        body = setOid.body();
        JInvocation invocation = body.invoke(JExpr.invoke(getReference), setOid.name());
        invocation.arg(setOid.listParams()[0]);
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
        Set<JDefinedClass> containers = new HashSet<JDefinedClass>();
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null || !hasAnnotation(classOutline, PROPERTY_CONTAINER)) {
                continue;
            }

            //todo remove, only till propertyContainer annotation is on ObjectType
            if (hasAnnotation(classOutline, MIDPOINT_CONTAINER) && hasAnnotation(classOutline, PROPERTY_CONTAINER)) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;
            containers.add(definedClass);

            //inserting MidPointObject field into ObjectType class
            JVar container = definedClass.field(JMod.PRIVATE, PrismContainerValue.class, CONTAINER_FIELD_NAME);
            //adding XmlTransient annotation
            container.annotate(CLASS_MAP.get(XmlTransient.class));

            //create getContainer
            createGetContainerValueMethod(classOutline, container);
            //create setContainer
            createSetContainerValueMethod(definedClass, container);

            System.out.println("Creating toString, equals, hashCode methods.");
            //create toString, equals, hashCode
            createToStringMethod(definedClass);
            createEqualsMethod(definedClass);
            createHashCodeMethod(definedClass);
        }

        return containers;
    }

    private Set<JDefinedClass> updateMidPointContainer(Outline outline) {
        Set<JDefinedClass> containers = new HashSet<JDefinedClass>();
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null || !hasAnnotation(classOutline, MIDPOINT_CONTAINER)) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;
            definedClass._implements(CLASS_MAP.get(Objectable.class));
            containers.add(definedClass);

            //inserting PrismObject field into ObjectType class
            JVar container = definedClass.field(JMod.PRIVATE, PrismObject.class, CONTAINER_FIELD_NAME);
            //adding XmlTransient annotation
            container.annotate(CLASS_MAP.get(XmlTransient.class));

            //create getContainer
            createGetContainerMethod(classOutline, container);
            //create setContainer
            createSetContainerMethod(definedClass, container);

            System.out.println("Creating toString, equals, hashCode methods.");
            //create toString, equals, hashCode
            createToStringMethod(definedClass);
            createEqualsMethod(definedClass);
            createHashCodeMethod(definedClass);
            //create toDebugName, toDebugType
            createToDebugName(definedClass);
            createToDebugType(definedClass);
        }

        return containers;
    }

    private void createToDebugName(JDefinedClass definedClass) {
        JMethod method = definedClass.method(JMod.PUBLIC, String.class, "toDebugName");
        method.annotate(CLASS_MAP.get(Override.class));
        JBlock body = method.body();
        JVar builder = body.decl(CLASS_MAP.get(StringBuilder.class), "builder",
                JExpr._new(CLASS_MAP.get(StringBuilder.class)));

        invokeAppendOnBuilder(body, builder, JExpr.dotclass(definedClass).invoke("getSimpleName"));
        invokeAppendOnBuilder(body, builder, JExpr.lit("["));
        invokeAppendOnBuilder(body, builder, JExpr.invoke("getOid"));
        invokeAppendOnBuilder(body, builder, JExpr.lit(", "));
        invokeAppendOnBuilder(body, builder, JExpr.invoke("getName"));
        invokeAppendOnBuilder(body, builder, JExpr.lit("]"));
        body._return(JExpr.invoke(builder, "toString"));
    }

    private void createToDebugType(JDefinedClass definedClass) {
        JMethod method = definedClass.method(JMod.PUBLIC, String.class, "toDebugType");
        method.annotate(CLASS_MAP.get(Override.class));
        JBlock body = method.body();
        JVar builder = body.decl(CLASS_MAP.get(StringBuilder.class), "builder",
                JExpr._new(CLASS_MAP.get(StringBuilder.class)));

        invokeAppendOnBuilder(body, builder, JExpr.dotclass(definedClass).invoke("getSimpleName"));

        body._return(JExpr.invoke(builder, "toString"));
    }

    private void invokeAppendOnBuilder(JBlock body, JVar builder, JExpression expression) {
        JInvocation invocation = body.invoke(builder, "append");
        invocation.arg(expression);
    }

    private void createHashCodeMethod(JDefinedClass definedClass) {
        JMethod hashCode = definedClass.method(JMod.PUBLIC, int.class, METHOD_HASH_CODE);
        hashCode.annotate(CLASS_MAP.get(Override.class));
        JBlock body = hashCode.body();
        body._return(JExpr.invoke(METHOD_GET_CONTAINER).invoke(METHOD_HASH_CODE));
    }

    private void createEqualsMethod(JDefinedClass definedClass) {
        JMethod equals = definedClass.method(JMod.PUBLIC, boolean.class, METHOD_EQUALS);
        JVar obj = equals.param(CLASS_MAP.get(Object.class), "obj");
        equals.annotate(CLASS_MAP.get(Override.class));

        JBlock body = equals.body();
        JBlock ifNull = body._if(obj._instanceof(definedClass).not())._then();
        ifNull._return(JExpr.lit(false));

        JVar other = body.decl(definedClass, "other", JExpr.cast(definedClass, obj));

        JInvocation invocation = JExpr.invoke(METHOD_GET_CONTAINER).invoke(METHOD_EQUIVALENT);
        invocation.arg(other.invoke(METHOD_GET_CONTAINER));
        body._return(invocation);
    }

    private void createToStringMethod(JDefinedClass definedClass) {
        JMethod toString = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(String.class), METHOD_TO_STRING);
        toString.annotate(CLASS_MAP.get(Override.class));

        JBlock body = toString.body();
        JInvocation invocation = JExpr.invoke(METHOD_GET_CONTAINER).invoke(METHOD_DEBUG_DUMP);
        invocation.arg(JExpr.lit(METHOD_DEBUG_DUMP_INDENT));
        body._return(invocation);
    }

    private void createGetContainerValueMethod(ClassOutline classOutline, JVar container) {
        JDefinedClass definedClass = classOutline.implClass;
        JMethod getContainer = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(PrismContainerValue.class),
                METHOD_GET_CONTAINER);

        //create method body
        JBlock body = getContainer.body();
        JBlock then = body._if(container.eq(JExpr._null()))._then();
        then.assign(container, JExpr._new(CLASS_MAP.get(PrismContainerValue.class)));

        body._return(container);
    }

    private void createSetContainerValueMethod(JDefinedClass definedClass, JVar container) {
        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, METHOD_SET_CONTAINER);
        JVar methodContainer = setContainer.param(PrismContainerValue.class, "container");
        //create method body
        JBlock body = setContainer.body();
        JBlock then = body._if(methodContainer.eq(JExpr._null()))._then();
        then.assign(JExpr._this().ref(container), JExpr._null());
        then._return();

        body.assign(JExpr._this().ref(container), methodContainer);
    }

    private void createGetContainerMethod(ClassOutline classOutline, JVar container) {
        JDefinedClass definedClass = classOutline.implClass;
        JMethod getContainer = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(PrismObject.class),
                METHOD_GET_CONTAINER);

        //create method body
        JBlock body = getContainer.body();
        JBlock then = body._if(container.eq(JExpr._null()))._then();

        JInvocation newContainer = JExpr._new(CLASS_MAP.get(PrismObject.class));
        newContainer.arg(JExpr.invoke(METHOD_GET_CONTAINER_NAME));
        newContainer.arg(JExpr.dotclass(definedClass));
        then.assign(container, newContainer);

        body._return(container);
    }

    private void createSetContainerMethod(JDefinedClass definedClass, JVar container) {
        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, METHOD_SET_CONTAINER);
        JVar methodContainer = setContainer.param(PrismObject.class, "container");
        //create method body
        JBlock body = setContainer.body();
        JBlock then = body._if(methodContainer.eq(JExpr._null()))._then();
        then.assign(JExpr._this().ref(container), JExpr._null());
        then._return();

        JInvocation equals = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER_NAME), "equals");
        equals.arg(methodContainer.invoke("getName"));

        then = body._if(equals.not())._then();
        JInvocation exception = JExpr._new(CLASS_MAP.get(IllegalArgumentException.class));

        JExpression message = JExpr.lit("Container qname '").plus(JExpr.invoke(methodContainer, "getName"))
                .plus(JExpr.lit("' doesn't equals to '")).plus(JExpr.invoke(METHOD_GET_CONTAINER_NAME))
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
        Map<QName, List<QName>> complexTypeToElementName = null;

        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            CClassInfo classInfo = entry.getValue();
            ClassOutline classOutline = outline.getClazz(classInfo);
            if (complexTypeToElementName == null) {
                complexTypeToElementName = getComplexTypeToElementName(classOutline);
            }

            QName qname = getCClassInfoQName(classInfo);
            if (qname == null || !hasParentAnnotation(classOutline, MIDPOINT_CONTAINER)) {
                continue;
            }

            //element name
            List<QName> qnames = complexTypeToElementName.get(qname);
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
            JInvocation invocation = JExpr._new(CLASS_MAP.get(QName.class));
            if (var != null) {
                JClass schemaClass = outline.getModel().codeModel._getClass(StepSchemaConstants.CLASS_NAME);
                invocation.arg(schemaClass.staticRef(var));
                invocation.arg(qname.getLocalPart());
            } else {
                invocation.arg(qname.getNamespaceURI());
                invocation.arg(qname.getLocalPart());

            }
            body._return(invocation);
        }
    }

    private boolean hasParentAnnotation(ClassOutline classOutline, QName annotation) {
        if (classOutline.getSuperClass() == null) {
            return hasAnnotation(classOutline, annotation);
        }

        return hasAnnotation(classOutline, annotation) || hasParentAnnotation(classOutline.getSuperClass(), annotation);
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
        Map<QName, List<QName>> complexTypeToElementName = new HashMap<QName, List<QName>>();

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
        JClass schemaClass = outline.getModel().codeModel._getClass(StepSchemaConstants.CLASS_NAME);

        JInvocation invocation = JExpr._new(CLASS_MAP.get(QName.class));
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
                boxes.add(new FieldBox<QName>(fieldName, new QName(qname.getNamespaceURI(), field)));
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

    private void updateFields(Outline outline) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (qname == null) {
                continue;
            }

            JDefinedClass implClass = classOutline.implClass;
            Map<String, JFieldVar> fields = implClass.fields();

            if (fields == null || !isContainer(classOutline.implClass, outline)) {
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
                } else if ("id".equals(field)) {
                    System.out.println("Updating container id field: " + fieldVar.name());
                    remove = updateIdField(fieldVar, classOutline);
                } else if (isFieldReference(fieldVar, classOutline)) {
                    System.out.println("Updating field (reference): " + fieldVar.name());
                    remove = updateFieldReference(fieldVar, classOutline);
                } else if (isFieldReferenceUse(fieldVar, classOutline)) {
                    System.out.println("Updating field (reference usage): " + fieldVar.name());
                    remove = updateFieldReferenceUse(fieldVar, classOutline);
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

    private boolean updateIdField(JFieldVar field, ClassOutline classOutline) {
        JMethod method = recreateGetter(field, classOutline);
        JBlock body = method.body();
        body._return(JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "getId"));

        method = recreateSetter(field, classOutline);
        body = method.body();
        JInvocation invocation = body.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "setId");
        invocation.arg(method.listParams()[0]);

        return true;
    }

    private JMethod recreateSetter(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass definedClass = classOutline.implClass;
        String methodName = getSetterMethod(classOutline, field);
        JMethod method = definedClass.getMethod(methodName, new JType[]{field.type()});
        return recreateMethod(method, definedClass);
    }

    private JMethod recreateGetter(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass definedClass = classOutline.implClass;
        String methodName = getGetterMethod(classOutline, field);
        JMethod method = definedClass.getMethod(methodName, new JType[]{});
        JMethod getMethod = recreateMethod(method, definedClass);
        copyAnnotations(getMethod, field);

        return getMethod;
    }

    private boolean updateFieldReference(JFieldVar field, ClassOutline classOutline) {
        JMethod method = recreateGetter(field, classOutline);

        boolean isList = isList(field.type());
        createFieldReferenceGetterBody(field, classOutline, method.body(), isList);

        //setter method update
        if (isList) {
            return false;
        }

        method = recreateSetter(field, classOutline);
        JVar param = method.listParams()[0];
        createFieldReferenceSetterBody(field, param, method.body());

        return true;
    }

    private void createFieldReferenceSetterBody(JFieldVar field, JVar param, JBlock body) {
        JVar cont = body.decl(CLASS_MAP.get(PrismReferenceValue.class), REFERENCE_FIELD_NAME,
                JOp.cond(param.ne(JExpr._null()), JExpr.invoke(param, METHOD_GET_REFERENCE), JExpr._null()));
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_SET_REFERENCE_VALUE);
        invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(cont);
    }

    // todo reimplement, now we're using inner classes
    // JDefinedClass annonymous = outline.getCodeModel().anonymousClass(clazz);
    // annonymous.hide();
    private JDefinedClass createFieldReferenceGetterListAnon(JFieldVar field, ClassOutline classOutline) {
        //add generics type to list field.type.getTypeParameters()...
        JClass type = ((JClass) field.type()).getTypeParameters().get(0);
        JClass clazz = CLASS_MAP.get(PrismReferenceArrayList.class).narrow(type);

        JDefinedClass anonymous;
        try {
            CPropertyInfo propertyInfo = classOutline.target.getProperty(field.name());
            anonymous = classOutline.implClass._class(JMod.PRIVATE | JMod.STATIC, "Anon" + propertyInfo.getName(true));
            JDocComment comment = anonymous.javadoc();
            comment.append("todo can't be anonymous because of NPE bug in CodeModel generator, will be fixed later.");
        } catch (JClassAlreadyExistsException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        anonymous._extends(clazz);
        JMethod constructor = anonymous.constructor(JMod.PUBLIC);
        constructor.param(CLASS_MAP.get(PrismReference.class), REFERENCE_FIELD_NAME);
        JBlock constructorBody = constructor.body();
        JInvocation invocation = constructorBody.invoke("super");
        invocation.arg(constructor.listParams()[0]);

        JMethod createItem = anonymous.method(JMod.PUBLIC, type, "createItem");
        createItem.annotate(CLASS_MAP.get(Override.class));
        createItem.param(CLASS_MAP.get(PrismReferenceValue.class), "value");

        return anonymous;
    }

    private void createFieldReferenceCreateItemBody(JFieldVar field, JMethod method) {
        JClass type = ((JClass) field.type()).getTypeParameters().get(0);

        JBlock body = method.body();
        JVar decl = body.decl(type, field.name(), JExpr._new(type));
        JInvocation invocation = body.invoke(decl, METHOD_SET_REFERENCE);
        invocation.arg(method.listParams()[0]);
        body._return(decl);
    }

    private void createFieldReferenceGetterBody(JFieldVar field, ClassOutline classOutline, JBlock body,
            boolean isList) {
        JFieldRef qnameRef = JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name()));
        if (isList) {
            //if it's List<ObjectReferenceType> ...
            JInvocation invoke = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "findOrCreateReference");
            invoke.arg(qnameRef);
            JVar ref = body.decl(CLASS_MAP.get(PrismReference.class), REFERENCE_FIELD_NAME, invoke);

            JDefinedClass anonymous = createFieldReferenceGetterListAnon(field, classOutline);
            createFieldReferenceCreateItemBody(field, findMethod(anonymous, "createItem"));
            JInvocation newList = JExpr._new(anonymous);
            newList.arg(ref);
            body._return(newList);
        } else {
            //if it's ObjectReferenceType
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_REFERENCE_VALUE);
            invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
            invocation.arg(qnameRef);

            JVar container = body.decl(CLASS_MAP.get(PrismReferenceValue.class), REFERENCE_FIELD_NAME, invocation);

            JBlock then = body._if(container.eq(JExpr._null()))._then();
            then._return(JExpr._null());
            JVar wrapper = body.decl(field.type(), field.name(), JExpr._new(field.type()));
            invocation = body.invoke(wrapper, METHOD_SET_REFERENCE);
            invocation.arg(container);
            body._return(wrapper);
        }
    }

    private JFieldVar getReferencedField(JFieldVar field, ClassOutline classOutline) {
        QName qname = getFieldReferenceUseAnnotationQName(field, classOutline);
        CPropertyInfo propertyInfo = classOutline.target.getProperty(qname.getLocalPart());
        return classOutline.implClass.fields().get(propertyInfo.getName(false));
    }

    private boolean updateFieldReferenceUse(JFieldVar field, ClassOutline classOutline) {
        //getter method update
        JMethod method = recreateGetter(field, classOutline);
        boolean isList = isList(field.type());
        createFieldReferenceUseGetterBody(field, classOutline, method.body(), isList);

        //setter method update
        if (isList) {
            return true;
        }
        method = recreateSetter(field, classOutline);
        createFieldReferenceUseSetterBody(field, classOutline, method.listParams()[0], method.body());

        return true;
    }

    private void createFieldReferenceUseSetterBody(JFieldVar field, ClassOutline classOutline, JVar param,
            JBlock body) {
        JVar cont = body.decl(CLASS_MAP.get(PrismObject.class), CONTAINER_FIELD_NAME, JOp.cond(param.ne(JExpr._null()),
                JExpr.invoke(param, METHOD_GET_CONTAINER), JExpr._null()));
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_SET_REFERENCE_OBJECT);
        invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));

        JFieldVar referencedField = getReferencedField(field, classOutline);
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(referencedField.name())));
        invocation.arg(cont);
    }

    private void createFieldReferenceUseCreateItemBody(JFieldVar field, JMethod method) {
        JClass type = ((JClass) field.type()).getTypeParameters().get(0);

        JBlock body = method.body();
        JVar decl = body.decl(type, field.name(), JExpr._new(type));
        JInvocation invocation = body.invoke(decl, METHOD_SET_CONTAINER);
        invocation.arg(JExpr.cast(CLASS_MAP.get(PrismObject.class), JExpr.invoke(method.listParams()[0], "getObject")));
        body._return(decl);
    }

    private void createFieldReferenceUseGetterBody(JFieldVar field, ClassOutline classOutline, JBlock body,
            boolean isList) {
        JFieldRef qnameRef = JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name()));

        if (isList) {
            JInvocation invoke = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER), "findOrCreateReference");
            invoke.arg(qnameRef);
            JVar ref = body.decl(CLASS_MAP.get(PrismReference.class), REFERENCE_FIELD_NAME, invoke);

            JDefinedClass anonymous = createFieldReferenceGetterListAnon(field, classOutline);
            createFieldReferenceUseCreateItemBody(field, findMethod(anonymous, "createItem"));

            JInvocation newList = JExpr._new(anonymous);
            newList.arg(ref);
            body._return(newList);
        } else {
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_REFERENCE_VALUE);
            invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
            invocation.arg(qnameRef);

            JVar reference = body.decl(CLASS_MAP.get(PrismReferenceValue.class), REFERENCE_FIELD_NAME, invocation);            

            JBlock then = body._if(reference.eq(JExpr._null()).cor(JExpr.invoke(reference, "getObject").eq(JExpr._null())))._then();
            then._return(JExpr._null());
            JVar wrapper = body.decl(field.type(), field.name(), JExpr._new(field.type()));
            invocation = body.invoke(wrapper, METHOD_SET_CONTAINER);
            invocation.arg(JExpr.cast(CLASS_MAP.get(PrismObject.class), JExpr.invoke(reference, "getObject")));
            body._return(wrapper);
        }
    }

    private boolean isFieldReference(JFieldVar field, ClassOutline classOutline) {
        CPropertyInfo propertyInfo = classOutline.target.getProperty(field.name());
        Collection<? extends CTypeInfo> collection = propertyInfo.ref();
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        CTypeInfo info = collection.iterator().next();
        if (info instanceof CClassInfo) {
            CClassInfo classInfo = (CClassInfo) info;
            if (OBJECT_REFERENCE_TYPE.equals(classInfo.getTypeName())) {
                return true;
            }
        }

        return false;
    }

    private QName getFieldReferenceUseAnnotationQName(JFieldVar field, ClassOutline classOutline) {
        BIDeclaration declaration = hasAnnotation(classOutline, field, A_OBJECT_REFERENCE);
        if (!(declaration instanceof BIXPluginCustomization)) {
            return null;
        }

        BIXPluginCustomization customization = (BIXPluginCustomization) declaration;
        if (customization.element == null) {
            return null;
        }

        Element element = customization.element;
        String strQName = element.getTextContent();
        String[] array = strQName.split(":");
        if (array.length == 2) {
            return new QName(PrefixMapper.C.getNamespace(), array[1]);
        } else if (array.length == 1) {
            return new QName(PrefixMapper.C.getNamespace(), array[0]);
        }

        return null;
    }

    private boolean isFieldReferenceUse(JFieldVar field, ClassOutline classOutline) {
        return getFieldReferenceUseAnnotationQName(field, classOutline) != null;
    }

    private ClassOutline findClassOutline(JDefinedClass definedClass, Outline outline) {
        if (definedClass == null) {
            return null;
        }

        ClassOutline classOutline = null;
        for (ClassOutline clazz : outline.getClasses()) {
            if (definedClass.equals(clazz.implClass)) {
                classOutline = clazz;
                break;
            }
        }

        return classOutline;
    }

    private boolean isContainer(JDefinedClass definedClass, Outline outline) {
        ClassOutline classOutline = findClassOutline(definedClass, outline);
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

        return isContainer((JDefinedClass) definedClass._extends(), outline);
    }

    private boolean isFieldTypeContainer(JFieldVar field, ClassOutline classOutline) {
        Outline outline = classOutline.parent();

        JType type = field.type();
        if (type instanceof JDefinedClass) {
            return isContainer((JDefinedClass) type, outline);
        }

        return false;
    }

    private boolean updateOidField(JFieldVar field, ClassOutline classOutline) {
        //getter method update
        JMethod method = recreateGetter(field, classOutline);
        JBlock body = method.body();
        body._return(JExpr.invoke(METHOD_GET_CONTAINER).invoke("getOid"));
        //setter method update
        method = recreateSetter(field, classOutline);
        body = method.body();
        JInvocation invocation = body.invoke(JExpr.invoke(METHOD_GET_CONTAINER), method.name());
        invocation.arg(method.listParams()[0]);

        return true;
    }

    private boolean updateContainerFieldType(JFieldVar field, ClassOutline classOutline) {
        //getter method update
        JMethod method = recreateGetter(field, classOutline);
        createContainerFieldGetterBody(field, classOutline, method);

        //setter method update
        method = recreateSetter(field, classOutline);
        createContainerFieldSetterBody(field, classOutline, method);

        return true;
    }

    private void createContainerFieldSetterBody(JFieldVar field, ClassOutline classOutline, JMethod method) {
        JVar param = method.listParams()[0];
        JBlock body = method.body();

        JVar cont;
        if (isPrismContainer(param.type(), classOutline.parent())) {
            cont = body.decl(CLASS_MAP.get(PrismObject.class), CONTAINER_FIELD_NAME, JOp.cond(param.ne(JExpr._null()),
                    JExpr.invoke(param, METHOD_GET_CONTAINER), JExpr._null()));
        } else {
            cont = body.decl(CLASS_MAP.get(PrismContainerValue.class), CONTAINER_FIELD_NAME,
                    JOp.cond(param.ne(JExpr._null()), JExpr.invoke(param, METHOD_GET_CONTAINER), JExpr._null()));
        }
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_SET_CONTAINER_VALUE);
        invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(cont);
    }

    private void createContainerFieldGetterBody(JFieldVar field, ClassOutline classOutline, JMethod method) {
        JBlock body = method.body();
        JVar container;
        if (isPrismContainer(method.type(), classOutline.parent())) {
            //handle PrismObject
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_CONTAINER);
            invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
            invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
            invocation.arg(JExpr.dotclass(CLASS_MAP.get(PrismObject.class)));

            container = body.decl(CLASS_MAP.get(PrismObject.class), CONTAINER_FIELD_NAME, invocation);
        } else {
            //handle PrismContainerValue
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_CONTAINER_VALUE);
            invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
            invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));

            container = body.decl(CLASS_MAP.get(PrismContainerValue.class), CONTAINER_FIELD_NAME, invocation);
        }
        JBlock then = body._if(container.eq(JExpr._null()))._then();
        then._return(JExpr._null());
        JVar wrapper = body.decl(field.type(), field.name(), JExpr._new(field.type()));
        JInvocation invocation = body.invoke(wrapper, METHOD_SET_CONTAINER);
        invocation.arg(container);
        body._return(wrapper);
    }

    private boolean isPrismContainer(JType type, Outline outline) {
        if (!(type instanceof JDefinedClass)) {
            return false;
        }

        ClassOutline classOutline = findClassOutline((JDefinedClass) type, outline);
        if (classOutline == null) {
            return false;
        }

        return hasParentAnnotation(classOutline, MIDPOINT_CONTAINER);
    }

    private boolean isList(JType type) {
        boolean isList = false;
        if (type instanceof JClass) {
            isList = CLASS_MAP.get(List.class).equals(((JClass) type).erasure());
        }

        return isList;
    }

    private boolean updateField(JFieldVar field, ClassOutline classOutline) {
        //update getter
        JMethod method = recreateGetter(field, classOutline);
        boolean isList = isList(field.type());
        createFieldGetterBody(method, field, isList);

        //update setter
        if (isList) {
            //setter for list field members was not created
            return true;
        }

        method = recreateSetter(field, classOutline);
        createFieldSetterBody(method, field);

        return true;
    }

    private void createFieldSetterBody(JMethod method, JFieldVar field) {
        JBlock body = method.body();
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_SET_PROPERTY_VALUE);
        //push arguments
        invocation.arg(JExpr.invoke(METHOD_GET_CONTAINER));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(method.listParams()[0]);
    }

    private void createFieldGetterBody(JMethod method, JFieldVar field, boolean isList) {
        JBlock body = method.body();
        JInvocation invocation;
        if (isList) {
            invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_PROPERTY_VALUES);
        } else {
            invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_PROPERTY_VALUE);
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
