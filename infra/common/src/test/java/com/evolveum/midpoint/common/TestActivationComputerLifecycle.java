/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.common;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * @author semancik
 *
 */
public class TestActivationComputerLifecycle extends AbstractActivationComputerTest {
	
	private static final File SYSTEM_CONFIGURATION_LIFECYCLE_FILE = new File("src/test/resources/system-configuration-lifecycle.xml");

    @Override
    protected LifecycleStateModelType createLifecycleModel() throws SchemaException, IOException {
    	PrismObject<SystemConfigurationType> systemConfig = PrismTestUtil.parseObject(SYSTEM_CONFIGURATION_LIFECYCLE_FILE);
    	List<ObjectPolicyConfigurationType> objectPolicyConfigurations = systemConfig.asObjectable().getDefaultObjectPolicyConfiguration();
    	return objectPolicyConfigurations.get(0).getLifecycleStateModel();
    }    

    @Test
    @Override
    public void testGetProposedAdministrativeEnabled() throws Exception {
    	System.out.println("\n===[ testGetProposedAdministrativeEnabled ]===\n");

        // GIVEN
    	Clock clock = createClock(YEAR_START);
    	ActivationComputer activationComputer = createActivationComputer(clock);
    	ActivationType activationType = createActivationType(ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

    	// WHEN
    	ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(SchemaConstants.LIFECYCLE_PROPOSED, activationType, createLifecycleModel());

    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatusType.ENABLED, effectiveStatus);
    }
    
    @Test
    @Override
    public void testGetDraftAdministrativeEnabled() throws Exception {
    	System.out.println("\n===[ testGetDraftAdministrativeEnabled ]===\n");

        // GIVEN
    	Clock clock = createClock(YEAR_START);
    	ActivationComputer activationComputer = createActivationComputer(clock);
    	ActivationType activationType = createActivationType(ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

    	// WHEN
    	ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(SchemaConstants.LIFECYCLE_DRAFT, activationType, createLifecycleModel());

    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatusType.ARCHIVED, effectiveStatus);
    }
    
    @Override
    protected void testComputeDraft(final String TEST_NAME, XMLGregorianCalendar now, ActivationStatusType administrativeStatus, XMLGregorianCalendar validFrom,
			XMLGregorianCalendar validTo, ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity) throws SchemaException, IOException {
    	testCompute(TEST_NAME, SchemaConstants.LIFECYCLE_DRAFT, now, administrativeStatus, validFrom, validTo, ActivationStatusType.ARCHIVED, expectedValidity);
    }
    
    @Override
    protected void testComputeProposed(final String TEST_NAME, XMLGregorianCalendar now, ActivationStatusType administrativeStatus, XMLGregorianCalendar validFrom,
			XMLGregorianCalendar validTo, ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity) throws SchemaException, IOException {
    	testCompute(TEST_NAME, SchemaConstants.LIFECYCLE_PROPOSED, now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
    }
    
    @Override
    protected void testComputeCharmed(final String TEST_NAME, XMLGregorianCalendar now, ActivationStatusType administrativeStatus, XMLGregorianCalendar validFrom,
			XMLGregorianCalendar validTo, ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity) throws SchemaException, IOException {
    	testCompute(TEST_NAME, LIFECYCLE_STATE_CHARMED, now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
    }
    
    @Override
    protected void testComputeInhumed(final String TEST_NAME, XMLGregorianCalendar now, ActivationStatusType administrativeStatus, XMLGregorianCalendar validFrom,
			XMLGregorianCalendar validTo, ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity) throws SchemaException, IOException {
    	testCompute(TEST_NAME, LIFECYCLE_STATE_INHUMED, now, administrativeStatus, validFrom, validTo, ActivationStatusType.ARCHIVED, expectedValidity);
    }
    
}
