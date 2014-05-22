/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var ACE_EDITOR_POSTFIX = "_editor";
var DISABLED_CLASS = "disabled";
$.aceEditors = {};

function initEditor(textAreaId, readonly, minSize) {
    var jqTextArea = '#' + textAreaId;
    var editorId = textAreaId + ACE_EDITOR_POSTFIX;
    var jqEditor = '#' + editorId;

    if (!isIE9OrNewer()) {
        //todo handle readonly for text area [lazyman]
        return;
    }

    $('<div id="' + editorId + '" class="aceEditor"></div>').insertAfter($('#' + textAreaId));

    $(jqEditor).text($(jqTextArea).val());
    $(jqTextArea).hide();

    var editor = ace.edit(editorId);
    editor.setTheme("ace/theme/eclipse");
    editor.getSession().setMode("ace/mode/xml");
    editor.setShowPrintMargin(false);
    editor.setFadeFoldWidgets(false);
    setReadonly(jqEditor, editor, readonly);
    editor.on('blur', function () {
        $(jqTextArea).val(editor.getSession().getValue());
        $(jqTextArea).trigger('onBlur');
    });

    //add editor to global map, so we can find it later
    $.aceEditors[editorId] = editor;

    //todo handle readonly for text area [lazyman] add "disabled" class to .ace_scroller

    $(document).ready(function () {
        //38 + 1 + 21 is menu outer height
        var newHeight = $(document).innerHeight() - $('div.mainContainer').outerHeight(true) - 60;
        if (newHeight < minSize) {
            newHeight = minSize;
        }

        $('#' + editorId).height(newHeight.toString() + "px");
        $('#' + editorId + '-section').height(newHeight.toString() + "px");

        editor.resize();
    });
}

function refreshReadonly(textAreaId, readonly) {
    var jqTextArea = '#' + textAreaId;
    if (!isIE9OrNewer()) {
        var area = $(jqTextArea);
        area.attr('readonly', readonly);
        area.focus();

        if (readonly) {
            area.addClass(DISABLED_CLASS);
        } else {
            area.removeClass(DISABLED_CLASS);
        }

        return;
    }

    var editorId = textAreaId + ACE_EDITOR_POSTFIX;
    var jqEditor = '#' + editorId;

    var editor = $.aceEditors[editorId];
    setReadonly(jqEditor, editor, readonly);
    editor.focus();
}

function setReadonly(jqEditor, editor, readonly) {
    editor.setReadOnly(readonly);
    if (readonly) {
        $(jqEditor).addClass(DISABLED_CLASS);
    } else {
        $(jqEditor).removeClass(DISABLED_CLASS);
    }
}