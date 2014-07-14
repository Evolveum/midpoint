/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Set of prism-related asserts.
 * 
 * DO NOT use this in the main code. Although it is placed in "main" for convenience, is should only be used in tests.
 * 
 * @author Radovan Semancik
 *
 */
public class PrismAsserts {
	
	private static final Trace LOGGER = TraceManager.getTrace(PrismAsserts.class);
	
	// VALUE asserts
		
	public static <T> void assertPropertyValue(PrismContainer<?> container, QName propQName, T... realPropValues) {
		PrismContainerValue<?> containerValue = container.getValue();
		assertSame("Wrong parent for value of container "+container, container, containerValue.getParent());
		assertPropertyValue(containerValue, propQName, realPropValues);
	}
		
	public static <T> void assertPropertyValue(PrismContainerValue<?> containerValue, QName propQName, T... realPropValues) {
		PrismProperty<T> property = containerValue.findProperty(propQName);
		assertNotNull("Property " + propQName + " not found in " + containerValue.getParent(), property);
		assertSame("Wrong parent for property " + property, containerValue, property.getParent());
		assertPropertyValueDesc(property, containerValue.getParent().toString(), realPropValues);
	}
	
	public static <T> void assertPropertyValue(PrismContainer<?> container, ItemPath propPath, T... realPropValues) {
		PrismContainerValue<?> containerValue = container.getValue();
		assertSame("Wrong parent for value of container "+container, container, containerValue.getParent());
		assertPropertyValue(containerValue, propPath, realPropValues);
	}
		
	public static <T> void assertPropertyValue(PrismContainerValue<?> containerValue, ItemPath propPath, T... realPropValues) {
		PrismProperty<T> property = containerValue.findProperty(propPath);
		assertNotNull("Property " + propPath + " not found in " + containerValue.getParent(), property);
		assertPropertyValueDesc(property, containerValue.getParent().toString(), realPropValues);
	}
	
	public static <T> void assertPropertyValue(PrismProperty<T> property, T... expectedPropValues) {
		assertPropertyValueDesc(property, null, expectedPropValues);
	}
		
	public static <T> void assertPropertyValueDesc(PrismProperty<T> property, String contextDescrition, T... expectedPropValues) {
		Collection<PrismPropertyValue<T>> pvals = property.getValues();
		QName propQName = property.getElementName();
		assert pvals != null && !pvals.isEmpty() : "Empty property "+propQName;
		assertSet("property "+propQName + (contextDescrition == null ? "" : " in " + contextDescrition), 
				"value", pvals, expectedPropValues);
	}
	
	public static <T> void assertPropertyValues(String message, Collection<T> expected, Collection<PrismPropertyValue<T>> results) {
		assertEquals(message+" - unexpected number of results", expected.size(), results.size());

        Set<Object> values = new HashSet<Object>();
        for (PrismPropertyValue<T> result : results) {
            values.add(result.getValue());
        }
        assertEquals(message, expected, values);
    }

	public static <T> void assertPropertyValues(String message, Collection<PrismPropertyValue<T>> results, T... expectedValues) {
		assertSet(message, "value", results, expectedValues);
    }

	public static void assertReferenceValues(PrismReference ref, String... oids) {
		assert oids.length == ref.getValues().size() : "Wrong number of values in "+ref+"; expected "+oids.length+" but was "+ref.getValues().size();
		for (String oid: oids) {
			assertReferenceValue(ref, oid);
		}
	}
	
	public static void assertReferenceValue(PrismReference ref, String oid) {
		for (PrismReferenceValue val: ref.getValues()) {
			if (oid.equals(val.getOid())) {
				return;
			}
		}
		fail("Oid "+oid+" not found in reference "+ref);
	}
	
	public static void assertNoItem(PrismContainer<?> object, QName itemName) {
		assertNoItem(object, new ItemPath(itemName));
	}

	public static void assertNoItem(PrismContainer<?> object, ItemPath itemPath) {
		Item<?> item = object.findItem(itemPath);
		assert item == null : "Unexpected item "+item+" in "+object;
	}
	
	public static void assertNotEmpty(Item<?> item) {
		assert !item.isEmpty() : "Item "+item+" is empty";
	}
	
	public static void assertNoEmptyItem(PrismContainer<?> container) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (visitable != null && visitable instanceof Item) {
					assertNotEmpty((Item<?>)visitable);
				}
			}
		};
		container.accept(visitor);
	}

	// DEFINITION asserts
	
	public static <T extends Objectable> void assertObjectDefinition(PrismObjectDefinition<T> objDef, QName elementName,
			QName typeName, Class<T> compileTimeClass) {
		assertNotNull("No definition", objDef);
		assertEquals("Wrong elementName for "+objDef, elementName, objDef.getName());
		assertEquals("Wrong typeName for "+objDef, typeName, objDef.getTypeName());
		assertEquals("Wrong compileTimeClass for "+objDef, compileTimeClass, objDef.getCompileTimeClass());
	}
	
	public static void assertDefinition(Item item, QName type, int minOccurs, int maxOccurs) {
		ItemDefinition definition = item.getDefinition();
		assertDefinition(definition, item.getElementName(), type, minOccurs, maxOccurs);
	}
		
	public static void assertPropertyDefinition(PrismContainer<?> container, QName propertyName, QName type, int minOccurs, int maxOccurs) {
		PrismProperty<?> findProperty = container.findProperty(propertyName);
		PrismPropertyDefinition<?> definition = findProperty.getDefinition();
		assertDefinition(definition, propertyName, type, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyDefinition(ComplexTypeDefinition container, QName propertyName, QName type, int minOccurs, int maxOccurs) {
		PrismPropertyDefinition<?> definition = container.findPropertyDefinition(propertyName);
		assertDefinition(definition, propertyName, type, minOccurs, maxOccurs);
	}
	
    public static void assertPropertyDefinition(PrismProperty property, QName type, int minOccurs, int maxOccurs, Boolean indexed) {
        assertDefinition(property, type, minOccurs, maxOccurs);

        PrismPropertyDefinition definition = property.getDefinition();
        assert equals(indexed, definition.isIndexed()) : "Property should have indexed=" + indexed + ", but it has indexed=" + definition.isIndexed();
    }

    public static void assertPropertyDefinition(PrismContainerDefinition<?> containerDef, QName propertyName, QName type, int minOccurs, int maxOccurs, boolean indexed) {
        assertPropertyDefinition(containerDef, propertyName, type, minOccurs, maxOccurs);

        PrismPropertyDefinition definition = containerDef.findPropertyDefinition(propertyName);
        assert equals(indexed, definition.isIndexed()) : "Property should have indexed=" + indexed + ", but it has indexed=" + definition.isIndexed();
    }
	
	public static void assertPropertyDefinition(PrismContainerDefinition<?> containerDef, QName propertyName, QName type, int minOccurs, int maxOccurs) {
		PrismPropertyDefinition definition = containerDef.findPropertyDefinition(propertyName);
		assertDefinition(definition, propertyName, type, minOccurs, maxOccurs);
	}

	public static void assertItemDefinitionDisplayName(PrismContainerDefinition<?> containerDef, QName propertyName, String expectedDisplayName) {
		ItemDefinition definition = containerDef.findItemDefinition(propertyName);
		assert equals(expectedDisplayName, definition.getDisplayName()) : "Wrong display name for item "+propertyName+", expected " +
			expectedDisplayName + ", was " + definition.getDisplayName();
	}
	
	public static void assertItemDefinitionDisplayName(ComplexTypeDefinition containerDef, QName propertyName, String expectedDisplayName) {
		ItemDefinition definition = containerDef.findItemDefinition(propertyName, ItemDefinition.class);
		assert equals(expectedDisplayName, definition.getDisplayName()) : "Wrong display name for item "+propertyName+", expected " +
			expectedDisplayName + ", was " + definition.getDisplayName();
	}

	public static void assertItemDefinitionDisplayOrder(PrismContainerDefinition<?> containerDef, QName propertyName, Integer expectedDisplayOrder) {
		ItemDefinition definition = containerDef.findItemDefinition(propertyName);
		assert equals(expectedDisplayOrder, definition.getDisplayOrder()) : "Wrong display order for item "+propertyName+", expected " +
		expectedDisplayOrder + ", was " + definition.getDisplayOrder();
	}
	
	public static void assertItemDefinitionDisplayOrder(ComplexTypeDefinition containerDef, QName propertyName, Integer expectedDisplayOrder) {
		ItemDefinition definition = containerDef.findItemDefinition(propertyName, ItemDefinition.class);
		assert equals(expectedDisplayOrder, definition.getDisplayOrder()) : "Wrong display order for item "+propertyName+", expected " +
		expectedDisplayOrder + ", was " + definition.getDisplayOrder();
	}
		
	public static void assertItemDefinitionHelp(PrismContainerDefinition<?> containerDef, QName propertyName, String expectedHelp) {
		ItemDefinition definition = containerDef.findItemDefinition(propertyName);
		assert equals(expectedHelp, definition.getHelp()) : "Wrong help for item "+propertyName+", expected " +
			expectedHelp + ", was " + definition.getHelp();
	}
	
	public static void assertDefinition(ItemDefinition definition, QName itemName, QName type, int minOccurs, int maxOccurs) {
		assertNotNull("No definition for "+itemName, definition);
		assertEquals("Wrong definition type for "+itemName, type, definition.getTypeName());
		assertEquals("Wrong definition minOccurs for "+itemName, minOccurs, definition.getMinOccurs());
		assertEquals("Wrong definition maxOccurs for "+itemName, maxOccurs, definition.getMaxOccurs());
	}
	
	public static void assertIndexed(PrismContainerDefinition<? extends Containerable> containerDef, QName itemQName,
			Boolean expected) {
		PrismPropertyDefinition propertyDefinition = containerDef.findPropertyDefinition(itemQName);
		assertEquals("Wrong value of 'indexed' in property '"+PrettyPrinter.prettyPrint(itemQName)+" in "+containerDef, expected, propertyDefinition.isIndexed());
	}

	
	public static<C extends Containerable> void assertValueId(Long expectedId, PrismContainer<C> container) {
		List<Long> ids = new ArrayList<Long>();
		for (PrismContainerValue<C> value: container.getValues()) {
			if (MiscUtil.equals(expectedId, value.getId())) {
				return;
			}
			ids.add(value.getId());
		}
		assert false : "Expected that container "+container+" will have value id '"+expectedId+"' but it has not; existing IDs: "+ids;
	}

	
	// MISC asserts
	
	public static void assertParentConsistency(PrismContainerValue<?> pval) {
		for (Item<?> item: pval.getItems()) {
			assert item.getParent() == pval : "Wrong parent in "+item;
			assertParentConsistency(item);
		}
	}

	public static void assertParentConsistency(Item<?> item) {
		for (PrismValue pval: item.getValues()) {
			assert pval.getParent() == item : "Wrong parent of "+pval+" in "+PrettyPrinter.prettyPrint(item.getElementName());
			if (pval instanceof PrismContainerValue) {
				assertParentConsistency((PrismContainerValue)pval);
			}
		}
	}
	
	// DELTA asserts
	
	public static void assertModifications(ObjectDelta<?> objectDelta, int expectedNumberOfModifications) {
		assertModifications(null, objectDelta, expectedNumberOfModifications);
	}
	
	public static void assertModifications(String message, ObjectDelta<?> objectDelta, int expectedNumberOfModifications) {
		assertIsModify(objectDelta);
		assert objectDelta.getModifications().size() == expectedNumberOfModifications :
				(message == null ? "" : (message + ": ")) +
				"Wrong number of modifications in object delta "
					+ objectDelta + ". Expected "+expectedNumberOfModifications+", was "+objectDelta.getModifications().size();
	}

	public static void assertIsModify(ObjectDelta<?> objectDelta) {
		assert objectDelta.isModify() : "Expected that object delta "+objectDelta+" is MODIFY, but it is "+objectDelta.getChangeType();
	}
	
	public static void assertIsAdd(ObjectDelta<?> objectDelta) {
		assert objectDelta.isAdd() : "Expected that object delta "+objectDelta+" is ADD, but it is "+objectDelta.getChangeType();
		assert objectDelta.getObjectToAdd() != null : "Object to add is null in add delta " + objectDelta;
	}
	
	public static void assertIsDelete(ObjectDelta<?> objectDelta) {
		assert objectDelta.isDelete() : "Expected that object delta "+objectDelta+" is DELETE, but it is "+objectDelta.getChangeType();
	}

	public static void assertPropertyReplace(ObjectDelta<?> objectDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta<Object> propertyDelta = objectDelta.findPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertReplace(propertyDelta, expectedValues);
	}

    public static void assertPropertyReplaceSimple(ObjectDelta<?> objectDelta, QName propertyName) {
        PropertyDelta<Object> propertyDelta = objectDelta.findPropertyDelta(propertyName);
        assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
        assertTrue("No values to replace", propertyDelta.getValuesToReplace() != null && !propertyDelta.getValuesToReplace().isEmpty());
    }

    public static <T> void assertReplace(PropertyDelta<T> propertyDelta, T... expectedValues) {
		assertSet("delta "+propertyDelta+" for "+propertyDelta.getElementName(), "replace", propertyDelta.getValuesToReplace(), expectedValues);
	}

	public static void assertPropertyAdd(ObjectDelta<?> objectDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = objectDelta.findPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyName, "add", propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	public static void assertPropertyDelete(ObjectDelta<?> objectDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = objectDelta.findPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyName, "delete", propertyDelta.getValuesToDelete(), expectedValues);
	}

	public static <T> void assertPropertyReplace(ObjectDelta<?> userDelta, ItemPath propertyPath, T... expectedValues) {
		PropertyDelta<T> propertyDelta = userDelta.findPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyPath.last(), "replace", propertyDelta.getValuesToReplace(), expectedValues);
	}

	public static void assertPropertyAdd(ObjectDelta<?> userDelta, ItemPath propertyPath, Object... expectedValues) {
		PropertyDelta<Object> propertyDelta = userDelta.findPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertAdd(propertyDelta, expectedValues);
	}
		
	public static <T> void assertAdd(PropertyDelta<T> propertyDelta, T... expectedValues) {
		assertSet("delta "+propertyDelta+" for "+propertyDelta.getElementName(), "add", propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	public static <T> void assertDelete(PropertyDelta<T> propertyDelta, T... expectedValues) {
		assertSet("delta "+propertyDelta+" for "+propertyDelta.getElementName(), "delete", propertyDelta.getValuesToDelete(), expectedValues);
	}

	public static void assertPropertyDelete(ObjectDelta<?> userDelta, ItemPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.findPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyPath.last(), "delete", propertyDelta.getValuesToDelete(), expectedValues);
	}
	
	public static void assertPropertyReplace(Collection<? extends ItemDelta> modifications, ItemPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = ItemDelta.findPropertyDelta(modifications, propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyPath.last(), "replace", propertyDelta.getValuesToReplace(), expectedValues);
	}

	public static void assertPropertyAdd(Collection<? extends ItemDelta> modifications, ItemPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = ItemDelta.findPropertyDelta(modifications, propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyPath.last(), "add", propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	public static void assertPropertyDelete(Collection<? extends ItemDelta> modifications, ItemPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = ItemDelta.findPropertyDelta(modifications, propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet("delta "+propertyDelta+" for "+propertyPath.last(), "delete", propertyDelta.getValuesToDelete(), expectedValues);
	}
	
	public static void assertNoItemDelta(ObjectDelta<?> userDelta, ItemPath propertyPath) {
		if (userDelta == null) {
			return;
		}
		assert !userDelta.hasItemDelta(propertyPath) : "Delta for item "+propertyPath+" present while not expecting it";
	}
	
	public static ContainerDelta<?> assertContainerAdd(ObjectDelta<?> objectDelta, QName name) {
		return assertContainerAdd(objectDelta, new ItemPath(name));
	}
	
	public static ContainerDelta<?> assertContainerAdd(ObjectDelta<?> objectDelta, ItemPath propertyPath) {
		ContainerDelta<?> delta = objectDelta.findContainerDelta(propertyPath);
		assertNotNull("Container delta for "+propertyPath+" not found",delta);
		assert !delta.isEmpty() : "Container delta for "+propertyPath+" is empty";
		assert delta.getValuesToAdd() != null : "Container delta for "+propertyPath+" has null values to add";
		assert !delta.getValuesToAdd().isEmpty() : "Container delta for "+propertyPath+" has empty values to add";
		return delta;
	}

	public static ContainerDelta<?> assertContainerDelete(ObjectDelta<?> objectDelta, QName name) {
		return assertContainerDelete(objectDelta, new ItemPath(name));
	}
	
	public static ContainerDelta<?> assertContainerDelete(ObjectDelta<?> objectDelta, ItemPath propertyPath) {
		ContainerDelta<?> delta = objectDelta.findContainerDelta(propertyPath);
		assertNotNull("Container delta for "+propertyPath+" not found",delta);
		assert !delta.isEmpty() : "Container delta for "+propertyPath+" is empty";
		assert delta.getValuesToDelete() != null : "Container delta for "+propertyPath+" has null values to delete";
		assert !delta.getValuesToDelete().isEmpty() : "Container delta for "+propertyPath+" has empty values to delete";
		return delta;
	}
	
	public static <T> void assertOrigin(Visitable visitableItem, final OriginType... expectedOriginTypes) {
		assertOrigin(visitableItem, null, expectedOriginTypes);
	}
	
	public static <T> void assertOrigin(final Visitable visitableItem, final Objectable expectedOriginObject, final OriginType... expectedOriginTypes) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if (visitable instanceof PrismValue) {
					PrismValue pval = (PrismValue)visitable;
					
					assert MiscUtil.contains(pval.getOriginType(), expectedOriginTypes) : "Wrong origin type in "+visitable+" in "+visitableItem+
							"; expected "+expectedOriginTypes+", was "+pval.getOriginType();
					if (expectedOriginObject != null) {
						assert pval.getOriginObject() == expectedOriginObject : "Wrong origin object in "+visitable+" in "+visitableItem+
								"; expected "+expectedOriginObject+", was "+pval.getOriginObject();
					}
				}
			}
		};
		visitableItem.accept(visitor);
	}
	
	public static void asserHasDelta(String message, Collection<? extends ObjectDelta<? extends Objectable>> deltas, ChangeType expectedChangeType, Class<?> expectedClass) {
		for (ObjectDelta<?> delta: deltas) {
			if (delta.getObjectTypeClass() == expectedClass && delta.getChangeType() == expectedChangeType) {
				return;
			}
		}
		assert false : message+": Delta for "+expectedClass+" of type "+expectedChangeType+" was not found in collection "+deltas;
	}
		
	public static <V extends PrismValue> void assertNoReplace(ItemDelta<V> delta) {
		assertNoReplace(null, delta);
	}
	
	public static <V extends PrismValue> void assertNoReplace(String message, ItemDelta<V> delta) {
		assertNoSet(message, "replace", delta.getValuesToReplace());
	}
	
	public static <V extends PrismValue> void assertNoAdd(ItemDelta<V> delta) {
		assertNoAdd(null, delta);
	}

	public static <V extends PrismValue> void assertNoAdd(String message, ItemDelta<V> delta) {
		assertNoSet(message, "add", delta.getValuesToAdd());
	}
	
	public static <V extends PrismValue> void assertNoDelete(ItemDelta<V> delta) {
		assertNoDelete(null, delta);
	}

	public static <V extends PrismValue> void assertNoDelete(String message, ItemDelta<V> delta) {
		assertNoSet(message, "delete", delta.getValuesToDelete());
	}

	private static <V extends PrismValue> void assertNoSet(String message, String type, Collection<V> set) {
		assert set == null : (message == null ? "" : message + ": ") + "Delta "+type+" set expected to be null but it is "+set;
	}
	
	public static void assertNoDelta(String message, ObjectDelta<?> delta) {
		assert delta == null : "Unexpected "+message+"; got "+delta;
	}

	
	// DeltaSetTriple asserts
	
	public static <T, V extends PrismValue> void assertTriplePlus(PrismValueDeltaSetTriple<V> triple, T... expectedValues) {
		assert triple != null : "triple is null";
		assertTripleSet("plus set", triple.getPlusSet(), expectedValues);
	}

	public static <T, V extends PrismValue> void assertTripleZero(PrismValueDeltaSetTriple<V> triple, T... expectedValues) {
		assert triple != null : "triple is null";
		assertTripleSet("zero set", triple.getZeroSet(), expectedValues);
	}

	public static <T, V extends PrismValue> void assertTripleMinus(PrismValueDeltaSetTriple<V> triple, T... expectedValues) {
		assert triple != null : "triple is null";
		assertTripleSet("minus set", triple.getMinusSet(), expectedValues);
	}
	
	public static <T, V extends PrismValue> void assertTripleSet(String setName, Collection<V> tripleSet, T... expectedValues) {
		assert tripleSet.size() == expectedValues.length : "Unexpected number of elements in triple "+setName+", expected "+
			expectedValues.length + ", was " + tripleSet.size() + ": "+tripleSet;
		for (T expectedValue: expectedValues) {
			boolean found = false;
			for (V tval: tripleSet) {
				if (tval instanceof PrismPropertyValue) {
					PrismPropertyValue<T> pval = (PrismPropertyValue<T>)tval;
					if (expectedValue.equals(pval.getValue())) {
						found = true;
						break;
					}
				} else {
					throw new IllegalArgumentException("Unknown type of prism value "+tval);
				}
			}
			if (!found) {
				assert false : "Expected value '"+DebugUtil.valueAndClass(expectedValue)+"' was not found in triple "+setName+"; values :"+tripleSet;
			}
		}
	}
	
	public static <V extends PrismValue> void assertTripleNoPlus(PrismValueDeltaSetTriple<V> triple) {
		assert triple != null : "triple is null";
		assertTripleNoSet("plus set", triple.getPlusSet());
	}

	public static <V extends PrismValue> void assertTripleNoZero(PrismValueDeltaSetTriple<V> triple) {
		assert triple != null : "triple is null";
		assertTripleNoSet("zero set", triple.getZeroSet());
	}

	public static <V extends PrismValue> void assertTripleNoMinus(PrismValueDeltaSetTriple<V> triple) {
		assert triple != null : "triple is null";
		assertTripleNoSet("minus set", triple.getMinusSet());
	}
	
	public static <V extends PrismValue> void assertTripleNoSet(String setName, Collection<V> set) {
		assert set == null || set.isEmpty() : "Expected triple "+setName+" to be empty, but it was: "+set;
	}
	
	public static void assertEquals(String message, PolyString expected, PolyString actual) {
		assert expected.equals(actual) : message + "; expected " + DebugUtil.dump(expected) + ", was " +
					DebugUtil.dump(actual);
	}

	public static void assertEqualsPolyString(String message, String expectedOrig, PolyString actual) {
		PolyString expected = new PolyString(expectedOrig);
		expected.recompute(PrismTestUtil.getPrismContext().getDefaultPolyStringNormalizer());
		assertEquals(message, expected, actual);
	}

	public static void assertEqualsPolyString(String message, String expectedOrig, PolyStringType actual) {
		if (expectedOrig == null && actual == null) {
			return;
		}
		assert actual != null : message + ": null value";
		assert MiscUtil.equals(expectedOrig, actual.getOrig()) : message+"; expected orig '"+expectedOrig+ "' but was '" + actual.getOrig() + "'";
		PolyString expected = new PolyString(expectedOrig);
		expected.recompute(PrismTestUtil.getPrismContext().getDefaultPolyStringNormalizer());
		assert MiscUtil.equals(expected.getNorm(), actual.getNorm()) : message+"; expected norm '"+expected.getNorm()+ "' but was '" + actual.getNorm() + "'";
	}
	
	public static void assertEqualsPolyString(String message, PolyStringType expected, PolyStringType actual) {
		assert actual != null : message + ": null value";
		assert MiscUtil.equals(expected.getOrig(), actual.getOrig()) : message+"; expected orig '"+expected.getOrig()+ "' but was '" + actual.getOrig() + "'";
		assert MiscUtil.equals(expected.getNorm(), actual.getNorm()) : message+"; expected norm '"+expected.getNorm()+ "' but was '" + actual.getNorm() + "'";
	} 

	// Calendar asserts
	
	public static void assertEquals(String message, XMLGregorianCalendar expected, Object actual) {
		if (actual instanceof XMLGregorianCalendar) {
			XMLGregorianCalendar actualXmlCal = (XMLGregorianCalendar)actual;
			assertEquals(message, XmlTypeConverter.toMillis(expected), XmlTypeConverter.toMillis(actualXmlCal));
		} else {
			assert false : message+": expected instance of XMLGregorianCalendar but got "+actual.getClass().getName();
		}
	}

	// Misc
	
	public static void assertClass(String message, Class<?> expectedClass, PrismObject<?> actualObject) {
		assert actualObject != null : message + "is null";
		Class<?> actualCompileTimeClass = actualObject.getCompileTimeClass();
		assert actualCompileTimeClass == expectedClass : message+" wrong class, expected "+expectedClass+" but was "+actualCompileTimeClass;
	}
	
	public static void assertClass(String message, Class<?> expectedClass, Objectable actualObject) {
		assert actualObject != null : message + "is null";
		Class<?> actualCompileTimeClass = actualObject.getClass();
		assert actualCompileTimeClass == expectedClass : message+" wrong class, expected "+expectedClass+" but was "+actualCompileTimeClass;
	}
	
	public static void assertClass(String message, Class<?> expectedClass, ObjectDelta<?> actualDelta) {
		assert actualDelta != null : message + "is null";
		Class<?> actualCompileTimeClass = actualDelta.getObjectTypeClass();
		assert actualCompileTimeClass == expectedClass : message+" wrong class, expected "+expectedClass+" but was "+actualCompileTimeClass;
	}
	
	// OBJECT asserts
	
	public static void assertElementsEquals(Object expected, Object actual) throws SchemaException {
		assertEquals(elementToPrism(expected), elementToPrism(actual));
    }
	
	public static void assertEquals(File fileNewXml, String objectString) throws SchemaException, IOException {
		assertEquals(toPrism(fileNewXml), toPrism(objectString));
    }
	
	public static void assertEquals(Objectable expected, Objectable actual) throws SchemaException {
		assertEquals(actual.asPrismObject(), actual.asPrismObject());
    }
	
	public static void assertEquals(File fileNewXml, Objectable objectable) throws SchemaException, IOException {
		assertEquals(toPrism(fileNewXml), objectable.asPrismObject());
    }
	
	public static <O extends Objectable> void assertEquals(File fileNewXml, PrismObject<O> actual) throws SchemaException, IOException {
		PrismObject<O> expected = toPrism(fileNewXml);
		assertEquals(expected, actual);
    }

	public static <O extends Objectable> void assertEquals(PrismObject<O> prism1, PrismObject<O> prism2) {
		assertEquals(null, prism1, prism2);
	}
	
	public static <O extends Objectable> void assertEquals(String message, PrismObject<O> expected, PrismObject<O> actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null) {
			fail(message + ": expected null, was "+actual);
		}
		if (actual == null) {
			fail(message + ": expected "+expected+", was null");
		}
		if (expected.equals(actual)) {
			return;
		}
		if (message == null) {
			message = "Prism object not equal";
		}
		ObjectDelta delta = expected.diff(actual);
		String suffix = "the difference: "+delta;
		if (delta.isEmpty()) {
			suffix += ": Empty delta. The difference is most likely in meta-data";
		}
		LOGGER.error("ASSERT: {}: {} and {} not equals, delta:\n{}", new Object[]{
				message, expected, actual, delta.debugDump()
		});
		assert false: message + ": " + suffix;
	}
	
	public static <O extends Objectable> void assertEquivalent(File expectedFile, PrismObject<O> actual) throws SchemaException, IOException {
		assertEquivalent("Object "+actual+" not equivalent to that from file "+expectedFile,expectedFile,actual);
	}
	
	public static <O extends Objectable> void assertEquivalent(String message, File expectedFile, PrismObject<O> actual) throws SchemaException, IOException {
		PrismObject<O> expected = toPrism(expectedFile);
		assertEquivalent(message, expected, actual);
	}
	
	public static <O extends Objectable> void assertEquivalent(String message, PrismObject<O> expected, PrismObject<O> actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null) {
			fail(message + ": expected null, was "+actual);
		}
		if (actual == null) {
			fail(message + ": expected "+expected+", was null");
		}
		if (expected.equivalent(actual)) {
			return;
		}
		if (message == null) {
			message = "Prism object not equal";
		}
		ObjectDelta<O> delta = expected.diff(actual);
		String suffix = "the difference: "+delta;
		if (delta.isEmpty()) {
			suffix += ": Empty delta. This is not expected. Somethig has got quite wrong here.";
		}
		LOGGER.error("ASSERT: {}: {} and {} not equivalent, delta:\n{}", new Object[]{
				message, expected, actual, delta.debugDump()
		});
		assert false: message + ": " + suffix;
	}

	private static <T> void assertSet(String inMessage, String setName, Collection<PrismPropertyValue<T>> actualPValues, T[] expectedValues) {
		assertValues(setName+" set in "+inMessage, actualPValues, expectedValues);
	}
	
	public static <T> void assertValues(String message, Collection<PrismPropertyValue<T>> actualPValues, T[] expectedValues) {
		assertNotNull("Null set in " + message, actualPValues);
		assertEquals("Wrong number of values in " + message, expectedValues.length, actualPValues.size());
		for (PrismPropertyValue<?> actualPValue: actualPValues) {
			boolean found = false;
			for (T value: expectedValues) {
				if (value.equals(actualPValue.getValue())) {
					found = true;
				}
			}
			if (!found) {
				fail("Unexpected value "+actualPValue+" in " + message + "; expected (real values) "
						+PrettyPrinter.prettyPrint(expectedValues)+"; has (pvalues) "+actualPValues);
			}
		}
	}
	
	public static <T> void assertSets(String message, Collection<T> actualValues, T... expectedValues) {
		assertSets(message, null, actualValues, expectedValues);
	}
	
	public static <T> void assertSets(String message, MatchingRule<T> matchingRule, Collection<T> actualValues, T... expectedValues) {
		assertNotNull("Null set in " + message, actualValues);
		assertEquals("Wrong number of values in " + message, expectedValues.length, actualValues.size());
		for (T actualValue: actualValues) {
			boolean found = false;
			for (T value: expectedValues) {
				if (matchingRule == null) {
					if (value.equals(actualValue)) {
						found = true;
					}
				} else {
					if (matchingRule.match(value, actualValue)) {
						found = true;
					}
				}
			}
			if (!found) {
				fail("Unexpected value "+actualValue+" in " + message + "; expected (real values) "
						+PrettyPrinter.prettyPrint(expectedValues)+"; has (pvalues) "+actualValues);
			}
		}
	}
	
	private static <O extends Objectable> PrismObject<O> toPrism(String objectString) throws SchemaException {
		return PrismTestUtil.parseObject(objectString);
	}

	private static <O extends Objectable> PrismObject<O> toPrism(File objectFile) throws SchemaException, IOException {
		return PrismTestUtil.parseObject(objectFile);
	}
	
	private static <O extends Objectable> PrismObject<O> toPrism(Element domNode) throws SchemaException {
		return PrismTestUtil.parseObject(domNode);
	}

	private static <O extends Objectable> PrismObject<O> elementToPrism(Object element) throws SchemaException {
		if (element instanceof Element) {
			return toPrism((Element)element);
		} else if (element instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbElement = (JAXBElement)element;
			Object value = jaxbElement.getValue();
			if (value instanceof Objectable) {
				return ((Objectable)value).asPrismObject();
			} else {
				throw new IllegalArgumentException("Unknown JAXB element value "+value);
			}
		} else {
			throw new IllegalArgumentException("Unknown element type "+element);
		}
	}
	
	// XNode asserts
	
	public static void assertSize(MapXNode xmap, int expectedSize) {
		assertEquals("Wrong size of "+xmap, expectedSize, xmap.size());
	}
	
	public static void assertSize(ListXNode xlist, int expectedSize) {
		assertEquals("Wrong size of "+xlist, expectedSize, xlist.size());
	}
	
	public static void assertSubnode(MapXNode xmap, QName key, Class expectedClass) {
		XNode xsubnode = xmap.get(key);
		assert xsubnode != null : "No subnode "+key+" in "+xmap;
		assert expectedClass.isAssignableFrom(xsubnode.getClass()) : "Wrong class of subnode "+key+" in "+xmap+"; expected "+expectedClass+", got "+xsubnode.getClass();
	}
	
	public static void assertAllParsedNodes(XNode xnode) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				if ((visitable instanceof PrimitiveXNode<?>)) {
					assert ((PrimitiveXNode<?>)visitable).isParsed() : "Xnode "+visitable+" is not parsed";
				}
			}
		};
		xnode.accept(visitor);
	}
	
	// Query asserts
	
	public static void assertOrFilter(ObjectFilter filter, int conditions) {
		assertEquals("Wrong filter class", OrFilter.class, filter.getClass());
		assertEquals("Wrong number of filter conditions", conditions, ((OrFilter) filter).getConditions().size());
	}

	public static void assertAndFilter(ObjectFilter filter, int conditions) {
		assertEquals("Wrong filter class", AndFilter.class, filter.getClass());
		assertEquals("Wrong number of filter conditions", conditions, ((AndFilter) filter).getConditions().size());
	}
	
	public static void assertEqualsFilter(ObjectFilter objectFilter, QName expectedFilterDef,
			QName expectedTypeName, ItemPath path) {
		assertEquals("Wrong filter class", EqualFilter.class, objectFilter.getClass());
		EqualFilter filter = (EqualFilter) objectFilter;
		//we don't have definition in all situation..this is almost OK..it will be computed dynamicaly
		if (filter.getDefinition() != null){
			assertEquals("Wrong filter definition element name", expectedFilterDef, filter.getDefinition().getName());
			assertEquals("Wrong filter definition type", expectedTypeName, filter.getDefinition().getTypeName());
		}
		assertEquals("Wrong filter path", path, filter.getFullPath());
	}
	
	public static <T> void assertEqualsFilterValue(EqualFilter filter, T value) {
		List<? extends PrismValue> values = filter.getValues();
		assertEquals("Wrong number of filter values", 1, values.size());
		assertEquals("Wrong filter value class", PrismPropertyValue.class, values.get(0).getClass());
		PrismPropertyValue val = (PrismPropertyValue) values.get(0);
		assertEquals("Wrong filter value", value, val.getValue());
	}

	
	public static void assertRefFilter(ObjectFilter objectFilter, QName expectedFilterDef, QName expectedTypeName,
			ItemPath path) {
		assertEquals("Wrong filter class", RefFilter.class, objectFilter.getClass());
		RefFilter filter = (RefFilter) objectFilter;
		assertEquals("Wrong filter definition element name", expectedFilterDef, filter.getDefinition().getName());
		assertEquals("Wrong filter definition type", expectedTypeName, filter.getDefinition().getTypeName());
		assertEquals("Wrong filter path", path, filter.getFullPath());
	}
	
	// Local version of JUnit assers to avoid pulling JUnit dependecy to main
	
	static void assertNotNull(String string, Object object) {
		assert object != null : string;
	}

    private static void assertTrue(String message, boolean test) {
        assert test : message;
    }

    public static void assertEquals(String message, Object expected, Object actual) {
		assert MiscUtil.equals(expected, actual) : message 
				+ ": expected " + MiscUtil.getValueWithClass(expected)
				+ ", was " + MiscUtil.getValueWithClass(actual);
	}
	
	public static <T> void assertEquals(String message, MatchingRule<T> matchingRule, T expected, T actual) {
		assert equals(matchingRule, expected, actual) : message 
				+ ": expected " + MiscUtil.getValueWithClass(expected)
				+ ", was " + MiscUtil.getValueWithClass(actual);
	}
	
	static void assertSame(String message, Object expected, Object actual) {
		assert expected == actual : message 
				+ ": expected ("+expected.getClass().getSimpleName() + ")"  + expected 
				+ ", was (" + actual.getClass().getSimpleName() + ")" + actual;
	}
	
	static void fail(String message) {
		assert false: message;
	}
	
	private static <T> boolean equals(MatchingRule<T> matchingRule, T a, T b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (matchingRule == null) {
			return a.equals(b);
		} else {
			return matchingRule.match(a, b);
		}
	}

	private static boolean equals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
	
	public static <T> void assertEqualsCollectionUnordered(String message, Collection<T> actualCollection, T... expectedValues) {
		List<T> expectedCollection = Arrays.asList(expectedValues);
		assert MiscUtil.unorderedCollectionEquals(actualCollection, expectedCollection) : message + ": expected "+expectedCollection+
			"; was "+actualCollection;
	}

	public static void assertAssignableFrom(Class<?> expected, Class<?> actual) {
		assert expected.isAssignableFrom(actual) : "Expected "+expected+" but got "+actual;
	}

	public static void assertAssignableFrom(Class<?> expected, Object actualObject) {
		assert expected.isAssignableFrom(actualObject.getClass()) : "Expected "+expected+" but got "+actualObject.getClass();
	}

}
