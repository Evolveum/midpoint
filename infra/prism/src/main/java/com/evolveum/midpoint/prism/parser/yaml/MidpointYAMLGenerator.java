package com.evolveum.midpoint.prism.parser.yaml;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.events.ImplicitTuple;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.io.IOException;
import java.io.Writer;

public class MidpointYAMLGenerator extends YAMLGenerator {

	public MidpointYAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures, ObjectCodec codec,
			Writer out, DumperOptions.Version version) throws IOException {
		super(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
	}

	@Override
	protected void _writeScalar(String value, String type, Character style) throws IOException {
		_emitter.emit(_scalarEvent(value, style));
	}

	@Override
	protected ScalarEvent _scalarEvent(String value, Character style) {
		ImplicitTuple implicitTuple = new ImplicitTuple(true, false);
		String yamlTag = _typeId;
		if (yamlTag == null) {
			// to avoid of writing default "!" when typing is not needed
			implicitTuple = new ImplicitTuple(true, true);
			//	        	yamlTag = "ADDEDSPECIAL";
			//	            _typeId = null;
		}
		String anchor = _objectId;
		if (anchor != null) {
			//	            _objectId = null;
		}
		return new ScalarEvent(anchor, yamlTag, implicitTuple, value, null, null, style);
	}
}
