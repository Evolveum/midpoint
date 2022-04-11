/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import * as ace from '../../../../../../node_modules/ace-builds/src-noconflict/ace'

require('../../../../../../node_modules/ace-builds/src-noconflict/theme-eclipse')
require('../../../../../../node_modules/ace-builds/src-noconflict/mode-xml')
require('../../../../../../node_modules/ace-builds/src-noconflict/ext-language_tools')

var ACE_EDITOR_POSTFIX = "_editor";
// var DISABLED_CLASS = "disabled";
$.aceEditors = {};

export default class MidPointAceEditor {

    initEditor(textAreaId, readonly, resize, height, minHeight, mode) {
        var jqTextArea = '#' + textAreaId;
        var editorId = textAreaId + ACE_EDITOR_POSTFIX;
        var jqEditor = '#' + editorId;
        //
        var newHeight = height;
        if (resize) {
            newHeight = this.getMaxSizeHeight(minHeight);
        }
        $('<div id="' + editorId + '" class="aceEditor" style="height: ' + newHeight + 'px;"></div>').insertAfter($('#' + textAreaId));

        $(jqEditor).text($(jqTextArea).val());
        $(jqTextArea).hide();

        // var langTools = ace.require("ace/ext/language_tools");
        //todo implement completer based
        // var completer = {
        //
        //     getCompletions: function(editor, session, pos, prefix, callback) {
        //         //example
        //         var completions = [];
        //         completions.push({ name:"testing1", value:"testing1", meta: "code1" });
        //         completions.push({ name:"testing2", value:"testing2", meta: "code2" });
        //         callback(null, completions);
        //     }
        // }
        // langTools.addCompleter(completer);

        var editor = ace.edit(editorId,{
            mode: "ace/mode/xml",
            highlightActiveLine : true,
            highlightSelectedWord: true,
            autoScrollEditorIntoView: true,
            minLines: 10,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true,
            selectionStyle: "text"
        });

        // editor.setOptions({
        //     enableBasicAutocompletion: true
        // });

        // editor.setMode('ace/mode/xml');
        // editor.session.setMode(mode_xml);
        // editor.getSession().setTabSize(3);

        editor.setTheme('ace/theme/eclipse');
        // if (mode != null) {
        //
        // }
        // editor.setShowPrintMargin(false);
        // editor.setFadeFoldWidgets(false);
        // setReadonly(jqEditor, editor, readonly);
        // editor.on('blur', function () {
        //     $(jqTextArea).val(editor.getSession().getValue());
        //     $(jqTextArea).trigger('blur');
        // });
        // editor.on('change', function () {
        //     $(jqTextArea).val(editor.getSession().getValue());
        //     $(jqTextArea).trigger('change');
        // });
        //
        // //add editor to global map, so we can find it later
        $.aceEditors[editorId] = editor;
        //
        // //todo handle readonly for text area [lazyman] add "disabled" class to .ace_scroller

        // $(document).ready(function () {
        //
        //     var self = this;
        //
        //     if (height < minHeight) {
        //         height = minHeight;
        //     }
        //
        //     if (resize) {
        //         self.resizeToMaxHeight(editorId, minHeight);
        //     } else {
        //         self.resizeToFixedHeight(editorId, height);
        //     }
        // });
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

}

