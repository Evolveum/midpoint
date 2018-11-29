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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static java.util.Collections.emptySet;
import static org.testng.AssertJUnit.assertEquals;

/**
 * The same as ModifyTest but with "executeIfNoChanges" (a.k.a. "reindex") option set.
 * Although this option should do no harm in objects other than certification cases and lookup tables,
 * it is better to check.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTestReindex extends ModifyTest {

	@Override
	protected RepoModifyOptions getModifyOptions() {
		return RepoModifyOptions.createExecuteIfNoChanges();
	}

	@Test
	public void testReindex() throws Exception {
		final String TEST_NAME = "testReindex";
		TestUtil.displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);

		PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
				.name("unstable")
				.asPrismObject();
		UniformItemPath UNSTABLE_PATH = prismContext.path(UserType.F_EXTENSION, "unstable");
		PrismPropertyDefinition<String> unstableDef = user.getDefinition().findPropertyDefinition(UNSTABLE_PATH);
		PrismProperty<String> unstable = unstableDef.instantiate();
		unstable.setRealValue("hi");
		user.addExtensionItem(unstable);

		String oid = repositoryService.addObject(user, null, result);

		// brutal hack -- may stop working in the future!
		((PrismPropertyDefinitionImpl) unstableDef).setIndexed(true);

		repositoryService.modifyObject(UserType.class, oid, emptySet(), getModifyOptions(), result);

		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.item(UNSTABLE_PATH).eq("hi")
				.build();
		int count = repositoryService.countObjects(UserType.class, query, null, result);
		assertEquals("Wrong # of objects found", 1, count);
	}

}
