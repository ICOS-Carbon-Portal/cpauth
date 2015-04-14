
function switchToLoggedOutState(){
	hideMessage();
	$("#hello").hide();
	$("#forLocalsOnly").hide();
	$("#goodbye").show();
}


function switchToLoggedInState(){
	$("#goodbye").hide();
	$("#hello").show();
}


function displayUserInfo(uinfo){

	switchToLoggedInState();

	$("#givenName").html(uinfo.givenName);
	$("#surname").html(uinfo.surname);
	$("#mail").html(uinfo.mail);
}


function reportError(xhr){
	showMessage(xhr.responseText, "alert-danger");
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


function showDeleteAccountConfirmation(){
	$("#deleteAccountButton").hide();
	$("#deleteAccountConfirmation").show();
}


function hideDeleteAccountConfirmation(){
	$("#deleteAccountConfirmation").hide();
	$("#deleteAccountButton").show();
}


function enterKeyHandler(innerFun){
	return function(e){
		if(e.which == 13) innerFun(e);
	}
}


function signOut(){
	$.get("/logout")
		.done(switchToLoggedOutState)
		.fail(reportError);
}


function changePassword(){
	var query = $.param({
		oldPass: $("#oldPassword").val(),
		newPass: $("#newPassword").val()
	});

	$.post("/password/changepassword", query)
		.fail(reportError)
		.done(function(){
			$("#oldPassword").val('');
			$("#newPassword").val('');
			reportSuccess("Password was changed successfully");
		});
}

function deleteAccount(){
	$.post("/password/deleteownaccount")
		.fail(reportError)
		.done(switchToLoggedOutState);
}

$(function(){
	$.getJSON('/whoami')
		.done(displayUserInfo)
		.fail(switchToLoggedOutState);

	$.getJSON('/password/amilocal')
		.done(function(userIsLocal){
			if(userIsLocal) $("#forLocalsOnly").show();
		});

	$("#signOutButton").click(signOut);
	$("#changePasswordButton").click(changePassword);
	$("#deleteAccountButton").click(showDeleteAccountConfirmation);
	$("#cancelDeleteButton").click(hideDeleteAccountConfirmation);
	$("#reallyDeleteButton").click(deleteAccount);

	$("#newPassword").keypress(enterKeyHandler(changePassword));
	$("#oldPassword").keypress(enterKeyHandler(function(){
		$("#newPassword").focus();
	}));

});

