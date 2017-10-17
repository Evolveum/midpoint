/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.intest.rbac;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractRbacTest extends AbstractInitializedModelIntegrationTest {

	protected static final File TEST_DIR = new File("src/test/resources", "rbac");

	protected static final File ROLE_ADRIATIC_PIRATE_FILE = new File(TEST_DIR, "role-adriatic-pirate.xml");
	protected static final String ROLE_ADRIATIC_PIRATE_OID = "12345678-d34d-b33f-f00d-5555555566aa";

	protected static final File ROLE_BLACK_SEA_PIRATE_FILE = new File(TEST_DIR, "role-black-sea-pirate.xml");
	protected static final String ROLE_BLACK_SEA_PIRATE_OID = "12345678-d34d-b33f-f00d-5555555566bb";

	protected static final File ROLE_INDIAN_OCEAN_PIRATE_FILE = new File(TEST_DIR, "role-indian-ocean-pirate.xml");
	protected static final String ROLE_INDIAN_OCEAN_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556610";

	protected static final File ROLE_HONORABILITY_FILE = new File(TEST_DIR, "role-honorability.xml");
	protected static final String ROLE_HONORABILITY_OID = "12345678-d34d-b33f-f00d-555555557701";

	protected static final File ROLE_CLERIC_FILE = new File(TEST_DIR, "role-cleric.xml");
	protected static final String ROLE_CLERIC_OID = "12345678-d34d-b33f-f00d-555555557702";

	protected static final File ROLE_WANNABE_FILE = new File(TEST_DIR, "role-wannabe.xml");
	protected static final String ROLE_WANNABE_OID = "12345678-d34d-b33f-f00d-555555557703";

	protected static final File ROLE_HONORABLE_WANNABE_FILE = new File(TEST_DIR, "role-honorable-wannabe.xml");
	protected static final String ROLE_HONORABLE_WANNABE_OID = "12345678-d34d-b33f-f00d-555555557704";

	protected static final File ROLE_GOVERNOR_FILE = new File(TEST_DIR, "role-governor.xml");
	protected static final String ROLE_GOVERNOR_OID = "12345678-d34d-b33f-f00d-555555557705";

	protected static final File ROLE_CANNIBAL_FILE = new File(TEST_DIR, "role-cannibal.xml");
	protected static final String ROLE_CANNIBAL_OID = "12345678-d34d-b33f-f00d-555555557706";

	protected static final File ROLE_PROJECT_OMNINAMAGER_FILE = new File(TEST_DIR, "role-project-omnimanager.xml");
	protected static final String ROLE_PROJECT_OMNINAMAGER_OID = "f23ab26c-69df-11e6-8330-979c643ea51c";

	protected static final File ROLE_WEAK_GOSSIPER_FILE = new File(TEST_DIR, "role-weak-gossiper.xml");
	protected static final String ROLE_WEAK_GOSSIPER_OID = "e8fb2226-7f48-11e6-8cf1-630ce5c3f80b";

	protected static final File ROLE_WEAK_SINGER_FILE = new File(TEST_DIR, "role-weak-singer.xml");
	protected static final String ROLE_WEAK_SINGER_OID = "caa7daf2-dd68-11e6-a780-ef610c7c3a06";
	protected static final String ROLE_WEAK_SINGER_TITLE = "Singer";

	protected static final File ROLE_IMMUTABLE_FILE = new File(TEST_DIR, "role-immutable.xml");
	protected static final String ROLE_IMMUTABLE_OID = "e53baf94-aa99-11e6-962a-5362ec2dd7df";
	protected static final String ROLE_IMMUTABLE_DESCRIPTION = "Role that cannot be modified because there is a modification rule with enforcement action.";

	protected static final File ROLE_IMMUTABLE_GLOBAL_FILE = new File(TEST_DIR, "role-immutable-global.xml");
	protected static final String ROLE_IMMUTABLE_GLOBAL_OID = "e7ba8884-b2f6-11e6-a0b9-d3540dd687d6";
	protected static final String ROLE_IMMUTABLE_GLOBAL_DESCRIPTION = "Thou shalt not modify this role!";
	protected static final String ROLE_IMMUTABLE_GLOBAL_IDENTIFIER = "GIG001";

	protected static final File ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_FILE = new File(TEST_DIR, "role-immutable-description-global.xml");
	protected static final String ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_OID = "b7ea1c0c-31d6-445e-8949-9f8f4a665b3b";
	protected static final String ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_DESCRIPTION = "Thou shalt not modify description of this role!";
	protected static final String ROLE_IMMUTABLE_DESCRIPTION_GLOBAL_IDENTIFIER = "GIG001D";

	protected static final File ROLE_NON_ASSIGNABLE_FILE = new File(TEST_DIR, "role-non-assignable.xml");
	protected static final String ROLE_NON_ASSIGNABLE_OID = "db67d2f0-abd8-11e6-9c30-b35abe3e4e3a";

	protected static final File ROLE_SCREAMING_FILE = new File(TEST_DIR, "role-screaming.xml");
	protected static final String ROLE_SCREAMING_OID = "024e204d-ce40-4fe9-ae5a-0da3cbce989a";

	protected static final File ROLE_NON_CREATEABLE_FILE = new File(TEST_DIR, "role-non-createable.xml");
	protected static final String ROLE_NON_CREATEABLE_OID = "c45a25ce-b2e8-11e6-923e-938d2c54d334";

	protected static final File ROLE_CREATEABLE_FILE = new File(TEST_DIR, "role-createable.xml");
	protected static final String ROLE_CREATEABLE_OID = "667a242c-dd13-4dbb-bc9f-ab678ba80d37";

	protected static final File ROLE_IMMUTABLE_ASSIGN_FILE = new File(TEST_DIR, "role-immutable-assign.xml");
	protected static final String ROLE_IMMUTABLE_ASSIGN_OID = "a6b10a7c-b57e-11e6-bcb3-1ba47cb07e2e";

	protected static final File ROLE_META_UNTOUCHABLE_FILE = new File(TEST_DIR, "role-meta-untouchable.xml");
	protected static final String ROLE_META_UNTOUCHABLE_OID = "a80c9572-b57d-11e6-80a9-6fdae1dc39bc";

	protected static final File ROLE_META_FOOL_FILE = new File(TEST_DIR, "role-meta-fool.xml");
	protected static final String ROLE_META_FOOL_OID = "2edc5fe4-af3c-11e6-a81e-eb332578ec4f";

	protected static final File ROLE_BLOODY_FOOL_FILE = new File(TEST_DIR, "role-bloody-fool.xml");
	protected static final String ROLE_BLOODY_FOOL_OID = "0a0ac150-af3d-11e6-9901-67fbcbd5bb25";

	protected static final File ROLE_TREASURE_GOLD_FILE = new File(TEST_DIR, "role-treasure-gold.xml");
	protected static final String ROLE_TREASURE_GOLD_OID = "00a1db70-5817-11e7-93eb-9305bec579fe";

	protected static final File ROLE_TREASURE_SILVER_FILE = new File(TEST_DIR, "role-treasure-silver.xml");
	protected static final String ROLE_TREASURE_SILVER_OID = "4e237ee4-5817-11e7-8345-8731a29a1fcb";

	protected static final File ROLE_TREASURE_BRONZE_FILE = new File(TEST_DIR, "role-treasure-bronze.xml");
	protected static final String ROLE_TREASURE_BRONZE_OID = "60f9352c-5817-11e7-bc1e-f7e208714c43";

	protected static final File ROLE_ALL_TREASURE_FILE = new File(TEST_DIR, "role-all-treasure.xml");
	protected static final String ROLE_ALL_TREASURE_OID = "7fda5d86-5817-11e7-ac85-3b1cba81d3ef";

	protected static final File ROLE_LOOT_DIAMONDS_FILE = new File(TEST_DIR, "role-loot-diamonds.xml");
	protected static final String ROLE_LOOT_DIAMONDS_OID = "974d7156-581c-11e7-916d-03ed3d47d102";

	protected static final File ROLE_ALL_LOOT_FILE = new File(TEST_DIR, "role-all-loot.xml");
	protected static final String ROLE_ALL_LOOT_OID = "aaede614-581c-11e7-91bf-db837eb406b7";

	protected static final File ROLE_ALL_YOU_CAN_GET_FILE = new File(TEST_DIR, "role-all-you-can-get.xml");
	protected static final String ROLE_ALL_YOU_CAN_GET_OID = "4671874e-5822-11e7-a571-8b43dc7d2876";
protected static final File ROLE_STRONG_RICH_SAILOR_FILE = new File(TEST_DIR, "role-strong-rich-sailor.xml");
	protected static final String ROLE_STRONG_RICH_SAILOR_OID = "c86ea3ab-a92c-45d2-bb6e-b638f7a66002";

	protected static final File ROLE_RICH_SAILOR_FILE = new File(TEST_DIR, "role-rich-sailor.xml");
	protected static final String ROLE_RICH_SAILOR_OID = "e62d69a5-ffa8-46ef-9625-4c9a10966627";
	protected static final File ORG_PROJECT_RECLAIM_BLACK_PEARL_FILE = new File(TEST_DIR, "org-project-reclaim-black-pearl.xml");
	protected static final String ORG_PROJECT_RECLAIM_BLACK_PEARL_OID = "00000000-8888-6666-0000-200000005000";

	protected static final String USER_LEMONHEAD_NAME = "lemonhead";
	protected static final String USER_LEMONHEAD_FULLNAME = "Cannibal Lemonhead";

	protected static final String USER_SHARPTOOTH_NAME = "sharptooth";
	protected static final String USER_SHARPTOOTH_FULLNAME = "Cannibal Sharptooth";

	protected static final String USER_REDSKULL_NAME = "redskull";
	protected static final String USER_REDSKULL_FULLNAME = "Cannibal Redskull";

	protected static final String USER_BIGNOSE_NAME = "bignose";
	protected static final String USER_BIGNOSE_FULLNAME = "Bignose the Noncannibal";

	protected static final String GROUP_FOOLS_NAME = "fools";
	protected static final String GROUP_SIMPLETONS_NAME = "simpletons";

	/**
	 * Undefined relation. It is not standard relation not a relation that is in any way configured.
	 */
	protected static final QName RELATION_COMPLICATED_QNAME = new QName("http://exmple.com/relation", "complicated");

	protected File getRoleGovernorFile() {
		return ROLE_GOVERNOR_FILE;
	}

	protected File getRoleCannibalFile() {
		return ROLE_CANNIBAL_FILE;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		repoAddObjectFromFile(ROLE_ADRIATIC_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BLACK_SEA_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_INDIAN_OCEAN_PIRATE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_HONORABILITY_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_CLERIC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WANNABE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_HONORABLE_WANNABE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(getRoleGovernorFile(), RoleType.class, initResult);
		repoAddObjectFromFile(getRoleCannibalFile(), RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_PROJECT_OMNINAMAGER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WEAK_GOSSIPER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_WEAK_SINGER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_IMMUTABLE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_NON_ASSIGNABLE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_SCREAMING_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_META_UNTOUCHABLE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_META_FOOL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BLOODY_FOOL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_TREASURE_SILVER_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_TREASURE_BRONZE_FILE, RoleType.class, initResult);
		// ROLE_TREASURE_GOLD is NOT loaded by purpose. It will come in later.
		repoAddObjectFromFile(ROLE_LOOT_DIAMONDS_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_ALL_LOOT_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_ALL_YOU_CAN_GET_FILE, RoleType.class, initResult);

		repoAddObjectFromFile(ROLE_STRONG_RICH_SAILOR_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_RICH_SAILOR_FILE, RoleType.class, initResult);

		repoAddObjectFromFile(USER_RAPP_FILE, initResult);

		dummyResourceCtl.addGroup(GROUP_FOOLS_NAME);
		dummyResourceCtl.addGroup(GROUP_SIMPLETONS_NAME);

	}

}
