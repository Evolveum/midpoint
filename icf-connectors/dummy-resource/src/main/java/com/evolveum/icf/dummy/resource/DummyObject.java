/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import static java.util.Collections.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * Represents an object on this dummy resource.
 *
 * TODO Treat null attribute values more consistently.
 *
 * @author Radovan Semancik
 */
@SuppressWarnings("UnusedReturnValue") // Occurs quite often. Not everyone makes use of the fluent API.
public abstract class DummyObject implements DebugDumpable {

    /**
     * Randomly generated or externally-supplied ID. This is the primary key for all objects on the resource,
     * so it must be unique resource-wide.
     *
     * @see UidMode
     */
    private String id;

    /** Client-supplied object name. */
    private String name;

    private final Map<String,Set<Object>> attributes = new ConcurrentHashMap<>();
    private Boolean enabled = true;
    private Date validFrom = null;
    private Date validTo = null;
    private Date lastLoginDate = null;
    private String lastModifier;

    /**
     * Normally set up when adding this object to resource; but some utility methods require this value,
     * so it can be provided at object construction time as well.
     */
    protected DummyResource resource;

    private boolean presentOnResource;

    /** Present only if hierarchy is supported. Set when object is added on the resource (we need to know the normalization). */
    private HierarchicalName normalizedHierarchicalName;

    private final Set<String> auxiliaryObjectClassNames = ConcurrentHashMap.newKeySet();

    private BreakMode modifyBreakMode = null;

    public DummyObject() {
    }

    public String getId() {
        return id;
    }

    public DummyObject setId(String id) {
        this.id = id;
        return this;
    }

    public DummyObject(String name) {
        this.name = name;
    }

    public DummyObject(String name, DummyResource dummyResource) {
        this.name = name;
        this.resource = dummyResource;
    }

    public DummyResource getResource() {
        return resource;
    }

    public DummyResource getResourceRequired() {
        return MiscUtil.stateNonNull(resource, "No resource set for %s", this);
    }

    void setPresentOnResource(DummyResource resource) {
        this.resource = resource;
        presentOnResource = true;
    }

    void setNotPresentOnResource() {
        presentOnResource = false;
    }

    public String getName() {
        return name;
    }

    public DummyObject setName(String username) {
        this.name = username;
        if (resource != null) {
            resource.updateNormalizedHierarchicalName(this);
        }
        return this;
    }

    /**
     * The normalized hierarchical name cannot be computed by the object itself. It needs the resource
     * (normalization, hierarchy support). This method is called from there. It is intentionally package-private.
     */
    void setNormalizedHierarchicalName(HierarchicalName normalizedHierarchicalName) {
        this.normalizedHierarchicalName = normalizedHierarchicalName;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public DummyObject setEnabled(Boolean enabled)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();
        this.enabled = enabled;
        recordModify("_ENABLED", null, null, singletonList(enabled));
        return this;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public DummyObject setLastLoginDate(Date lastLoginDate)
            throws ConflictException, FileNotFoundException, SchemaViolationException, ConnectException, InterruptedException {

        checkModifyBreak();
        delayOperation();

        this.lastLoginDate = lastLoginDate;

        recordModify("_LAST_LOGIN_DATE", null, null, singletonList(lastLoginDate));

        return this;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public DummyObject setValidFrom(Date validFrom)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();
        this.validFrom = validFrom;
        recordModify("_VALID_FROM", null, null, singletonList(validFrom));
        return this;
    }

    public Date getValidTo() {
        return validTo;
    }

    public DummyObject setValidTo(Date validTo)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();
        this.validTo = validTo;
        recordModify("_VALID_TO", null, null, singletonList(validTo));
        return this;
    }

    public String getLastModifier() {
        return lastModifier;
    }

    public DummyObject setLastModifier(String lastModifier) {
        this.lastModifier = lastModifier;
        return this;
    }

    public BreakMode getModifyBreakMode() {
        return modifyBreakMode;
    }

    public DummyObject setModifyBreakMode(BreakMode modifyBreakMode) {
        this.modifyBreakMode = modifyBreakMode;
        return this;
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public <T> Set<T> getAttributeValues(String attrName, Class<T> type) {
        //noinspection unchecked
        return (Set<T>) attributes.get(attrName);
    }

    public <T> T getAttributeValue(String attrName, Class<T> type) {
        Set<T> values = getAttributeValues(attrName, type);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException("Attempt to fetch single value from a multi-valued attribute " + attrName);
        }
        return values.iterator().next();
    }

    public String getAttributeValue(String attrName) {
        return getAttributeValue(attrName, String.class);
    }

    public DummyObject replaceAttributeValue(String name, Object value)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return replaceAttributeValues(name, createCollection(value));
    }

    private Set<Object> createCollection(Object value) {
        return value != null ? singleton(value) : emptySet();
    }

    public DummyObject replaceAttributeValues(String name, Collection<Object> values)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();

        Set<Object> currentValues = getOrCreateAttributeValueSet(name);
        currentValues.clear();

        addNonNullValues(currentValues, values);
        checkSchema(name, values, "replace");
        recordModify(name, null, null, values);
        return this;
    }

    private void addNonNullValues(Set<Object> currentValues, Collection<Object> values) {
        for (Object value : values) {
            if (value != null) {
                currentValues.add(value);
            }
        }
    }

    private Set<Object> getOrCreateAttributeValueSet(String name) {
        return attributes.computeIfAbsent(name, k -> ConcurrentHashMap.newKeySet());
    }

    public DummyObject replaceAttributeValues(String name, Object... values)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();

        Set<Object> currentValues = getOrCreateAttributeValueSet(name);
        currentValues.clear();

        List<Object> valuesList = Arrays.asList(values);
        addNonNullValues(currentValues, valuesList);
        checkSchema(name, valuesList, "replace");

        // Why isn't the same code in the "collection" version of this method (above)?
        // Maybe to check null-attribute handling of UCF layer, see TestDummy.test129NullAttributeValue.
        if (valuesList.isEmpty()) {
            attributes.remove(name);
        }
        recordModify(name, null, null, valuesList);
        return this;
    }

    public DummyObject addAttributeValue(String name, Object value)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return addAttributeValues(name, createCollection(value));
    }

    public <T> DummyObject addAttributeValues(String name, Collection<T> valuesToAdd)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();
        Set<Object> currentValues = getOrCreateAttributeValueSet(name);
        for (T valueToAdd: valuesToAdd) {
            addAttributeValue(name, currentValues, valueToAdd);
        }
        recordModify(name, valuesToAdd, null, null);
        return this;
    }

    public DummyObject addAttributeValues(String name, String... valuesToAdd)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();
        Set<Object> currentValues = getOrCreateAttributeValueSet(name);
        for (Object valueToAdd: valuesToAdd) {
            addAttributeValue(name, currentValues, valueToAdd);
        }
        recordModify(name, Arrays.asList(valuesToAdd), null, null);
        return this;
    }

    private DummyObject addAttributeValue(String attrName, Set<Object> currentValues, Object valueToAdd)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();

        if (valueToAdd == null) {
            return this; // Concurrent hash map does not allow null values.
        }
        if (resource != null && !resource.isTolerateDuplicateValues()) {
            for (Object currentValue: currentValues) {
                if (currentValue.equals(valueToAdd)) {
                    throw new IllegalArgumentException("The value '"+valueToAdd+"' of attribute '"+attrName+"' conflicts with existing value: Attempt to add value that already exists");
                }
                if (resource.isCaseIgnoreValues() && (valueToAdd instanceof String)) {
                    if (StringUtils.equalsIgnoreCase((String)currentValue, (String)valueToAdd)) {
                        throw new IllegalArgumentException("The value '"+valueToAdd+"' of attribute '"+attrName+"' conflicts with existing value: Attempt to add value that already exists");
                    }
                }
            }
        }

        if (resource != null && resource.isMonsterization() && DummyResource.VALUE_MONSTER.equals(valueToAdd)) {
            currentValues.removeIf(DummyResource.VALUE_COOKIE::equals);
        }

        // an optimization: let's avoid attribute manipulation if we don't want to check the schema anyway
        if (resource != null && resource.isEnforceSchema()) {
            Set<Object> valuesToCheck = new HashSet<>(currentValues);
            valuesToCheck.add(valueToAdd);
            checkSchema(attrName, valuesToCheck, "add");
        }
        currentValues.add(valueToAdd);
        return this;
    }

    public DummyObject removeAttributeValue(String name, Object value)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return removeAttributeValues(name, createCollection(value));
    }

    public <T> DummyObject removeAttributeValues(String name, Collection<T> values)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkModifyBreak();
        delayOperation();
        Set<Object> currentValues = getOrCreateAttributeValueSet(name);

        Set<Object> valuesToCheck = new HashSet<>(currentValues);
        valuesToCheck.removeAll(values);
        checkSchema(name, valuesToCheck, "remove");

        Iterator<Object> iterator = currentValues.iterator();
        boolean foundMember = false;

        if (name.equals(DummyGroup.ATTR_MEMBERS_NAME) && !getResourceRequired().isTolerateDuplicateValues()) {
            checkIfExist(values, currentValues);
        }

        while (iterator.hasNext()) {
            Object currentValue = iterator.next();
            boolean found = false;
            for (Object value: values) {
                if (getResourceRequired().isCaseIgnoreValues() && currentValue instanceof String && value instanceof String) {
                    if (StringUtils.equalsIgnoreCase((String)currentValue, (String)value)) {
                        found = true;
                        break;
                    }
                } else {
                    if (currentValue.equals(value)) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                iterator.remove();
            }

        }

        recordModify(name, null, values, null);
        return this;
    }

    public Set<String> getAuxiliaryObjectClassNames() {
        return auxiliaryObjectClassNames;
    }

    public DummyObject addAuxiliaryObjectClassName(String name) {
        auxiliaryObjectClassNames.add(name);
        return this;
    }

    public DummyObject replaceAuxiliaryObjectClassNames(List<?> values) {
        auxiliaryObjectClassNames.clear();
        addAuxiliaryObjectClassNames(values);
        return this;
    }

    public DummyObject deleteAuxiliaryObjectClassNames(List<?> values) {
        for (Object value : values) {
            auxiliaryObjectClassNames.remove(String.valueOf(value));
        }
        return this;
    }

    public DummyObject addAuxiliaryObjectClassNames(List<?> values) {
        for (Object value : values) {
            auxiliaryObjectClassNames.add(String.valueOf(value));
        }
        return this;
    }

    private <T> void checkIfExist(Collection<T> valuesToDelete, Set<Object> currentValues) throws SchemaViolationException{
        for (T valueToDelete : valuesToDelete) {
            boolean found = false;
            for (Object currentValue : currentValues) {
                if (getResourceRequired().isCaseIgnoreValues()
                        && currentValue instanceof String
                        && valueToDelete instanceof String) {
                    if (StringUtils.equalsIgnoreCase((String)currentValue, (String)valueToDelete)) {
                        found = true;
                        break;
                    }
                } else {
                    if (currentValue.equals(valueToDelete)) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                throw new SchemaViolationException("no such member: " + valueToDelete + " in " + currentValues);
            }
        }
    }

    protected void checkModifyBreak() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        if (resource == null) {
            return;
        }
        BreakMode modifyBreakMode = this.modifyBreakMode;
        if (modifyBreakMode == null) {
            modifyBreakMode = resource.getModifyBreakMode();
        }
        if (modifyBreakMode == BreakMode.NONE) {
            // go on
        } else if (modifyBreakMode == BreakMode.NETWORK) {
            throw new ConnectException("Network error (simulated error)");
        } else if (modifyBreakMode == BreakMode.IO) {
            throw new FileNotFoundException("IO error (simulated error)");
        } else if (modifyBreakMode == BreakMode.SCHEMA) {
            throw new SchemaViolationException("Schema violation (simulated error)");
        } else if (modifyBreakMode == BreakMode.CONFLICT) {
            throw new ConflictException("Conflict (simulated error)");
        } else if (modifyBreakMode == BreakMode.GENERIC) {
            // The connector will react with generic exception
            throw new IllegalArgumentException("Generic error (simulated error)");
        } else if (modifyBreakMode == BreakMode.RUNTIME) {
            // The connector will just pass this up
            throw new IllegalStateException("Generic error (simulated error)");
        } else if (modifyBreakMode == BreakMode.UNSUPPORTED) {
            throw new UnsupportedOperationException("Not supported (simulated error)");
        } else {
            // This is a real error. Use this strange thing to make sure it passes up
            throw new RuntimeException("Unknown schema break mode "+modifyBreakMode);
        }
    }

    private <T> void recordModify(String attributeName, Collection<T> valuesAdded, Collection<T> valuesDeleted, Collection<T> valuesReplaced) {
        if (resource != null && presentOnResource) {
            resource.recordModify(this, attributeName, valuesAdded, valuesDeleted, valuesReplaced);
        }
    }

    private void delayOperation() throws InterruptedException {
        if (resource != null) {
            resource.delayOperation();
        }
    }

    protected void checkSchema(String attrName, Collection<Object> values, String operationName) throws SchemaViolationException {
        if (resource == null || !resource.isEnforceSchema() || resource.isSchemaBroken()) {
            return;
        }
        DummyAttributeDefinition attributeDefinition = getAttributeDefinition(attrName);
        if (attributeDefinition == null) {
            throw new SchemaViolationException("Attribute "+attrName+" is not defined in resource schema");
        }
        if (attributeDefinition.isRequired() && (values == null || values.isEmpty())) {
            throw new SchemaViolationException(operationName + " of required attribute "+attrName+" results in no values");
        }
        if (!attributeDefinition.isMulti() && values != null && values.size() > 1) {
            throw new SchemaViolationException(operationName + " of single-valued attribute "+attrName+" results in "+values.size()+" values");
        }
    }

    /** {@link #resource} must be set up! */
    public DummyAttributeDefinition getAttributeDefinition(String attrName) {
        DummyAttributeDefinition def = getStructuralObjectClass().getAttributeDefinition(attrName);
        if (def != null) {
            return def;
        }
        for (String auxClassName : getAuxiliaryObjectClassNames()) {
            DummyObjectClass auxObjectClass = getResourceRequired().getAuxiliaryObjectClassMap().get(auxClassName);
            if (auxObjectClass == null) {
                throw new IllegalStateException("Auxiliary object class " + auxClassName + " couldn't be found");
            }
            def = auxObjectClass.getAttributeDefinition(attrName);
            if (def != null) {
                return def;
            }
        }
        return null;
    }

    /** {@link #resource} must be set up! */
    public @NotNull DummyObjectClass getStructuralObjectClass() {
        return getResourceRequired().getStructuralObjectClass(getObjectClassName());
    }

    public abstract String getShortTypeName();

    @Override
    public String toString() {
        return getClass().getSimpleName()+"(" + toStringContent() + ")";
    }

    protected String toStringContent() {
        return "name=" + name + ", attributes=" + attributes + ", enabled=" + enabled;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append(": ").append(name);
        if (id != null && !id.equals(name)) {
            sb.append(" (").append(id).append(")");
        }
        if (!auxiliaryObjectClassNames.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabelToString(sb, "Auxiliary object classes", auxiliaryObjectClassNames, indent + 1);
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToString(sb, "Enabled", enabled, indent + 1);
        if (validFrom != null || validTo != null) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "Validity", indent + 1);
            sb.append(" ").append(PrettyPrinter.prettyPrint(validFrom)).append(" - ").append(PrettyPrinter.prettyPrint(validTo));
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Last modifier", lastModifier, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Attributes", attributes, indent + 1);
        if (resource != null) {
            for (var linkDefinition : getStructuralObjectClass().getLinkDefinitions()) {
                if (!linkDefinition.getParticipant().isVisible()) {
                    continue;
                }
                var linkName = linkDefinition.getParticipant().getLinkNameRequired();
                sb.append("\n");
                var label = "Linked objects (" + linkName + ")";
                var linkedObjects = getLinkedObjects(linkName);
                if (linkDefinition.getParticipant().isExpandedByDefault()) {
                    DebugUtil.debugDumpWithLabel(sb, label, linkedObjects, indent + 1);
                } else {
                    var names = linkedObjects.stream().map(obj -> obj.getName()).toList();
                    DebugUtil.debugDumpWithLabel(sb, label, names, indent + 1);
                }
            }
        }
        extendDebugDump(sb, indent);
        return sb.toString();
    }

    protected void extendDebugDump(StringBuilder sb, int indent) {
        // Nothing to do
    }

    public boolean isReturnedByDefault(String attrName) {
        final DummyAttributeDefinition attributeDefinition = getAttributeDefinition(attrName);
        if (attributeDefinition != null) {
            return attributeDefinition.isReturnedByDefault();
        } else {
            System.out.println("Warning: attribute " + attrName + " is not defined in " + this);
            return false;
        }
    }

    /** Assumes hierarchical object support is on. */
    boolean containedByOrg(HierarchicalName normalizedOrgName) {
        return normalizedHierarchicalName != null && normalizedHierarchicalName.residesIn(normalizedOrgName);
    }

    /** {@link #resource} must be set up! */
    public String getNormalizedName() {
        return getResourceRequired().normalizeName(name);
    }

    @NotNull public abstract String getObjectClassName();

    public abstract String getObjectClassDescription();

    /** {@link #resource} must be set up! */
    public Collection<DummyObject> getLinkedObjects(@NotNull String linkName) {
        return getResourceRequired().getLinkedObjects(this, linkName);
    }

    /**
     * Adds a link to given object. (It must already exist on the resource.)
     *
     * The {@link #resource} must be set up for the current object.
     */
    public void addLinkValue(@NotNull String linkName, @NotNull DummyObject linkedObject) {
        var resource = getResourceRequired();
        var linkDef = getStructuralObjectClass().getLinkDefinitionRequired(linkName);
        if (linkDef.isFirst()) {
            resource.addLinkValue(linkDef.getLinkClassName(), this, linkedObject);
        } else {
            resource.addLinkValue(linkDef.getLinkClassName(), linkedObject, this);
        }
    }

    /**
     * Deletes a link value. The linked object will be deleted if it's stored "by value".
     *
     * The {@link #resource} must be set up for the current object.
     */
    public void deleteLinkValue(@NotNull String linkName, @NotNull DummyObject linkedObject)
            throws ConflictException, FileNotFoundException, SchemaViolationException,
            InterruptedException, ConnectException {
        var resource = getResourceRequired();
        var linkDef = getStructuralObjectClass().getLinkDefinitionRequired(linkName);
        if (linkDef.isFirst()) {
            resource.deleteLinkValue(linkDef.getLinkClassName(), this, linkedObject);
        } else {
            resource.deleteLinkValue(linkDef.getLinkClassName(), linkedObject, this);
        }
        if (linkDef.getParticipant().isExpandedByDefault()) {
            try {
                resource.deleteObjectById(linkedObject.getObjectClassName(), linkedObject.getId());
            } catch (ObjectDoesNotExistException e) {
                throw new IllegalStateException("Linked object " + linkedObject + " does not exist", e);
            }
        }
    }

    /**
     * Deletes all values of a given link, along with the linked objects if they are stored "by value".
     *
     * The {@link #resource} must be set up for the current object.
     */
    public void deleteAllLinkValues(@NotNull String linkName)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        for (DummyObject linkedObject : getLinkedObjects(linkName)) {
            deleteLinkValue(linkName, linkedObject);
        }
    }

    /** {@link #resource} must be set up! */
    public boolean isLink(String name) {
        return getStructuralObjectClass().getLinkDefinition(name) != null;
    }
}
