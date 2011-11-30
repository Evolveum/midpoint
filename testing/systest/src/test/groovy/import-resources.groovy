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
