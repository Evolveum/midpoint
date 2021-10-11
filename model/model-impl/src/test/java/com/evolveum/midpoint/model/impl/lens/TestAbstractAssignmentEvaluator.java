/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.*;
import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createDuration;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_ADMINISTRATIVE_STATUS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.F_ACTIVATION;

import java.io.File;
import java.util.*;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestAbstractAssignmentEvaluator extends AbstractLensTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    @Qualifier("modelObjectResolver")
    private ObjectResolver objectResolver;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Autowired
    private RelationRegistry relationRegistry;

    @Autowired
    private Clock clock;

    @Autowired
    private ActivationComputer activationComputer;

    @Autowired
    private MappingFactory mappingFactory;

    @Autowired
    private MappingEvaluator mappingEvaluator;

    @Autowired
    private Projector projector;

    public abstract File[] getRoleCorpFiles();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(ORG_BRETHREN_FILE);
        addObject(TEMPLATE_DYNAMIC_ORG_ASSIGNMENT_FILE);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, DYNAMIC_ORG_ASSIGNMENT_EMPLOYEE_TYPE, TEMPLATE_DYNAMIC_ORG_ASSIGNMENT_OID, initResult);
    }

    @Test
    public void test100Direct() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_DIRECT_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testDirect", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
        displayDumpable("Evaluated construction", construction);
        assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test110DirectExpression() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_DIRECT_EXPRESSION_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());
        userOdo.recompute();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testDirect", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayDumpable("Evaluated assignment", evaluatedAssignment);
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
        assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test120DirectExpressionReplaceDescription() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        AssignmentType assignmentType = unmarshalValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
        user.asObjectable().getAssignment().add(assignmentType.clone());

        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT, 123L, AssignmentType.F_DESCRIPTION);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, USER_JACK_OID,
                        path, "captain");
        ObjectDeltaObject<UserType> userOdo = createUserOdo(user, userDelta);
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);

        display("Assignment old", assignmentType);
        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);
        assignmentIdi.setResolvePath(UserType.F_ASSIGNMENT);
        assignmentIdi.setSubItemDeltas(userDelta.getModifications());
        assignmentIdi.recompute();
        displayDumpable("Assignment IDI", assignmentIdi);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testDirect", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayDumpable("Evaluated assignment", evaluatedAssignment);
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(user);

        Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
        assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
        assertEquals(1, construction.getAttributeMappings().size());
        MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>> attributeMapping =
                (MappingImpl<PrismPropertyValue<String>, PrismPropertyDefinition<String>>) construction.getAttributeMappings().iterator().next();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = attributeMapping.getOutputTriple();
        displayDumpable("output triple", outputTriple);
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "The best captain the world has ever seen");
        PrismAsserts.assertTripleMinus(outputTriple, "The best pirate the world has ever seen");

        // the same using other words

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS, "The best captain the world has ever seen");
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS, "The best pirate the world has ever seen");
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test130DirectExpressionReplaceDescriptionFromNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        AssignmentType assignmentType = unmarshalValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
        assignmentType.setDescription(null);
        user.asObjectable().getAssignment().add(assignmentType.clone());

        ItemPath path = ItemPath.create(UserType.F_ASSIGNMENT, 123L, AssignmentType.F_DESCRIPTION);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, USER_JACK_OID,
                        path, "sailor");
        ObjectDeltaObject<UserType> userOdo = createUserOdo(user, userDelta);
        userOdo.recompute();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);
        assignmentIdi.setResolvePath(UserType.F_ASSIGNMENT);
        assignmentIdi.setSubItemDeltas(userDelta.getModifications());
        assignmentIdi.recompute();

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testDirect", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayDumpable("Evaluated assignment", evaluatedAssignment);
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(user);

        Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
        assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
        assertEquals(1, construction.getAttributeMappings().size());
        PrismValueDeltaSetTripleProducer<PrismPropertyValue<String>, PrismPropertyDefinition<String>> attributeMapping =
                (PrismValueDeltaSetTripleProducer<PrismPropertyValue<String>, PrismPropertyDefinition<String>>) construction.getAttributeMappings().iterator().next();
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = attributeMapping.getOutputTriple();
        PrismAsserts.assertTripleNoZero(outputTriple);
        PrismAsserts.assertTriplePlus(outputTriple, "The best sailor the world has ever seen");
        PrismAsserts.assertTripleMinus(outputTriple, "The best man the world has ever seen");

        // the same using other words

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS, "The best sailor the world has ever seen");
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS, "The best man the world has ever seen");
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    /*

    Explanation for roles structure (copied from role-corp-generic-metarole.xml)

        user-assignable roles:

          roles of unspecified type
          - Visitor
          - Customer

          roles of type: job
          - Contractor
          - Employee
            - Engineer (induces Employee)
            - Manager (induces Employee)

        metaroles:

          - Generic Metarole:                                   assigned to Visitor and Customer                            [ induces ri:location attribute - from user/locality ]
            - Job Metarole (induces Generic Metarole):          assigned to Contractor, Employee, Engineer, Manager         [ induces ri:title attribute - from role/name ]

     */

    @Test
    public void test140RoleVisitor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_VISITOR_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(
                assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack,
                getTestNameShort(), AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");
        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test142RoleVisitorDisabledAssignment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_VISITOR_FILE);
        assignmentType.setActivation(ActivationUtil.createDisabled());

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(
                assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack,
                getTestNameShort(), AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");
        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test150RoleEngineer() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testRoleEngineer", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee", "Engineer");
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test160AddRoleEngineer() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        AssignmentType assignmentForUser = assignmentType.clone();
        assignmentForUser.asPrismContainerValue().setParent(null);
        //noinspection unchecked
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(UserType.class, USER_JACK_OID, UserType.F_ASSIGNMENT,
                        assignmentForUser.asPrismContainerValue());
        ObjectDeltaObject<UserType> userOdo = createUserOdo(user, userDelta);
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(
                assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack,
                getTestNameShort(), AssignmentOrigin.createNotVirtual(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals("Wrong number of constructions", 4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        /*
         *  Here we observe an important thing about AssignmentEvaluator/AssignmentProcessor.
         *  The evaluator does not consider whether the assignment as such is being added or deleted or stays present.
         *  In all these cases all the constructions go into the ZERO set of constructions.
         *
         *  However, it considers changes in data that are used by assignments - either in conditions or in mappings.
         *  Changes of data used in mappings are demonstrated by testDirectExpressionReplaceDescription
         *  and testDirectExpressionReplaceDescriptionFromNull. Changes of data used in conditions are demonstrated by
         *  a couple of tests below.
         *
         *  Changes in assignment presence (add/delete) are reflected into plus/minus sets by AssignmentProcessor.
         */

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee", "Engineer");
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    /**
     * jack has assigned role Manager.
     * <p>
     * However, condition in job metarole for Manager is such that it needs "management"
     * to be present in user/costCenter in order to be active.
     */
    @Test
    public void test170RoleManagerChangeCostCenter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_MANAGER_FILE);

        AssignmentType assignmentForUser = assignmentType.clone();
        assignmentForUser.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentForUser);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, USER_JACK_OID,
                        UserType.F_COST_CENTER, "management");
        ObjectDeltaObject<UserType> userOdo = createUserOdo(user, userDelta);
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(
                assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack,
                getTestNameShort(), AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee");                   // because Employee's job metarole is active even if Manager's is not
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertConstruction(evaluatedAssignment, PLUS, "title", ZERO, "Manager");                    // because Manager's job metarole is originally not active
        assertConstruction(evaluatedAssignment, PLUS, "title", PLUS);
        assertConstruction(evaluatedAssignment, PLUS, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");               // because Generic Metarole is active all the time
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");
    }

    /**
     * jack has assigned role Manager.
     * <p>
     * However, condition in job metarole for Manager is such that it needs "management"
     * to be present in user/costCenter in order to be active.
     * <p>
     * In this test we remove the value of "management" from jack.
     */
    @Test
    public void test180RoleManagerRemoveCostCenter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        user.asObjectable().setCostCenter("management");
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_MANAGER_FILE);

        AssignmentType assignmentForUser = assignmentType.clone();
        assignmentForUser.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentForUser);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, USER_JACK_OID,
                        UserType.F_COST_CENTER);
        ObjectDeltaObject<UserType> userOdo = createUserOdo(user, userDelta);
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, getTestNameShort(), AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee");                   // because Employee's job metarole is active even if Manager's is not
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertConstruction(evaluatedAssignment, MINUS, "title", ZERO, "Manager");                    // because Manager's job metarole is not active any more
        assertConstruction(evaluatedAssignment, MINUS, "title", PLUS);
        assertConstruction(evaluatedAssignment, MINUS, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");               // because Generic Metarole is active all the time
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");
    }

    /**
     * Disable Engineer -> Employee inducement.
     */

    @Test(enabled = false)
    public void test200DisableEngineerEmployeeInducement() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // disable Engineer->Employee inducement
        ObjectDelta disableInducementDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT, 3, F_ACTIVATION, F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(ROLE_CORP_ENGINEER_OID);
        modelService.executeChanges(Collections.singletonList(disableInducementDelta),
                null, task, result);

        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testRoleEngineer", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());

        // TODO
    }

    @Test(enabled = false)
    public void test299ReenableEngineerEmployeeInducement() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // disable Engineer->Employee inducement
        ObjectDelta enableInducementDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT, 3, F_ACTIVATION, F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(ROLE_CORP_ENGINEER_OID);
        modelService.executeChanges(Collections.singletonList(enableInducementDelta),
                null, task, result);
    }

    /**
     * Jack is an Engineer which induces Employee. But role Employee is not valid anymore.
     */

    @Test
    public void test300DisableRoleEmployee() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // disable role Employee
        ObjectDelta disableEmployeeDelta = prismContext.deltaFor(RoleType.class)
                .item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(ROLE_CORP_EMPLOYEE_OID);
        modelService.executeChanges(Collections.singletonList(disableEmployeeDelta),
                null, task, result);

        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testRoleEngineer", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(2, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Engineer");
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test310DisableRoleEngineer() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // disable role Engineer
        ObjectDelta disableEngineerDelta = prismContext.deltaFor(RoleType.class)
                .item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(ROLE_CORP_ENGINEER_OID);
        modelService.executeChanges(Collections.singletonList(disableEngineerDelta),
                null, task, result);

        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        ObjectDeltaObject<UserType> userOdo = createUserOdo(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi = createAssignmentIdi(assignmentType);

        // WHEN
        when();
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO,
                false, userTypeJack, "testRoleEngineer", AssignmentOrigin.createInObject(), task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        displayValue("Evaluated assignment", evaluatedAssignment.debugDump());
        assertEquals(2, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        for (Construction<UserType> construction : evaluatedAssignment.getConstructionSet(ZERO)) {
            assertEquals("Wrong validity for " + construction, false, construction.isValid());
        }

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Engineer");
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    // MID-4251
    @Test
    public void test400UserFred() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String pastTime = XmlTypeConverter.fromNow(createDuration("-P3D")).toString();
        String futureTime = XmlTypeConverter.fromNow(createDuration("P3D")).toString();
        UserType fred = new UserType(prismContext)
                .name("fred")
                .description(futureTime)
                .employeeType(DYNAMIC_ORG_ASSIGNMENT_EMPLOYEE_TYPE);
        addObject(fred.asPrismObject());
        PrismObject<UserType> fredAsCreated = findUserByUsername("fred");
        display("fred as created", fredAsCreated);

        ObjectDelta<UserType> descriptionDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(pastTime)
                .asObjectDeltaCast(fredAsCreated.getOid());

        LensContext<UserType> lensContext = createUserLensContext();
        fillContextWithUser(lensContext, fredAsCreated.getOid(), result);
        addFocusDeltaToContext(lensContext, descriptionDelta);

        // WHEN
        when();
        projector.project(lensContext, "test", task, result);

        // THEN
        then();
        assertSuccess(result);

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> triple = lensContext.getEvaluatedAssignmentTriple();
        displayValue("Evaluated assignment triple", triple.debugDump());
        assertEquals("Wrong # of evaluated assignments zero set", 0, triple.getZeroSet().size());
        assertEquals("Wrong # of evaluated assignments plus set", 1, triple.getPlusSet().size());
        assertEquals("Wrong # of evaluated assignments minus set", 1, triple.getMinusSet().size());
    }

    protected void assertNoConstruction(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, PlusMinusZero constructionSet, String attributeName) {
        Collection<Construction<UserType>> constructions = evaluatedAssignment.getConstructionSet(constructionSet);
        for (Construction construction : constructions) {
            PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping =
                    construction.getAttributeMapping(new QName(MidPointConstants.NS_RI, attributeName));
            assertNull("Unexpected mapping for " + attributeName, mapping);
        }
    }

    protected void assertConstruction(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, PlusMinusZero constructionSet, String attributeName, PlusMinusZero attributeSet, String... expectedValues) {
        Collection<Construction<UserType>> constructions = evaluatedAssignment.getConstructionSet(constructionSet);
        Set<String> realValues = new HashSet<>();
        for (Construction construction : constructions) {
            PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping =
                    construction.getAttributeMapping(new QName(MidPointConstants.NS_RI, attributeName));
            if (mapping != null && mapping.getOutputTriple() != null) {
                Collection<? extends PrismPropertyValue<?>> valsInMapping = mapping.getOutputTriple().getSet(attributeSet);
                if (valsInMapping != null) {
                    for (PrismPropertyValue value : valsInMapping) {
                        if (value.getValue() instanceof String) {
                            realValues.add((String) value.getValue());
                        }
                    }
                }
            }
        }
        AssertJUnit.assertEquals("Wrong values", new HashSet<>(Arrays.asList(expectedValues)), realValues);
    }

    protected AssignmentEvaluator<UserType> createAssignmentEvaluator() throws SchemaException {
        PrismObject<UserType> userJack = userTypeJack.asPrismObject();
        ObjectDeltaObject<UserType> focusOdo = createUserOdo(userJack);
        return createAssignmentEvaluator(focusOdo);
    }

    protected AssignmentEvaluator<UserType> createAssignmentEvaluator(ObjectDeltaObject<UserType> focusOdo) {
        LensContext<UserType> lensContext = createLensContext(UserType.class);
        LensFocusContext<UserType> focusContext = lensContext.getOrCreateFocusContext();
        focusContext.setObjectOld(focusOdo.getOldObject());
        focusContext.setPrimaryDelta(focusOdo.getObjectDelta());
        focusContext.setObjectCurrent(focusOdo.getOldObject());
        focusContext.setObjectNew(focusOdo.getNewObject());

        return new AssignmentEvaluator.Builder<UserType>()
                .repository(repositoryService)
                .focusOdo(focusOdo)
                .objectResolver(objectResolver)
                .systemObjectCache(systemObjectCache)
                .relationRegistry(relationRegistry)
                .prismContext(prismContext)
                .activationComputer(activationComputer)
                .now(clock.currentTimeXMLGregorianCalendar())
                .mappingFactory(mappingFactory)
                .mappingEvaluator(mappingEvaluator)
                .lensContext(lensContext)
                .build();
    }

    private ObjectDeltaObject<UserType> createUserOdo(PrismObject<UserType> user) throws SchemaException {
        return createUserOdo(user, null);
    }

    private ObjectDeltaObject<UserType> createUserOdo(PrismObject<UserType> user, ObjectDelta<UserType> userDelta) throws SchemaException {
        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null, user.getDefinition());
        userOdo.recompute();
        return userOdo;
    }

    private ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> createAssignmentIdi(AssignmentType assignmentType) throws SchemaException {
        return new ItemDeltaItem<>(LensUtil.createAssignmentSingleValueContainer(assignmentType), getAssignmentDefinition());
    }
}
