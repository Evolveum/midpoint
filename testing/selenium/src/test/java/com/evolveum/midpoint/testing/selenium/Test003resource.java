/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
 */


package com.evolveum.midpoint.testing.selenium;

import java.util.HashMap;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;

import org.junit.*;

import com.evolveum.midpoint.testing.Selenium;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

import static org.junit.Assert.*;

public class Test003resource {

	Selenium se;
	static String baseUrl = "http://localhost:8080/idm";

	private static final transient Trace LOGGER = TraceManager.getTrace(Test003resource.class);

	@Before
	/***
	 * Do login as Admin for each test
	 */
	public void start() {

		WebDriver driver = new FirefoxDriver();
		// WebDriver driver = new ChromeDriver();
		se = new Selenium(driver, baseUrl);
		se.setBrowserLogLevel("5");

		se.open("/");
		se.waitForText("Login", 10);

		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "secret");
		se.click("loginForm:loginButton");
		se.waitForText("Administrator", 10);

		assertEquals(baseUrl + "/index.iface", se.getLocation());

	}

	/*
	 * close browser
	 */
	@After
	public void stop() {
		se.stop();

	}

	//TODO not implemented
	@Test
	public void test001createResourceLDAP() {
	}
	
	/**
	 * TODO description
	 */
	@Test
	public void test002importResourceLDAP() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>\n"
	+ "<resource xmlns='http://midpoint.evolveum.com/xml/ns/public/common/common-2' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2'\n"
	+ "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' oid='ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff'\n"
	+ "  xsi:schemaLocation='http://midpoint.evolveum.com/xml/ns/public/common/common-2 ../../../../../../infra/schema/src/main/resources/xml/ns/public/common/common-2\n"
	+ "                http://www.w3.org/2001/XMLSchema ../../../../../../infra/schema/src/test/resources/standard/XMLSchema.xsd'>\n"
	+ "\n"
	+ "  <name>Localhost OpenDJ</name>\n"
	+ "  <connectorRef oid='icf1-org.identityconnectors.ldap-1.0.5754openidm-org.identityconnectors.ldap.LdapConnector' />\n"
	+ "  <namespace>http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff</namespace>\n"
	+ "  <configuration\n"
	+ "    xmlns:icfcldap='http://midpoint.evolveum.com/xml/ns/resource/icf/icf1-org.identityconnectors.ldap-1.0.5754openidm-org.identityconnectors.ldap.LdapConnector'\n"
	+ "    xmlns:icfc='http://midpoint.evolveum.com/xml/ns/public/resource/icf/configuration-1.xsd'>\n"
	+ "\n"
	+ "    <icfcldap:configurationProperties>\n"
	+ "      <icfcldap:port>389</icfcldap:port>\n"
	+ "      <icfcldap:host>localhost</icfcldap:host>\n"
	+ "      <icfcldap:baseContexts>dc=example,dc=com</icfcldap:baseContexts>\n"
	+ "      <icfcldap:principal>uid=idm,ou=Administrators,dc=example,dc=com</icfcldap:principal>\n"
	+ "      <icfcldap:credentials>secret</icfcldap:credentials>\n"
	+ "      <icfcldap:vlvSortAttribute>uid</icfcldap:vlvSortAttribute>\n"
	+ "    </icfcldap:configurationProperties>\n"
	+ "\n"
	+ "    <icfc:connectorPoolConfiguration>\n"
	+ "      <icfc:minEvictableIdleTimeMillis>120000</icfc:minEvictableIdleTimeMillis>\n"
	+ "      <icfc:minIdle>1</icfc:minIdle>\n"
	+ "      <icfc:maxIdle>10</icfc:maxIdle>\n"
	+ "      <icfc:maxObjects>10</icfc:maxObjects>\n"
	+ "      <icfc:maxWait>150000</icfc:maxWait>\n"
	+ "    </icfc:connectorPoolConfiguration>\n"
	+ "\n"
	+ "    <icfc:producerBufferSize>100</icfc:producerBufferSize>\n"
	+ "\n"
	+ "    <icfc:timeout>\n"
	+ "      <icfc:operation name='create'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='update'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='delete'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='test'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='scriptOnConnector'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='scriptOnResource'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='get'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='authenticate'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='search'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='validate'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='sync'>-1</icfc:operation>\n"
	+ "      <icfc:operation name='schema'>-1</icfc:operation>\n"
	+ "    </icfc:timeout>\n"
	+ "\n"
	+ "  </configuration>\n"
	+ "  <c:schema>\n"
	+ "    <xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'\n"
	+ "      xmlns:tns='http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff'\n"
	+ "      xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-2' xmlns:ids='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-2.xsd'\n"
	+ "      xmlns:vr0='http://midpoint.evolveum.com/xml/ns/public/resource/icf/schema-1.xsd' elementFormDefault='qualified'\n"
	+ "      targetNamespace='http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff'>\n"
	+ "      <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/common/common-2' />\n"
	+ "      <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff' />\n"
	+ "      <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/resource/icf/schema-1.xsd' />\n"
	+ "      <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-2.xsd' />\n"
	+ "      <xsd:complexType name='AccountObjectClass'>\n"
	+ "        <xsd:annotation>\n"
	+ "          <xsd:appinfo>\n"
	+ "            <r:identifier xmlns:r='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-2.xsd'\n"
	+ "              ref='vr0:uid' />\n"
	+ "            <r:accountType xmlns:r='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-2.xsd'\n"
	+ "              default='true' />\n"
	+ "          </xsd:appinfo>\n"
	+ "        </xsd:annotation>\n"
	+ "        <xsd:complexContent>\n"
	+ "          <xsd:extension xmlns:r='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-2.xsd'\n"
	+ "            base='r:ResourceObjectClass'>\n"
	+ "            <xsd:sequence>\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='seeAlso' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='x500UniqueIdentifier' type='xsd:base64Binary' />\n"
	+ "              <xsd:element minOccurs='0' ref='vr0:password' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='photo' type='xsd:base64Binary' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='title' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='description' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='userPassword' type='xsd:base64Binary' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='x121Address' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='userSMIMECertificate' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='l' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='destinationIndicator' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' name='sn' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='internationaliSDNNumber' type='xsd:string' />\n"
	+ "              <xsd:element minOccurs='0' name='employeeNumber' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='facsimileTelephoneNumber' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='objectClass' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='initials' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='telexNumber' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='registeredAddress' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='givenName' type='xsd:string' />\n"
	+ "              <xsd:element ref='vr0:uid' />\n"
	+ "              <xsd:element ref='vr0:name' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='teletexTerminalIdentifier' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='st' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='ou' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='uid' type='xsd:string' />\n"
	+ "              <xsd:element minOccurs='0' name='preferredLanguage' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='street' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='roomNumber' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='businessCategory' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='secretary' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='audio' type='xsd:base64Binary' />\n"
	+ "              <xsd:element minOccurs='0' name='preferredDeliveryMethod' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='telephoneNumber' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='labeledURI' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='homePostalAddress' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='postOfficeBox' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='userPKCS12' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='userCertificate' type='xsd:base64Binary' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='mail' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='departmentNumber' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='carLicense' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='jpegPhoto' type='xsd:base64Binary' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='o' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='manager' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='employeeType' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='mobile' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='physicalDeliveryOfficeName' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='pager' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='postalCode' type='xsd:string' />\n"
	+ "              <xsd:element minOccurs='0' name='displayName' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='postalAddress' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='homePhone' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' name='cn' type='xsd:string' />\n"
	+ "            </xsd:sequence>\n"
	+ "          </xsd:extension>\n"
	+ "        </xsd:complexContent>\n"
	+ "      </xsd:complexType>\n"
	+ "      <xsd:complexType name='GroupObjectClass'>\n"
	+ "        <xsd:annotation>\n"
	+ "          <xsd:appinfo>\n"
	+ "            <r:identifier xmlns:r='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-2.xsd'\n"
	+ "              ref='vr0:uid' />\n"
	+ "          </xsd:appinfo>\n"
	+ "        </xsd:annotation>\n"
	+ "        <xsd:complexContent>\n"
	+ "          <xsd:extension xmlns:r='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-2.xsd'\n"
	+ "            base='r:ResourceObjectClass'>\n"
	+ "            <xsd:sequence>\n"
	+ "              <xsd:element maxOccurs='unbounded' name='cn' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='objectClass' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='description' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='seeAlso' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='ou' type='xsd:string' />\n"
	+ "              <xsd:element ref='vr0:name' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='uniqueMember' type='xsd:string' />\n"
	+ "              <xsd:element ref='vr0:uid' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='owner' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='o' type='xsd:string' />\n"
	+ "              <xsd:element maxOccurs='unbounded' minOccurs='0' name='businessCategory' type='xsd:string' />\n"
	+ "            </xsd:sequence>\n"
	+ "          </xsd:extension>\n"
	+ "        </xsd:complexContent>\n"
	+ "      </xsd:complexType>\n"
	+ "    </xsd:schema>\n"
	+ "  </c:schema>\n"
	+ "  <c:schemaHandling>\n"
	+ "        <!-- No need to schemaHandling in this case. Everything is in schema (annotations) -->\n"
	+ "  </c:schemaHandling>\n"
	+ "</resource>";
		
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));
		
		se.type("importForm:editor", xml);
		se.click("importForm:uploadButton");
		assertTrue(se.waitForText("Added object: jack"));
	}
	
	
	
}
