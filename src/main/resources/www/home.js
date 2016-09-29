
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

var stringKeys = ['givenName', 'surname', 'orcid', 'affiliation', 'affiliation2', 'workPhone', 'mobilePhone',
	'streetAddress', 'city', 'zipCode', 'country', 'gender', 'birthYear'];

function displayUserInfo(uid){
	switchToLoggedInState();
	$("#email").html(uid.email);

	var keys = '{profile: 1}';

	$.getJSON('/db/users/' + uid.email + '?keys=' + encodeURIComponent(keys))
		.done(function(userInfo){
			var profile = userInfo.profile;
			stringKeys.forEach(function(key){
				$('#' + key).val(profile[key]);
			});
			document.getElementById("icosLicenceOk").checked = profile.icosLicenceOk;
		}).fail(reportError);
}


function updateUserProfile(){
	var email = $("#email").html();

	var payload = stringKeys.reduce(
		function(seed, key){
			seed[key] = $('#' + key).val();
			return seed;
		},
		{icosLicenceOk: document.getElementById("icosLicenceOk").checked}
	);

	$.ajax({
		method: "PATCH",
		url: "/db/users/" + email,
		contentType: 'application/json',
		data: JSON.stringify({profile: payload})
	}).done(function(){
		reportSuccess('Profile updated');
	}).fail(reportError);
}


function displayToken(token) {
	$("#tokenValue").html(token.value);
	$("#tokenExpiry").html(new Date(token.expiry).toISOString());
	$("#tokenSource").html(token.source);
}

function selectToken() {
	var doc = document;
	var text = doc.getElementById("tokenValue");

	if (doc.body.createTextRange) { // ms
		var range = doc.body.createTextRange();
		range.moveToElementText(text);
		range.select();
	} else if (window.getSelection) { // moz, opera, webkit
		var selection = window.getSelection();
		var range = doc.createRange();
		range.selectNodeContents(text);
		selection.removeAllRanges();
		selection.addRange(range);
	}
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
	$.ajax({
			method: 'DELETE',
			url: '/db/users/' + $('#email').html()
		})
		.pipe(function(){
			return $.post("/password/deleteownaccount");
		})
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

	$.ajax({
		type: "GET",
		url: "../cpauthcookie",
		dateType: "text"
	}).done(function(result){
		displayToken(result);
		$("#tokenValue").click(selectToken);
	});

	$("#signOutButton").click(signOut);
	$("#updateProfileButton").click(updateUserProfile);
	$("#changePasswordButton").click(changePassword);
	$("#deleteAccountButton").click(showDeleteAccountConfirmation);
	$("#cancelDeleteButton").click(hideDeleteAccountConfirmation);
	$("#reallyDeleteButton").click(deleteAccount);

	$("#newPassword").keypress(enterKeyHandler(changePassword));
	$("#oldPassword").keypress(enterKeyHandler(function(){
		$("#newPassword").focus();
	}));

});

