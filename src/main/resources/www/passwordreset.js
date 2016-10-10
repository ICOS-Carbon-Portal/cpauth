
function passwordIsGood(pass){
	return pass && pass.length >= 6;
}

function setPassword() {
	hideMessage();

	var pass = $('#newPass').val();
	var pass2 = $('#newPass2').val();

	if(pass == pass2 && passwordIsGood(pass)){
		var form = $('#password-reset').serializeArray();

		$.post("/password/setpassword", form)
			.done(function() {
				reportSuccess('Your password has been successfully (re)set', 60000);
			})
			.fail(reportError);
	}else if(!passwordIsGood(pass))
		reportError('Password too short');
	else
		reportError('New password and password confirmation do not match');
}

$(function(){

	$("#setPasswordButton").click(setPassword);

});

