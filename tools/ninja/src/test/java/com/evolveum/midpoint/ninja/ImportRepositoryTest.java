package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportRepositoryTest extends BaseTest {

    @Override
    protected void beforeMethodInternal(Method method) throws Exception {
        setupMidpointHome();
    }

    @Test
    public void importByOid() {
        String[] input = new String[]{"-m", getMidpointHome(), "import", "-o", "00000000-0000-0000-0000-111100000002",
                "-i", RESOURCES_FOLDER + "/objects.zip", "-z"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        executeTest(null,
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(0, count);
                },
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count users");
                    int count = repo.countObjects(UserType.class, null, null, result);

                    AssertJUnit.assertEquals(1, count);
                },
                true, true, input);

        List<String> out = getSystemOut();
        AssertJUnit.assertEquals(out.toString(), 5, out.size());
        AssertJUnit.assertTrue(getSystemErr().isEmpty());
    }

    @Test
    public void importByFilter() throws Exception {

    }

    @Test
    public void importRaw() throws Exception {

    }

    @Test
    public void importFromZipFileByFilterAllowOverwrite() throws Exception {

    }
}
