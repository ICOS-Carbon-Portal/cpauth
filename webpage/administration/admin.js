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
	
		$.post("/password/createaccount", $form)

		.done(function() {
			resetNew();
			newMessageSuccess('A new account is created!');
		})
			
		.fail(function(data) {
			newMessageError(data.responseText);
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
	populateListAccounts();
});


function populateListAccounts() {
	$('#list-accounts table').empty();
	
	$.getJSON("/password/accountslist")
	
	.done(function(data) {
		
		$.each(data, function(i, entry){
			
			var info = entry.info;
			var isAdmin = entry.isAdmin;
			
			var form = '<form><input type="hidden" name="mail" value="\'' + info.mail + '\'"></form>';
			
			var editAdmin = '';
			if (isAdmin) {
				editAdmin = '<input type="checkbox" onClick="unmakeAdmin(\'' + form + '\')" checked />';
			} else {
				editAdmin = '<input type="checkbox" onClick="makeAdmin(\'' + form + '\')" />';
			}
			
			$('#list-accounts table').append('<tr><td>' + info.givenName + '&nbsp;' + info.surname + '&nbsp;(' + info.mail + ')</td><td><button class="btn btn-default" onClick="deleteAccount(\'' + form + '\')">Delete</button></td><td>' + editAdmin + '</td></tr>');
			
		});
		
	})
	
	.fail(function(data) {
		editMessageError(data.responseText);
	});
}


function deleteAccount(account) {
	$.post("/password/deleteaccount", account)

	.done(function() {
		
	})
		
	.fail(function(data) {
		editMessageError(data.responseText);
	});
	
	populateListAccounts();
}

function makeAdmin(account) {
	$.post("/password/makeadmin", account)

	.done(function() {
		
	})
		
	.fail(function(data) {
		editMessageError(data.responseText);
	});
	
	populateListAccounts();
}

function unmakeAdmin(account) {
	$.post("/password/unmakeadmin", account)

	.done(function() {
		
	})
		
	.fail(function(data) {
		editMessageError(data.responseText);
	});
	
	populateListAccounts();
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

