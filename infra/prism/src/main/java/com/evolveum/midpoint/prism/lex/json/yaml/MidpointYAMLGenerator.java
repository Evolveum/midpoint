package com.evolveum.midpoint.prism.lex.json.yaml;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.ImplicitTuple;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

public class MidpointYAMLGenerator extends YAMLGenerator {

	private DumperOptions.Version version;

	public MidpointYAMLGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures, ObjectCodec codec,
			Writer out, DumperOptions.Version version) throws IOException {
		super(ctxt, jsonFeatures, yamlFeatures, codec, out, version);
		this.version = version;
	}

	/**
	 * Brutal hack, as default behavior has lead to the following:
	 *  - !<http://midpoint.evolveum.com/xml/ns/public/model/scripting-3/SearchExpressionType>
	 *    !<http://midpoint.evolveum.com/xml/ns/public/model/scripting-3/SearchExpressionType> '@element': "http://midpoint.evolveum.com/xml/ns/public/model/scripting-3#search"
	 *
	 * (so we need to explicitly reset typeId after writing it)
	 */
	public void resetTypeId() {
		_typeId = null;
	}

	@Override
	protected ScalarEvent _scalarEvent(String value, Character style) {
		if (value.indexOf('\n') != -1) {
			style = Character.valueOf('|');
		}

		ImplicitTuple implicit;
		String yamlTag = _typeId;
		if (yamlTag != null) {
			_typeId = null;
			implicit = new ImplicitTuple(false, false);			// we want to always preserve the tags (if they are present)
		} else {
			implicit = new ImplicitTuple(true, true);
		}
		String anchor = _objectId;
		if (anchor != null) {
			_objectId = null;
		}
		return new ScalarEvent(anchor, yamlTag, implicit, value, null, null, style);
	}

	public void newDocument() throws IOException {
		_emitter.emit(new DocumentEndEvent(null, null, false));
		_emitter.emit(new DocumentStartEvent(null, null, true, version, Collections.emptyMap()));
	}

}
