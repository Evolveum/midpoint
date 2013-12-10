using System;
using System.Security.Cryptography;
using System.ServiceModel.Channels;
using System.Text;
using System.Xml;
using System.ServiceModel;
using System.ServiceModel.Description;
using System.ServiceModel.Dispatcher;

/*
 * A code that manually inserts necessary WS-Security headers.
 * 
 * Based on:
 * - http://isyourcode.blogspot.co.uk/2010/08/attaching-oasis-username-tokens-headers.html
 * - http://social.msdn.microsoft.com/Forums/vstudio/en-US/3a01625f-7c15-4d70-80f4-175ef1768f57/add-values-into-existing-security-header
 * 
 * Thanks to Paul Heaney for pointing to those links.
 */

namespace ModelClientSample
{
    public class SecurityHeader : MessageHeader
    {
        public string SystemUser { get; set; }
        public string SystemPassword { get; set; }

        public SecurityHeader(string systemUser, string systemPassword)
        {
            SystemUser = systemUser;
            SystemPassword = systemPassword;
        }

        public override string Name
        {
            get { return "Security"; }
        }

        public override string Namespace
        {
            get { return "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"; }
        }

        protected override void OnWriteHeaderContents(XmlDictionaryWriter writer, MessageVersion messageVersion)
        {
            WriteHeader(writer);
        }

        protected override void OnWriteStartHeader(XmlDictionaryWriter writer, MessageVersion messageVersion)
        {
            writer.WriteStartElement("wsse", Name, Namespace);
            writer.WriteXmlnsAttribute("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            writer.WriteAttributeString("s:mustUnderstand", "1");
        }

        private void WriteHeader(XmlDictionaryWriter writer)
        {
            var nonce = new byte[64];
            RandomNumberGenerator.Create().GetBytes(nonce);
            string created = DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss.mszzzz");

            writer.WriteStartElement("wsse", "UsernameToken", null);
            writer.WriteAttributeString("wsu:Id", "UsernameToken-1");
            writer.WriteStartElement("wsse", "Username", null);
            writer.WriteString(SystemUser);
            writer.WriteEndElement();//End Username 
            writer.WriteStartElement("wsse", "Password", null);
            writer.WriteAttributeString("Type", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest");
            writer.WriteString(ComputePasswordDigest(SystemPassword, nonce, created));
            writer.WriteEndElement();//End Password 
            writer.WriteStartElement("wsse", "Nonce", null);
            writer.WriteAttributeString("EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
            writer.WriteBase64(nonce, 0, nonce.Length);
            writer.WriteEndElement();//End Nonce 
            writer.WriteStartElement("wsu", "Created", null);
            writer.WriteString(created);
            writer.WriteEndElement();//End Created
            writer.WriteEndElement();//End UsernameToken
            writer.Flush();
        }

        private string ComputePasswordDigest(string secret, byte[] nonceInBytes, string created)
        {
            byte[] createdInBytes = Encoding.UTF8.GetBytes(created);
            byte[] secretInBytes = Encoding.UTF8.GetBytes(secret);
            byte[] concatenation = new byte[nonceInBytes.Length + createdInBytes.Length + secretInBytes.Length];
            Array.Copy(nonceInBytes, concatenation, nonceInBytes.Length);
            Array.Copy(createdInBytes, 0, concatenation, nonceInBytes.Length, createdInBytes.Length);
            Array.Copy(secretInBytes, 0, concatenation, (nonceInBytes.Length + createdInBytes.Length), secretInBytes.Length);
            return Convert.ToBase64String(SHA1.Create().ComputeHash(concatenation));
        }
    }

    public class ClientInspector : IClientMessageInspector
    {
        public MessageHeader[] Headers { get; set; }

        public ClientInspector(params MessageHeader[] headers)
        {
            Headers = headers;
        }

        public object BeforeSendRequest(ref Message request, IClientChannel channel)
        {
            if (Headers != null)
            {
                for (int i = Headers.Length - 1; i >= 0; i--)
                {
                    request.Headers.Insert(0, Headers[i]);
                }
            }

            return request;
        }

        public void AfterReceiveReply(ref Message reply, object correlationState)
        {
        }
    }

    public class InspectorBehavior : IEndpointBehavior
    {
        public ClientInspector ClientInspector { get; set; }

        public InspectorBehavior(ClientInspector clientInspector)
        {
            ClientInspector = clientInspector;
        }

        public void Validate(ServiceEndpoint endpoint)
        {
        }

        public void AddBindingParameters(ServiceEndpoint endpoint, BindingParameterCollection bindingParameters)
        {
        }
        public void ApplyDispatchBehavior(ServiceEndpoint endpoint, EndpointDispatcher endpointDispatcher)
        {
        }

        public void ApplyClientBehavior(ServiceEndpoint endpoint, ClientRuntime clientRuntime)
        {
            if (this.ClientInspector == null)
            {
                throw new InvalidOperationException("Caller must supply ClientInspector.");
            }

            clientRuntime.MessageInspectors.Add(ClientInspector);
        }
    }


}
