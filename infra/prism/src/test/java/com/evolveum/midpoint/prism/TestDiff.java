/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.util.Collection;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestDiff {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
    public void testUserSimplePropertyDiffNoChange() throws Exception {
		System.out.println("\n\n===[ testUserSimplePropertyDiffNoChange ]===\n");
		// GIVEN
		PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
		
		PrismObject<UserType> user1 = userDef.instantiate();
		user1.setOid(USER_JACK_OID);
		user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
		
		PrismObject<UserType> user2 = userDef.instantiate();
		user2.setOid(USER_JACK_OID);
		user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
		
		// WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);
        
        // THEN
        assertNotNull(delta);
        assertEquals("Unexpected number of midifications", 0, delta.getModifications().size());
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

	@Test
    public void testUserSimplePropertyDiffReplace() throws Exception {
		System.out.println("\n\n===[ testUserSimplePropertyDiffReplace ]===\n");
		// GIVEN
		PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
		
		PrismObject<UserType> user1 = userDef.instantiate();
		user1.setOid(USER_JACK_OID);
		user1.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("test name"));
		
		PrismObject<UserType> user2 = userDef.instantiate();
		user2.setOid(USER_JACK_OID);
		user2.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("other name"));
		
		// WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);
        
        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        assertEquals("Unexpected number of midifications", 1, delta.getModifications().size());
        PrismAsserts.assertPropertyReplace(delta, UserType.F_NAME, PrismTestUtil.createPolyString("other name"));
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testUserSimpleDiffMultiNoChange() throws Exception {
    	System.out.println("\n\n===[ testUserSimpleDiffMultiNoChange ]===\n");
    	
    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
		
		PrismObject<UserType> user1 = userDef.instantiate();
		user1.setOid(USER_JACK_OID);
		PrismProperty<String> anamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp1.addRealValue("foo");
		anamesProp1.addRealValue("bar");
		
		PrismObject<UserType> user2 = userDef.instantiate();
		user2.setOid(USER_JACK_OID);
		PrismProperty<String> anamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp2.addRealValue("foo");
		anamesProp2.addRealValue("bar");
		
		// WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);
        
        // THEN
        assertNotNull(delta);
        assertEquals("Unexpected number of midifications", 0, delta.getModifications().size());
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testUserSimpleDiffMultiAdd() throws Exception {
    	System.out.println("\n\n===[ testUserSimpleDiffMulti ]===\n");
    	
    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
		
		PrismObject<UserType> user1 = userDef.instantiate();
		user1.setOid(USER_JACK_OID);
		PrismProperty<String> anamesProp1 = user1.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp1.addRealValue("foo");
		anamesProp1.addRealValue("bar");
		
		PrismObject<UserType> user2 = userDef.instantiate();
		user2.setOid(USER_JACK_OID);
		PrismProperty<String> anamesProp2 = user2.findOrCreateProperty(UserType.F_ADDITIONAL_NAMES);
		anamesProp2.addRealValue("foo");
		anamesProp2.addRealValue("bar");
		anamesProp2.addRealValue("baz");
		
		// WHEN
        ObjectDelta<UserType> delta = user1.diff(user2);
        
        // THEN
        assertNotNull(delta);
        System.out.println(delta.debugDump());
        assertEquals("Unexpected number of midifications", 1, delta.getModifications().size());
        PrismAsserts.assertPropertyAdd(delta, UserType.F_ADDITIONAL_NAMES, "baz");
        assertEquals("Wrong OID", USER_JACK_OID, delta.getOid());
        delta.checkConsistence();
    }

    @Test
    public void testContainerSimpleDiffNoChange() throws Exception {
    	System.out.println("\n\n===[ testContainerSimpleDiffNoChange ]===\n");
    	
    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
    	PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
    	
    	PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
    	PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
    	ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());
    	
    	PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
    	PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
    	ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());
		
		// WHEN
    	Collection<? extends ItemDelta> modifications = ass1.diff(ass2);
        
        // THEN
        assertNotNull(modifications);
        System.out.println(DebugUtil.debugDump(modifications));
        assertEquals("Unexpected number of midifications", 0, modifications.size());
    	ItemDelta.checkConsistence(modifications);
    }

    @Test
    public void testContainerDiffDesciption() throws Exception {
    	System.out.println("\n\n===[ testContainerDiffDesciption ]===\n");
    	
    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
    	PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
    	
    	PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
    	PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
    	ass1cval.setId(1L);
    	ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());
    	
    	PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
    	PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
    	ass2cval.setId(1L);
    	ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala", PrismTestUtil.getPrismContext());
		
		// WHEN
    	Collection<? extends ItemDelta> modifications = ass1.diff(ass2);
        
        // THEN
        assertNotNull(modifications);
        System.out.println(DebugUtil.debugDump(modifications));
        assertEquals("Unexpected number of midifications", 1, modifications.size());
        PrismAsserts.assertPropertyReplace(
        		modifications, 
        		new ItemPath(
        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
        				new IdItemPathSegment(1L),
        				new NameItemPathSegment(AssignmentType.F_DESCRIPTION)),
        		"chamalalia patlama paprtala");
        ItemDelta.checkConsistence(modifications);
    }
    
    @Test
    public void testContainerValueDiffDesciptionNoPath() throws Exception {
    	System.out.println("\n\n===[ testContainerValueDiffDesciptionNoPath ]===\n");
    	
    	// GIVEN
    	PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
    	PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
    	
    	PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
    	PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
    	ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah", PrismTestUtil.getPrismContext());
    	
    	PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
    	PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
    	ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala", PrismTestUtil.getPrismContext());
		
		// WHEN
    	Collection<? extends ItemDelta> modifications = ass1cval.diff(ass2cval);
        
        // THEN
        assertNotNull(modifications);
        System.out.println(DebugUtil.debugDump(modifications));
        assertEquals("Unexpected number of midifications", 1, modifications.size());
        PrismAsserts.assertPropertyReplace(
        		modifications, 
        		new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION),
        		"chamalalia patlama paprtala");
        ItemDelta.checkConsistence(modifications);
    }

//    @Test
//    public void testContainerValueDiffDesciptionPath() throws Exception {
//    	System.out.println("\n\n===[ testContainerValueDiffDesciptionPath ]===\n");
//    	
//    	// GIVEN
//    	PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();
//    	PrismContainerDefinition<AssignmentType> assignmentContDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
//    	
//    	PrismContainer<AssignmentType> ass1 = assignmentContDef.instantiate();
//    	PrismContainerValue<AssignmentType> ass1cval = ass1.createNewValue();
//    	ass1cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "blah blah");
//    	
//    	PrismContainer<AssignmentType> ass2 = assignmentContDef.instantiate();
//    	PrismContainerValue<AssignmentType> ass2cval = ass2.createNewValue();
//    	ass2cval.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "chamalalia patlama paprtala");
//		
//		ItemPath pathPrefix = new ItemPath(
//				new NameItemPathSegment(UserType.F_ASSIGNMENT),
//				new IdItemPathSegment("1"));
//		
//		// WHEN
//    	Collection<? extends ItemDelta> modifications = ass1cval.diff(ass2cval, pathPrefix, true, false);
//        
//        // THEN
//        assertNotNull(modifications);
//        System.out.println(DebugUtil.debugDump(modifications));
//        assertEquals("Unexpected number of midifications", 1, modifications.size());
//        PrismAsserts.assertPropertyReplace(
//        		modifications, 
//        		new ItemPath(
//        				new NameItemPathSegment(UserType.F_ASSIGNMENT),
//        				new IdItemPathSegment("1"),
//        				new NameItemPathSegment(AssignmentType.F_DESCRIPTION)),
//        		"chamalalia patlama paprtala");
//        ItemDelta.checkConsistence(modifications);
//    }

}
