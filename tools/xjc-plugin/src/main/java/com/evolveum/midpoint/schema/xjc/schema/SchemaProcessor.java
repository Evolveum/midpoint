/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.xjc.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.*;
import com.evolveum.midpoint.prism.impl.xjc.PrismContainerArrayList;
import com.evolveum.midpoint.prism.impl.xjc.PrismForJAXBUtil;
import com.evolveum.midpoint.prism.impl.xjc.PrismReferenceArrayList;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.evolveum.midpoint.schema.xjc.Processor;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDeclaration;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIXPluginCustomization;
import com.sun.xml.xsom.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static com.evolveum.midpoint.schema.xjc.util.ProcessorUtils.*;

/**
 * Custom XJC plugin used to update JAXB classes implementation and use Prism stuff as
 * internal data representation.
 *
 * @author lazyman
 */
public class SchemaProcessor implements Processor {

    private static final boolean PRINT_DEBUG_INFO = false;

    //qname for object reference type
    private static final QName OBJECT_REFERENCE_TYPE = new QName(PrefixMapper.C.getNamespace(), "ObjectReferenceType");
    public static final QName A_OBJECT_REFERENCE = new QName(PrefixMapper.A.getNamespace(), "objectReference");

    //annotations for schema processor
    public static final QName A_PRISM_CONTAINER = new QName(PrefixMapper.A.getNamespace(), "container");
    public static final QName A_PRISM_OBJECT = new QName(PrefixMapper.A.getNamespace(), "object");
    public static final QName A_RAW_TYPE = new QName(PrefixMapper.A.getNamespace(), "rawType");

    //Public fields
    private static final String COMPLEX_TYPE_FIELD_NAME = "COMPLEX_TYPE";

    // Public generated methods
    // The "as" prefix is chosen to avoid clash with usual "get" for the fields and also to indicate that
    //   the it returns the same object in a different representation and not a composed/aggregated object
    public static final String METHOD_AS_PRISM_OBJECT = "asPrismObject";
    public static final String METHOD_AS_PRISM_OBJECT_VALUE = "asPrismObjectValue";
    public static final String METHOD_AS_PRISM_CONTAINER_VALUE = "asPrismContainerValue";
    private static final String METHOD_AS_PRISM_CONTAINER = "asPrismContainer";
    // The "setup" prefix is chosen avoid collision with regular setters for generated fields
    public static final String METHOD_SETUP_CONTAINER_VALUE = "setupContainerValue";
    public static final String METHOD_SETUP_CONTAINER = "setupContainer";
    public static final String METHOD_AS_REFERENCE_VALUE = "asReferenceValue";
    public static final String METHOD_GET_OBJECT = "getObject";
    public static final String METHOD_GET_OBJECTABLE = "getObjectable";
    public static final String METHOD_SETUP_REFERENCE_VALUE = "setupReferenceValue";

    // Internal fields and methods. Although some of these fields needs to be public (so they can be used by
    // prism classes), they are not really intended for public usage. We also want to avoid conflicts with code
    // generated for regular fields. Hence the underscore.
    private static final String CONTAINER_FIELD_NAME = "_container";
    private static final String CONTAINER_VALUE_FIELD_NAME = "_containerValue";
    private static final String METHOD_GET_CONTAINER_NAME = "_getContainerName";
    private static final String METHOD_GET_CONTAINER_TYPE = "_getContainerType";
    private static final String REFERENCE_VALUE_FIELD_NAME = "_referenceValue";

    //methods in PrismForJAXBUtil
    private static final String METHOD_PRISM_UTIL_GET_FIELD_SINGLE_CONTAINERABLE = "getFieldSingleContainerable";
    private static final String METHOD_PRISM_UTIL_GET_PROPERTY_VALUE = "getPropertyValue";
    private static final String METHOD_PRISM_UTIL_GET_PROPERTY_VALUES = "getPropertyValues";
    private static final String METHOD_PRISM_UTIL_SET_PROPERTY_VALUE = "setPropertyValue";
    private static final String METHOD_PRISM_UTIL_GET_CONTAINER = "getContainer";
    private static final String METHOD_PRISM_UTIL_SET_FIELD_CONTAINER_VALUE = "setFieldContainerValue";
    private static final String METHOD_PRISM_UTIL_GET_REFERENCE = "getReference";
    private static final String METHOD_PRISM_UTIL_GET_REFERENCE_VALUE = "getReferenceValue";
    private static final String METHOD_PRISM_UTIL_GET_REFERENCE_OBJECTABLE = "getReferenceObjectable";
    private static final String METHOD_PRISM_UTIL_SET_REFERENCE_VALUE_AS_REF = "setReferenceValueAsRef";
    private static final String METHOD_PRISM_UTIL_SET_REFERENCE_VALUE_AS_OBJECT = "setReferenceValueAsObject";
    private static final String METHOD_PRISM_UTIL_GET_REFERENCE_FILTER_CLAUSE_XNODE = "getReferenceFilterClauseXNode";
    private static final String METHOD_PRISM_UTIL_SET_REFERENCE_FILTER_CLAUSE_XNODE = "setReferenceFilterClauseXNode";
    private static final String METHOD_PRISM_UTIL_GET_REFERENCE_TARGET_NAME = "getReferenceTargetName";
    private static final String METHOD_PRISM_UTIL_SET_REFERENCE_TARGET_NAME = "setReferenceTargetName";
    private static final String METHOD_PRISM_UTIL_OBJECTABLE_AS_REFERENCE_VALUE = "objectableAsReferenceValue";
    private static final String METHOD_PRISM_UTIL_SETUP_CONTAINER_VALUE = "setupContainerValue";
    private static final String METHOD_PRISM_UTIL_CREATE_TARGET_INSTANCE = "createTargetInstance";
    private static final String METHOD_PRISM_UTIL_ACCEPT = "accept";

    private static final String METHOD_ACCEPT = "accept";
    private static final String METHOD_VISIT = "visit";

    // ???
    private static final String METHOD_PRISM_GET_ANY = "getAny";

    private static final String METHOD_CONTAINER_GET_VALUE = "getValue";

    private static final String CONTAINER_VALUE_LOCAL_VAR_NAME = "containerValue";
    private static final String FIELD_CONTAINER_VALUE_LOCAL_VAR_NAME = "fieldContainerValue";
    private static final String OBJECT_LOCAL_FIELD_NAME = "object";
    private static final String REFERENCE_LOCAL_VARIABLE_NAME = "reference";

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
            createClassMap(CLASS_MAP, outline,
                    PrismReferenceValue.class, PrismReferenceValueImpl.class,
                    PrismReference.class, PrismReferenceImpl.class,
                    PrismObject.class, PrismObjectImpl.class,
                    String.class, Object.class, XmlTransient.class, Override.class, IllegalArgumentException.class,
                    ItemName.class,
                    QName.class, PrismForJAXBUtil.class, PrismReferenceArrayList.class,
                    PrismContainerValue.class, PrismContainerValueImpl.class,
                    List.class, Objectable.class, StringBuilder.class, XmlAccessorType.class, XmlElement.class, XmlType.class,
                    XmlAttribute.class, XmlAnyAttribute.class, XmlAnyElement.class,
                    PrismContainer.class, PrismContainerImpl.class,
                    Equals.class,
                    PrismContainerArrayList.class, HashCode.class, PrismContainerDefinition.class, Containerable.class,
                    Referencable.class, Raw.class, Enum.class, XmlEnum.class, PolyStringType.class, XmlTypeConverter.class,
                    PrismObjectValue.class, PrismObjectValueImpl.class);

            StepSchemaConstants stepSchemaConstants = new StepSchemaConstants();
            stepSchemaConstants.run(outline, options, errorHandler);

            Map<String, JFieldVar> namespaceFields = stepSchemaConstants.getNamespaceFields();
            addComplexType(outline, namespaceFields);
            addContainerName(outline, namespaceFields);
            addFieldQNames(outline, namespaceFields);

            updatePrismObject(outline);
            updatePrismContainer(outline);
            updateFields(outline);

            updateObjectReferenceType(outline);

            updateObjectFactoryElements(outline);

            createAcceptMethods(outline);
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
            if (!OBJECT_REFERENCE_TYPE.equals(qname)) {
                continue;
            }
            objectReferenceOutline = outline.getClazz(entry.getValue());
            break;
        }

        if (objectReferenceOutline == null) {
            //object reference type class not found
            return;
        }

        updateClassAnnotation(objectReferenceOutline);

        JDefinedClass definedClass = objectReferenceOutline.implClass;
        definedClass._implements(CLASS_MAP.get(Referencable.class));

        createDefaultConstructor(definedClass);

        //add prism reference and get/set method for it
        JVar reference = definedClass.field(JMod.PRIVATE, PrismReferenceValue.class, REFERENCE_VALUE_FIELD_NAME);
        JMethod asReferenceValueMethod = definedClass.method(JMod.PUBLIC, PrismReferenceValue.class, METHOD_AS_REFERENCE_VALUE);
//        getReference.annotate(CLASS_MAP.get(XmlTransient.class));
        JBlock body = asReferenceValueMethod.body();
        JBlock then = body._if(reference.eq(JExpr._null()))._then();
        JInvocation newReference = JExpr._new(CLASS_MAP.get(PrismReferenceValueImpl.class));
        then.assign(reference, newReference);
        body._return(reference);

        JMethod setReference = definedClass.method(JMod.PUBLIC, definedClass, METHOD_SETUP_REFERENCE_VALUE);
        JVar value = setReference.param(PrismReferenceValue.class, "value");
        body = setReference.body();
        body.assign(reference, value);
        body._return(JExpr._this());

        //update for oid methods
        updateObjectReferenceOid(definedClass, asReferenceValueMethod);
        //update for type methods
        updateObjectReferenceType(definedClass, asReferenceValueMethod);
        updateObjectReferenceRelation(definedClass, asReferenceValueMethod);
        updateObjectReferenceDescription(definedClass, asReferenceValueMethod);
        updateObjectReferenceFilter(definedClass, asReferenceValueMethod);
        updateObjectReferenceResolutionTime(definedClass, asReferenceValueMethod);
        updateObjectReferenceReferentialIntegrity(definedClass, asReferenceValueMethod);
        updateObjectReferenceGetObject(definedClass, asReferenceValueMethod);
        updateObjectReferenceGetObjectable(definedClass, asReferenceValueMethod);

        createReferenceFluentEnd(objectReferenceOutline);
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
        invocation.arg(JExpr.lit(true));

        JFieldVar targetNameField = definedClass.fields().get("targetName");
        JMethod getTargetName = recreateMethod(findMethod(definedClass, "getTargetName"), definedClass);
        copyAnnotations(getTargetName, targetNameField);
        JBlock getTargetNamebody = getTargetName.body();
        JInvocation getTargetNameInvocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE_TARGET_NAME);
        getTargetNameInvocation.arg(JExpr.invoke(getReference));
        getTargetNamebody._return(getTargetNameInvocation);

        definedClass.removeField(targetNameField);

        JMethod setTargetName = recreateMethod(findMethod(definedClass, "setTargetName"), definedClass);
        JBlock setTargetNamebody = setTargetName.body();
        JInvocation setTagetNameInvocation = setTargetNamebody.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class), METHOD_PRISM_UTIL_SET_REFERENCE_TARGET_NAME);
        setTagetNameInvocation.arg(JExpr.invoke(getReference));
        setTagetNameInvocation.arg(setTargetName.listParams()[0]);
    }

    private void updateObjectReferenceRelation(JDefinedClass definedClass, JMethod asReferenceMethod) {
        JFieldVar typeField = definedClass.fields().get("relation");
        JMethod getType = recreateMethod(findMethod(definedClass, "getRelation"), definedClass);
        copyAnnotations(getType, typeField);
        JBlock body = getType.body();
        body._return(JExpr.invoke(JExpr.invoke(asReferenceMethod), "getRelation"));

        definedClass.removeField(typeField);
        JMethod setType = recreateMethod(findMethod(definedClass, "setRelation"), definedClass);
        body = setType.body();
        JInvocation invocation = body.invoke(JExpr.invoke(asReferenceMethod), "setRelation");
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

    private void updateObjectReferenceDescription(JDefinedClass definedClass, JMethod getReference) {
        JFieldVar descriptionField = definedClass.fields().get("description");
        JMethod getDescription = recreateMethod(findMethod(definedClass, "getDescription"), definedClass);
        copyAnnotations(getDescription, descriptionField);
        definedClass.removeField(descriptionField);
        JBlock body = getDescription.body();
        body._return(JExpr.invoke(JExpr.invoke(getReference), getDescription.name()));

        JMethod setDescription = recreateMethod(findMethod(definedClass, "setDescription"), definedClass);
        body = setDescription.body();
        JInvocation invocation = body.invoke(JExpr.invoke(getReference), setDescription.name());
        invocation.arg(setDescription.listParams()[0]);
    }

    private void updateObjectReferenceFilter(JDefinedClass definedClass, JMethod asReferenceValue) {
        JFieldVar filterField = definedClass.fields().get("filter");

        JMethod getFilter = recreateMethod(findMethod(definedClass, "getFilter"), definedClass);
        copyAnnotations(getFilter, filterField);
        definedClass.removeField(filterField);
        JBlock body = getFilter.body();
        JType innerFilterType = getFilter.type();
        JVar filterClassVar = body.decl(innerFilterType, "filter", JExpr._new(innerFilterType));
        JInvocation getFilterElementInvocation =CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE_FILTER_CLAUSE_XNODE);
        getFilterElementInvocation.arg(JExpr.invoke(asReferenceValue));
        JInvocation setFilterInvocation = body.invoke(filterClassVar, "setFilterClauseXNode");
        setFilterInvocation.arg(getFilterElementInvocation);
        body._return(filterClassVar);

        JMethod setFilter = recreateMethod(findMethod(definedClass, "setFilter"), definedClass);
        body = setFilter.body();
        JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_SET_REFERENCE_FILTER_CLAUSE_XNODE);
        invocation.arg(JExpr.invoke(asReferenceValue));
        invocation.arg(setFilter.listParams()[0]);
        body.add(invocation);
    }

    private void updateObjectReferenceResolutionTime(JDefinedClass definedClass, JMethod asReferenceMethod) {
        JFieldVar typeField = definedClass.fields().get("resolutionTime");
        JMethod getType = recreateMethod(findMethod(definedClass, "getResolutionTime"), definedClass);
        copyAnnotations(getType, typeField);
        JBlock body = getType.body();
        body._return(JExpr.invoke(JExpr.invoke(asReferenceMethod), "getResolutionTime"));

        definedClass.removeField(typeField);
        JMethod setType = recreateMethod(findMethod(definedClass, "setResolutionTime"), definedClass);
        body = setType.body();
        JInvocation invocation = body.invoke(JExpr.invoke(asReferenceMethod), "setResolutionTime");
        invocation.arg(setType.listParams()[0]);
    }

    private void updateObjectReferenceReferentialIntegrity(JDefinedClass definedClass, JMethod asReferenceMethod) {
        JFieldVar typeField = definedClass.fields().get("referentialIntegrity");
        JMethod getType = recreateMethod(findMethod(definedClass, "getReferentialIntegrity"), definedClass);
        copyAnnotations(getType, typeField);
        JBlock body = getType.body();
        body._return(JExpr.invoke(JExpr.invoke(asReferenceMethod), "getReferentialIntegrity"));

        definedClass.removeField(typeField);
        JMethod setType = recreateMethod(findMethod(definedClass, "setReferentialIntegrity"), definedClass);
        body = setType.body();
        JInvocation invocation = body.invoke(JExpr.invoke(asReferenceMethod), "setReferentialIntegrity");
        invocation.arg(setType.listParams()[0]);
    }

    private void updateObjectReferenceGetObject(JDefinedClass definedClass, JMethod asReferenceMethod) {
        JMethod method = definedClass.method(JMod.PUBLIC, PrismObject.class, METHOD_GET_OBJECT);
        JBlock body = method.body();
        body._return(JExpr.invoke(JExpr.invoke(asReferenceMethod), "getObject"));
    }

    private void updateObjectReferenceGetObjectable(JDefinedClass definedClass, JMethod asReferenceMethod) {
        JMethod method = definedClass.method(JMod.PUBLIC, Objectable.class, METHOD_GET_OBJECTABLE);
        JBlock body = method.body();
        JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE_OBJECTABLE);
        invocation.arg(JExpr.invoke(asReferenceMethod));
        body._return(invocation);
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

    private Set<JDefinedClass> updatePrismContainer(Outline outline) {
        Set<JDefinedClass> containers = new HashSet<>();
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());

            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null || !hasAnnotation(classOutline, A_PRISM_CONTAINER)) {
                continue;
            }

            if (hasAnnotation(classOutline, A_PRISM_OBJECT) && hasAnnotation(classOutline, A_PRISM_CONTAINER)) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;
            definedClass._implements(CLASS_MAP.get(Containerable.class));
            containers.add(definedClass);

            //inserting MidPointObject field into ObjectType class
            JVar containerValue = definedClass.field(JMod.PRIVATE, PrismContainerValue.class, CONTAINER_VALUE_FIELD_NAME);

            // default constructor
            createDefaultConstructor(definedClass);

            //create asPrismContainer
//            createAsPrismContainer(classOutline, containerValue);
            createAsPrismContainerValue(definedClass, containerValue);

            //create setContainer
            JMethod setupContainerMethod = createSetContainerValueMethod(definedClass, containerValue);

            // constructor with prismContext
            createPrismContextContainerableConstructor(definedClass, setupContainerMethod);

            //create toString, equals, hashCode
            createToStringMethod(definedClass, METHOD_AS_PRISM_CONTAINER_VALUE);
            createEqualsMethod(classOutline, METHOD_AS_PRISM_CONTAINER_VALUE);
            createHashCodeMethod(definedClass, METHOD_AS_PRISM_CONTAINER_VALUE);

            //get container type
            JMethod getContainerType = definedClass.method(JMod.NONE, QName.class, METHOD_GET_CONTAINER_TYPE);
//            getContainerType.annotate(CLASS_MAP.get(XmlTransient.class));
            JBlock body = getContainerType.body();
            body._return(definedClass.staticRef(COMPLEX_TYPE_FIELD_NAME));
        }

        removeCustomGeneratedMethod(outline);

        return containers;
    }

    private JMethod createDefaultConstructor(JDefinedClass definedClass) {
        JMethod constructor = definedClass.constructor(JMod.PUBLIC);
        constructor.body().invoke("super").invoke("aaa");
        return constructor;
    }

    private JMethod createPrismContextContainerableConstructor(JDefinedClass definedClass, JMethod setupContainerMethod) {
        JMethod constructor = definedClass.constructor(JMod.PUBLIC);
        constructor.param(PrismContext.class, "prismContext");

        JBlock body = constructor.body();
        body.invoke(setupContainerMethod)                                                        // setupContainerValue(
                .arg(JExpr._new(CLASS_MAP.get(PrismContainerValueImpl.class).narrow(new JClass[0]))  //    new PrismContainerValueImpl<>(
                        .arg(JExpr._this())                                                      //       this,
                        .arg(constructor.params().get(0)));                                      //       prismContext);
        return constructor;
    }

    /*
        public UserType(PrismContext prismContext) {
            setupContainer(new PrismObject(_getContainerName(), this.getClass(), prismContext));
        }
     */

    private JMethod createPrismContextObjectableConstructor(JDefinedClass definedClass) {
        JMethod constructor = definedClass.constructor(JMod.PUBLIC);
        constructor.param(PrismContext.class, "prismContext");

        JBlock body = constructor.body();
        body.invoke("setupContainer")
                .arg(JExpr._new(CLASS_MAP.get(PrismObjectImpl.class))
                        .arg(JExpr.invoke("_getContainerName"))
                        .arg(JExpr.invoke("getClass"))
                        .arg(constructor.params().get(0)));
        return constructor;
    }

    private void createAsPrismContainerValueInObject(JDefinedClass definedClass) {
        JMethod getContainer = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(PrismContainerValue.class),
                METHOD_AS_PRISM_CONTAINER_VALUE);
        getContainer.annotate(CLASS_MAP.get(Override.class));

        //create method body
        JBlock body = getContainer.body();
        body._return(JExpr.invoke(METHOD_AS_PRISM_CONTAINER).invoke(METHOD_CONTAINER_GET_VALUE));
    }

    private void createAsPrismContainerValue(JDefinedClass definedClass, JVar containerValueVar) {
        JMethod getContainer = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(PrismContainerValue.class),
                METHOD_AS_PRISM_CONTAINER_VALUE);
//        getContainer.annotate(CLASS_MAP.get(XmlTransient.class));

        //create method body
        JBlock body = getContainer.body();
        body._if(containerValueVar.eq(JExpr._null())).                                              // if (_containerValue == null) {
            _then()                                                                                 //
                .assign(containerValueVar,                                                          //    _containerValue =
                        JExpr._new(CLASS_MAP.get(PrismContainerValueImpl.class).narrow(new JClass[0]))  //       new PrismContainerValueImpl<>(
                                .arg(JExpr._this())                                                 //          this)
                );
        body._return(containerValueVar);
    }

    private Set<JDefinedClass> updatePrismObject(Outline outline) {
        Set<JDefinedClass> containers = new HashSet<>();
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());

            if (qname == null) {
                continue;
            }

            boolean isDirectPrismObject = hasAnnotation(classOutline, A_PRISM_OBJECT);
            boolean isIndirectPrismObject = hasParentAnnotation(classOutline, A_PRISM_OBJECT);

            if (!isIndirectPrismObject) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;

            createDefaultConstructor(definedClass);
            createPrismContextObjectableConstructor(definedClass);

            createAsPrismObject(definedClass);

            if (!isDirectPrismObject) {
                continue;
            }

            definedClass._implements(CLASS_MAP.get(Objectable.class));
            containers.add(definedClass);

            //inserting PrismObject field into ObjectType class
            JVar container = definedClass.field(JMod.PRIVATE, PrismObject.class, CONTAINER_FIELD_NAME);

            //create getContainer
//            createGetContainerMethod(classOutline, container);
            //create setContainer
            createSetContainerMethod(definedClass, container);

            //create asPrismObject()
            createAsPrismContainer(classOutline, container);
            // Objectable is also Containerable, we also need these
            createAsPrismContainerValueInObject(definedClass);
            createSetContainerValueMethodInObject(definedClass, container);

            print("Creating toString, equals, hashCode methods.");
            //create toString, equals, hashCode
            createToStringMethod(definedClass, METHOD_AS_PRISM_CONTAINER);
            createEqualsMethod(classOutline, METHOD_AS_PRISM_CONTAINER);
            createHashCodeMethod(definedClass, METHOD_AS_PRISM_CONTAINER);
            //create toDebugName, toDebugType
            createToDebugName(definedClass);
            createToDebugType(definedClass);
        }

        removeCustomGeneratedMethod(outline);

        return containers;
    }

    /**
     * Marks ObjectFactory.createXYZ methods for elements with a:rawType annotation as @Raw.
     */
    private void updateObjectFactoryElements(Outline outline) {
        XSSchemaSet schemaSet = outline.getModel().schemaComponent;
        for (CElementInfo elementInfo : outline.getModel().getAllElements()) {
            QName name = elementInfo.getElementName();
            XSComponent elementDecl;
            if (elementInfo.getSchemaComponent() != null) {     // it's strange but elements seem not to have this filled-in...
                elementDecl = elementInfo.getSchemaComponent();
            } else {
                elementDecl = schemaSet.getElementDecl(name.getNamespaceURI(), name.getLocalPart());
            }
            boolean isRaw = hasAnnotation(elementDecl, A_RAW_TYPE);
            if (isRaw) {
                print("*** Raw element found: " + elementInfo.getElementName());
                JDefinedClass objectFactory = outline.getPackageContext(elementInfo._package()).objectFactory();
                boolean methodFound = false;    // finding method corresponding to the given element
                for (JMethod method : objectFactory.methods()) {
                    for (JAnnotationUse annotationUse : method.annotations()) {
                        if (XmlElementDecl.class.getName().equals(annotationUse.getAnnotationClass().fullName())) {
                            // ugly method of finding the string value of the annotation members (couldn't find any better)
                            JAnnotationValue namespaceValue = annotationUse.getAnnotationMembers().get("namespace");
                            StringWriter namespaceWriter = new StringWriter();
                            JFormatter namespaceFormatter = new JFormatter(namespaceWriter);
                            namespaceValue.generate(namespaceFormatter);

                            JAnnotationValue nameValue = annotationUse.getAnnotationMembers().get("name");
                            StringWriter nameWriter = new StringWriter();
                            JFormatter nameFormatter = new JFormatter(nameWriter);
                            nameValue.generate(nameFormatter);

                            if (("\""+name.getNamespaceURI()+"\"").equals(namespaceWriter.toString()) &&
                                    ("\""+name.getLocalPart()+"\"").equals(nameWriter.toString())) {
                                print("*** Annotating method as @Raw: " + method.name());
                                method.annotate(Raw.class);
                                methodFound = true;
                                break;
                            }
                        }
                    }
                }
                if (!methodFound) {
                    throw new IllegalStateException("No factory method found for element " + name);
                }
            }
        }
    }

    private void createAsPrismObject(JDefinedClass definedClass) {
        JClass prismObjectClass = CLASS_MAP.get(PrismObject.class);
        JType returnType;
        if (definedClass.isAbstract()) {
            returnType = prismObjectClass.narrow(definedClass.wildcard());
        } else {
            // e.g. PrismObject<TaskType> for TaskType
            // we assume that we don't subclass a non-abstract object class into another one
            returnType = prismObjectClass.narrow(definedClass);
        }
        JMethod asPrismObject = definedClass.method(JMod.PUBLIC, returnType, METHOD_AS_PRISM_OBJECT);
        asPrismObject.annotate(CLASS_MAP.get(Override.class));

        //create method body
        JBlock body = asPrismObject.body();
        body._return(JExpr.invoke(METHOD_AS_PRISM_CONTAINER));
    }

    private void updateClassAnnotation(ClassOutline classOutline) {
        try {
            JDefinedClass definedClass = classOutline.implClass;
            List<JAnnotationUse> existingAnnotations = getAnnotations(definedClass);
            for (JAnnotationUse annotation : existingAnnotations) {
                if (isAnnotationTypeOf(annotation, XmlAccessorType.class)) {
                    Field field = getField(JAnnotationUse.class, "memberValues");
                    field.setAccessible(true);
                    Map<String, Object> map = (Map<String, Object>) field.get(annotation);
                    field.setAccessible(false);
                    map.clear();
                    annotation.param("value", XmlAccessType.PROPERTY);
                }
                if (isAnnotationTypeOf(annotation, XmlType.class)) {
                    Field field = getField(JAnnotationUse.class, "memberValues");
                    field.setAccessible(true);
                    Map<String, Object> map = (Map<String, Object>) field.get(annotation);
                    Object propOrder = map.get("propOrder");
                    if (propOrder != null) {
                        JAnnotationArrayMember paramArray = (JAnnotationArrayMember)propOrder;
                        Field valField = getField(JAnnotationArrayMember.class, "values");
                        valField.setAccessible(true);
                        List<JAnnotationValue> values = (List<JAnnotationValue>) valField.get(paramArray);
                        for (int i=0; i < values.size(); i++) {
                            JAnnotationValue jAnnValue = values.get(i);
                            String value = extractString(jAnnValue);
                            if (value.startsWith("_")) {
                                paramArray.param(value.substring(1));
                                values.set(i, values.get(values.size() - 1));
                                values.remove(values.size() - 1);
                            }
//                            String valAfter = extractString(values.get(i));
//                            print("PPPPPPPPPPPPPPPPPPP: "+value+" -> "+valAfter);
                        }
                        valField.setAccessible(false);
                    }
                    field.setAccessible(false);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private String extractString(JAnnotationValue jAnnValue) {
        StringWriter writer = new StringWriter();
        JFormatter formatter = new JFormatter(writer);
        jAnnValue.generate(formatter);
        String value = writer.getBuffer().toString();
        return value.substring(1, value.length() - 1);
    }

    private boolean isAnnotationTypeOf(JAnnotationUse annotation, Class clazz) {
        try {
            Field field = getField(JAnnotationUse.class, "clazz");
            field.setAccessible(true);
            JClass jClass = (JClass) field.get(annotation);
            field.setAccessible(false);

            if (CLASS_MAP.get(clazz).equals(jClass)) {
                return true;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }

        return false;
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

    private void createHashCodeMethod(JDefinedClass definedClass, String baseMethodName) {
        JMethod hashCode = definedClass.getMethod(METHOD_HASH_CODE, new JType[]{});
        if (hashCode == null) {
            hashCode = definedClass.method(JMod.PUBLIC, int.class, METHOD_HASH_CODE);
        } else {
            hashCode = recreateMethod(hashCode, definedClass);
        }
        hashCode.annotate(CLASS_MAP.get(Override.class));
        JBlock body = hashCode.body();
        body._return(JExpr.invoke(baseMethodName).invoke(METHOD_HASH_CODE));
    }

    /**
     * remove generated equals methods from classes which extends from prism containers/objects
     */
    private void removeCustomGeneratedMethod(Outline outline) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null || (!hasParentAnnotation(classOutline, A_PRISM_OBJECT)
                    && !hasParentAnnotation(classOutline, A_PRISM_CONTAINER))) {
                continue;
            }

            JDefinedClass definedClass = classOutline.implClass;
            Iterator<JClass> iterator = definedClass._implements();
            while (iterator.hasNext()) {
                JClass clazz = iterator.next();
                if (clazz.equals(CLASS_MAP.get(Equals.class)) || clazz.equals(CLASS_MAP.get(HashCode.class))) {
                    iterator.remove();
                }
            }

            boolean isMidpointContainer = hasParentAnnotation(classOutline, A_PRISM_OBJECT);
            removeOldCustomGeneratedEquals(classOutline, isMidpointContainer);
            removeOldCustomGenerated(classOutline, isMidpointContainer, METHOD_HASH_CODE);
            removeOldCustomGenerated(classOutline, isMidpointContainer, METHOD_TO_STRING);
        }
    }

    private void removeOldCustomGenerated(ClassOutline classOutline, boolean isPrismObject, String methodName) {
        JDefinedClass definedClass = classOutline.implClass;
        Iterator<JMethod> methods = definedClass.methods().iterator();
        while (methods.hasNext()) {
            JMethod method = methods.next();
            if (isPrismObject && !hasAnnotation(classOutline, A_PRISM_OBJECT)) {
                if (method.name().equals(methodName)) {
                    methods.remove();
                }
            } else {
                if (method.name().equals(methodName) && method.listParams().length != 0) {
                    methods.remove();
                }
            }
        }
    }

    private void removeOldCustomGeneratedEquals(ClassOutline classOutline, boolean isPrismObject) {
        JDefinedClass definedClass = classOutline.implClass;
        Iterator<JMethod> methods = definedClass.methods().iterator();
        while (methods.hasNext()) {
            JMethod method = methods.next();
            if (isPrismObject && !hasAnnotation(classOutline, A_PRISM_OBJECT)) {
                if (method.name().equals(METHOD_EQUALS)) {
                    methods.remove();
                }
            } else {
                if (method.name().equals(METHOD_EQUALS) && method.listParams().length != 1) {
                    methods.remove();
                }
            }
        }
    }

    private void createEqualsMethod(ClassOutline classOutline, String baseMethod) {
        JDefinedClass definedClass = classOutline.implClass;
        JMethod equals = definedClass.getMethod(METHOD_EQUALS, new JType[]{CLASS_MAP.get(Object.class)});

        if (equals != null) {
//            removeOldCustomGeneratedEquals(classOutline, hasParentAnnotation(classOutline, PRISM_OBJECT));  todo can this be removed?
            equals = recreateMethod(equals, definedClass);
        } else {
            equals = definedClass.method(JMod.PUBLIC, boolean.class, METHOD_EQUALS);
        }
        equals.annotate(CLASS_MAP.get(Override.class));

        JBlock body = equals.body();
        JVar obj = equals.listParams()[0];
        JBlock ifNull = body._if(obj._instanceof(definedClass).not())._then();
        ifNull._return(JExpr.lit(false));

        JVar other = body.decl(definedClass, "other", JExpr.cast(definedClass, obj));

        JInvocation invocation = JExpr.invoke(baseMethod).invoke(METHOD_EQUIVALENT);
        invocation.arg(other.invoke(baseMethod));
        body._return(invocation);
    }

    private void createToStringMethod(JDefinedClass definedClass, String baseMethod) {
        JMethod toString = definedClass.getMethod("toString", new JType[]{});
        if (toString == null) {
            toString = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(String.class), METHOD_TO_STRING);
        } else {
            toString = recreateMethod(toString, definedClass);
        }
        toString.annotate(CLASS_MAP.get(Override.class));

        JBlock body = toString.body();
        JInvocation invocation = JExpr.invoke(baseMethod).invoke(METHOD_TO_STRING);
        body._return(invocation);
    }

    private JMethod createSetContainerValueMethod(JDefinedClass definedClass, JVar container) {
        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, METHOD_SETUP_CONTAINER_VALUE);
        JVar methodContainer = setContainer.param(PrismContainerValue.class, "containerValue");
        //create method body
        JBlock body = setContainer.body();
//        JBlock then = body._if(methodContainer.eq(JExpr._null()))._then();
//        then.assign(JExpr._this().ref(container), JExpr._null());
//        then._return();
//
//        then = body._if(methodContainer.invoke("getParent").eq(JExpr._null()))._then();
//        then.assign(JExpr._this().ref(container), methodContainer);
//        then._return();
//
//        JVar definition = body.decl(CLASS_MAP.get(PrismContainerDefinition.class), "definition",
//                JExpr.invoke(JExpr.invoke(methodContainer, "getParent"), "getDefinition"));
//
//        JInvocation equals = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER_TYPE), "equals");
//        equals.arg(definition.invoke("getTypeName"));
//        then = body._if(definition.ne(JExpr._null()).cand(equals.not()))._then();
//        JInvocation exception = JExpr._new(CLASS_MAP.get(IllegalArgumentException.class));
//
//        JExpression message = JExpr.lit("Container definition type qname '").plus(JExpr.invoke(definition, "getTypeName"))
//                .plus(JExpr.lit("' doesn't equals to '")).plus(JExpr.invoke(METHOD_GET_CONTAINER_TYPE))
//                .plus(JExpr.lit("'."));
//        exception.arg(message);
//        then._throw(exception);

        body.assign(JExpr._this().ref(container), methodContainer);
        return setContainer;
    }

    private void createSetContainerValueMethodInObject(JDefinedClass definedClass, JVar container) {
        JMethod setContainerValue = definedClass.method(JMod.PUBLIC, void.class, METHOD_SETUP_CONTAINER_VALUE);
        setContainerValue.annotate(CLASS_MAP.get(Override.class));
        JVar containerValue = setContainerValue.param(PrismContainerValue.class, "containerValue");
        //create method body
        JBlock body = setContainerValue.body();
        JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_SETUP_CONTAINER_VALUE);
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER));
        invocation.arg(containerValue);
        body.assign(container, invocation);
    }

    private void createAsPrismContainer(ClassOutline classOutline, JVar container) {
        JDefinedClass definedClass = classOutline.implClass;
        JMethod getContainer = definedClass.method(JMod.PUBLIC, CLASS_MAP.get(PrismObject.class),
                METHOD_AS_PRISM_CONTAINER);

        //create method body
        JBlock body = getContainer.body();
        JBlock then = body._if(container.eq(JExpr._null()))._then();

        JInvocation newContainer = JExpr._new(CLASS_MAP.get(PrismObjectImpl.class));
        newContainer.arg(JExpr.invoke(METHOD_GET_CONTAINER_NAME));
        newContainer.arg(JExpr._this().invoke("getClass"));
//        newContainer.arg(JExpr.dotclass(definedClass));
        then.assign(container, newContainer);

        body._return(container);
    }

    private JMethod createSetContainerMethod(JDefinedClass definedClass, JVar container) {
        JMethod setContainer = definedClass.method(JMod.PUBLIC, void.class, METHOD_SETUP_CONTAINER);
        JVar methodContainer = setContainer.param(PrismObject.class, "container");
        //create method body
        JBlock body = setContainer.body();
//        JBlock then = body._if(methodContainer.eq(JExpr._null()))._then();
//        then.assign(JExpr._this().ref(container), JExpr._null());
//        then._return();
//
//        JVar definition = body.decl(CLASS_MAP.get(PrismContainerDefinition.class), "definition",
//                JExpr.invoke(methodContainer, "getDefinition"));
//
//        JInvocation equals = JExpr.invoke(JExpr.invoke(METHOD_GET_CONTAINER_TYPE), "equals");
//        equals.arg(definition.invoke("getTypeName"));
//        then = body._if(definition.ne(JExpr._null()).cand(equals.not()))._then();
//        JInvocation exception = JExpr._new(CLASS_MAP.get(IllegalArgumentException.class));
//
//        JExpression message = JExpr.lit("Container definition type qname '").plus(JExpr.invoke(definition, "getTypeName"))
//                .plus(JExpr.lit("' doesn't equals to '")).plus(JExpr.invoke(METHOD_GET_CONTAINER_TYPE))
//                .plus(JExpr.lit("'."));
//        exception.arg(message);
//        then._throw(exception);

        body.assign(JExpr._this().ref(container), methodContainer);
        return setContainer;
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
            if (qname == null || !hasParentAnnotation(classOutline, A_PRISM_OBJECT)) {
                continue;
            }

            //element name
            List<QName> qnames = complexTypeToElementName.get(qname);
            if (qnames == null || qnames.size() != 1) {
                printWarning("Found zero or more than one element names for type '"
                        + qname + "', " + qnames + ".");
                continue;
            }
            qname = qnames.get(0);

            JDefinedClass definedClass = classOutline.implClass;
            JMethod getContainerName = definedClass.method(JMod.NONE, QName.class, METHOD_GET_CONTAINER_NAME);
//            getContainerName.annotate(CLASS_MAP.get(XmlTransient.class));
            JBlock body = getContainerName.body();

            JFieldVar var = namespaceFields.get(qname.getNamespaceURI());
            JInvocation invocation = JExpr._new(CLASS_MAP.get(QName.class));
            if (var != null) {
                JClass schemaClass = outline.getModel().codeModel._getClass(StepSchemaConstants.SCHEMA_CONSTANTS_GENERATED_CLASS_NAME);
                invocation.arg(schemaClass.staticRef(var));
                invocation.arg(qname.getLocalPart());
            } else {
                invocation.arg(qname.getNamespaceURI());
                invocation.arg(qname.getLocalPart());

            }
            body._return(invocation);

            //get container type
            JMethod getContainerType = definedClass.method(JMod.NONE, QName.class, METHOD_GET_CONTAINER_TYPE);
//            getContainerType.annotate(CLASS_MAP.get(XmlTransient.class));
            body = getContainerType.body();
            body._return(definedClass.staticRef(COMPLEX_TYPE_FIELD_NAME));
        }
    }

    private boolean hasParentAnnotation(ClassOutline classOutline, QName annotation) {
        if (classOutline.getSuperClass() == null) {
            return hasAnnotation(classOutline, annotation);
        }

        return hasAnnotation(classOutline, annotation) || hasParentAnnotation(classOutline.getSuperClass(), annotation);
    }

    private void addComplexType(Outline outline, Map<String, JFieldVar> namespaceFields) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = entry.getValue().getTypeName();
            if (qname == null) {
                continue;
            }
            JFieldVar namespaceField = namespaceFields.get(qname.getNamespaceURI());
            createQName(outline, classOutline.implClass, COMPLEX_TYPE_FIELD_NAME, qname, namespaceField, false, false);
        }
    }

    private Map<QName, List<QName>> getComplexTypeToElementName(ClassOutline classOutline) {
        Map<QName, List<QName>> complexTypeToElementName = new HashMap<>();

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
                    qnames = new ArrayList<>();
                    complexTypeToElementName.put(type, qnames);
                }
                qnames.add(new QName(decl.getTargetNamespace(), decl.getName()));
            }
        }

        return complexTypeToElementName;
    }

    private void addFieldQNames(Outline outline, Map<String, JFieldVar> namespaceFields) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            QName qname = getCClassInfoQName(entry.getValue());
            if (qname == null) {
                continue;
            }

            JDefinedClass implClass = classOutline.implClass;
            Map<String, JFieldVar> fields = implClass.fields();

            if (fields == null) {
                continue;
            }

            boolean isObject = hasAnnotation(classOutline, A_PRISM_OBJECT);

            List<FieldBox<QName>> boxes = new ArrayList<>();
            for (Entry<String, JFieldVar> fieldEntry : fields.entrySet()) {
                String field = normalizeFieldName(fieldEntry.getKey());
                if ((isObject && ("oid".equals(field) || "version".equals(field)) ||
                        "serialVersionUID".equals(field) || "id".equals(field) || COMPLEX_TYPE_FIELD_NAME.equals(field))) {
                    continue;
                }

                if (hasAnnotationClass(fieldEntry.getValue(), XmlAnyElement.class)) {
                    continue;
                }

                String fieldName = fieldFPrefixUnderscoredUpperCase(field);
                boxes.add(new FieldBox<>(fieldName, new QName(qname.getNamespaceURI(), field)));
            }

            JFieldVar var = namespaceFields.get(qname.getNamespaceURI());
            for (FieldBox<QName> box : boxes) {
                createQName(outline, implClass, box.getFieldName(), box.getValue(), var, false, true);
            }
        }
    }

    private void updateFields(Outline outline) {
        Map<JDefinedClass, List<JFieldVar>> allFieldsToBeRemoved = new HashMap<>();

        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());

            JDefinedClass implClass = classOutline.implClass;
            Map<String, JFieldVar> fields = implClass.fields();

            if (fields == null) {
                continue;
            }

            print("Updating fields and get/set methods: " + classOutline.implClass.fullName());

            for (Map.Entry<String, JFieldVar> field : fields.entrySet()) {
                JFieldVar fieldVar = field.getValue();
                // marks a:rawType fields with @Raw - this has to be executed for any bean, not only for prism containers
                if (hasAnnotation(classOutline, fieldVar, A_RAW_TYPE) != null) {
                    annotateFieldAsRaw(fieldVar);
                }
            }

            if (isContainer(classOutline.implClass, outline)) {
                processContainerFields(classOutline, allFieldsToBeRemoved);
            }
            createFluentFieldMethods(classOutline, classOutline);

            print("Finished updating fields and get/set methods for " + classOutline.implClass.fullName());
        }

        allFieldsToBeRemoved.forEach((jDefinedClass, jFieldVars) -> {
            jFieldVars.forEach(jDefinedClass::removeField);
        });
    }

    private void processContainerFields(ClassOutline classOutline, Map<JDefinedClass, List<JFieldVar>> allFieldsToBeRemoved) {
        JDefinedClass implClass = classOutline.implClass;
        Map<String, JFieldVar> fields = implClass.fields();

        createContainerFluentEnd(classOutline);            // not available for beans (no parent there)

        updateClassAnnotation(classOutline);
        boolean isObject = hasAnnotation(classOutline, A_PRISM_OBJECT);

        List<JFieldVar> fieldsToBeRemoved = new ArrayList<>();
        // WARNING: cannot change to entrySet. For some reason entrySet does not work here.
        for (String field : fields.keySet()) {
            JFieldVar fieldVar = fields.get(field);
            if (isAuxiliaryField(fieldVar)) {
                continue;
            }

            boolean remove;
            if (isObject && ("oid".equals(field) || "version".equals(field))) {
                print("Updating simple field: " + fieldVar.name());
                remove = updateSimpleField(fieldVar, classOutline, METHOD_AS_PRISM_CONTAINER);
            } else if ("id".equals(field)) {
                print("Updating container id field: " + fieldVar.name());
                remove = updateIdField(fieldVar, classOutline);
            } else if (isFieldReference(fieldVar, classOutline)) {
                print("Updating field (reference): " + fieldVar.name());
                remove = updateFieldReference(fieldVar, classOutline);
            } else if (isFieldReferenceUse(fieldVar, classOutline)) {
                print("Updating field (reference usage): " + fieldVar.name());
                remove = updateFieldReferenceUse(fieldVar, classOutline);
            } else if (isFieldTypeContainer(fieldVar, classOutline)) {
                print("Updating container field: " + fieldVar.name());
                remove = updateContainerFieldType(fieldVar, classOutline);
            } else {
                print("Updating field: " + fieldVar.name());
                remove = updateField(fieldVar, classOutline);
            }
            if (remove) {
                fieldsToBeRemoved.add(fieldVar);
            }
        }
        allFieldsToBeRemoved.put(implClass, fieldsToBeRemoved);
    }

    private void createFluentFieldMethods(ClassOutline targetClass, ClassOutline sourceClass) {
        Map<String, JFieldVar> fields = sourceClass.implClass.fields();
        for (Map.Entry<String, JFieldVar> field : fields.entrySet()) {
            JFieldVar fieldVar = field.getValue();
            if (!isAuxiliaryField(fieldVar) && !hasAnnotationClass(fieldVar, XmlAnyElement.class)) {
                createFluentFieldMethods(fieldVar, targetClass, sourceClass);
            }
        }
        if (sourceClass.getSuperClass() != null) {
            createFluentFieldMethods(targetClass, sourceClass.getSuperClass());
        }
    }

    private boolean isAuxiliaryField(JFieldVar fieldVar) {
        String field = fieldVar.name();
        return "serialVersionUID".equals(field) || COMPLEX_TYPE_FIELD_NAME.equals(field)
                || CONTAINER_FIELD_NAME.equals(field) || CONTAINER_VALUE_FIELD_NAME.equals(field)
                || "otherAttributes".equals(field) && fieldVar.type().name().equals("Map<QName,String>")
                || isFField(fieldVar);
    }

    private boolean isFField(JFieldVar fieldVar) {
        boolean isPublicStaticFinal = (fieldVar.mods().getValue() & (JMod.STATIC | JMod.FINAL)) != 0;
        if (fieldVar.name().startsWith("F_") && isPublicStaticFinal) {
            //our QName constant fields
            return true;
        }
        return false;
    }

    private boolean updateIdField(JFieldVar field, ClassOutline classOutline) {
        JMethod method = recreateGetter(field, classOutline);
        JBlock body = method.body();
        body._return(JExpr.invoke(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE), "getId"));

        method = recreateSetter(field, classOutline);
        body = method.body();
        JInvocation invocation = body.invoke(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE), "setId");
        invocation.arg(method.listParams()[0]);

        return true;
    }

    private JMethod recreateSetter(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass definedClass = classOutline.implClass;
        JMethod method = findSetterMethod(field, classOutline);
        return recreateMethod(method, definedClass);
    }

    private JMethod findSetterMethod(JFieldVar field, ClassOutline classOutline) {
        String methodName = getSetterMethodName(classOutline, field);
        return classOutline.implClass.getMethod(methodName, new JType[]{field.type()});
    }

    private JMethod recreateGetter(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass definedClass = classOutline.implClass;
        JMethod method = findGetterMethod(field, classOutline);
        JMethod newGetter = recreateMethod(method, definedClass);
        copyAnnotations(newGetter, field);
        return newGetter;
    }

    private JMethod findGetterMethod(JFieldVar field, ClassOutline classOutline) {
        String methodName = getGetterMethodName(classOutline, field);
        return classOutline.implClass.getMethod(methodName, new JType[]{});
    }

    private boolean updateFieldReference(JFieldVar field, ClassOutline classOutline) {
        JMethod getterMethod = recreateGetter(field, classOutline);
        annotateMethodWithXmlElement(getterMethod, field);
        boolean isList = isList(field.type());
        createFieldReferenceGetterBody(field, classOutline, getterMethod.body(), isList);

        //setter method update
        if (!isList) {
            JMethod setterMethod = recreateSetter(field, classOutline);
            JVar param = setterMethod.listParams()[0];
            createFieldReferenceSetterBody(field, param, setterMethod.body());
        } else {
            createFieldListCreator(field, classOutline, getterMethod, "createReference");
        }
        return true;
    }

    private void createFieldReferenceSetterBody(JFieldVar field, JVar param, JBlock body) {
        JVar cont = body.decl(CLASS_MAP.get(PrismReferenceValue.class), REFERENCE_VALUE_FIELD_NAME,
                JOp.cond(param.ne(JExpr._null()), JExpr.invoke(param, METHOD_AS_REFERENCE_VALUE), JExpr._null()));
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_UTIL_SET_REFERENCE_VALUE_AS_REF);
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
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

        JDefinedClass anonymous = createAnonListClass(field, classOutline);

        anonymous._implements(Serializable.class);
        anonymous._extends(clazz);
        JMethod constructor = anonymous.constructor(JMod.PUBLIC);
        constructor.param(CLASS_MAP.get(PrismReference.class), REFERENCE_LOCAL_VARIABLE_NAME);
        constructor.param(CLASS_MAP.get(PrismContainerValue.class), "parent");
        JBlock constructorBody = constructor.body();
        JInvocation invocation = constructorBody.invoke("super");
        invocation.arg(constructor.listParams()[0]);
        invocation.arg(constructor.listParams()[1]);

        JMethod createItem = anonymous.method(JMod.PROTECTED, type, "createItem");
        createItem.annotate(CLASS_MAP.get(Override.class));
        createItem.param(CLASS_MAP.get(PrismReferenceValue.class), "value");

        JMethod getValueFrom = anonymous.method(JMod.PROTECTED, CLASS_MAP.get(PrismReferenceValue.class), "getValueFrom");
        getValueFrom.annotate(CLASS_MAP.get(Override.class));
        getValueFrom.param(type, "value");

        JMethod willClear = anonymous.method(JMod.PROTECTED, boolean.class, "willClear");
        willClear.annotate(CLASS_MAP.get(Override.class));
        willClear.param(CLASS_MAP.get(PrismReferenceValue.class), "value");

        return anonymous;
    }

    @NotNull
    private JDefinedClass createAnonListClass(JFieldVar field, ClassOutline classOutline) {
        JDefinedClass anonymous;
        try {
            CPropertyInfo propertyInfo = classOutline.target.getProperty(field.name());
            anonymous = classOutline.implClass._class(JMod.PRIVATE | JMod.STATIC, "Anon" + propertyInfo.getName(true));
            JDocComment comment = anonymous.javadoc();
            comment.append("TODO Can't be anonymous because of NPE bug in CodeModel generator, will be fixed later.");
        } catch (JClassAlreadyExistsException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return anonymous;
    }

    private void createFieldReferenceCreateItemBody(JFieldVar field, JMethod method) {
        JClass type = ((JClass) field.type()).getTypeParameters().get(0);

        JBlock body = method.body();
        JExpression initExpr;
        initExpr = constructorExpression(method, type);
        JVar decl = body.decl(type, field.name(), initExpr);
        JInvocation invocation = body.invoke(decl, METHOD_SETUP_REFERENCE_VALUE);
        invocation.arg(method.listParams()[0]);
        body._return(decl);
    }

    private JExpression constructorExpression(JMethod method, JClass type) {
        JExpression initExpr;
        if (type.isAbstract()) {
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_CREATE_TARGET_INSTANCE);
            invocation.arg(method.listParams()[0]);
            initExpr = invocation;
        } else {
            initExpr = JExpr._new(type);
        }
        return initExpr;
    }

    private void createFieldReferenceGetValueFrom(JFieldVar field, JMethod method) {
        JBlock body = method.body();
        body._return(JExpr.invoke(method.listParams()[0], METHOD_AS_REFERENCE_VALUE));
    }

    private void createFieldReferenceWillClear(JFieldVar field, JMethod method) {
        JBlock body = method.body();
        JInvocation getObject = JExpr.invoke(method.listParams()[0], "getObject");
        body._return(getObject.eq(JExpr._null()));
    }

    private void createFieldReferenceUseWillClear(JFieldVar field, JMethod method) {
        JBlock body = method.body();
        JInvocation getObject = JExpr.invoke(method.listParams()[0], "getObject");
        body._return(getObject.ne(JExpr._null()));
    }

    private void createFieldReferenceGetterBody(JFieldVar field, ClassOutline classOutline, JBlock body,
            boolean isList) {
        JFieldRef qnameRef = JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name()));
        if (isList) {
            //if it's List<ObjectReferenceType> ...
            // PrismContainerValue pcv = asPrismContainerValue();
            JVar pcv = body.decl(CLASS_MAP.get(PrismContainerValue.class), "pcv", JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));

            // PrismReference reference = PrismForJAXBUtil.getReference(pcv, F_LINK_REF);
            JInvocation invoke = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE);
            invoke.arg(pcv);
            invoke.arg(qnameRef);
            JVar reference = body.decl(CLASS_MAP.get(PrismReference.class), REFERENCE_LOCAL_VARIABLE_NAME, invoke);

            // FocusType.AnonLinkRef + its methods
            JDefinedClass anonymous = createFieldReferenceGetterListAnon(field, classOutline);
            createFieldReferenceCreateItemBody(field, findMethod(anonymous, "createItem"));
            createFieldReferenceGetValueFrom(field, findMethod(anonymous, "getValueFrom"));
            createFieldReferenceWillClear(field, findMethod(anonymous, "willClear"));

            // return new FocusType.AnonLinkRef(reference, pcv);
            JInvocation newList = JExpr._new(anonymous);
            newList.arg(reference);
            newList.arg(pcv);
            body._return(newList);
        } else {
            //if it's ObjectReferenceType
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE_VALUE);
            invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
            invocation.arg(qnameRef);

            JVar container = body.decl(CLASS_MAP.get(PrismReferenceValue.class), REFERENCE_LOCAL_VARIABLE_NAME, invocation);

            JBlock then = body._if(container.eq(JExpr._null()))._then();
            then._return(JExpr._null());
            JVar wrapper = body.decl(field.type(), field.name(), JExpr._new(field.type()));
            invocation = body.invoke(wrapper, METHOD_SETUP_REFERENCE_VALUE);
            invocation.arg(container);
            body._return(wrapper);
        }
    }

    private JFieldVar getReferencedField(JFieldVar field, ClassOutline classOutline) {
        QName qname = getFieldReferenceUseAnnotationQName(field, classOutline);
        CPropertyInfo propertyInfo = classOutline.target.getProperty(qname.getLocalPart());
        if (propertyInfo == null) {
            throw new IllegalArgumentException("No property "+qname.getLocalPart()+" in "+classOutline.target);
        }
        return classOutline.implClass.fields().get(propertyInfo.getName(false));
    }

    // e.g. c:link (as opposed to c:linkRef)
    private boolean updateFieldReferenceUse(JFieldVar field, ClassOutline classOutline) {
        //getter method update
        JMethod getterMethod = recreateGetter(field, classOutline);
        annotateMethodWithXmlElement(getterMethod, field);
        boolean isList = isList(field.type());
        createFieldReferenceUseGetterBody(field, classOutline, getterMethod.body(), isList);

        //setter method update
        if (!isList) {
            JMethod setterMethod = recreateSetter(field, classOutline);
            createFieldReferenceUseSetterBody(field, classOutline, setterMethod.listParams()[0], setterMethod.body());
        } else {
            createFieldListCreator(field, classOutline, getterMethod, "createReference");
        }
        return true;
    }

    private void createFieldReferenceUseSetterBody(JFieldVar field, ClassOutline classOutline, JVar param,
            JBlock body) {
        JVar cont = body.decl(CLASS_MAP.get(PrismObject.class), OBJECT_LOCAL_FIELD_NAME, JOp.cond(param.ne(JExpr._null()),
                JExpr.invoke(param, METHOD_AS_PRISM_CONTAINER), JExpr._null()));
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_UTIL_SET_REFERENCE_VALUE_AS_OBJECT);
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));

        JFieldVar referencedField = getReferencedField(field, classOutline);
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(referencedField.name())));
        invocation.arg(cont);
    }

    private void createFieldReferenceUseCreateItemBody(JFieldVar field, JMethod method) {
        JClass type = ((JClass) field.type()).getTypeParameters().get(0);

        JBlock body = method.body();
        JExpression initExpr;
        initExpr = constructorExpression(method, type);
        JVar decl = body.decl(type, field.name(), initExpr);
        JInvocation invocation = body.invoke(decl, METHOD_SETUP_CONTAINER);
        invocation.arg(JExpr.invoke(method.listParams()[0], "getObject"));
        body._return(decl);
    }

    private void createFieldReferenceUseGetValueFrom(JFieldVar field, JMethod method) {
        JBlock body = method.body();

        JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_OBJECTABLE_AS_REFERENCE_VALUE);
        invocation.arg(method.listParams()[0]);
        invocation.arg(JExpr.invoke("getReference"));
        body._return(invocation);

//        JVar object = body.decl(CLASS_MAP.get(PrismObject.class), "object",
//                JExpr.invoke(method.listParams()[0], METHOD_AS_PRISM_OBJECT));
//        JVar reference = body.decl(CLASS_MAP.get(PrismReference.class), "reference",
//                JExpr.invoke("getReference"));
//        JForEach forEach = body.forEach(CLASS_MAP.get(PrismReferenceValue.class), "refValue",
//                JExpr.invoke(reference, "getValues"));
//        JBlock forBody = forEach.body();
//        JBlock then = forBody._if(object.eq(JExpr.invoke(forEach.var(), "getObject")))._then();
//        then._return(forEach.var());
//        body._return(JExpr._null());
    }

    private void createFieldReferenceUseGetterBody(JFieldVar field, ClassOutline classOutline, JBlock body,
            boolean isList) {
        JFieldVar refField = getReferencedField(field, classOutline);
        JFieldRef qnameRef = JExpr.ref(fieldFPrefixUnderscoredUpperCase(refField.name()));

        if (isList) {
            // PrismContainerValue pcv = asPrismContainerValue()
            JVar pcv = body.decl(CLASS_MAP.get(PrismContainerValue.class), "pcv", JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));

            // PrismReference reference = PrismForJAXBUtil.getReference(pcv, F_LINK_REF);
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE);
            invocation.arg(pcv);
            invocation.arg(qnameRef);
            JVar reference = body.decl(CLASS_MAP.get(PrismReference.class), REFERENCE_LOCAL_VARIABLE_NAME, invocation);

            // anonymous class (e.g. FocusType.AnonLink) and its methods
            JDefinedClass anonymous = createFieldReferenceGetterListAnon(field, classOutline);
            createFieldReferenceUseCreateItemBody(field, findMethod(anonymous, "createItem"));
            createFieldReferenceUseGetValueFrom(field, findMethod(anonymous, "getValueFrom"));
            createFieldReferenceUseWillClear(field, findMethod(anonymous, "willClear"));

            // return new FocusType.AnonLinkRef(reference, pcv);
            JInvocation newList = JExpr._new(anonymous);
            newList.arg(reference);
            newList.arg(pcv);
            body._return(newList);
        } else {
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_REFERENCE_VALUE);
            invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
            invocation.arg(qnameRef);

            JVar reference = body.decl(CLASS_MAP.get(PrismReferenceValue.class), REFERENCE_LOCAL_VARIABLE_NAME, invocation);

            JBlock then = body._if(reference.eq(JExpr._null()).cor(JExpr.invoke(reference, "getObject")
                    .eq(JExpr._null())))._then();
            then._return(JExpr._null());

            body._return(JExpr.cast(field.type(), JExpr.invoke(reference, "getObject").invoke("asObjectable")));
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

    // e.g. c:link (as opposed to c:linkRef)
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

    private boolean isContainer(JDefinedClass definedClass, Outline outline) {
        ClassOutline classOutline = findClassOutline(definedClass, outline);
        if (classOutline == null) {
            return false;
        }

        boolean isContainer = hasAnnotation(classOutline, A_PRISM_CONTAINER)
                || hasAnnotation(classOutline, A_PRISM_OBJECT);

        if (isContainer) {
            return true;
        }

        if (!(definedClass._extends() instanceof JDefinedClass)) {
            return false;
        }

        return isContainer((JDefinedClass) definedClass._extends(), outline);
    }

    private boolean isFieldTypeContainer(JFieldVar field, ClassOutline classOutline) {
        JType type = field.type();
        return isFieldTypeContainer(type, classOutline);
    }

    private boolean isFieldTypeContainer(JType type, ClassOutline classOutline) {
        if (type instanceof JDefinedClass) {
            return isContainer((JDefinedClass) type, classOutline.parent());
        } else if (isList(type)) {
            JClass clazz = (JClass) type;
            return isFieldTypeContainer(clazz.getTypeParameters().get(0), classOutline);
        }

        return false;
    }

    private boolean updateSimpleField(JFieldVar field, ClassOutline classOutline, String baseMethod) {
        //getter method update
        JMethod method = recreateGetter(field, classOutline);
        JBlock body = method.body();
        body._return(JExpr.invoke(baseMethod).invoke(getGetterMethodName(classOutline, field)));
        //setter method update
        method = recreateSetter(field, classOutline);
        body = method.body();
        JInvocation invocation = body.invoke(JExpr.invoke(baseMethod), getSetterMethodName(classOutline, field));
        invocation.arg(method.listParams()[0]);

        return true;
    }

    private boolean updateContainerFieldType(JFieldVar field, ClassOutline classOutline) {
        //getter method update
        JMethod getterMethod = recreateGetter(field, classOutline);
        annotateMethodWithXmlElement(getterMethod, field);
        createContainerFieldGetterBody(field, classOutline, getterMethod);

        //setter method update
        if (!isList(field.type())) {
            JMethod setterMethod = recreateSetter(field, classOutline);
            createContainerFieldSetterBody(field, classOutline, setterMethod);
        } else {
            createFieldListCreator(field, classOutline, getterMethod, "createContainer");
        }
        return true;
    }

    private void createFluentFieldMethods(JFieldVar field, ClassOutline targetClass, ClassOutline fieldsFromClass) {
        print("createFluentFieldMethods for " + field.name() + " on " + targetClass.implClass.fullName() + " (from " + fieldsFromClass.implClass.fullName() + ")");
        JMethod fluentSetter = createFluentSetter(field, targetClass, fieldsFromClass);
        createMethodStringVersion(field, targetClass, fluentSetter);

        // FIXME ugly hack - we create beginXYZ only for our own structures
        // TODO not for all!
        JType basicType = getContentType(field);
        if (basicType.fullName().startsWith("com.evolveum.") && isInstantiable(basicType)) {
            createFluentBegin(field, targetClass, fieldsFromClass, fluentSetter);
        }
    }

    /**
     * e.g. name(PolyStringType value) -> name(String value) { return name(PolyStringType.fromOrig(value); }
     * time(XmlGregorianCalendar value) -> time(String value) { return time(XmlTypeConverter.createXMLGregorianCalendar(value); }
     * also ObjectReferenceType: create by oid + type + [relation]
     */
    private void createMethodStringVersion(JFieldVar field, ClassOutline classOutline, JMethod originalMethod) {
        Function<JVar,JExpression> expression;
        JVar param = originalMethod.params().get(0);
        if (param.type().fullName().equals(PolyStringType.class.getName())) {
            expression = value -> JExpr.invoke(originalMethod)
                    .arg(CLASS_MAP.get(PolyStringType.class).staticInvoke("fromOrig").arg(value));
        } else if (param.type().fullName().equals(XMLGregorianCalendar.class.getName())) {
            expression = value -> JExpr.invoke(originalMethod)
                    .arg(CLASS_MAP.get(XmlTypeConverter.class).staticInvoke("createXMLGregorianCalendar").arg(value));
        } else if (param.type().name().equals(ObjectReferenceType.class.getSimpleName())) {
            createReferenceStringVersionOidType(field, classOutline, originalMethod, param.type());
            createReferenceStringVersionOidTypeRelation(field, classOutline, originalMethod, param.type());
            return;
        } else {
            return;
        }
        JMethod newMethod = classOutline.implClass.method(JMod.PUBLIC, originalMethod.type(), originalMethod.name());
        JVar value = newMethod.param(String.class, "value");
        newMethod.body()._return(expression.apply(value));
    }

    private void createReferenceStringVersionOidType(JFieldVar field, ClassOutline classOutline, JMethod originalMethod, JType objectReferenceType) {
        JMethod newMethod = classOutline.implClass.method(JMod.PUBLIC, originalMethod.type(), originalMethod.name());
        JVar oid = newMethod.param(String.class, "oid");
        JVar type = newMethod.param(QName.class, "type");
        JBlock body = newMethod.body();
        JVar refVal = body.decl(CLASS_MAP.get(PrismReferenceValue.class), "refVal",
                JExpr._new(CLASS_MAP.get(PrismReferenceValueImpl.class))
                        .arg(oid).arg(type));
        JVar ort = body.decl(objectReferenceType, "ort", JExpr._new(objectReferenceType));
        body.invoke(ort, METHOD_SETUP_REFERENCE_VALUE).arg(refVal);
        body._return(JExpr.invoke(originalMethod).arg(ort));
    }

    private void createReferenceStringVersionOidTypeRelation(JFieldVar field, ClassOutline classOutline, JMethod originalMethod, JType objectReferenceType) {
        JMethod newMethod = classOutline.implClass.method(JMod.PUBLIC, originalMethod.type(), originalMethod.name());
        JVar oid = newMethod.param(String.class, "oid");
        JVar type = newMethod.param(QName.class, "type");
        JVar relation = newMethod.param(QName.class, "relation");
        JBlock body = newMethod.body();
        JVar refVal = body.decl(CLASS_MAP.get(PrismReferenceValue.class), "refVal",
                JExpr._new(CLASS_MAP.get(PrismReferenceValueImpl.class))
                        .arg(oid).arg(type));
        body.invoke(refVal, "setRelation").arg(relation);
        JVar ort = body.decl(objectReferenceType, "ort", JExpr._new(objectReferenceType));
        body.invoke(ort, METHOD_SETUP_REFERENCE_VALUE).arg(refVal);
        body._return(JExpr.invoke(originalMethod).arg(ort));
    }

    private boolean isInstantiable(JType type) {
        if (!(type instanceof JClass)) {
            return false;
        }
        JClass clazz = (JClass) type;
        if (clazz.isAbstract()) {
            return false;
        }
        if (clazz instanceof JDefinedClass) {
            if (hasAnnotationClass(((JDefinedClass) clazz), XmlEnum.class)) {
                return false;
            }
        }
        return true;
    }

    private void createContainerFieldSetterBody(JFieldVar field, ClassOutline classOutline, JMethod method) {
        JVar param = method.listParams()[0];
        JBlock body = method.body();

        JVar cont;
        if (isPrismContainer(param.type(), classOutline.parent())) {
            cont = body.decl(CLASS_MAP.get(PrismContainerValue.class), FIELD_CONTAINER_VALUE_LOCAL_VAR_NAME,
                    JOp.cond(param.ne(JExpr._null()), JExpr.invoke(param, METHOD_AS_PRISM_CONTAINER_VALUE), JExpr._null()));
        } else {
            cont = body.decl(CLASS_MAP.get(PrismContainerValue.class), FIELD_CONTAINER_VALUE_LOCAL_VAR_NAME,
                    JOp.cond(param.ne(JExpr._null()), JExpr.invoke(param, METHOD_AS_PRISM_CONTAINER_VALUE), JExpr._null()));
        }
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_UTIL_SET_FIELD_CONTAINER_VALUE);
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(cont);
    }

    /*
        public AssignmentType beginAssignment() {
        AssignmentType value = new AssignmentType();
        addAssignment(value);
        return value;
    }
     */

    private JMethod createFluentBegin(JFieldVar field, ClassOutline targetClass, ClassOutline fieldsFromClass,
            JMethod fluentSetter) {

        JType valueClass = getContentType(field);

        // e.g. public AssignmentType beginAssignment() {
        String methodName = getMethodName(fieldsFromClass, field, "begin");
        JMethod beginMethod = targetClass.implClass.method(JMod.PUBLIC, valueClass, methodName);
        JBlock body = beginMethod.body();

        // AssignmentType value = new AssignmentType();
        JVar value = body.decl(valueClass, "value", JExpr._new(valueClass));

        // addAssignment(value);
        body.invoke(fluentSetter).arg(value);

        // return value;
        body._return(value);

        return beginMethod;
    }

    /*

    public <X> X endFocus() {
        return (X) ((PrismContainerValue) ((PrismContainer) asPrismContainerValue().getParent()).getParent()).asContainerable();
    }
     */

    private JMethod createContainerFluentEnd(ClassOutline classOutline) {
        String methodName = "end";
        JMethod method = classOutline.implClass.method(JMod.PUBLIC, (JType) null, methodName);
        method.type(method.generify("X"));
        JBlock body = method.body();

        body._return(JExpr.cast(method.type(),
                JExpr.invoke(JExpr.cast(CLASS_MAP.get(PrismContainerValue.class),
                                JExpr.invoke(JExpr.cast(CLASS_MAP.get(PrismContainer.class),
                                        JExpr.invoke(JExpr.invoke("asPrismContainerValue"),"getParent")), "getParent")),
                "asContainerable")));

        return method;
    }

    private JMethod createReferenceFluentEnd(ClassOutline classOutline) {
        String methodName = "end";
        JMethod method = classOutline.implClass.method(JMod.PUBLIC, (JType) null, methodName);
        method.type(method.generify("X"));
        JBlock body = method.body();

        body._return(JExpr.cast(method.type(),
                JExpr.invoke(JExpr.cast(CLASS_MAP.get(PrismContainerValue.class),
                                JExpr.invoke(JExpr.cast(CLASS_MAP.get(PrismReference.class),
                                        JExpr.invoke(JExpr.invoke("asReferenceValue"),"getParent")), "getParent")),
                "asContainerable")));

        return method;
    }

    private JType getContentType(JFieldVar field) {
        boolean multi = isList(field.type());
        JType valueClass;
        if (multi) {
            valueClass = ((JClass) field.type()).getTypeParameters().get(0);
        } else {
            valueClass = field.type();
        }
        return valueClass;
    }

    private JMethod createFluentSetter(JFieldVar field, ClassOutline targetClass, ClassOutline fieldsFromClass) {
        // e.g. UserType name(String value)
        String methodName = getFluentSetterMethodName(fieldsFromClass, field);
        JMethod fluentSetter = targetClass.implClass.method(JMod.PUBLIC, targetClass.implClass, methodName);
        JType contentType = getContentType(field);
        JVar value = fluentSetter.param(contentType, "value");
        JBlock body = fluentSetter.body();

        if (isList(field.type())) {
            // getAssignment().add(value)
            JMethod getterMethod = findGetterMethod(field, fieldsFromClass);
            body.invoke(JExpr.invoke(getterMethod), "add").arg(value);
        } else {
            // setName(value);
            JMethod setterMethod = findSetterMethod(field, fieldsFromClass);
            body.invoke(setterMethod).arg(value);
        }

        // return this;
        body._return(JExpr._this());

        return fluentSetter;
    }

    private JMethod createFluentAdder(JFieldVar field, ClassOutline targetClass, ClassOutline fieldsFromClass) {
        JType valueType = getContentType(field);

        // e.g. FocusType assignment(AssignmentType value)
        //String methodName = getMethodName(fieldsFromClass, field, "add");
        String methodName = getFluentSetterMethodName(fieldsFromClass, field);
        JMethod fluentSetter = targetClass.implClass.method(JMod.PUBLIC, targetClass.implClass, methodName);
        JVar value = fluentSetter.param(valueType, "value");
        JBlock body = fluentSetter.body();


        // return this;
        body._return(JExpr._this());

        return fluentSetter;
    }

    private JMethod createFieldListCreator(JFieldVar field, ClassOutline classOutline, JMethod getterMethod, String itemCreationMethodName) {
        JFieldRef qnameRef = JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name()));        // F_ASSIGNMENT

        // e.g. List<AssignmentType> createAssignmentList()
        String methodName = getMethodName(classOutline, field, "create") + "List";
        JMethod method = classOutline.implClass.method(JMod.PUBLIC, field.type(), methodName);
        JBlock body = method.body();

        // PrismForJAXBUtil.createContainer(asPrismContainerValue(), F_ASSIGNMENT)
        body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class), itemCreationMethodName)
                .arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE))
                .arg(qnameRef);

        // return getAssignment();
        body._return(JExpr.invoke(getterMethod));

        return method;
    }

    private JDefinedClass createFieldContainerGetterListAnon(JFieldVar field, ClassOutline classOutline) {
        //add generics type to list field.type.getTypeParameters()...
        JClass type = ((JClass) field.type()).getTypeParameters().get(0);
        JClass clazz = CLASS_MAP.get(PrismContainerArrayList.class).narrow(type);

        JDefinedClass anonymous = createAnonListClass(field, classOutline);

        anonymous._implements(Serializable.class);
        anonymous._extends(clazz);

        JClass list = (JClass) field.type();
        JClass listType = list.getTypeParameters().get(0);

        JMethod constructor = anonymous.constructor(JMod.PUBLIC);
        constructor.param(CLASS_MAP.get(PrismContainer.class), "container");
        constructor.param(CLASS_MAP.get(PrismContainerValue.class), "parent");
        JBlock constructorBody = constructor.body();
        JInvocation invocation = constructorBody.invoke("super");
        invocation.arg(constructor.listParams()[0]);
        invocation.arg(constructor.listParams()[1]);

        // Default constructor, for deserialization
        JMethod defaultConstructor = anonymous.constructor(JMod.PUBLIC);
        JBlock defaultConstructorBody = defaultConstructor.body();
        defaultConstructorBody.invoke("super");

        JMethod createItem = anonymous.method(JMod.PROTECTED, listType, "createItem");
        createItem.annotate(CLASS_MAP.get(Override.class));
        createItem.param(CLASS_MAP.get(PrismContainerValue.class), "value");

        JMethod getValueFrom = anonymous.method(JMod.PROTECTED, CLASS_MAP.get(PrismContainerValue.class), "getValueFrom");
        getValueFrom.annotate(CLASS_MAP.get(Override.class));
        getValueFrom.param(listType, "value");

        return anonymous;
    }

    private void createFieldContainerCreateItemBody(JFieldVar field, JMethod method) {
        JClass list = (JClass) field.type();
        JClass listType = list.getTypeParameters().get(0);

        JBlock body = method.body();
        JVar decl = body.decl(listType, field.name(), constructorExpression(method, listType));
        JInvocation invocation = body.invoke(decl, METHOD_SETUP_CONTAINER_VALUE);
        invocation.arg(method.listParams()[0]);
        body._return(decl);
    }

    private void createFieldContainerGetValueFrom(JFieldVar field, JMethod method) {
        JBlock body = method.body();
        body._return(JExpr.invoke(method.listParams()[0], METHOD_AS_PRISM_CONTAINER_VALUE));
    }

    private void createContainerFieldGetterBody(JFieldVar field, ClassOutline classOutline, JMethod method) {
        JBlock body = method.body();

        List<JAnnotationUse> existingAnnotations = (List<JAnnotationUse>) getAnnotations(method);
        for (JAnnotationUse annotation : existingAnnotations) {
            if (isAnnotationTypeOf(annotation, XmlElement.class)) {
                Field mfield = getField(JAnnotationUse.class, "memberValues");
                mfield.setAccessible(true);
                Map<String, Object> map;
                try {
                    map = (Map<String, Object>) mfield.get(annotation);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                mfield.setAccessible(false);
                map.remove("name");
                annotation.param("name", normalizeFieldName(field.name()));
            }
        }

        if (isList(field.type())) {
            //JClass list = (JClass) field.type();
            //JClass listType = list.getTypeParameters().get(0);

            // PrismContainerValue pcv = asPrismContainerValue()
            JVar pcvVar = body.decl(CLASS_MAP.get(PrismContainerValue.class), "pcv", JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));

            // PrismContainer container = PrismForJAXBUtil.getContainer(pcv, F_ASSIGNMENT);
            JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_CONTAINER);
            invocation.arg(pcvVar);
            invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
            JVar containerVar = body.decl(CLASS_MAP.get(PrismContainer.class), "container", invocation);

            // anonymous class (e.g. FocusType.AnonAssignment and its methods)
            JDefinedClass anonymousClass = createFieldContainerGetterListAnon(field, classOutline);
            createFieldContainerCreateItemBody(field, findMethod(anonymousClass, "createItem"));
            createFieldContainerGetValueFrom(field, findMethod(anonymousClass, "getValueFrom"));

            // return new FocusType.AnonAssignment(container, pcv);
            JInvocation newList = JExpr._new(anonymousClass);
            newList.arg(containerVar);
            newList.arg(pcvVar);
            body._return(newList);

            return;
        }

        JInvocation invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_FIELD_SINGLE_CONTAINERABLE);
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(JExpr.dotclass((JClass) field.type()));
        body._return(invocation);
    }

    private boolean isPrismContainer(JType type, Outline outline) {
        if (!(type instanceof JDefinedClass)) {
            return false;
        }

        ClassOutline classOutline = findClassOutline((JDefinedClass) type, outline);
        if (classOutline == null) {
            return false;
        }

        return hasParentAnnotation(classOutline, A_PRISM_OBJECT);
    }

    private boolean isList(JType type) {
        boolean isList = false;
        if (type instanceof JClass) {
            isList = CLASS_MAP.get(List.class).equals(((JClass) type).erasure());
        }

        return isList;
    }

    private void annotateFieldAsRaw(JFieldVar fieldVar) {
        fieldVar.annotate(CLASS_MAP.get(Raw.class));
    }

    private void annotateMethodWithXmlElement(JMethod method, JFieldVar field) {
        List<JAnnotationUse> existingAnnotations = getAnnotations(method);
        for (JAnnotationUse annotation : existingAnnotations) {
            if (isAnnotationTypeOf(annotation, XmlAttribute.class) ||
                    isAnnotationTypeOf(annotation, XmlAnyElement.class) ||
                    isAnnotationTypeOf(annotation, XmlAnyAttribute.class)) {
                return;
            }
        }

        JAnnotationUse xmlElement = null;
        for (JAnnotationUse annotation : existingAnnotations) {
            if (!isAnnotationTypeOf(annotation, XmlElement.class)) {
                continue;
            }
            xmlElement = annotation;
            break;
        }
        if (xmlElement == null) {
            xmlElement = method.annotate(CLASS_MAP.get(XmlElement.class));
        }
        xmlElement.param("name", field.name());
    }

    private boolean updateField(JFieldVar field, ClassOutline classOutline) {
        //update getter
        JMethod getterMethod = recreateGetter(field, classOutline);
        annotateMethodWithXmlElement(getterMethod, field);
        boolean isList = isList(field.type());
        createFieldGetterBody(getterMethod, field, isList);

        //update setter
        if (!isList) {
            JMethod setterMethod = recreateSetter(field, classOutline);
            createFieldSetterBody(setterMethod, field);
        } else {
            if (!hasAnnotationClass(field, XmlAnyElement.class)) {
                createFieldListCreator(field, classOutline, getterMethod, "createProperty");
            }
        }
        return true;
    }

    private void createFieldSetterBody(JMethod method, JFieldVar field) {
        JBlock body = method.body();
        JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class),
                METHOD_PRISM_UTIL_SET_PROPERTY_VALUE);
        //push arguments
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
        invocation.arg(JExpr.ref(fieldFPrefixUnderscoredUpperCase(field.name())));
        invocation.arg(method.listParams()[0]);
    }

    private <T> boolean hasAnnotationClass(JAnnotatable method, Class<T> annotationType) {
        List<JAnnotationUse> annotations = getAnnotations(method);
        for (JAnnotationUse annotation : annotations) {
            if (isAnnotationTypeOf(annotation, annotationType)) {
                return true;
            }
        }

        return false;
    }

    private void createFieldGetterBody(JMethod method, JFieldVar field, boolean isList) {
        JBlock body = method.body();
        JInvocation invocation;
        if (hasAnnotationClass(method, XmlAnyElement.class)) {
            //handling xsd any
            invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_GET_ANY);
            invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));

            JClass clazz = (JClass) field.type();
            invocation.arg(JExpr.dotclass(clazz.getTypeParameters().get(0)));
            body._return(invocation);
            return;
        }

        if (isList) {
            invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_PROPERTY_VALUES);
        } else {
            invocation = CLASS_MAP.get(PrismForJAXBUtil.class).staticInvoke(METHOD_PRISM_UTIL_GET_PROPERTY_VALUE);
        }
        //push arguments
        invocation.arg(JExpr.invoke(METHOD_AS_PRISM_CONTAINER_VALUE));
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

    public static void print(String s) {
        if (PRINT_DEBUG_INFO) {
            System.out.println(s);
        }
    }

    public static void printWarning(String s) {
        System.out.println(s);
    }


    private void createAcceptMethods(Outline outline) {
        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            if (!hasParentAnnotation(classOutline, A_PRISM_OBJECT) && !hasParentAnnotation(classOutline, A_PRISM_CONTAINER)) {
                createAcceptMethod(classOutline);
            }
        }
    }

    private void createAcceptMethod(ClassOutline classOutline) {
        JDefinedClass impl = classOutline.implClass;
        impl._implements(JaxbVisitable.class);

        JMethod acceptMethod = impl.method(JMod.PUBLIC, classOutline.parent().getCodeModel().VOID, METHOD_ACCEPT);
        acceptMethod.annotate(Override.class);
        JVar visitor = acceptMethod.param(JaxbVisitor.class, "visitor");
        JBlock body = acceptMethod.body();

        if (classOutline.getSuperClass() != null) {
            JInvocation superInvocation = body.invoke(JExpr._super(), METHOD_ACCEPT);
            superInvocation.arg(visitor);
        } else {
            JInvocation visitInvocation = body.invoke(visitor, METHOD_VISIT);
            visitInvocation.arg(JExpr._this());
        }

        for (JFieldVar fieldVar : impl.fields().values()) {
            if ((fieldVar.mods().getValue() & (JMod.STATIC | JMod.FINAL)) == 0) {
                JInvocation invocation = body.staticInvoke(CLASS_MAP.get(PrismForJAXBUtil.class), METHOD_PRISM_UTIL_ACCEPT);
                invocation.arg(fieldVar);
                invocation.arg(visitor);
            }
        }
    }

}
