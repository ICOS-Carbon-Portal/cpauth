function createNewAccount() {

	resetValidateNewAccountError();
	
	if (validateNewAccount()) {
		
		var $form = $('#new-account').serializeArray();
	
		$.post("/password/account/create", $form).complete(function(data) {
	
			if (data.status == '200') {
				resetNewAccount();
				newAccountMessageSuccess('A new account is created!');
	
			} else if (data.status == '400') {
				newAccountMessageError('Authentication error! This service requires administrator rights!');
				
			} else if (data.status == '401') {
				newAccountMessageError('Authentication error! This service requires administrator rights!');
	
			} else if (data.status == '403') {
				newAccountMessageError('An account with the same email already exists!');
				
			} else {
				newAccountMessageError('An unexpected server error has occured.');
	
			}
	
		});
	
	} else {
		newAccountMessageError('You must complete all fields!');
	}
}

function newAccountMessageError(message) {
	$('#new-account-message-success').hide();
	
	$('#new-account-message-error').html(message);
	$('#new-account-message-error').addClass('alert alert-danger');
	$('#new-account-message-error').attr('role', 'alert');
	$('#new-account-message-error').show();
}

function newAccountMessageSuccess(message) {
	$('#new-account-message-error').hide();
	
	$('#new-account-message-success').html(message);
	$('#new-account-message-success').addClass('alert alert-success');
	$('#new-account-message-success').attr('role', 'alert');
	$('#new-account-message-success').show();
}

function validateNewAccount() {
	var ok = true;
	
	if( $('#givenName').val().length < 1) {
		$('#givenName').parent().addClass('has-error');
		ok = false;
	}
	
	if( $('#surname').val().length < 1) {
		$('#surname').parent().addClass('has-error');
		ok = false;
	}
	
	if( $('#mail').val().length < 1) {
		$('#mail').parent().addClass('has-error');
		ok = false;
	}
	
	if( $('#password').val().length < 1) {
		$('#password').parent().addClass('has-error');
		ok = false;
	}
	
	return ok;
}

function resetNewAccount() {
	$('#givenName').val('');
	$('#surname').val('');
	$('#mail').val('');
	$('#password').val('');
	
}

function resetValidateNewAccountError() {
	$('#givenName').parent().removeClass('has-error');
	$('#surname').parent().removeClass('has-error');
	$('#mail').parent().removeClass('has-error');
	$('#password').parent().removeClass('has-error');
}