/**
 * Copyright (c) 2016-2018 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * OpenDJ, but without permissive modify, shortcut attributes, with manual matching rules, etc.
 *
 * @author semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOpenDjDumber extends TestOpenDj {

	private static final int INITIAL_SYNC_TOKEN = 21;

	@Override
	protected File getBaseDir() {
		return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "opendj-dumber");
	}

	@Override
	protected boolean hasAssociationShortcut() {
		return false;
	}

	@Override
	protected int getInitialSyncToken() {
		return INITIAL_SYNC_TOKEN;
	}

	@Override
	protected boolean isUsingGroupShortcutAttribute() {
		return false;
	}
	
	@Test
	@Override
    public void test350SeachInvisibleAccount() throws Exception {
		final String TEST_NAME = "test350SeachInvisibleAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN        
        createBilboEntry();

        SearchResultList<PrismObject<ShadowType>> shadows = searchBilbo(TEST_NAME);
        
        assertEquals("Unexpected search result: "+shadows, 0, shadows.size());
	}
}
