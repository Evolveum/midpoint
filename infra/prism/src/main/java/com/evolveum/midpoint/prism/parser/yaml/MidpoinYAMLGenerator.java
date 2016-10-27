package com.evolveum.midpoint.prism.parser.yaml;

import java.io.IOException;
import java.io.Writer;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.Version;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.ImplicitTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.events.ScalarEvent;

public class MidpoinYAMLGenerator extends YAMLGenerator{

	public MidpoinYAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures, ObjectCodec codec,
			Writer out, Version version) throws IOException {
		super(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
		// TODO Auto-generated constructor stub
	}
	
//	public MidpoinYAMLGenerator(YAMLGenerator generator, YAMLFactory factory) throws IOException {
//		super.
//		super(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
		// TODO Auto-generated constructor stub
//	}
	
	@Override
	 protected void _writeScalar(String value, String type, Character style) throws IOException
	    {
	        _emitter.emit(_scalarEvent(value, style));
	    }
	    
	@Override
	    protected ScalarEvent _scalarEvent(String value, Character style)
	    {
		ImplicitTuple implicitTuple = new ImplicitTuple(true, false);
	        String yamlTag = _typeId;
	        if (yamlTag == null) {
	        	// to avid of writing default "!" when typing is not needed
	        	implicitTuple = new ImplicitTuple(true, true);
//	        	yamlTag = "ADDEDSPECIAL";
//	            _typeId = null;
	        }
	        String anchor = _objectId;
	        if (anchor != null) {
//	            _objectId = null;
	        }
	        return new ScalarEvent(anchor, yamlTag, implicitTuple, value,
	                null, null, style);
	    }
}
