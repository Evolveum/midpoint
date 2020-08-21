/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.test.asserter.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class PrismObjectAsserter<O extends ObjectType,RA> extends AbstractAsserter<RA> {

    private PrismObject<O> object;

    // Cache of focus-related objects: projections, targets, orgs, ...
    private Map<String,PrismObject<? extends ObjectType>> objectCache = new HashMap<>();

    public PrismObjectAsserter(PrismObject<O> object) {
        super();
        this.object = object;
    }

    public PrismObjectAsserter(PrismObject<O> object, String details) {
        super(details);
        this.object = object;
    }

    public PrismObjectAsserter(PrismObject<O> object, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.object = object;
    }

    public PrismObject<O> getObject() {
        return object;
    }

    public O getObjectable() {
        return object.asObjectable();
    }

    public static <O extends ObjectType> PrismObjectAsserter<O,Void> forObject(PrismObject<O> object) {
        return new PrismObjectAsserter<>(object);
    }

    public static <O extends ObjectType> PrismObjectAsserter<O,Void> forObject(PrismObject<O> object, String details) {
        return new PrismObjectAsserter<>(object, details);
    }

    public PrismObjectAsserter<O,RA> assertOid() {
        assertNotNull("No OID in "+desc(), getObject().getOid());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertOid(String expected) {
        assertEquals("Wrong OID in "+desc(), expected, getObject().getOid());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertOidDifferentThan(String oid) {
        assertFalse("Expected that "+desc()+" will have different OID than "+oid+", but it has the same", oid.equals(getObject().getOid()));
        return this;
    }

    public PrismObjectAsserter<O,RA> assertConsistence() {
        object.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
        return this;
    }

    public PrismObjectAsserter<O, RA> assertDefinition() {
        assertTrue("Incomplete definition in "+object, object.hasCompleteDefinition());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertSanity() {
        assertConsistence();
        assertDefinition();
        assertOid();
        assertName();
        return this;
    }

    public PrismObjectAsserter<O,RA> assertName() {
        assertNotNull("No name in "+desc(), getObject().getName());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertName(String expectedOrig) {
        PrismAsserts.assertEqualsPolyString("Wrong name in "+desc(), expectedOrig, getObject().getName());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertNameOrig(String expectedOrig) {
        assertThat(getObject().getName().getOrig())
                .as("Name (orig) in " + desc())
                .isEqualTo(expectedOrig);
        return this;
    }

    public PolyStringAsserter<? extends PrismObjectAsserter<O,RA>> name() {
        PolyStringAsserter<PrismObjectAsserter<O,RA>> asserter = new PolyStringAsserter<>(getPolyStringPropertyValue(ObjectType.F_NAME), this, "name in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O,RA> assertDescription(String expected) {
        assertEquals("Wrong description in "+desc(), expected, getObject().asObjectable().getDescription());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertNoDescription() {
        assertNull("Unexpected description in "+desc()+": "+getObject().asObjectable().getDescription(), getObject().asObjectable().getDescription());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertSubtype(String... expected) {
        PrismAsserts.assertEqualsCollectionUnordered("Wrong subtype in "+desc(), getObject().asObjectable().getSubtype(), expected);
        return this;
    }

    public PrismObjectAsserter<O,RA> assertTenantRef(String expectedOid) {
        ObjectReferenceType tenantRef = getObject().asObjectable().getTenantRef();
        if (tenantRef == null && expectedOid == null) {
            return this;
        }
        assertNotNull("No tenantRef in "+desc(), tenantRef);
        assertEquals("Wrong tenantRef OID in "+desc(), expectedOid, tenantRef.getOid());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertLifecycleState(String expected) {
        assertEquals("Wrong lifecycleState in "+desc(), expected, getObject().asObjectable().getLifecycleState());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertActiveLifecycleState() {
        String actualLifecycleState = getObject().asObjectable().getLifecycleState();
        if (actualLifecycleState != null) {
            assertEquals("Wrong lifecycleState in "+desc(), SchemaConstants.LIFECYCLE_ACTIVE, actualLifecycleState);
        }
        return this;
    }

    public PrismObjectAsserter<O,RA> assertIndestructible(Boolean expected) {
        assertEquals("Wrong 'indestructible' in "+desc(), expected, getObject().asObjectable().isIndestructible());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertIndestructible() {
        assertEquals("Wrong 'indestructible' in "+desc(), Boolean.TRUE, getObject().asObjectable().isIndestructible());
        return this;
    }

    public PrismObjectAsserter<O,RA> assertDestructible() {
        if (Boolean.TRUE.equals(getObject().asObjectable().isIndestructible())) {
            fail("Unexpected indestructible=TRUE in "+desc());
        }
        return this;
    }

    public UserAsserter<PrismObjectAsserter<O,RA>> asUser() {
        UserAsserter<PrismObjectAsserter<O,RA>> asserter = new UserAsserter<>((PrismObject<UserType>) getObject(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public RoleAsserter<PrismObjectAsserter<O,RA>> asRole() {
        RoleAsserter<PrismObjectAsserter<O,RA>> asserter = new RoleAsserter<>((PrismObject<RoleType>) getObject(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public OrgAsserter<PrismObjectAsserter<O,RA>> asOrg() {
        OrgAsserter<PrismObjectAsserter<O,RA>> asserter = new OrgAsserter<>((PrismObject<OrgType>) getObject(), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return descWithDetails(object);
    }

    public PrismObjectAsserter<O,RA> display() {
        display(desc());
        return this;
    }

    public PrismObjectAsserter<O,RA> display(String message) {
        IntegrationTestTools.display(message, object);
        return this;
    }

    public PrismObjectAsserter<O,RA> displayXml() throws SchemaException {
        displayXml(desc());
        return this;
    }

    public PrismObjectAsserter<O,RA> displayXml(String message) throws SchemaException {
        IntegrationTestTools.displayXml(message, object);
        return this;
    }

    protected PolyString getPolyStringPropertyValue(QName propName) {
        PrismProperty<PolyString> prop = getObject().findProperty(ItemName.fromQName(propName));
        if (prop == null) {
            return null;
        }
        return prop.getRealValue();
    }

    public PrismObjectAsserter<O,RA> assertExtensionValue(String localName, Object realValue) {
        Item extensionItem = getObject().findExtensionItem(localName);
        assertNotNull("No extension item " + localName, extensionItem);
        assertTrue("Real value " + realValue + " not in " + extensionItem, extensionItem.getRealValues().contains(realValue));
        return this;
    }

    public PrismContainerAsserter<?, ? extends PrismObjectAsserter<O,RA>> extensionContainer(String localName) {
        return createExtensionContainerAsserter(localName, getObject().findExtensionItem(localName));
    }

    public PrismContainerAsserter<?, ? extends PrismObjectAsserter<O,RA>> extensionContainer(ItemName name) {
        return createExtensionContainerAsserter(name, getObject().findExtensionItem(name));
    }

    @NotNull
    private PrismContainerAsserter<?, ? extends PrismObjectAsserter<O, RA>> createExtensionContainerAsserter(Object localName,
            Item<?, ?> extensionItem) {
        assertNotNull("No extension item " + localName, extensionItem);
        assertTrue("Extension item " + localName + " is not a container: " + extensionItem.getClass(), extensionItem instanceof PrismContainer);
        PrismContainerAsserter<?, ? extends PrismObjectAsserter<O,RA>> asserter = new PrismContainerAsserter<>((PrismContainer<?>) extensionItem, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O,RA> assertExtensionValues(int count) {
        assertEquals("Wrong # of extension items values", count, getExtensionValuesCount());
        return this;
    }

    private int getExtensionValuesCount() {
        PrismContainer<?> extension = getObject().getExtension();
        int count = 0;
        if (extension != null) {
            for (Item<?, ?> item : extension.getValue().getItems()) {
                count += item.size();
            }
        }
        return count;
    }

    public PrismObjectAsserter<O,RA> assertExtensionItems(int count) {
        assertEquals("Wrong # of extension items", count, getExtensionItemsCount());
        return this;
    }

    private int getExtensionItemsCount() {
        PrismContainer<?> extension = getObject().getExtension();
        return extension != null ? extension.getValue().size() : 0;
    }

    public PrismObjectAsserter<O,RA> assertPolyStringProperty(QName propName, String expectedOrig) {
        PrismProperty<PolyString> prop = getObject().findProperty(ItemName.fromQName(propName));
        assertNotNull("No "+propName.getLocalPart()+" in "+desc(), prop);
        PrismAsserts.assertEqualsPolyString("Wrong "+propName.getLocalPart()+" in "+desc(), expectedOrig, prop.getRealValue());
        return this;
    }

    protected void assertPolyStringPropertyMulti(QName propName, String... expectedOrigs) {
        PrismProperty<PolyString> prop = getObject().findProperty(ItemName.fromQName(propName));
        if (expectedOrigs.length > 0) {
            assertNotNull("No " + propName.getLocalPart() + " in " + desc(), prop);
            PrismAsserts.assertEqualsPolyStringMulti("Wrong "+propName.getLocalPart()+" in "+desc(), prop.getRealValues(), expectedOrigs);
        } else {
            assertTrue("Property is not empty even if it should be: " + prop, prop == null || prop.isEmpty());
        }
    }

    protected <T> void assertPropertyEquals(QName propName, T expected) {
        PrismProperty<T> prop = getObject().findProperty(ItemName.fromQName(propName));
        if (prop == null && expected == null) {
            return;
        }
        assertNotNull("No "+propName.getLocalPart()+" in "+desc(), prop);
        T realValue = prop.getRealValue();
        assertNotNull("No value in "+propName.getLocalPart()+" in "+desc(), realValue);
        assertEquals("Wrong "+propName.getLocalPart()+" in "+desc(), expected, realValue);
    }

    public PrismObjectAsserter<O,RA> assertNoItem(ItemPath itemPath) {
        Item<PrismValue, ItemDefinition> item = getObject().findItem(itemPath);
        assertNull("Unexpected item "+itemPath+" in "+desc(), item);
        return this;
    }

    public PrismObjectAsserter<O,RA> assertNoTrigger() {
        List<TriggerType> triggers = object.asObjectable().getTrigger();
        if (triggers != null && !triggers.isEmpty()) {
            AssertJUnit.fail("Expected that "+object+" will have no triggers but it has "+triggers.size()+ " trigger: "+ triggers + "; in "+desc());
        }
        return this;
    }

    public String getOid() {
        return getObject().getOid();
    }

    public ParentOrgRefsAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> parentOrgRefs() {
        ParentOrgRefsAsserter<O,PrismObjectAsserter<O,RA>,RA> asserter = new ParentOrgRefsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O,RA> assertParentOrgRefs(String... expectedOids) {
        parentOrgRefs().assertRefs(expectedOids);
        return this;
    }

    public <CO extends ObjectType> PrismObject<CO> getCachedObject(Class<CO> type, String oid) throws ObjectNotFoundException, SchemaException {
        PrismObject<CO> object = (PrismObject<CO>) objectCache.get(oid);
        if (object == null) {
            object = resolveObject(type, oid);
            objectCache.put(oid, object);
        }
        return object;
    }

    public ExtensionAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> extension() {
        ExtensionAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> asserter = new ExtensionAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ValueMetadataAsserter<? extends PrismObjectAsserter<O, RA>> valueMetadata(ItemPath path) throws SchemaException {
        return createValueMetadataAsserter(path, getValueMetadata(path, null));
    }

    public ValueMetadataAsserter<? extends PrismObjectAsserter<O, RA>> valueMetadata(ItemPath path, ValueSelector<?> valueSelector)
            throws SchemaException {
        return createValueMetadataAsserter(path, getValueMetadata(path, valueSelector));
    }

    public ValueMetadataValueAsserter<? extends PrismObjectAsserter<O, RA>> valueMetadataSingle(ItemPath path) throws SchemaException {
        return createValueMetadataValueAsserter(path, getValueMetadata(path, null));
    }

    public ValueMetadataValueAsserter<? extends PrismObjectAsserter<O, RA>> valueMetadataSingle(ItemPath path, ValueSelector<?> valueSelector)
            throws SchemaException {
        return createValueMetadataValueAsserter(path, getValueMetadata(path, valueSelector));
    }

    @NotNull
    private ValueMetadataAsserter<? extends PrismObjectAsserter<O, RA>> createValueMetadataAsserter(ItemPath path,
            PrismContainer<ValueMetadataType> valueMetadata) {
        ValueMetadataAsserter<? extends PrismObjectAsserter<O, RA>> asserter =
                new ValueMetadataAsserter<>(valueMetadata, this, String.valueOf(path)); // TODO details
        copySetupTo(asserter);
        return asserter;
    }

    @NotNull
    private ValueMetadataValueAsserter<? extends PrismObjectAsserter<O, RA>> createValueMetadataValueAsserter(ItemPath path,
            PrismContainer<ValueMetadataType> valueMetadata) {
        if (valueMetadata.size() != 1) {
            fail("Value metadata container has none or multiple values: " + valueMetadata);
        }
        ValueMetadataValueAsserter<? extends PrismObjectAsserter<O, RA>> asserter =
                new ValueMetadataValueAsserter<>(valueMetadata.getValue(), this, String.valueOf(path)); // TODO details
        copySetupTo(asserter);
        return asserter;
    }

    private PrismContainer<ValueMetadataType> getValueMetadata(ItemPath path, ValueSelector<? extends PrismValue> valueSelector) throws SchemaException {
        Object o = getObject().find(path);
        if (o instanceof PrismValue) {
            return ((PrismValue) o).getValueMetadataAsContainer();
        } else if (o instanceof Item) {
            Item<?, ?> item = (Item<?, ?>) o;
            if (valueSelector == null) {
                if (item.size() == 1) {
                    return item.getValue().getValueMetadataAsContainer();
                } else {
                    throw new AssertionError("Item '" + path + "' has not a single value in " + getObject() +
                            ": " + item.size() + " values: " + item);
                }
            } else {
                //noinspection unchecked
                PrismValue anyValue = item.getAnyValue((ValueSelector) valueSelector);
                if (anyValue != null) {
                    return anyValue.getValueMetadataAsContainer();
                } else {
                    throw new AssertionError("Item '" + path + "' has no value matching given selector in " + getObject() +
                            ": " + item.size() + " values: " + item);
                }
            }
        } else if (o != null) {
            throw new AssertionError("Object '" + path + "' has no unexpected value matching given selector in " +
                    getObject() + ": " + o);
        } else {
            throw new AssertionError("Item '" + path + "' not found in " + getObject());
        }
    }

    public TriggersAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> triggers() {
        TriggersAsserter<O, ? extends PrismObjectAsserter<O,RA>, RA> asserter = new TriggersAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O,RA> assertArchetypeRef(String expectedArchetypeOid) {
        List<ObjectReferenceType> archetypeRefs = getArchetypeRefs();
        if (archetypeRefs.isEmpty()) {
            fail("No archetypeRefs while archetype "+expectedArchetypeOid+" expected");
        }
        if (archetypeRefs.size() > 1) {
            fail("Too many archetypes while archetypeRefs "+expectedArchetypeOid+" expected: "+archetypeRefs);
        }
        assertEquals("Wrong archetypeRef in "+desc(), expectedArchetypeOid, archetypeRefs.get(0).getOid());
        return this;
    }

    @NotNull
    private List<ObjectReferenceType> getArchetypeRefs() {
        O objectable = getObject().asObjectable();
        return objectable instanceof AssignmentHolderType ?
                    ((AssignmentHolderType) objectable).getArchetypeRef() : emptyList();
    }

    public PrismObjectAsserter<O,RA> assertNoArchetypeRef() {
        List<ObjectReferenceType> archetypeRefs = getArchetypeRefs();
        if (!archetypeRefs.isEmpty()) {
            fail("Found archetypeRefs while not expected any: "+archetypeRefs);
        }
        return this;
    }
}
