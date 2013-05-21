/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.io.File
import java.net.MalformedURLException
import java.net.URL
import javax.xml.namespace.QName
import javax.jws.WebMethod
import javax.jws.WebParam
import javax.jws.WebResult
import javax.jws.WebService
import javax.jws.soap.SOAPBinding
import javax.xml.bind.annotation.XmlSeeAlso
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelService
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType
import com.evolveum.midpoint.xml.ns._public.common.common_1.*
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.*
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType

import com.evolveum.midpoint.xml.ns._public.common.annotation_1.*
import com.evolveum.midpoint.xml.ns._public.common.common_1.*
import com.evolveum.midpoint.xml.ns._public.common.fault_1.*
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.*
import com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_1.*
import com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_1.*
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.*
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.*
import com.evolveum.midpoint.xml.ns._public.resource.resource_schema_1.*
import com.evolveum.midpoint.schema.util.JAXBUtil
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.bind.Unmarshaller
import javax.xml.ws.Holder

import org.apache.directory.groovyldap.*


//######################### common initialization ###########################
def config = new Properties()
new File("environment.properties").withInputStream {
  stream -> config.load(stream)
}

//Define webservice name and listener
QName SERVICE_NAME = new QName("http://midpoint.evolveum.com/xml/ns/public/model/model-1.wsdl", "modelService")
URL wsdlURL = new URL (config['midpoint.soap.url']+"?WSDL")

//initialize webservice
ModelService ss = new ModelService(wsdlURL,SERVICE_NAME);
//Get modelWebservice object
ModelPortType model = ss.getModelPort()

//LDAP1 connect (http://directory.apache.org/api/groovy-ldap.html)
//http://directory.apache.org/api/2-groovy-ldap-user-guide.html
ldap1 = LDAP.newInstance( config['ldap1.url'], config['ldap1.binddn'] , config['ldap1.password'] )
ldap2 = LDAP.newInstance( config['ldap2.url'], config['ldap2.binddn'] , config['ldap2.password'] )

//######################### end of common initialization  ###########################

//Search for all Connector types try to find ldap connector ref
Holder<OperationResultType> result = new Holder<OperationResultType>()
Holder<ObjectListType> objs = new Holder<ObjectListType>()

PagingType paging = new PagingType()
try {
	model.listObjects("http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#ConnectorType",paging,objs,result)
} catch ( Exception ex ) {
        println "Error while listObjs: " + ex.getMessage()
        println ex.printStackTrace()
}

def ldapConnectorRefOid 
for(def obj in objs.value.getObject()) {
	if ( obj.name == "ICF org.identityconnectors.ldap.LdapConnector" ) {
		ldapConnectorRefOid = obj.oid
	}
}
//add resource ldap1
ResourceType  object = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File("src/test/resources/xml/resource-ldap1-dc=example.xml"))).getValue();

object.getConnectorRef().setOid(ldapConnectorRefOid); 

Holder<String> newOid = new Holder<String>()


try {
	model.addObject(object,newOid,result);
} catch ( Exception ex ) {
	//println "Error: " + ex.getMessage()
	println ex.printStackTrace()
}

println "Inserting object: "  + object.getName() 
println "oid: " + newOid.value 
println "Result: " + result.value

ldap1Oid = newOid.value


try {
	def res = model.testResource(ldap1Oid)
	println res
} catch ( Exception ex ){
	println "Error test connection to ldap1 : " + ex.getMessage()
}



