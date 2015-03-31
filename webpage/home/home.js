function switchToLoggedOutState(){
	hideError();
	$("#hello").hide();
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

function reportError(message){
	$("#errorMessage div").html(message);
	$("#errorMessage").show();
}

function hideError(){
	$("#errorMessage").hide();
}

function signOut(){
	$.get("/logout")
		.done(switchToLoggedOutState)
		.fail(function(err){
			reportError(err.statusText);
		});
}

$(function(){
	$.getJSON('/whoami')
		.done(displayUserInfo)
		.fail(switchToLoggedOutState);

	$("#signOutButton").click(signOut);
});
