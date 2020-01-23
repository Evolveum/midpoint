/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json.yaml;

import com.fasterxml.jackson.core.Base64Variant;
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
import java.util.Arrays;
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
     * {@code
     *  - !<http://midpoint.evolveum.com/xml/ns/public/model/scripting-3/SearchExpressionType>
     *    !<http://midpoint.evolveum.com/xml/ns/public/model/scripting-3/SearchExpressionType> '@element': "http://midpoint.evolveum.com/xml/ns/public/model/scripting-3#search"
     * }
     *
     * (so we need to explicitly reset typeId after writing it)
     */
    public void resetTypeId() {
        _typeId = null;
    }

    @Override
    protected ScalarEvent _scalarEvent(String value, DumperOptions.ScalarStyle style) {
        if (value.indexOf('\n') != -1) {
            style = DumperOptions.ScalarStyle.createStyle('|');
        }

        ImplicitTuple implicit;
        String yamlTag = _typeId;
        if (yamlTag != null) {
            _typeId = null;
            implicit = new ImplicitTuple(false, false);            // we want to always preserve the tags (if they are present)
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

    /*
     * UGLY HACK - working around YAML serialization problem
     * (https://github.com/FasterXML/jackson-dataformats-text/issues/90)
     *
     * TODO - after YAML problem is fixed, remove this code block (MID-4974)
     * TODO - if upgrading YAML library before that, make sure everything works (because this code is almost literally copied from YAMLGenerator class)
     */

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException
    {
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        if (offset > 0 || (offset+len) != data.length) {
            data = Arrays.copyOfRange(data, offset, offset+len);
        }
        writeScalarBinaryPatched(b64variant, data);
    }

    private final static ImplicitTuple EXPLICIT_TAGS = new ImplicitTuple(false, false);
    private final static Character STYLE_LITERAL = '|';
    private final static Character STYLE_BASE64 = STYLE_LITERAL;

    private void writeScalarBinaryPatched(Base64Variant b64variant, byte[] data) throws IOException
    {
        String encoded = b64variant.encode(data);
        _emitter.emit(new ScalarEvent(null, TAG_BINARY, EXPLICIT_TAGS, encoded,
                null, null, STYLE_BASE64));
    }

    /*
     *  END OF UGLY HACK
     */
}
