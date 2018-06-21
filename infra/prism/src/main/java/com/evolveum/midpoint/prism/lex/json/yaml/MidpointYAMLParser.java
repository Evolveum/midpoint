package com.evolveum.midpoint.prism.lex.json.yaml;

import java.io.Reader;

//import org.yaml.snakeyaml.events.CollectionStartEvent;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

public class MidpointYAMLParser extends YAMLParser {

	public MidpointYAMLParser(IOContext ctxt, BufferRecycler br, int parserFeatures, int csvFeatures,
			ObjectCodec codec, Reader reader) {
		super(ctxt, br, parserFeatures, csvFeatures, codec, reader);
		// TODO Auto-generated constructor stub
	}

//	  @Override
//	    public String getTypeId() throws IOException, JsonGenerationException
//	    {
//		  String tag = null;
//	        if (_lastEvent instanceof CollectionStartEvent) {
//	        	tag = ((CollectionStartEvent) _lastEvent).getTag();
//	        } else if (_lastEvent instanceof ScalarEvent){
//	        	tag = ((ScalarEvent) _lastEvent).getTag();
//	        }
//
//
//	            if (tag != null) {
//	                /* 04-Aug-2013, tatu: Looks like YAML parser's expose these in...
//	                 *   somewhat exotic ways sometimes. So let's prepare to peel off
//	                 *   some wrappings:
//	                 */
//	                while (tag.startsWith("!")) {
//	                    tag = tag.substring(1);
//	                }
//	                return tag;
//	            }
//
//	        return null;
//	    }

}
