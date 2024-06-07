/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * @author Radovan Semancik
 *
 */
public class DummyObjectClass {

    @NotNull private final Collection<DummyAttributeDefinition> attributeDefinitions = new ArrayList<>();

    /** Links relevant to this object class. Maintained by the resource itself. Indexed by (non-null) local link name.*/
    @NotNull private final Map<String, LinkDefinition> linkDefinitionsMap = new ConcurrentHashMap<>();

    /** True if this is an "association object class". */
    private final boolean associationObject;

    public DummyObjectClass() {
        this(false);
    }

    public DummyObjectClass(boolean associationObject) {
        this.associationObject = associationObject;
    }

    /**
     * Creates a standard/standalone object class, i.e. not an association one. The object class can be structural or auxiliary,
     * depending on how it's used.
     */
    public static DummyObjectClass standard() {
        return new DummyObjectClass();
    }

    /** Creates an association object class. Should be used as a structural one for now. */
    public static DummyObjectClass association() {
        return new DummyObjectClass(true);
    }

    public @NotNull Collection<DummyAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public DummyAttributeDefinition getAttributeDefinition(String attrName) {
        for (DummyAttributeDefinition attrDef: attributeDefinitions) {
            if (attrName.equals(attrDef.getAttributeName())) {
                return attrDef;
            }
        }
        return null;
    }

    public void add(DummyAttributeDefinition attrDef) {
        attributeDefinitions.add(attrDef);
    }

    public void clear() {
        attributeDefinitions.clear();
        linkDefinitionsMap.clear();
    }

    public void addAttributeDefinition(String attributeName) {
        addAttributeDefinition(attributeName, String.class, false, false);
    }

    public void addAttributeDefinition(
            String attributeName, Class<?> attributeType, boolean isRequired, boolean isMulti) {
        add(new DummyAttributeDefinition(attributeName, attributeType, isRequired, isMulti));
    }

    public Collection<LinkDefinition> getLinkDefinitions() {
        return Collections.unmodifiableCollection(
                linkDefinitionsMap.values());
    }

    LinkDefinition getLinkDefinition(@NotNull String linkName) {
        return linkDefinitionsMap.get(linkName);
    }

    LinkDefinition getLinkDefinitionRequired(@NotNull String linkName) {
        return MiscUtil.argNonNull(
                linkDefinitionsMap.get(linkName),
                "No link '%s' definition in %s", linkName, this);
    }

    synchronized void addLinkDefinition(LinkDefinition definition) {
        var name = argNonNull(definition.getLinkName(), "No link name in %s", definition);
        stateCheck(!linkDefinitionsMap.containsKey(name), "Link definition for %s already exists", name);
        linkDefinitionsMap.put(name, definition);
    }

    public boolean isAssociationObject() {
        return associationObject;
    }
}
