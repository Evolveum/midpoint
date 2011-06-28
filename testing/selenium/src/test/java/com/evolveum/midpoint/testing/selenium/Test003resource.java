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

	private static final transient Trace logger = TraceManager.getTrace(Test003resource.class);

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
		+ "<c:objects xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
		+ "           xmlns:i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
		+ "           xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
		+ "           xmlns:ri='http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2'\n"
		+ "           xmlns:ids='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd'>\n"
		+ "    <c:resource oid='ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2'>\n"
		+ "        <c:name>Localhost OpenDJ</c:name>\n"
		+ "        <c:type>http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resourceaccessconfiguration-1.xsd</c:type>\n"
		+ "        <c:namespace>http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2</c:namespace>\n"
		+ "        <c:configuration>\n"
		+ "            <idc:ConnectorConfiguration xmlns:idc='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/configuration-1.xsd'\n"
		+ "                                        xmlns:iccldap='http://midpoint.evolveum.com/xml/ns/resource/idconnector/bundle/org.identityconnectors.ldap/org.identityconnectors.ldap.LdapConnector/1.0.5531'>\n"
		+ "                <idc:ConnectorRef bundleName='org.identityconnectors.ldap' bundleVersion='1.0.5754openidm' connectorName='org.identityconnectors.ldap.LdapConnector'>\n"
		+ "                </idc:ConnectorRef>\n"
		+ "                <idc:BundleProperties>\n"
		+ "                    <iccldap:port>389</iccldap:port>\n"
		+ "                    <iccldap:host>localhost</iccldap:host>\n"
		+ "                    <iccldap:baseContexts>ou=Administrators,dc=example,dc=com</iccldap:baseContexts>\n"
		+ "                    <iccldap:principal>uid=idm</iccldap:principal>\n"
		+ "                    <iccldap:credentials>secret</iccldap:credentials>\n"
		+ "                    <iccldap:usePagedResultControl>true</iccldap:usePagedResultControl>\n"
		+ "                </idc:BundleProperties>\n"
		+ "                <idc:PoolConfigOption minEvictTimeMillis='5000' minIdle='5' maxIdle='30' maxObjects='120' maxWait='5000'/>\n"
		+ "                <idc:OperationTimeouts>\n"
		+ "                    <idc:OperationTimeout name='create' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='update' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='delete' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='test' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='scriptOnConnector' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='scriptOnResource' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='get' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='authenticate' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='search' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='validate' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='sync' timeout='50000'/>\n"
		+ "                    <idc:OperationTimeout name='schema' timeout='50000'/>\n"
		+ "                </idc:OperationTimeouts>\n"
		+ "            </idc:ConnectorConfiguration>\n"
		+ "        </c:configuration>\n"
		+ "        <c:schema>\n"
		+ "            <xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' \n"
		+ "                        targetNamespace='http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2'\n"
		+ "                        elementFormDefault='qualified'\n"
		+ "                        xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
		+ "                        xmlns:r='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd'\n"
		+ "                        xmlns:ri='http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2'\n"
		+ "                        xmlns:ids='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd'>\n"
		+ "                <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd'/>\n"
		+ "                <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'/>\n"
		+ "                <xsd:import namespace='http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd'/>\n"
		+ "                <xsd:complexType name='Account'>\n"
		+ "                    <xsd:annotation>\n"
		+ "                        <xsd:appinfo>\n"
		+ "                            <r:identifier ref='ids:__UID__'/>\n"
		+ "                            <r:secondaryIdentifier ref='ids:__NAME__'/>\n"
		+ "                            <r:displayName ref='ri:description'/>\n"
		+ "                            <r:descriptionAttribute ref='ri:description'/>\n"
		+ "                            <r:nativeObjectClass>__ACCOUNT__</r:nativeObjectClass>\n"
		+ "                            <r:accountType default='true'/>\n"
		+ "                        </xsd:appinfo>\n"
		+ "                    </xsd:annotation>\n"
		+ "                    <xsd:complexContent>\n"
		+ "                        <xsd:extension base='r:ResourceObjectClass'>\n"
		+ "                            <xsd:sequence>\n"
		+ "                                <xsd:element ref='ids:__NAME__'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:nativeAttributeName>__NAME__</r:nativeAttributeName>\n"
		+ "                                            <r:attributeDisplayName>DN</r:attributeDisplayName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element minOccurs='0' name='description' type='xsd:string'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:help>DESCRIPTION_NAME_HELP_KEY</r:help>\n"
		+ "                                            <r:nativeAttributeName>description</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element ref='ids:__PASSWORD__'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:attributeFlag>PASSWORD</r:attributeFlag>\n"
		+ "                                            <r:classifiedAttribute>\n"
		+ "                                                <r:encryption>HASH</r:encryption>\n"
		+ "                                                <r:classificationLevel>password</r:classificationLevel>\n"
		+ "                                            </r:classifiedAttribute>\n"
		+ "                                            <r:nativeAttributeName>__PASSWORD__</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element name='sn' type='xsd:string'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:nativeAttributeName>sn</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element name='cn' type='xsd:string'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:nativeAttributeName>cn</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element ref='ids:__UID__' minOccurs='0'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:attributeFlag>NOT_UPDATEABLE</r:attributeFlag>\n"
		+ "                                            <r:nativeAttributeName>__UID__</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element minOccurs='0' name='givenName' type='xsd:string'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:nativeAttributeName>givenName</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                            </xsd:sequence>\n"
		+ "                        </xsd:extension>\n"
		+ "                    </xsd:complexContent>\n"
		+ "                </xsd:complexType>\n"
		+ "                <xsd:complexType name='Group'>\n"
		+ "                    <xsd:annotation>\n"
		+ "                        <xsd:appinfo>\n"
		+ "                            <r:identifier ref='ids:__UID__'/>\n"
		+ "                            <r:secondaryIdentifier ref='ids:__NAME__'/>\n"
		+ "                            <r:displayName ref='ri:description'/>\n"
		+ "                            <r:descriptionAttribute ref='ri:description'/>\n"
		+ "                            <r:nativeObjectClass>__GROUP__</r:nativeObjectClass>\n"
		+ "                            <r:container/>\n"
		+ "                        </xsd:appinfo>\n"
		+ "                    </xsd:annotation>\n"
		+ "                    <xsd:complexContent>\n"
		+ "                        <xsd:extension base='r:ResourceObjectClass'>\n"
		+ "                            <xsd:sequence>\n"
		+ "                                <xsd:element ref='ids:__NAME__'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:nativeAttributeName>__NAME__</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element minOccurs='0' name='description' type='xsd:string'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:help>DESCRIPTION_NAME_HELP_KEY</r:help>\n"
		+ "                                            <r:nativeAttributeName>description</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                                <xsd:element ref='ids:__UID__'>\n"
		+ "                                    <xsd:annotation>\n"
		+ "                                        <xsd:appinfo>\n"
		+ "                                            <r:attributeFlag>NOT_UPDATEABLE</r:attributeFlag>\n"
		+ "                                            <r:nativeAttributeName>__UID__</r:nativeAttributeName>\n"
		+ "                                        </xsd:appinfo>\n"
		+ "                                    </xsd:annotation>\n"
		+ "                                </xsd:element>\n"
		+ "                            </xsd:sequence>\n"
		+ "                        </xsd:extension>\n"
		+ "                    </xsd:complexContent>\n"
		+ "                </xsd:complexType>\n"
		+ "            </xsd:schema>\n"
		+ "        </c:schema>\n"
		+ "        <c:schemaHandling>\n"
		+ "            <c:accountType default='true'>\n"
		+ "                <c:name>Default Account</c:name>\n"
		+ "                <i:objectClass>ri:Account</i:objectClass>\n"
		+ "                <i:attribute ref='ids:__NAME__'>\n"
		+ "                    <c:name>Distinguished Name</c:name>\n"
		+ "                    <c:access>create</c:access>\n"
		+ "                    <c:access>read</c:access>\n"
		+ "                    <i:outbound default='true'>\n"
		+ "                        <i:valueExpression>\n"
		+ "                            declare namespace i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n"
		+ "                            concat('uid=', $i:user/c:name, ',ou=people,dc=example,dc=com')\n"
		+ "                            </i:valueExpression>\n"
		+ "                    </i:outbound>\n"
		+ "                </i:attribute>\n"
		+ "                <i:attribute ref='ids:__UID__'>\n"
		+ "                    <c:name>Entry UUID</c:name>\n"
		+ "                    <c:access>read</c:access>\n"
		+ "                </i:attribute>\n"
		+ "                <i:attribute ref='ri:cn'>\n"
		+ "                    <c:name>Common Name</c:name>\n"
		+ "                    <c:access>create</c:access>\n"
		+ "                    <c:access>read</c:access>\n"
		+ "                    <c:access>update</c:access>\n"
		+ "                    <i:outbound>\n"
		+ "                        <i:valueExpression>\n"
		+ "                            declare namespace i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n"
		+ "                            $i:user/i:fullName\n"
		+ "                        </i:valueExpression>\n"
		+ "                    </i:outbound>\n"
		+ "                </i:attribute>\n"
		+ "                <i:attribute ref='ri:sn'>\n"
		+ "                    <c:name>Surname</c:name>\n"
		+ "                    <c:access>create</c:access>\n"
		+ "                    <c:access>read</c:access>\n"
		+ "                    <c:access>update</c:access>\n"
		+ "                    <i:outbound>\n"
		+ "                        <i:valueExpression>\n"
		+ "                            declare namespace i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n"
		+ "                            $i:user/i:familyName\n"
		+ "                        </i:valueExpression>\n"
		+ "                    </i:outbound>\n"
		+ "                </i:attribute>\n"
		+ "                <i:attribute ref='ri:givenName'>\n"
		+ "                    <c:name>Given Name</c:name>\n"
		+ "                    <c:access>create</c:access>\n"
		+ "                    <c:access>read</c:access>\n"
		+ "                    <c:access>update</c:access>\n"
		+ "                    <i:outbound>\n"
		+ "                        <i:valueExpression>\n"
		+ "                            declare namespace i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n"
		+ "                            $i:user/i:givenName\n"
		+ "                        </i:valueExpression>\n"
		+ "                    </i:outbound>\n"
		+ "                </i:attribute>\n"
		+ "                <i:attribute ref='ri:description'>\n"
		+ "                    <i:outbound default='true'>\n"
		+ "                        <i:value>Created by IDM</i:value>\n"
		+ "                    </i:outbound>\n"
		+ "                </i:attribute>\n"
		+ "            </c:accountType>\n"
		+ "        </c:schemaHandling>\n"
		+ "    </c:resource>\n"
		+ "</c:objects>\n";
		
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
