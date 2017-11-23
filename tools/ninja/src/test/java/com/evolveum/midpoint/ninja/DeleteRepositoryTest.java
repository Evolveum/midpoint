package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeleteRepositoryTest extends BaseTest {

    @Override
    protected void beforeMethodInternal(Method method) throws Exception {
        setupMidpointHome();
    }

    @Test
    public void deleteByOid() throws Exception {
        String type = ObjectTypes.ROLE.name();
        String oid = SystemObjectsType.ROLE_DELEGATOR.value();

        String[] input = new String[]{"-m", getMidpointHome(), "delete", "-o", oid, "-t", type};

        OperationResult result = new OperationResult("delete by oid");

        ExecutionValidator preExecValidator = (context) -> {
            RepositoryService repo = context.getRepository();

            PrismObject role = repo.getObject(ObjectTypes.ROLE.getClassDefinition(), oid,
                    GetOperationOptions.createRawCollection(), result);

            AssertJUnit.assertNotNull(role);
        };

        ExecutionValidator postExecValidator = (context) -> {
            RepositoryService repo = context.getRepository();
            try {
                repo.getObject(ObjectTypes.ROLE.getClassDefinition(), oid,
                        GetOperationOptions.createRawCollection(), result);

                AssertJUnit.fail();
            } catch (ObjectNotFoundException ex) {
            }
        };

        executeTest(input, preExecValidator, postExecValidator);
    }
}
