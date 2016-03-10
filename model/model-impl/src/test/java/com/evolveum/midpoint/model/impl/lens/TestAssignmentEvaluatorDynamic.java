package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.MINUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.PLUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.ZERO;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.apache.commons.collections.CollectionUtils;
import org.python.antlr.base.mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;


@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentEvaluatorDynamic extends TestAbstractAssignmentEvaluator {

	protected static final File ROLE_CORP_GENERIC_METAROLE_DYNAMIC_FILE = new File(TEST_DIR, "role-corp-generic-metarole-dynamic.xml");

	protected static final File ROLE_CORP_JOB_METAROLE_DYNAMIC_FILE = new File(TEST_DIR, "role-corp-job-metarole-dynamic.xml");
	
	protected static final File ROLE_CORP_MANAGER_DYNAMIC_FILE = new File(TEST_DIR, "role-corp-manager-dynamic.xml");
	
	protected static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
	
	protected static final File[] ROLE_CORP_FILES = {
            ROLE_CORP_GENERIC_METAROLE_DYNAMIC_FILE,
            ROLE_CORP_JOB_METAROLE_DYNAMIC_FILE,
            ROLE_CORP_VISITOR_FILE,
            ROLE_CORP_CUSTOMER_FILE,
            ROLE_CORP_CONTRACTOR_FILE,
            ROLE_CORP_EMPLOYEE_FILE,
            ROLE_CORP_ENGINEER_FILE,
            ROLE_CORP_MANAGER_FILE
    };
	
	@Override
	public File[] getRoleCorpFiles() {
		return ROLE_CORP_FILES;
	}
	
	
	 @Override
	    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
	        super.initSystem(initTask, initResult);

	        addObjects(getRoleCorpFiles());
	        
//	        PrismObject<UserType> userJack = getObject(UserType.class, USER_JACK_OID);
//	        ItemPath resourceNamePath = new ItemPath(FocusType.F_EXTENSION, new QName(NS_PIRACY, "resourceName"));
//	        ItemPath resourceRefPath = new ItemPath(FocusType.F_EXTENSION, new QName(NS_PIRACY, "resourceRef"));
//	       
//	       Collection modifications = new ArrayList<>();
//	       modifications.add(PropertyDelta.createModificationAddProperty(resourceNamePath, userJack.getDefinition().findPropertyDefinition(resourceNamePath), "Dummy Resource"));
//	       modifications.add(ReferenceDelta.createModificationAdd(resourceRefPath, userJack.getDefinition(), ObjectTypeUtil.createObjectRef("10000000-0000-0000-0000-000000000004", ObjectTypes.RESOURCE).asReferenceValue()));
//	       
//	       Collection deltas = new ArrayList<>();
//	       deltas.add(ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext));
//	       modelService.executeChanges(deltas, null, initTask, initResult);
//	       
//	       userTypeJack = getObject(UserType.class, USER_JACK_OID).asObjectable();
	       
	    }
	
}
