package com.evolveum.midpoint.prism.lex.json.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class MidpointYAMLFactory extends YAMLFactory {



	@Override
	protected MidpointYAMLGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
		int feats = _yamlGeneratorFeatures;
		MidpointYAMLGenerator gen = new MidpointYAMLGenerator(ctxt, _generatorFeatures, feats, _objectCodec, out, _version);
		// any other initializations? No?
		return gen;
	}

	@SuppressWarnings("resource")
	@Override
	protected MidpointYAMLParser _createParser(InputStream in, IOContext ctxt) throws IOException {
		return _createParser(_createReader(in, null, ctxt), ctxt);
	}

	@Override
	protected MidpointYAMLParser _createParser(Reader r, IOContext ctxt) throws IOException {
		MidpointYAMLParser p = new MidpointYAMLParser(ctxt, _getBufferRecycler(), _parserFeatures, _yamlParserFeatures, _objectCodec, r);
		p.enable(JsonParser.Feature.ALLOW_YAML_COMMENTS);
		return p;
	}
}
