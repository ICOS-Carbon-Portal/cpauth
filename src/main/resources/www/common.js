
function reportError(xhr){
	showMessage(xhr.responseText || xhr.toString(), "alert-danger");
}

function reportSuccess(msg){
	showMessage(msg, "alert-success", 1500);
}

function showMessage(message, msgType, hideAfter){
	var $msg = $("#message");
	var oldTimeout = $msg.data("hideTimeout");
	if(oldTimeout) clearTimeout(oldTimeout);

	fadeOutMessage(function(){
		$msg.html(message);
		$msg.addClass(msgType);
		$msg.show();

		if(hideAfter){
			var newTimeout = setTimeout(fadeOutMessage, hideAfter);
			$msg.data("hideTimeout", newTimeout);
		}
	});
}

function fadeOutMessage(runAfter){
	$("#message").fadeOut(function(){
		hideMessage();
		if($.isFunction(runAfter)) runAfter();
	});
}

function hideMessage(){
	$("#message").hide();
	$("#message").removeClass("alert-success alert-info alert-warning alert-danger");
}

