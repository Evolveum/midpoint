function initEditor(textAreaId, editorId, readonly) {
    if (!isIE9OrNewer()) {

        return;
    }

    var jqTextArea = '#' + textAreaId;
    var jqEditor = '#' + editorId;

    $('<div id="' + editorId + '" class="aceEditor"></div>').insertAfter($('#' + textAreaId));

    $(jqEditor).text($(jqTextArea).val());
    $(jQextArea).hide();

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