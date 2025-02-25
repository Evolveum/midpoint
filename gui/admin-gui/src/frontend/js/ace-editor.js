/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

var ACE_EDITOR_POSTFIX = "_editor";
var DISABLED_CLASS = "disabled";
$.aceEditors = {};

export default class MidPointAceEditor {

    changeMode(textAreaId, mode) {
        console.info("Updating editor mode for " + textAreaId + " mode: " + mode);

        var editorId = textAreaId + ACE_EDITOR_POSTFIX;

        var editor = $.aceEditors[editorId];
        editor.session.setMode(mode);
    }

    initEditor(textAreaId, readonly, resize, height, minHeight, mode, dark) {
        console.info("Initializing editor " + textAreaId + " readonly: " + readonly + " mode: " + mode + " dark: " + dark);

        var jqTextArea = '#' + textAreaId;
        var editorId = textAreaId + ACE_EDITOR_POSTFIX;
        var jqEditor = '#' + editorId;

        if ($("#" + editorId).length > 0){
            $(jqTextArea).hide();
            return;
        }

        //
        var newHeight = height;
        if (resize) {
            newHeight = this.getMaxSizeHeight(minHeight);
        }
        $('<div id="' + editorId + '" class="aceEditor" style="height: ' + newHeight + 'px;"></div>').insertAfter($('#' + textAreaId));

        $(jqEditor).text($(jqTextArea).val());
        $(jqTextArea).hide();

        $(jqEditor).addClass($(jqTextArea).attr("class"));

        ace.require("ace/ext/language_tools");

        var themeValue = dark ? 'ace/theme/idle_fingers' : 'ace/theme/eclipse';

        var editor = ace.edit(editorId,{
            theme: themeValue,
            mode: mode,
            highlightActiveLine : true,
            highlightSelectedWord: true,
            autoScrollEditorIntoView: true,
            minLines: 10,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true,
            enableSnippets: true,
            selectionStyle: "text",
            useSoftTabs: true,
            tabSize: 3,
            showPrintMargin: false,
            fadeFoldWidgets: false,
        });

        this.setReadonly(jqEditor, editor, readonly);

        editor.on('blur', function () {
            $(jqTextArea).val(editor.getSession().getValue());
            $(jqTextArea).trigger('blur');
        });

        editor.on('change', function () {
            const cursor = editor.getCursorPosition();
            const lines = editor.session.getLines(0, cursor.row);
            let position = cursor.column;

            lines.forEach((line, key) => {
                if (cursor.row > key) {
                    position = position + line.length
                }
            })

            window.MidPointAceEditor.cursorPosition = position;

            $(jqTextArea).val(editor.getSession().getValue());
            $(jqTextArea).trigger('change');
        });

        editor.commands.addCommand({
            name: 'runAutocomplete',
            bindKey: { win: "Ctrl-Space", mac: "Ctrl-Space" },
            exec: function (editor) {
                $(jqTextArea).trigger('change');
                editor.execCommand("startAutocomplete"); // trigger autocomplete
            },
            readOnly: true,
        })

        // add editor to global map, so we can find it later
        $.aceEditors[editorId] = editor;
        // //todo handle readonly for text area [lazyman] add "disabled" class to .ace_scroller
    }

    resizeToMaxHeight(editorId, minHeight) {
        //38 + 1 + 21 is menu outer height
        var newHeight = this.getMaxSizeHeight(minHeight);

        this.resizeToFixedHeight(editorId, newHeight);
    }

    getMaxSizeHeight(minHeight) {
        var footerHeight = $('footer.main-footer').outerHeight(true);

        var newHeight;
        if (footerHeight) {
            newHeight = $(document).innerHeight()
                - footerHeight - $('nav.main-header').outerHeight(true);
        } else {
            newHeight = $(document).innerHeight() - $('nav.main-header').outerHeight(true);
        }

        var boxHeader = $('div.card-header').outerHeight(true);
        var buttonsBar = $('div.main-button-bar').outerHeight(true);
        if (buttonsBar){
            newHeight = newHeight - buttonsBar;
        }
        if (boxHeader){
            newHeight = newHeight - boxHeader;
        }
        if (newHeight < minHeight) {
            newHeight = minHeight;
        }
        return newHeight;
    }

    resizeToFixedHeight(editorId, height) {
        $('#' + editorId).height(height.toString() + "px");
        $('#' + editorId + '-section').height(height.toString() + "px");

        $.aceEditors[editorId].resize();
    }

    refreshReadonly(textAreaId, readonly) {
        var jqTextArea = '#' + textAreaId;

        var editorId = textAreaId + ACE_EDITOR_POSTFIX;
        var jqEditor = '#' + editorId;

        var editor = $.aceEditors[editorId];
        setReadonly(jqEditor, editor, readonly);
        editor.focus();
    }

    setReadonly(jqEditor, editor, readonly) {
        editor.setReadOnly(readonly);
        if (readonly) {
            $(jqEditor).addClass(DISABLED_CLASS);
        } else {
            $(jqEditor).removeClass(DISABLED_CLASS);
        }
    }

    reloadTextarea(editor) {
        editor.setReadOnly(readonly);
        if (readonly) {
            $(jqEditor).addClass(DISABLED_CLASS);
        } else {
            $(jqEditor).removeClass(DISABLED_CLASS);
        }
    }

    syncContentAssist(suggestions, errors, editorId) {
        const editor = ace.edit(editorId + ACE_EDITOR_POSTFIX);

        // validation
        let annotations = [];
        errors.forEach(error => {
            annotations.push({
                row: error.lineStart - 1,
                column: error.charPositionInLineStart,
                text: error.message,
                type: "error",
            });
        });

        editor.session.setAnnotations(annotations);

        // code completions
        let customCompleter = {
            getCompletions: function (editor, session, pos, prefix, callback) {
                const aceSuggestions = [];

                suggestions.forEach(function (suggestion) {
                    aceSuggestions.push({caption: suggestion.name, value: suggestion.name, meta: suggestion.alias});
                })

                // select suggestions by prefix
                if (prefix.length > 0) {
                    callback(null, aceSuggestions.filter((s) => s.caption.startsWith(prefix)));
                } else {
                    callback(null, aceSuggestions);
                }
            },
        };

        editor.completers = [customCompleter];
        editor.execCommand('startAutocomplete');
    }
}
