/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

export default class MidPointAceEditor {

    initHoneypotFields(formId) {
        let form = $("#" + formId);

        if (!form.length) {
            return;
        }

        form.attr("autocomplete", "off");

        let inputs = form.find("input");
        if (inputs && inputs.length > 0)
            for (let i = 0; i < inputs.length; i++) {
                let input = inputs[i];

                let type = input.getAttribute("type");
                if (type === "hidden") {
                    continue;
                }

                let originalValue = input.getAttribute("value");
                if (originalValue != null) {
                    input.setAttribute("original-value", originalValue);
                }

                let clone = input.cloneNode(true);

                // The clone must not be submitted under the same name as the real field - two
                // values for one parameter get joined by the server (observed as a literal ";"
                // ending up as the field's bound value once both are empty). Keep the original
                // name around under a data attribute so addRightAttributeForForm() can still
                // report it as "hpField-<name>".
                let originalName = input.getAttribute("name");
                if (originalName != null) {
                    clone.setAttribute("data-honeypot-name", originalName);
                    clone.removeAttribute("name");
                }

                input.classList.add("midpoint-input-prefix");
                clone.classList.add("midpoint-input-suffix");

                let parent = input.parentElement;

                let newId = "id" + this.makeId(2);
                let e = $("#" + newId);
                while(e && e.length > 0) {
                    newId = "id" + this.makeId(2);
                    e = $("#" + newId);
                }
                clone.id = newId;

                let ariaControls = input.getAttribute("aria-controls");
                if (ariaControls != null) {
                    clone.setAttribute("aria-controls", "wicket-autocomplete-listbox-" + newId);
                }

                if (input.nextSibling) {
                    parent.insertBefore(clone, input.nextSibling);
                } else {
                    parent.appendChild(clone);
                }
            }

            form.submit(function(){
                let form = $("#" + formId);

                if (!form.length) {
                    return;
                }

                let inputs = form.find("input");

                console.log(formId);
                console.log(inputs);

                if (inputs && inputs.length > 0)
                    for (let i = 0; i < inputs.length; i++) {
                        let input = inputs[i];
                        if (input.classList.contains("midpoint-input-suffix")) {
                            $("#" + input.id).prop('disabled', true);
                        }
                    }
            });
    }

    makeId(length) {
        let result = '';
        const characters = 'abcdefghijklmnopqrstuvwxyz0123456789';
        const charactersLength = characters.length;
        let counter = 0;
        while (counter < length) {
          result += characters.charAt(Math.floor(Math.random() * charactersLength));
          counter += 1;
        }
        return result;
    }

    addRightAttributeForForm(attrs, formId) {
        let form = $("#" + formId);

        if (!form.length) {
            return;
        }

        let inputs = form.find("input");

        let valuesForAdd = new Map();

        for (let i = 0; i < inputs.length; i++) {
            let input = inputs[i];
            if (!input.classList.contains("midpoint-input-suffix")) {
                continue;
            }
            let name = input.getAttribute("data-honeypot-name");
            if (name == null) {
                continue;
            }
            let originalValue = input.getAttribute("original-value");
            let inputValue = input.value;
            if (originalValue != null && originalValue === input.value) {
                inputValue = "";
            }
            valuesForAdd.set("hpField-" + name, inputValue);
        }

        valuesForAdd.forEach((value, key) => {
            for (let [i, parameter] of attrs.ep.entries()) {
                if (parameter.name === key) {
                    attrs.ep.splice(i, 1);
                    break;
                }
            }
            attrs.ep.push({name: key, value: value});
        })

        return attrs;
    }
}
