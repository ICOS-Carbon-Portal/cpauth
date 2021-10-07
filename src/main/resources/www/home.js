
function redirectHome(){
	window.location.href = "/login";
}


function switchToLoggedInState(){
	$("#hello").show();
}

var stringKeys = ['givenName', 'surname', 'orcid', 'affiliation', 'affiliation2', 'workPhone', 'mobilePhone',
	'streetAddress', 'city', 'zipCode', 'country', 'gender', 'birthYear'];

function displayUserInfo(uid){
	switchToLoggedInState();
	$("#email").html(uid.email);

	var keys = '{profile: 1}';

	$.getJSON('/db/users/' + uid.email + '?keys=' + encodeURIComponent(keys))
		.then(null, function(xhr){
			return xhr.status == 404
				? $.ajax({ //create user profile in RESTHeart if it does not exist
					method: 'PUT',
					url: '/db/users/' + uid.email,
					contentType: 'application/json',
					dataType: 'text',
					data: '{profile: {}}'
				}).then(function(){
						reportSuccess('Profile created');
						return {profile: {}};
					})
				: $.Deferred().reject(xhr);
		})
		.done(function(userInfo){
			var profile = userInfo.profile;
			stringKeys.forEach(function(key){
				$('#' + key).val(profile[key]);
			});
			document.getElementById("icosLicenceOk").checked = profile.icosLicenceOk;
		})
		.fail(reportError);
}


function updateUserProfile(uid){

	var payload = stringKeys.reduce(
		function(seed, key){
			seed[key] = $('#' + key).val();
			return seed;
		},
		{icosLicenceOk: document.getElementById("icosLicenceOk").checked}
	);

	$.ajax({
		method: "PATCH",
		url: "/db/users/" + uid.email,
		contentType: 'application/json',
		dataType: 'text',
		data: JSON.stringify({profile: payload})
	}).done(function(){
		reportSuccess('Profile updated');
	}).fail(reportError);
}


function displayToken(token) {
	if(token.source == 'Password'){
		$("#forLocalsOnly").show();
	}

	$("#tokenValue").html(token.value);
	$("#tokenValue").click(selectToken);

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


function enterKeyHandler(innerFun){
	return function(e){
		if(e.which == 13) innerFun(e);
	}
}


function signOut(){
	$.get("/logout")
		.done(redirectHome)
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

function deleteProfile(uid){
	return $.ajax({
		method: 'DELETE',
		url: '/db/users/' + uid.email
	});
}

function deleteAccount(uid){
	deleteProfile(uid)
		.then(function(){
			return $.post("/password/deleteownaccount");
		})
		.then(redirectHome, reportError);
}

function deleteProfileAndRefresh(uid){
	deleteProfile(uid)
		.then(displayUserInfo.bind(null, uid), reportError);
}

function setupDangerousEvents(deletedInfo, realDeletion){
	$("#delete" + deletedInfo + "Button").click(function(){
		$("#delete" + deletedInfo + "Button").hide();
		$("#delete" + deletedInfo + "Confirmation").show();
	});

	function cancelDeletion(){
		$("#delete" + deletedInfo + "Confirmation").hide();
		$("#delete" + deletedInfo + "Button").show();
	}

	$("#cancelDelete" + deletedInfo + "Button").click(cancelDeletion);

	$("#reallyDelete" + deletedInfo + "Button").click(function(){
		realDeletion();
		cancelDeletion();
	});
}

function setupEvents(uid){
	$("#signOutButton").click(signOut);
	$("#updateProfileButton").click(updateUserProfile.bind(null, uid));
	$("#changePasswordButton").click(changePassword);

	setupDangerousEvents('Account', deleteAccount.bind(null, uid));
	setupDangerousEvents('Profile', deleteProfileAndRefresh.bind(null, uid));

	$("#newPassword").keypress(enterKeyHandler(changePassword));
	$("#oldPassword").keypress(enterKeyHandler(function(){
		$("#newPassword").focus();
	}));
}

$(function(){
	$.getJSON('/whoami')
		.done(function(uid){
			displayUserInfo(uid);
			setupEvents(uid);
		})
		.fail(redirectHome);

	$.getJSON("/cpauthcookie")
		.done(displayToken);
});

