
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
                        if (input.classList.contains("midpoint-input-sufix")) {
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
            let name = input.getAttribute("name");
            if (name != null) {
                if (input.classList.contains("midpoint-input-suffix")) {
                    let originalValue = input.getAttribute("original-value");
                    let inputValue = input.value;
                    if (originalValue != null && originalValue === input.value) {
                        inputValue = "";

                    }
                    valuesForAdd.set("hpField-" + name, inputValue);
                }
            }
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
