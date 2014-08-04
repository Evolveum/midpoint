package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SearchShadowOwnerTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(SearchShadowOwnerTest.class);

    @Test(expectedExceptions = ObjectNotFoundException.class)
    public void searchNonExistingShadowOwner() throws Exception {
        //searching owner for non existing shadow
        OperationResult result = new OperationResult("List owner");
        try {
            repositoryService.searchShadowOwner("12345", null, result);
        } finally {
            result.computeStatus();
            AssertJUnit.assertTrue("current status" + result.getStatus(), result.isFatalError());
        }
    }

    @Override
    public void initSystem() throws Exception {
        super.initSystem();

        OperationResult result = new OperationResult("Add sample data");

        //insert sample data
        final File OBJECTS_FILE = new File(FOLDER_BASIC, "objects.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.parseObjects(OBJECTS_FILE);
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            repositoryService.addObject(object, null, result);
        }
        result.computeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void searchShadowOwner() throws Exception {
        OperationResult result = new OperationResult("List owner");

        //look for account owner
        PrismObject<UserType> user = repositoryService.searchShadowOwner("11223344", null,  result);

        assertNotNull("No owner for account", user);
        PrismProperty name = user.findProperty(ObjectType.F_NAME);
        AssertJUnit.assertNotNull(name);
        AssertJUnit.assertEquals("atestuserX00003", ((PolyString) name.getRealValue()).getOrig());
    }

    @Test
    public void searchShadowOwnerIsRole() throws Exception {
        OperationResult result = new OperationResult("List owner");

        //look for account owner
        PrismObject<RoleType> role = repositoryService.searchShadowOwner("11223355", null, result);

        assertNotNull("No owner for account", role);
        PrismProperty name = role.findProperty(ObjectType.F_NAME);
        AssertJUnit.assertNotNull(name);
        AssertJUnit.assertEquals("Judge", ((PolyString) name.getRealValue()).getOrig());
    }
}
