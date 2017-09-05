package com.evolveum.midpoint.tools.ninja;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;

public class FileTransformer extends BaseNinjaAction{

	private String outputDirecorty;
	private String input;
	private String outputFormat;

	public void setOutputDirecorty(String outputDirecorty) {
		this.outputDirecorty = outputDirecorty;
	}

	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public void execute(){
		ApplicationContext context = new ClassPathXmlApplicationContext(CONTEXTS);
		PrismContext prismContext = context.getBean(PrismContext.class);


			File inFile = new File(input);

			File[] files = null;
			if (inFile.isDirectory()){
				 files = inFile.listFiles();
			}

			String output = getOutput(inFile);

			String outLang = getOutputLanguage(outputFormat);
			int errors = 0;
			int processed = 0;
			List<String> failedToParse = new ArrayList<String>();
			if (files != null){
				processed = files.length;
				for (int i = 0; i < files.length; i++){
					try {
						transform(prismContext, files[i], output, outLang);

					} catch (SchemaException | IOException e) {
						System.out.println("failed to transform: " + e.getMessage()+ ". Stack: " + e);
						errors++;
						failedToParse.add(files[i].getName());
					}
				}
			} else {
				processed = 1;
				try {
					transform(prismContext, inFile, output, outLang);
				} catch (SchemaException | IOException e) {
					errors++;
					failedToParse.add(inFile.getName());
				}
			}

			System.out.println("Processed " + processed +" files, got " +errors+ " errors. Files that was not successfully processed " + failedToParse);


	}

	private void transform(PrismContext prismContext, File inFile, String output, String lang) throws SchemaException, IOException{

		PrismObject parsed = prismContext.parseObject(inFile);

		String s = prismContext.serializeObjectToString(parsed, lang);


		if (StringUtils.isNotBlank(outputDirecorty)){
		}

		String outName = getOutputFileName(inFile, lang);
		System.out.println("file will be saved as: " + outName);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(outputDirecorty, outName)));

		PrismObject object = prismContext.parseObject(inFile);
		String serialized = prismContext.serializeObjectToString(object, lang);

		bos.write(serialized.getBytes());
		bos.flush();
		bos.close();

	}

	private String getOutputFileName(File inFile, String outFormat) {
		String out = inFile.getName();

		if (out.endsWith(".xml")){
			return out.replace("xml", outFormat);
		} else if (out.endsWith(".json")){
			return out.replace("json", outFormat);
		} else if (out.endsWith(".yaml")){
			return out.replace("yaml", outFormat);
		} else
			throw new UnsupportedOperationException("Transformation for file " + inFile.getName() +" not supported.");


	}

	private String getOutput(File inFile) {
		if (outputDirecorty == null){
			if (inFile.isDirectory()){
				return inFile.getAbsolutePath();
			} else{
				int end = inFile.getAbsolutePath().lastIndexOf("/");
				return inFile.getAbsolutePath().substring(0, end);
			}
		} else{
			return outputDirecorty;
		}
	}

	private String getOutputLanguage(String outFormat){
		if (outFormat.toLowerCase().equals(PrismContext.LANG_JSON)){
			return PrismContext.LANG_JSON;
		} else if (outFormat.toLowerCase().equals(PrismContext.LANG_YAML)){
			return PrismContext.LANG_YAML;
		} else if (outFormat.toLowerCase().equals(PrismContext.LANG_XML)){
			return PrismContext.LANG_XML;
		} else
			throw new UnsupportedOperationException("Specified output format '"+outFormat+"'not supported");
	}

}
