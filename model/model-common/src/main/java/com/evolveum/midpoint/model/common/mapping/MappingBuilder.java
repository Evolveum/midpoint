/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder is used to construct a configuration of Mapping object, which - after building - becomes
 * immutable.
 * <p>
 * In order to provide backward-compatibility with existing use of Mapping object, the builder has
 * also traditional setter methods. Both setters and "builder-style" methods MODIFY existing Builder
 * object (i.e. they do not create a new one).
 * <p>
 * TODO decide on which style of setters to keep (setters vs builder-style).
 */
@SuppressWarnings({ "unused", "BooleanMethodIsAlwaysInverted", "UnusedReturnValue", "WeakerAccess" })
public final class MappingBuilder<V extends PrismValue, D extends ItemDefinition> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingImpl.class);

    private ExpressionFactory expressionFactory;
    private final ExpressionVariables variables = new ExpressionVariables();
    private MappingType mappingBean;
    private MappingKindType mappingKind;
    private ItemPath implicitSourcePath; // for tracing purposes
    private ItemPath implicitTargetPath; // for tracing purposes
    private ObjectResolver objectResolver;
    private SecurityContextManager securityContextManager;
    private Source<?, ?> defaultSource;
    private final List<Source<?, ?>> additionalSources = new ArrayList<>();
    private D defaultTargetDefinition;
    private ExpressionProfile expressionProfile;
    private ItemPath defaultTargetPath;
    private Collection<V> originalTargetValues;
    private ObjectDeltaObject<?> sourceContext;
    private PrismObjectDefinition<?> targetContext;
    private OriginType originType;
    private ObjectType originObject;
    private ConfigurableValuePolicySupplier valuePolicySupplier;
    private VariableProducer variableProducer;
    private MappingPreExpression mappingPreExpression;
    private boolean conditionMaskOld = true;
    private boolean conditionMaskNew = true;
    private XMLGregorianCalendar now;
    private XMLGregorianCalendar defaultReferenceTime;
    private boolean profiling;
    private String contextDescription;
    private QName mappingQName;
    private RefinedObjectClassDefinition refinedObjectClassDefinition;
    private PrismContext prismContext;

    public MappingImpl<V, D> build() {
        return new MappingImpl<>(this);
    }

    //region Plain setters
    public MappingBuilder<V, D> expressionFactory(ExpressionFactory val) {
        expressionFactory = val;
        return this;
    }

    public MappingBuilder<V, D> variablesFrom(ExpressionVariables val) {
        variables.addVariableDefinitions(val);
        return this;
    }

    public MappingBuilder<V, D> mappingBean(MappingType val) {
        mappingBean = val;
        return this;
    }

    public MappingBuilder<V, D> mappingKind(MappingKindType val) {
        mappingKind = val;
        return this;
    }

    public MappingBuilder<V, D> implicitSourcePath(ItemPath val) {
        implicitSourcePath = val;
        return this;
    }

    public MappingBuilder<V, D> implicitTargetPath(ItemPath val) {
        implicitTargetPath = val;
        return this;
    }

    public MappingBuilder<V, D> objectResolver(ObjectResolver val) {
        objectResolver = val;
        return this;
    }

    public MappingBuilder<V, D> securityContextManager(SecurityContextManager val) {
        securityContextManager = val;
        return this;
    }

    public MappingBuilder<V, D> defaultSource(Source<?, ?> val) {
        defaultSource = val;
        return this;
    }

    public MappingBuilder<V, D> defaultTargetDefinition(D val) {
        defaultTargetDefinition = val;
        return this;
    }

    public MappingBuilder<V, D> expressionProfile(ExpressionProfile val) {
        expressionProfile = val;
        return this;
    }

    public MappingBuilder<V, D> defaultTargetPath(ItemPath val) {
        defaultTargetPath = val;
        return this;
    }

    public MappingBuilder<V, D> originalTargetValues(Collection<V> values) {
        originalTargetValues = values;
        return this;
    }

    public MappingBuilder<V, D> sourceContext(ObjectDeltaObject<?> val) {
        if (val.getDefinition() == null) {
            throw new IllegalArgumentException("Attempt to set mapping source context without a definition");
        }
        sourceContext = val;
        return this;
    }

    public MappingBuilder<V, D> targetContext(PrismObjectDefinition<?> val) {
        targetContext = val;
        return this;
    }

    public MappingBuilder<V, D> originType(OriginType val) {
        originType = val;
        return this;
    }

    public MappingBuilder<V, D> originObject(ObjectType val) {
        originObject = val;
        return this;
    }

    public MappingBuilder<V, D> valuePolicySupplier(ConfigurableValuePolicySupplier val) {
        valuePolicySupplier = val;
        return this;
    }

    public MappingBuilder<V, D> variableResolver(VariableProducer<V> variableProducer) {
        this.variableProducer = variableProducer;
        return this;
    }

    public MappingBuilder<V, D> mappingPreExpression(MappingPreExpression mappingPreExpression) {
        this.mappingPreExpression = mappingPreExpression;
        return this;
    }

    public MappingBuilder<V, D> conditionMaskOld(boolean val) {
        conditionMaskOld = val;
        return this;
    }

    public MappingBuilder<V, D> conditionMaskNew(boolean val) {
        conditionMaskNew = val;
        return this;
    }

    public MappingBuilder<V, D> now(XMLGregorianCalendar val) {
        now = val;
        return this;
    }

    public MappingBuilder<V, D> defaultReferenceTime(XMLGregorianCalendar val) {
        defaultReferenceTime = val;
        return this;
    }

    public MappingBuilder<V, D> profiling(boolean val) {
        profiling = val;
        return this;
    }

    public MappingBuilder<V, D> contextDescription(String val) {
        contextDescription = val;
        return this;
    }

    public MappingBuilder<V, D> mappingQName(QName val) {
        mappingQName = val;
        return this;
    }

    public MappingBuilder<V, D> refinedObjectClassDefinition(RefinedObjectClassDefinition val) {
        refinedObjectClassDefinition = val;
        return this;
    }

    public MappingBuilder<V, D> prismContext(PrismContext val) {
        prismContext = val;
        return this;
    }
    //endregion

    public MappingBuilder<V, D> rootNode(ObjectReferenceType objectRef) {
        return addVariableDefinition(null, objectRef);
    }

    public MappingBuilder<V, D> rootNode(ObjectDeltaObject<?> odo) {
        return addVariableDefinition(null, odo);
    }

    public <O extends ObjectType> MappingBuilder<V, D> rootNode(O objectType, PrismObjectDefinition<O> definition) {
        variables.put(null, objectType, definition);
        return this;
    }

    public <O extends ObjectType> MappingBuilder<V, D> rootNode(PrismObject<? extends ObjectType> mpObject, PrismObjectDefinition<O> definition) {
        variables.put(null, mpObject, definition);
        return this;
    }

    public MappingBuilder<V, D> addVariableDefinition(ExpressionVariableDefinitionType varDef) throws SchemaException {
        if (varDef.getObjectRef() != null) {
            ObjectReferenceType ref = varDef.getObjectRef();
            ref.setType(getPrismContext().getSchemaRegistry().qualifyTypeName(ref.getType()));
            return addVariableDefinition(varDef.getName().getLocalPart(), ref);
        } else if (varDef.getValue() != null) {
            // This is raw value. We do have definition here. The best we can do is autodetect.
            // Expression evaluation code will do that as a fallback behavior.
            return addVariableDefinition(varDef.getName().getLocalPart(), varDef.getValue(), Object.class);
        } else {
            LOGGER.warn("Empty definition of variable {} in {}, ignoring it", varDef.getName(), getContextDescription());
            return this;
        }
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, ObjectReferenceType objectRef) {
        return addVariableDefinition(name, objectRef, objectRef.asReferenceValue().getDefinition());
    }

    public <O extends ObjectType> MappingBuilder<V, D> addVariableDefinition(String name, O objectType, Class<O> expectedClass) {
        // Maybe determine definition from schema registry here in case that object is null. We can do that here.
        variables.putObject(name, objectType, expectedClass);
        return this;
    }

    public <O extends ObjectType> MappingBuilder<V, D> addVariableDefinition(String name, PrismObject<O> midpointObject, Class<O> expectedClass) {
        // Maybe determine definition from schema registry here in case that object is null. We can do that here.
        variables.putObject(name, midpointObject, expectedClass);
        return this;
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, String value) {
        MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                new QName(SchemaConstants.NS_C, name), PrimitiveType.STRING.getQname());
        return addVariableDefinition(name, value, def);
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, boolean value) {
        MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                new QName(SchemaConstants.NS_C, name), PrimitiveType.BOOLEAN.getQname());
        return addVariableDefinition(name, value, def);
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, int value) {
        MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                new QName(SchemaConstants.NS_C, name), PrimitiveType.INT.getQname());
        return addVariableDefinition(name, value, def);
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, PrismValue value) {
        return addVariableDefinition(name, value, value.getParent().getDefinition());
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, ObjectDeltaObject<?> value) {
        PrismObjectDefinition<?> definition = value.getDefinition();
        if (definition == null) {
            throw new IllegalArgumentException("Attempt to set variable '" + name + "' as ODO without a definition: " + value);
        }
        return addVariableDefinition(name, value, definition);
    }

    // mainVariable of "null" means the default source
    public MappingBuilder<V, D> addAliasRegistration(String alias, @Nullable String mainVariable) {
        variables.registerAlias(alias, mainVariable);
        return this;
    }

    public MappingBuilder<V, D> addVariableDefinitions(VariablesMap extraVariables) {
        variables.putAll(extraVariables);
        return this;
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, Object value, ItemDefinition definition) {
        variables.put(name, value, definition);
        return this;
    }

    public MappingBuilder<V, D> addVariableDefinition(String name, Object value, Class<?> typeClass) {
        variables.put(name, value, typeClass);
        return this;
    }

    public boolean hasVariableDefinition(String varName) {
        return variables.containsKey(varName);
    }

    public boolean isApplicableToChannel(String channel) {
        return MappingImpl.isApplicableToChannel(mappingBean, channel);
    }

    public MappingBuilder<V, D> additionalSource(Source<?, ?> source) {
        additionalSources.add(source);
        return this;
    }

    public MappingStrengthType getStrength() {
        return MappingImpl.getStrength(mappingBean);
    }

    //region Plain getters
    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public ExpressionVariables getVariables() {
        return variables;
    }

    public MappingType getMappingBean() {
        return mappingBean;
    }

    public MappingKindType getMappingKind() {
        return mappingKind;
    }

    public ItemPath getImplicitSourcePath() {
        return implicitSourcePath;
    }

    public ItemPath getImplicitTargetPath() {
        return implicitTargetPath;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    public Source<?, ?> getDefaultSource() {
        return defaultSource;
    }

    public List<Source<?, ?>> getAdditionalSources() {
        return additionalSources;
    }

    public D getDefaultTargetDefinition() {
        return defaultTargetDefinition;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public ItemPath getDefaultTargetPath() {
        return defaultTargetPath;
    }

    public Collection<V> getOriginalTargetValues() {
        return originalTargetValues;
    }

    public ObjectDeltaObject<?> getSourceContext() {
        return sourceContext;
    }

    public PrismObjectDefinition<?> getTargetContext() {
        return targetContext;
    }

    public OriginType getOriginType() {
        return originType;
    }

    public ObjectType getOriginObject() {
        return originObject;
    }

    public ConfigurableValuePolicySupplier getValuePolicySupplier() {
        return valuePolicySupplier;
    }

    public VariableProducer getVariableProducer() {
        return variableProducer;
    }

    public MappingPreExpression getMappingPreExpression() {
        return mappingPreExpression;
    }

    public boolean isConditionMaskOld() {
        return conditionMaskOld;
    }

    public boolean isConditionMaskNew() {
        return conditionMaskNew;
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    public XMLGregorianCalendar getDefaultReferenceTime() {
        return defaultReferenceTime;
    }

    public boolean isProfiling() {
        return profiling;
    }

    public String getContextDescription() {
        return contextDescription;
    }

    public QName getMappingQName() {
        return mappingQName;
    }

    public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
        return refinedObjectClassDefinition;
    }
    //endregion
}
