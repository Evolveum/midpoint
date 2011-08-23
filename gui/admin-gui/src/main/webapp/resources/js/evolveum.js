/**
 * 
 * @author lazyman
 * 
 */
function displayMessageDetails(id, showImage, hideImage) {
	var buttonElement = document.getElementById(id + "ImageButton");
	var messageElement = document.getElementById(id);
	var value = messageElement.style.display;
	if (value == 'none' || value == '') {
		messageElement.style.display = 'block';
		buttonElement.src = hideImage;
	} else {
		messageElement.style.display = 'none';
		buttonElement.src = showImage;
	}
}

function displayMessageErrorDetails(id) {
	var blockElement = document.getElementById(id + "_block");
	var value = blockElement.style.display;
	if (value == 'none' || value == '') {
		blockElement.style.display = 'block';
	} else {
		blockElement.style.display = 'none';
	}
}

function displayMessageCauseDetails(id) {
	var buttonElement = document.getElementById(id);
	var blockElement = document.getElementById(id + "_block");
	var value = blockElement.style.display;
	if (value == 'none' || value == '') {
		blockElement.style.display = 'block';
		buttonElement.innerHTML=" [Less]";
	} else {
		blockElement.style.display = 'none';
		buttonElement.innerHTML=" [More]";
	}
}