package com.evolveum.midpoint.common.crypto;

import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.CommonTestConstants;
import com.evolveum.midpoint.common.expression.ExpressionTestUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.test.util.DirectoryFileObjectResolver;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;

public class TestProtector {
	
	private PrismContext prismContext;
	
	private static transient Trace LOGGER = TraceManager.getTrace(TestProtector.class);
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
		prismContext = PrismTestUtil.createInitializedPrismContext();

	}
	
	
	private static Protector createProtector(PrismContext prismContext, String xmlCipher){
		AESProtector protector = new AESProtector();
		protector.setPrismContext(prismContext);
		protector.setKeyStorePassword(CommonTestConstants.KEYSTORE_PASSWORD);
		protector.setKeyStorePath(CommonTestConstants.KEYSTORE_PATH);
		protector.init();
		protector.setXmlCipher(xmlCipher);
		return protector;
	}
	
	
  @Test
  public void testProtectorKeyStore() throws Exception{
	  
	  String value = "someValue";
	
	  Protector protector256 = createProtector(prismContext, "http://www.w3.org/2001/04/xmlenc#aes256-cbc");
	  ProtectedStringType encrypted = protector256.encryptString(value);
	  
	  
	  Protector protector128 = createProtector(prismContext, null);
	  protector128.decrypt(encrypted);
	  
	  
  }
}
