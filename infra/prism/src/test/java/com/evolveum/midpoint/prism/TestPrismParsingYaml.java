package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

public class TestPrismParsingYaml extends TestPrismParsing {

	@Override
	protected String getSubdirName() {
		return "yaml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "yaml";
	}

	@Override
	protected String getOutputFormat() {
		return PrismContext.LANG_YAML;
	}
	
	@Test
	public void test() throws Exception{
//		File[] files = new File[]{USER_JACK_ADHOC_FILE};
//		PrismContext prismContext = constructInitializedPrismContext();
//		for (File f : files){
//			System.out.println("parsing file" + f.getName());
//			PrismObject o = prismContext.parseObject(f);
////			
//			String s = prismContext.serializeObjectToString(o, PrismContext.LANG_YAML);
//			System.out.println("parsed: " + s);
//			String fname = f.getName();
//			fname = fname.replace(".xml", "123.yaml");
//			
//			FileOutputStream bos = (new FileOutputStream(new File("src/test/resources/common/yaml/"+fname)));
//			byte[] bytes = s.getBytes();
//			bos.write(bytes);
//			bos.flush();
//			bos.close();
//		}
	}

}
