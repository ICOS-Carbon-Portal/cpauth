
function reportError(xhr, fallbackMessage){
	function makeMessage(xhr){
		var defaultMsg = fallbackMessage || "Unknown error";
		if(!xhr) return defaultMsg;
		if(xhr.responseText) return xhr.responseText;
		if(xhr.statusText) return xhr.statusText;
		if(xhr.toString() == '[Object object]') return defaultMsg;
		return xhr.toString();
	}
	showMessage(makeMessage(xhr), "alert-danger");
}

function reportSuccess(msg){
	showMessage(msg, "alert-success", 4000);
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

function enterKeyHandler(innerFun){
	return function(e){
		if(e.which == 13) innerFun(e);
	}
}

