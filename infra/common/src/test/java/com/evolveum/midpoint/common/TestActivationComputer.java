/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common;

import static org.testng.AssertJUnit.assertEquals;
import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatus;

/**
 * @author semancik
 *
 */
public class TestActivationComputer {
	
	private static final XMLGregorianCalendar YEAR_START = XmlTypeConverter.createXMLGregorianCalendar(2013, 1, 1, 0, 0, 0);
	private static final XMLGregorianCalendar SPRING_EQUINOX = XmlTypeConverter.createXMLGregorianCalendar(2013, 3, 20, 11, 2, 00);
	private static final XMLGregorianCalendar SUMMER_SOLSTICE = XmlTypeConverter.createXMLGregorianCalendar(2013, 6, 21, 5, 4, 00);
	private static final XMLGregorianCalendar AUTUMN_EQUINOX = XmlTypeConverter.createXMLGregorianCalendar(2013, 9, 22, 20, 4, 00);
	private static final XMLGregorianCalendar WINTER_SOLSTICE = XmlTypeConverter.createXMLGregorianCalendar(2013, 12, 21, 17, 11, 00);
	private static final XMLGregorianCalendar YEAR_END = XmlTypeConverter.createXMLGregorianCalendar(2013, 12, 31, 23, 59, 59);
	
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testGetAdministrativeEnabled() throws Exception {
    	System.out.println("\n===[ testGetAdministrativeEnabled ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(YEAR_START);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(ActivationStatus.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);
    	
    	// WHEN
    	ActivationStatus effectiveStatus = activationComputer.getEffectiveStatus(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatus.ENABLED, effectiveStatus);
    }
    
    @Test
    public void testGetAdministrativeDisabled() throws Exception {
    	System.out.println("\n===[ testGetAdministrativeDisabled ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(SUMMER_SOLSTICE);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(ActivationStatus.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);
    	
    	// WHEN
    	ActivationStatus effectiveStatus = activationComputer.getEffectiveStatus(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatus.DISABLED, effectiveStatus);
    }
    
    @Test
    public void testGetAdministrativeArchived() throws Exception {
    	System.out.println("\n===[ testGetAdministrativeArchived ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(SUMMER_SOLSTICE);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(ActivationStatus.ARCHIVED, SPRING_EQUINOX, AUTUMN_EQUINOX);
    	
    	// WHEN
    	ActivationStatus effectiveStatus = activationComputer.getEffectiveStatus(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatus.ARCHIVED, effectiveStatus);
    }
    
    @Test
    public void testGetBeforeValidity() throws Exception {
    	System.out.println("\n===[ testGetBeforeValidity ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(YEAR_START);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(null, SPRING_EQUINOX, AUTUMN_EQUINOX);
    	
    	// WHEN
    	ActivationStatus effectiveStatus = activationComputer.getEffectiveStatus(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatus.DISABLED, effectiveStatus);
    }
    
    @Test
    public void testGetInValidity() throws Exception {
    	System.out.println("\n===[ testGetInValidity ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(SUMMER_SOLSTICE);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(null, SPRING_EQUINOX, AUTUMN_EQUINOX);
    	
    	// WHEN
    	ActivationStatus effectiveStatus = activationComputer.getEffectiveStatus(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatus.ENABLED, effectiveStatus);
    }
    
    @Test
    public void testGetAfterValidity() throws Exception {
    	System.out.println("\n===[ testGetAfterValidity ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(WINTER_SOLSTICE);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(null, SPRING_EQUINOX, AUTUMN_EQUINOX);
    	
    	// WHEN
    	ActivationStatus effectiveStatus = activationComputer.getEffectiveStatus(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", ActivationStatus.DISABLED, effectiveStatus);
    }
    
    @Test
    public void testComputeAdministrativeEnabledBefore() throws Exception {
    	testCompute("testComputeAdministrativeEnabledBefore", YEAR_START, 
    			ActivationStatus.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.ENABLED, TimeIntervalStatus.BEFORE);
    }
    
    @Test
    public void testComputeAdministrativeEnabledIn() throws Exception {
    	testCompute("testComputeAdministrativeEnabledIn", SUMMER_SOLSTICE, 
    			ActivationStatus.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.ENABLED, TimeIntervalStatus.IN);
    }
    
    @Test
    public void testComputeAdministrativeEnabledAfter() throws Exception {
    	testCompute("testComputeAdministrativeEnabledBefore", WINTER_SOLSTICE, 
    			ActivationStatus.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.ENABLED, TimeIntervalStatus.AFTER);
    }
    
    @Test
    public void testComputeAdministrativeArchivedBefore() throws Exception {
    	testCompute("testComputeAdministrativeArchivedBefore", YEAR_START, 
    			ActivationStatus.ARCHIVED, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.ARCHIVED, TimeIntervalStatus.BEFORE);
    }
    
    @Test
    public void testComputeAdministrativeDisabledIn() throws Exception {
    	testCompute("testComputeAdministrativeDisabledIn", SUMMER_SOLSTICE, 
    			ActivationStatus.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.DISABLED, TimeIntervalStatus.IN);
    }
    
    @Test
    public void testComputeAdministrativeAbsentAfter() throws Exception {
    	testCompute("testComputeAdministrativeEnabledBefore", WINTER_SOLSTICE, 
    			ActivationStatus.ABSENT, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.ABSENT, TimeIntervalStatus.AFTER);
    }
    
    @Test
    public void testComputeBefore() throws Exception {
    	testCompute("testComputeAdministrativeEnabledBefore", YEAR_START, 
    			null, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.DISABLED, TimeIntervalStatus.BEFORE);
    }
    
    @Test
    public void testComputeIn() throws Exception {
    	testCompute("testComputeAdministrativeEnabledIn", SUMMER_SOLSTICE, 
    			null, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.ENABLED, TimeIntervalStatus.IN);
    }
    
    @Test
    public void testComputeAfter() throws Exception {
    	testCompute("testComputeAdministrativeEnabledBefore", WINTER_SOLSTICE, 
    			null, SPRING_EQUINOX, AUTUMN_EQUINOX, 
    			ActivationStatus.DISABLED, TimeIntervalStatus.AFTER);
    }
    
    void testCompute(final String TEST_NAME, XMLGregorianCalendar now, ActivationStatus administrativeStatus, XMLGregorianCalendar validFrom,
			XMLGregorianCalendar validTo, ActivationStatus expectedEffective, TimeIntervalStatus expectedValidity) {
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");
    	
        // GIVEN
    	Clock clock = createClock(now);
    	ActivationComputer activationComputer = createActivationComputer(clock); 
    	ActivationType activationType = createActivationType(administrativeStatus, validFrom, validTo);
    	
    	// WHEN
    	activationComputer.computeEffective(activationType);
    	
    	// THEN
    	assertEquals("Unexpected effective status", expectedEffective, activationType.getEffectiveStatus());
    	assertEquals("Unexpected validity status", expectedValidity, activationType.getValidityStatus());
    }

	private ActivationComputer createActivationComputer(Clock clock) {
		ActivationComputer activationComputer = new ActivationComputer(clock);
		return activationComputer;
	}

	private Clock createClock(XMLGregorianCalendar date) {
		Clock clock = new Clock();
		clock.override(date);
		return clock;
	}

	private ActivationType createActivationType(ActivationStatus administrativeStatus, XMLGregorianCalendar validFrom,
			XMLGregorianCalendar validTo) {
		ActivationType activationType = new ActivationType();
		activationType.setAdministrativeStatus(administrativeStatus);
		activationType.setValidFrom(validFrom);
		activationType.setValidTo(validTo);
		return activationType;
	}

}
