package com.evolveum.midpoint.prism.parser.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class MidpointYAMLFactory extends YAMLFactory{

	  @Override
	    protected MidpoinYAMLGenerator _createGenerator(Writer out, IOContext ctxt)
	        throws IOException
	    {
	        int feats = _yamlGeneratorFeatures;
	        MidpoinYAMLGenerator gen = new MidpoinYAMLGenerator(ctxt, _generatorFeatures, feats,
	                _objectCodec, out, _version);
	        // any other initializations? No?
	        return gen;
	    }
	  
	  @SuppressWarnings("resource")
	    @Override
	    protected MidpointYAMLParser _createParser(InputStream in, IOContext ctxt)
	        throws IOException, JsonParseException
	    {
	        Reader r = _createReader(in, null, ctxt);
	        return new MidpointYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
	                _objectCodec, r);
	    }
	  
	  @Override
	    protected MidpointYAMLParser _createParser(Reader r, IOContext ctxt)
	        throws IOException, JsonParseException
	    {
	        return new MidpointYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures,
	                _objectCodec, r);
	    }
}
