package com.evolveum.midpoint.testing.story.sysperf;

record ScriptingConfiguration(String language) {
    private static final String PROP_SCRIPTING_LANGUAGE = "scripting-language";
    private static final String DEFAULT_LANGUAGE = "Groovy";

    static ScriptingConfiguration setup() {
        return new ScriptingConfiguration(System.getProperty(PROP_SCRIPTING_LANGUAGE, DEFAULT_LANGUAGE));
    }

}
