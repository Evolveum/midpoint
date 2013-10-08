var ACE_EDITOR_POSTFIX = "_editor";
var DISABLED_CLASS = "disabled";
$.aceEditors = {};

function initEditor(textAreaId, readonly) {
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
    editor.setReadOnly(readonly);
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
        if (newHeight < 200) {
            newHeight = 200;
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
    editor.setReadOnly(readonly);
    editor.focus();
    if (readonly) {
        $(jqEditor).find(".ace_scroller").addClass(DISABLED_CLASS);
    } else {
        $(jqEditor).find(".ace_scroller").removeClass(DISABLED_CLASS);
    }
}