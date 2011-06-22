/**
 * 
 * 
 * @author lazyman
 * 
 */
function displayMessageDetails(id, showImage, hideImage) {
	var buttonElement = document.getElementById(id + "ImageButton");
	var messageElement = document.getElementById(id);
	var value = messageElement.style.display;
	if (value == 'none') {
		messageElement.style.display = 'block';
		buttonElement.src = hideImage;
	} else {
		messageElement.style.display = 'none';
		buttonElement.src = showImage;
	}
}