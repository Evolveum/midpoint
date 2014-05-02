/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Orgstruct test with a meta-role and focus mappings.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOrgStructMeta extends TestOrgStruct {
	
	private static final File OBJECT_TEMPLATE_ORG_FILE = new File(TEST_DIR, "object-template-org.xml");
	protected static final String OBJECT_TEMPLATE_ORG_OID = "3e62558c-ca0f-11e3-ba83-001e8c717e5b";
	
	protected static final File ROLE_META_FUNCTIONAL_ORG_FILE = new File(TEST_DIR, "role-meta-functional-org.xml");
    protected static final String ROLE_META_FUNCTIONAL_ORG_OID = "74aac2c8-ca0f-11e3-bb29-001e8c717e5b";
    
    @Override
	protected boolean doAddOrgstruct() {
		return false;
	}

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, ObjectTemplateType.class, initResult);
        setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);
        
        repoAddObjectFromFile(ROLE_META_FUNCTIONAL_ORG_FILE, RoleType.class, initResult);
    }
	
	@Override
	protected ResultHandler<OrgType> getOrgSanityCheckHandler() {
		return new ResultHandler<OrgType>() {
			@Override
			public boolean handle(PrismObject<OrgType> org, OperationResult parentResult) {
				OrgType orgType = org.asObjectable();
				if (orgType.getOrgType().contains("functional")) {
					assertAssigned(org, ROLE_META_FUNCTIONAL_ORG_OID, RoleType.COMPLEX_TYPE);
				} else if (orgType.getOrgType().contains("project")) {
					// Nothing to check (yet)
				} else {
					AssertJUnit.fail("Unexpected orgType in "+org);
				}
				return true;
			}
		};
	}

	/**
	 * Add org struct after the object template and metarole has been initialized.
	 */
	@Override
    protected void addOrgStruct() throws Exception {
        List<PrismObject<OrgType>> orgs = (List) PrismTestUtil.parseObjects(ORG_MONKEY_ISLAND_FILE);
        
        // WHEN
        for (PrismObject<OrgType> org: orgs) {
        	display("Adding", org);
        	addObject(org);
        }
        
        // Sanity is asserted in the inherited tests
	}

	@Override
	protected void assertUserOrg(PrismObject<UserType> user, String... orgOids) throws Exception {
		super.assertUserOrg(user, orgOids);
		List<PolyStringType> userOrganizations = user.asObjectable().getOrganization();
		List<PolyStringType> expextedOrgs = new ArrayList<PolyStringType>();
		for (String orgOid: orgOids) {
			PrismObject<OrgType> org = getObject(OrgType.class, orgOid);
			List<String> orgType = org.asObjectable().getOrgType();
			if (orgType.contains("functional")) {
				PolyStringType orgName = org.asObjectable().getName();
				assertTrue("Value "+orgName+" not found in user organization property: "+userOrganizations, userOrganizations.contains(orgName));
				if (!expextedOrgs.contains(orgName)) {
					expextedOrgs.add(orgName);
				}
			}
		}
		assertEquals("Wrong number of user organization property values: "+userOrganizations,  expextedOrgs.size(), userOrganizations.size());
	}

	@Override
	protected void assertUserNoOrg(PrismObject<UserType> user) throws Exception {
		super.assertUserNoOrg(user);
		List<PolyStringType> userOrganizations = user.asObjectable().getOrganization();
		assertTrue("Unexpected value in user organization property: "+userOrganizations, userOrganizations.isEmpty());
	}

	// test05x - test3xx inherited from superclass
	
	
}
