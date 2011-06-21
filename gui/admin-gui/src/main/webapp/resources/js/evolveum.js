/**
 * 
 * 
 * @author lazyman
 * 
 */
function displayMessageDetails(id) {
	var buttonElement = document.getElementById("image" + id);
	var element = document.getElementById(id);
	var value = element.style.display;
	if (value == 'none') {
		element.style.display = 'block';
		buttonElement.src = "/admin-gui/resources/images/delete.png";
	} else {
		element.style.display = 'none';
		buttonElement.src = "/admin-gui/resources/images/add.png";
	}
}