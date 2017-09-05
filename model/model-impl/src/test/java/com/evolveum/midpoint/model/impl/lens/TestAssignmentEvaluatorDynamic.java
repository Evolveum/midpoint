package com.evolveum.midpoint.model.impl.lens;

import java.io.File;


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
			ROLE_METAROLE_SOD_NOTIFICATION_FILE,
            ROLE_CORP_AUTH_FILE,			// TODO prepare a dynamic version of this file
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
