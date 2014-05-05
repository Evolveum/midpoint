using ModelClientSample.midpointModelService;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.Text;
using System.Threading.Tasks;

namespace ModelClientSample
{
    class Program
    {
        private const string LOGIN_USERNAME = "administrator";
        private const string LOGIN_PASSWORD = "5ecr3t";

        private const string NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
        private const string NS_Q = "http://prism.evolveum.com/xml/ns/public/query-2";

        private const string WS_URL = "http://localhost.:8080/midpoint/model/model-1?wsdl";   // when using fiddler, change "localhost" to "localhost."

        private const string ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

        static void Main(string[] args)
        {
            modelPortType modelPort = openConnection();

            OperationResultType result;
            //modelPort.getObject

            getObject getObject = new getObject();
            getObject.objectType = NS_C + "#UserType";
            getObject.options = new ObjectOperationOptionsType[0];
            getObject.oid = ADMINISTRATOR_OID;
            UserType obj = (UserType)modelPort.getObject(getObject).@object;

            //ObjectType o = modelPort.getObject(NS_C + "#UserType", ADMINISTRATOR_OID, new ObjectOperationOptionsType[0], out result);
            Console.WriteLine("returned object = " + obj);
            Console.ReadKey();
        }

        private static modelPortType openConnection()
        {
            WebRequest.DefaultWebProxy = new WebProxy("127.0.0.1", 8888);         // uncomment this line if you want to use fiddler

            modelPortTypeClient service = new modelPortTypeClient();


            //var binding = new BasicHttpBinding(BasicHttpSecurityMode.TransportWithMessageCredential);
            //binding.Security.Transport.ClientCredentialType = HttpClientCredentialType.None;
            //binding.Security.Message.ClientCredentialType = BasicHttpMessageCredentialType.UserName;

            //var securityElement = SecurityBindingElement.CreateUserNameOverTransportBindingElement();
            //securityElement.AllowInsecureTransport = true;
            //securityElement.ProtectTokens = true;
            //securityElement.EnableUnsecuredResponse = true;
            
            //var encodingElement = new TextMessageEncodingBindingElement(MessageVersion.Soap11, Encoding.UTF8);
            //var transportElement = new HttpTransportBindingElement();

            //var binding = new CustomBinding(securityElement, encodingElement, transportElement);
            //service.Endpoint.Binding = binding;

            service.ClientCredentials.UserName.UserName = LOGIN_USERNAME;
            service.ClientCredentials.UserName.Password = LOGIN_PASSWORD;
            
            service.Endpoint.Behaviors.Add(new InspectorBehavior(new ClientInspector(new SecurityHeader(LOGIN_USERNAME, LOGIN_PASSWORD))));
            return service.ChannelFactory.CreateChannel(new EndpointAddress(WS_URL));
        }

        private static ObjectType[] getAllUsers(modelPortType modelPort, string username)
        {
            searchObjects request = new searchObjects(NS_C + "#UserType", new QueryType(), new ObjectOperationOptionsType[0]);
            searchObjectsResponse response = modelPort.searchObjects(request);

            ObjectListType objectList = response.objectList;
            ObjectType[] objects = objectList.@object;

            Console.WriteLine("Response: " + response.ToString());
            return objects;
        }


    }
}
