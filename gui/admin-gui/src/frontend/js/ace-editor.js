/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
        $(jqEditor).data('resizeToMaxHeight', resize);
        $(jqEditor).data('minHeight', minHeight);

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
            enableSnippets: false,
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
            window.MidPointAceEditor.cursorPosition = editor.session.getDocument().positionToIndex(editor.getCursorPosition());
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
        this.registerWindowResizeHandler();
        // //todo handle readonly for text area [lazyman] add "disabled" class to .ace_scroller
    }

    resizeToMaxHeight(editorId, minHeight) {
        //38 + 1 + 21 is menu outer height
        var newHeight = this.getMaxSizeHeight(minHeight);

        this.resizeToFixedHeight(editorId, newHeight);
    }

    getMaxSizeHeight(minHeight) {
        var headerHeight = $('nav.app-header').outerHeight(true);
        var newHeight = $(window).height() - headerHeight;

        return Math.max(newHeight, minHeight);
    }

    resizeToFixedHeight(editorId, height) {
        $('#' + editorId).height(height.toString() + "px");
        $('#' + editorId + '-section').height(height.toString() + "px");

        $.aceEditors[editorId].resize();
    }

    registerWindowResizeHandler() {
        if (this.windowResizeHandlerRegistered) {
            return;
        }

        this.windowResizeHandlerRegistered = true;

        var self = this;
        var resizeTimeout;
        $(window).on('resize.midPointAceEditor', function () {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(function () {
                Object.keys($.aceEditors).forEach(function (editorId) {
                    var jqEditor = $('#' + editorId);
                    if (jqEditor.data('resizeToMaxHeight')) {
                        self.resizeToMaxHeight(editorId, jqEditor.data('minHeight'));
                    }
                });
            }, 100);
        });
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
                    aceSuggestions.push({caption: suggestion.name, value: suggestion.name, score: suggestion.priority, meta: suggestion.alias});
                })

                // select suggestions by prefix
                if (prefix.length > 0) {
                    callback(null, aceSuggestions.filter((s) => s.caption.startsWith(prefix)));
                } else {
                    callback(null, aceSuggestions);
                }
            }
        };

        editor.completers = [customCompleter];
        editor.execCommand('startAutocomplete');
    }
}
