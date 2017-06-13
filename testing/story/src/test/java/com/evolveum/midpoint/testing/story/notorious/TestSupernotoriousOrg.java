/*
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.testing.story.notorious;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalInspector;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Testing bushy roles hierarchy. Especially reuse of the same role
 * in the rich role hierarchy. It looks like this:
 * 
 *                    user
 *                     |
 *       +------+------+-----+-----+-....
 *       |      |      |     |     |
 *       v      v      v     v     v
 *      Ra1    Ra2    Ra3   Ra4   Ra5
 *       |      |      |     |     |
 *       +------+------+-----+-----+
 *                     |
 *                     v
 *  +--assignment--> supernotorious org
 *  |                  | 
 *  |    +------+------+-----+-----+-....
 *  |    |      |      |     |     |
 *  |    v      v      v     v     v
 *  +-- Rb1    Rb2    Rb3   Rb4   Rb5 ---..
 *      
 * Naive mode of evaluation would imply cartesian product of all Rax and Rbx
 * combinations. That's painfully inefficient. Therefore make sure that the
 * notorious roles is evaluated only once and the results of the evaluation
 * are reused.
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSupernotoriousOrg extends TestNotoriousOrg {
			
	@Override
	protected void fillLevelBRole(RoleType roleType, int i) {
		super.fillLevelBRole(roleType, i);
		roleType
			.beginAssignment()
				.targetRef(getNotoriousOid(), getNotoriousType())
//				.focusType(RoleType.COMPLEX_TYPE)
			.end();
	}
	
	@Test
    public void test010LevelBRolesSanity() throws Exception {
		final String TEST_NAME = "test010LevelBRolesSanity";
        displayTestTile(TEST_NAME);

        ObjectQuery query = queryFor(RoleType.class).item(RoleType.F_ROLE_TYPE).eq(ROLE_LEVEL_B_ROLETYPE).build();
		searchObjectsIterative(RoleType.class, query, 
				role -> {
					assertRoleMembershipRef(role, getNotoriousOid());
				}, NUMBER_OF_LEVEL_B_ROLES);
	}
}
