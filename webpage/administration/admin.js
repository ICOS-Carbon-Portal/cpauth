/*
 * Administration
 */


/*
 * Code regarding create new account
 */ 
function createNewAccount() {

	resetValidateNewError();
	
	if (validateNew()) {
		
		var $form = $('#new-account').serializeArray();
	
		$.post("/password/account/create", $form).complete(function(data) {	
			if (data.status == '200') {
				resetNew();
				newMessageSuccess('A new account is created!');
	
			} else if (data.status == '400') {
				newMessageError('Authentication error! This service requires administrator rights!');
				
			} else if (data.status == '401') {
				newMessageError('Authentication error! This service requires administrator rights!');
	
			} else if (data.status == '403') {
				newMessageError('An account with the same email already exists!');
				
			} else {
				newMessageError('An unexpected server error has occured.');
			}
	
		});
	
	} else {
		newMessageError('You must complete all fields!');
	}
}

function newMessageError(message) {
	$('#new-message-success').hide();
	
	$('#new-message-error').html(message);
	$('#new-message-error').addClass('alert alert-danger');
	$('#new-message-error').attr('role', 'alert');
	$('#new-message-error').show();
}

function newMessageSuccess(message) {
	$('#new-message-error').hide();
	
	$('#new-message-success').html(message);
	$('#new-message-success').addClass('alert alert-success');
	$('#new-message-success').attr('role', 'alert');
	$('#new-message-success').show();
}

function validateNew() {
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

function resetNew() {
	$('#givenName').val('');
	$('#surname').val('');
	$('#mail').val('');
	$('#password').val('');
	
}

function resetValidateNewError() {
	$('#givenName').parent().removeClass('has-error');
	$('#surname').parent().removeClass('has-error');
	$('#mail').parent().removeClass('has-error');
	$('#password').parent().removeClass('has-error');
}



/*
 * Code regarding edit account
 */
$('#list-accounts-butt').click(function() {
	
	$.getJSON( "/password/account/list", function() {
		
	})
	.done(function(data) {
		
		$.each(data, function(i, entry){
			
			var info = entry.info;
			var isAdmin = entry.isAdmin;
			
			var editAdmin = '';
			if (isAdmin) {
				//editAdmin = '<button class="btn btn-default" onClick="unmakeAdmin(\'' + info.mail + '\')">Unmake admin</button>';
				editAdmin = '<label class="checkbox-inline"><input type="checkbox" onClick="unmakeAdmin(\'' + info.mail + '\')" value="">Unmake admin</label>';
			} else {
				//editAdmin = '<button class="btn btn-default" onClick="makeAdmin(\'' + info.mail + '\')">Make admin</button>';
				editAdmin = '<label class="checkbox-inline"><input type="checkbox" onClick="makeAdmin(\'' + info.mail + '\')" value="">Make admin</label>';
			}
			
			$('#list-accounts table').append('<tr><td>' + info.givenName + '&nbsp;' + info.surname + '&nbsp;(' + info.mail + ')</td><td><button class="btn btn-default" onClick="deleteAccount(\'' + info.mail + '\')">Delete</button></td><td>' + editAdmin + '</td></tr>');
			
		});
		
	})
	.fail(function(data, status, error) {
		if (data.status == '400') {
			editMessageError('Authentication error! This service requires administrator rights!');

		} else if (data.status == '401') {
        	editMessageError('Authentication error! This service requires administrator rights!');

		} else {
			editMessageError('An unexpected server error has occured.');
		}

	});

});














function deleteAccount(account) {
	alert('deleteAccount ' + account);
}

function makeAdmin(account) {
	alert('makeAdmin ' + account);
}

function unmakeAdmin(account) {
	alert('unmakeAdmin ' + account);
}

function editMessageError(message) {
        $('#edit-message-success').hide();

        $('#edit-message-error').html(message);
        $('#edit-message-error').addClass('alert alert-danger');
        $('#edit-message-error').attr('role', 'alert');
        $('#edit-message-error').show();
}

function editMessageSuccess(message) {
        $('#edit-message-error').hide();

        $('#edit-message-success').html(message);
        $('#edit-message-success').addClass('alert alert-success');
        $('#edit-message-success').attr('role', 'alert');
        $('#edit-message-success').show();
}

