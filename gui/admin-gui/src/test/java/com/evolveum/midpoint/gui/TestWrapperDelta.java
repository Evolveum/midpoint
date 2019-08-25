package com.evolveum.midpoint.gui;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.AbstractInitializedGuiIntegrationTest;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
public class TestWrapperDelta extends AbstractInitializedGuiIntegrationTest {

    private static final transient Trace LOGGER = TraceManager.getTrace(TestWrapperDelta.class);

    private static final String TEST_DIR = "src/test/resources/delta";

    private static final File USER_ELAINE = new File(TEST_DIR, "user-elaine.xml");
    private static final String USER_ELAINE_OID = "00998628-b2fd-11e5-88c0-4f82a8602266";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importObjectFromFile(USER_ELAINE, initTask, initResult);
    }

    @Test
    public void test100modifyUserFullname() throws Exception {
        final String TEST_NAME = "test100CreateWrapperUserJack";
        TestUtil.displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TEST_NAME);

        OperationResult result = task.getResult();

        PrismObject<UserType> userElaineBefore = getUser(USER_ELAINE_OID);

        WrapperContext ctx = new WrapperContext(task, result);
        PrismObjectWrapper objectWrapper =createObjectWrapper(userElaineBefore, ItemStatus.NOT_CHANGED, ctx);

//        ModelServiceLocator locator = getServiceLocator(task);
//        PrismObjectWrapperFactory objectFactory = locator.findObjectWrapperFactory(userElaineBefore.getDefinition());
//
//        PrismObjectWrapper objectWrapper = objectFactory.createObjectWrapper(userElaineBefore, ItemStatus.NOT_CHANGED, ctx);

        PrismPropertyWrapper<PolyString> fullName = objectWrapper.findProperty(UserType.F_FULL_NAME);
        fullName.getValue().setRealValue(PrismTestUtil.createPolyString("Sparrow-Marley"));


        //GIVEN
        ObjectDelta<UserType> elaineDelta = objectWrapper.getObjectDelta();
        assertModificationsSize(elaineDelta, 1);

        assertModification(elaineDelta, UserType.F_FULL_NAME, ModificationTypeType.REPLACE, PrismTestUtil.createPolyString("Sparrow-Marley"));
//        assertNotNull("Unexpeted null delta", elaineDelta);
//
//        Collection<? extends ItemDelta<?,?>> modifications = elaineDelta.getModifications();
//        assertEquals( ObjectDelta<UserType> elaineDelta = objectWrapper.getObjectDelta();
//        assertNotNull("Unexpeted null delta", elaineDelta);
//
//        Collection<? extends ItemDelta<?,?>> modifications = elaineDelta.getModifications();
//        assertEquals("Enexpected modifications size", 1, modifications.size());

//        PropertyDelta<PolyString> fullNameDelta = elaineDelta.findPropertyDelta(UserType.F_FULL_NAME);
//        assertNotNull("Unexpected null fullName delta", fullNameDelta);
//        assertTrue(fullNameDelta.isReplace());
//
//        Collection<PrismPropertyValue<PolyString>> valuesToReplace = fullNameDelta.getValuesToReplace();
//        assertEquals("Unexpected numbers of values", 1, valuesToReplace.size());
//        PrismPropertyValue<PolyString> value = valuesToReplace.iterator().next();
//        assertEquals("Unexpected value to replace", "Sparrow-Marley", value.getValue().getOrig());


        //WHEN
        executeChanges(elaineDelta, null, task, result);

        //THEN
        PrismObject<UserType> userElaineAfter = getUser(USER_ELAINE_OID);
        UserAsserter.forUser(userElaineAfter).assertFullName("Sparrow-Marley");
    }

    @Test
    public void test101modifyUserWeapon() throws Exception {
        final String TEST_NAME = "test101modifyUserWeapon";
        TestUtil.displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TEST_NAME);

        OperationResult result = task.getResult();

        PrismObject<UserType> userElaineBefore = getUser(USER_ELAINE_OID);

        WrapperContext ctx = new WrapperContext(task, result);
        PrismObjectWrapper objectWrapper =createObjectWrapper(userElaineBefore, ItemStatus.NOT_CHANGED, ctx);

        PrismPropertyWrapper<String> weapon = objectWrapper.findProperty(ItemPath.create(UserType.F_EXTENSION, PIRACY_WEAPON));
        for (PrismPropertyValueWrapper<String> valueWrapper : weapon.getValues()) {
            valueWrapper.setRealValue(null);
        }

        ModelServiceLocator locator = getServiceLocator(task);
        PrismPropertyValueWrapper<String> newValue = locator.createValueWrapper(weapon, new PrismPropertyValueImpl("revolver"), ValueStatus.ADDED, ctx);
        weapon.getValues().add(newValue);

        ObjectDelta<UserType> elaineDelta = objectWrapper.getObjectDelta();
        assertModificationsSize(elaineDelta, 1);

        assertModification(elaineDelta, ItemPath.create(UserType.F_EXTENSION, PIRACY_WEAPON), ModificationTypeType.ADD, "revolver");
        assertModification(elaineDelta, ItemPath.create(UserType.F_EXTENSION, PIRACY_WEAPON), ModificationTypeType.DELETE, "pistol", "mouth");

        executeChanges(elaineDelta, null, task, result);

        PrismObject<UserType> userElaineAfter = getUser(USER_ELAINE_OID);
        LOGGER.info("ELAINE AFTER: {}", userElaineAfter.debugDump());
        UserAsserter.forUser(userElaineAfter).extension().assertPropertyValuesEqual(PIRACY_WEAPON, "revolver");

    }

    @Test
    public void test110modifyUserAddAssignment() throws Exception {
        final String TEST_NAME = "test110modifyUserAddAssignment";
        TestUtil.displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TEST_NAME);

        OperationResult result = task.getResult();

        PrismObject<UserType> userElaineBefore = getUser(USER_ELAINE_OID);

        WrapperContext ctx = new WrapperContext(task, result);
        PrismObjectWrapper objectWrapper =createObjectWrapper(userElaineBefore, ItemStatus.NOT_CHANGED, ctx);

        PrismContainerWrapper<AssignmentType> assignment = objectWrapper.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("unexpected null assignment wrapper", assignment);
        assertEquals("Unexpected values for assignment " + assignment.getValues().size(), 0, assignment.getValues().size());

        ModelServiceLocator locator = getServiceLocator(task);

        PrismContainerValue<AssignmentType> newAssignment = assignment.getItem().createNewValue();
        AssignmentType newAssignmentType = newAssignment.asContainerable();

        ConstructionType constructionType = new ConstructionType(locator.getPrismContext());
        constructionType.setResourceRef(ObjectTypeUtil.createObjectRef(resourceDummy, locator.getPrismContext()));
        constructionType.setKind(ShadowKindType.ACCOUNT);
        constructionType.setIntent("default");

        newAssignmentType.setConstruction(constructionType);

        PrismContainerValue<AssignmentType> newAssignmentClone = newAssignment.clone();

        PrismContainerValueWrapper vw = locator.createValueWrapper(assignment, newAssignment, ValueStatus.ADDED, ctx);
        assignment.getValues().add(vw);

        ObjectDelta<UserType> delta = objectWrapper.getObjectDelta();
        assertModificationsSize(delta, 1);

        assertModification(delta, UserType.F_ASSIGNMENT, ModificationTypeType.ADD, newAssignmentClone.asContainerable());

        executeChanges(delta, null, task, result);
        assertSuccess(result);

        PrismObject<UserType> userElaineAfter = getUser(USER_ELAINE_OID);
        UserAsserter.forUser(userElaineAfter).assignments().assertAssignments(1);

        PrismContainer<AssignmentType> assignmentAfter = userElaineAfter.findContainer(UserType.F_ASSIGNMENT);
        List<PrismContainerValue<AssignmentType>> assignmentValues = assignmentAfter.getValues();
        assertEquals("Unexpected number of assignments " + assignmentValues.size(), 1, assignmentValues.size());

        PrismContainerValue<AssignmentType> assignmentValue = assignmentValues.iterator().next();
        assertTrue(newAssignmentClone.equals(assignmentValue, ParameterizedEquivalenceStrategy.IGNORE_METADATA_CONSIDER_DIFFERENT_IDS));

    }

    @Test
    public void test111modifyUserAssignemnt() throws Exception {
        final String TEST_NAME = "test110modifyUserAddAssignment";
        TestUtil.displayTestTitle(TEST_NAME);
        Task task = taskManager.createTaskInstance(TEST_NAME);

        OperationResult result = task.getResult();

        PrismObject<UserType> userElaineBefore = getUser(USER_ELAINE_OID);

        WrapperContext ctx = new WrapperContext(task, result);
        PrismObjectWrapper<UserType> objectWrapper =createObjectWrapper(userElaineBefore, ItemStatus.NOT_CHANGED, ctx);

        PrismContainerWrapper<AssignmentType> assignment = objectWrapper.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("unexpected null assignment wrapper", assignment);
        assertEquals("Unexpected values for assignment " + assignment.getValues().size(), 1, assignment.getValues().size());

        PrismContainerValueWrapper<AssignmentType> assignmentValue = assignment.getValues().iterator().next();
        PrismContainerWrapper<ResourceAttributeDefinitionType> resourceAttrDefWrapper = assignmentValue.findContainer(ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE));
        assertNotNull("unexpected null assignment wrapper", resourceAttrDefWrapper);
        assertEquals("Unexpected values for assignment " + resourceAttrDefWrapper.getValues().size(), 0, resourceAttrDefWrapper.getValues().size());

        ModelServiceLocator locator = getServiceLocator(task);

        PrismContainerValue<ResourceAttributeDefinitionType> newAttribute = resourceAttrDefWrapper.getItem().createNewValue();

        PrismContainerValueWrapper<ResourceAttributeDefinitionType> resourceAttrDefValueWrapper = locator.createValueWrapper(resourceAttrDefWrapper, newAttribute, ValueStatus.ADDED, ctx);
        resourceAttrDefWrapper.getValues().add(resourceAttrDefValueWrapper);

        PrismPropertyWrapper<ItemPathType> attributeRefWrapper = resourceAttrDefValueWrapper.findProperty(ResourceAttributeDefinitionType.F_REF);
        attributeRefWrapper.getValue().setRealValue(new ItemPathType(ItemPath.create(SchemaConstants.ICFS_NAME)));

        PrismPropertyWrapper<QName> matchingRule = resourceAttrDefValueWrapper.findProperty(ResourceAttributeDefinitionType.F_MATCHING_RULE);
        matchingRule.getValue().setRealValue(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME);

        PrismContainerWrapper<MappingType> outbound = resourceAttrDefValueWrapper.findContainer(ResourceAttributeDefinitionType.F_OUTBOUND);

        PrismContainerValueWrapper<MappingType> mapping = outbound.getValue();

        MappingType mappingType = mapping.getRealValue();
        mappingType.setExpression(createAsIsExpression());

        ObjectDelta<UserType> delta = objectWrapper.getObjectDelta();
        assertModificationsSize(delta, 1);
        LOGGER.info("Attr delta: {}", delta.debugDump());

        ResourceAttributeDefinitionType expectedvalue = new ResourceAttributeDefinitionType(locator.getPrismContext());
        expectedvalue.setRef(new ItemPathType(ItemPath.create(SchemaConstants.ICFS_NAME)));
        expectedvalue.setMatchingRule(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME);
        MappingType expectedMapping = new MappingType();
        expectedMapping.setExpression(createAsIsExpression());
        expectedvalue.setOutbound(expectedMapping);

        assertModification(delta, resourceAttrDefValueWrapper.getPath(), ModificationTypeType.ADD, expectedvalue);

        executeChanges(delta, ModelExecuteOptions.createRaw(), task, result);
        assertSuccess(result);

        PrismObject<UserType> userElaineAfter = getUser(USER_ELAINE_OID);
        PrismContainer<AssignmentType> assignmentAfter = userElaineAfter.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("Unexpected null assignment container for " + userElaineAfter, assignmentAfter);

        assertEquals("Unexpected number of assignments, " + assignmentAfter.debugDump(), 1, assignmentAfter.getValues().size());
        PrismContainerValue<AssignmentType> assignmentTypeAfter = assignmentAfter.getValues().iterator().next();

        AssignmentType expected = new AssignmentType(locator.getPrismContext());
        ConstructionType constructionType = new ConstructionType(locator.getPrismContext());
        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(resourceDummy.getOid(), ObjectTypes.RESOURCE);
        ref.setRelation(SchemaConstants.ORG_DEFAULT);
        constructionType.setResourceRef(ref);
        constructionType.setKind(ShadowKindType.ACCOUNT);
        constructionType.setIntent("default");

        constructionType.getAttribute().add(expectedvalue);

        expected.setConstruction(constructionType);
        ActivationType activationType = new ActivationType(locator.getPrismContext());
        activationType.setEffectiveStatus(ActivationStatusType.ENABLED);
        expected.setActivation(activationType);

        //TODO assertions after

    }

    private ExpressionType createAsIsExpression() {
        ExpressionType expressionType = new ExpressionType();
        ObjectFactory objectFactory = new ObjectFactory();
        AsIsExpressionEvaluatorType asIsEvaluator = objectFactory.createAsIsExpressionEvaluatorType();
        JAXBElement<AsIsExpressionEvaluatorType> asIsJaxb = objectFactory.createAsIs(asIsEvaluator);

        expressionType.getExpressionEvaluator().add(asIsJaxb);
        return expressionType;
    }

    private <O extends ObjectType> void assertModificationsSize(ObjectDelta<O> delta, int expectedModifications) {
        assertNotNull("Unexpeted null delta", delta);
        LOGGER.trace("Delta: {}", delta.debugDump());

        Collection<? extends ItemDelta<?,?>> modifications = delta.getModifications();
        assertEquals("Unexpected modifications size", expectedModifications, modifications.size());

    }

    private <O extends ObjectType, D extends ItemDelta> void assertModification(ObjectDelta<O> delta, ItemPath itemPath, ModificationTypeType modificationType, Object... expectedValues) {
        D modification = (D) delta.findItemDelta(itemPath);
        assertNotNull("Unexpected null delta for " + itemPath, modification);
        Collection<? extends PrismValue> modificationValues = null;
        switch (modificationType) {
            case ADD:
                assertTrue("Expected add modification but was" + modification, modification.isAdd());
                modificationValues = modification.getValuesToAdd();
                break;
            case REPLACE:
                assertTrue("Expected replace modification but was" + modification, modification.isReplace());
                modificationValues = modification.getValuesToReplace();
                break;
            case DELETE:
                assertTrue("Expected delete modification but was" + modification, modification.isDelete());
                modificationValues = modification.getValuesToDelete();
                break;
        }


        assertEquals("Unexpected numbers of values", expectedValues.length, modificationValues.size());
        List<Object> realValues = modificationValues.stream().map(v -> v.getRealValue()).collect(Collectors.toList());
        for (Object expectedValue : expectedValues) {
            if (!realValues.contains(expectedValue)) {
                AssertJUnit.fail("Expected value: " + expectedValue + " not present in delta. Values present: " + realValues);
            }
        }

    }

    private <O extends ObjectType> PrismObjectWrapper<O> createObjectWrapper(PrismObject<O> object, ItemStatus status, WrapperContext context) throws Exception{
        ModelServiceLocator locator = getServiceLocator(context.getTask());
        PrismObjectWrapperFactory objectFactory = locator.findObjectWrapperFactory(object.getDefinition());

        PrismObjectWrapper objectWrapper = objectFactory.createObjectWrapper(object, status, context);
        return objectWrapper;
    }
}
