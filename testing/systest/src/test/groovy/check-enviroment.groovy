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

import org.apache.directory.groovyldap.*


boolean fail=false
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

// Try to delete unexisting object
try {
	println	model.deleteObject("http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#SystemConfigurationType", "00000000-0000-0000-0000-000000000002000")
	println "SOAP Webservice on " + config['midpoint.soap.url'] + " \t\tFAIL" 
	fail=true
} catch ( Exception e) {
	assert e.getMessage() == "Object not found. OID: 00000000-0000-0000-0000-000000000002000"
	println "SOAP Webservice on " + config['midpoint.soap.url'] + " \t\tPASS" 
}


//test tree exstincence
if ( ldap1.exists ('dc=example,dc=com')) {
	println "LDAP1 " + config['ldap1.url'] +" dc=example,dc=com \t\tPASS" 
} else {
	println "LDAP1 " + config['ldap1.url'] +" dc=example,dc=com \t\tFAIL" 
	fail=true
}
if ( ldap1.exists ('dc=systest,dc=com')) {
	println "LDAP1 " + config['ldap1.url'] +" dc=systest,dc=com \t\tPASS" 
} else {
	println "LDAP1 " + config['ldap1.url'] +" dc=systest,dc=com \t\tFAIL" 
	fail=true
}
if ( ldap1.exists ('cn=changelog')) {
	println "LDAP1 " + config['ldap1.url'] +" cn=changelog      \t\tPASS" 
} else {
	println "LDAP1 " + config['ldap1.url'] +" cn=changelog      \t\tFAIL" 
}

if ( ldap2.exists ('dc=example,dc=com')) {
	println "LDAP2 " + config['ldap2.url'] +" dc=example,dc=com \t\tFAIL" 
	fail=true
} else {
	println "LDAP2 " + config['ldap2.url'] +" dc=example,dc=com \t\tPASS" 
}
if ( ldap2.exists ('dc=systest,dc=com')) {
	println "LDAP2 " + config['ldap2.url'] +" dc=systest,dc=com \t\tPASS" 
} else {
	println "LDAP2 " + config['ldap2.url'] +" dc=systest,dc=com \t\tFAIL" 
	fail=true
}
if ( ldap2.exists ('cn=changelog')) {
	println "LDAP2 " + config['ldap2.url'] +" cn=changelog      \t\tPASS" 
} else {
	println "LDAP2 " + config['ldap2.url'] +" cn=changelog      \t\tFAIL" 
	fail=true
}


//Calculate result
if (fail) {
	println "Systest enviroment check failed."
	exit(1)
} else {

	println "Systest enviroment check passed."
	exit(0)
}

