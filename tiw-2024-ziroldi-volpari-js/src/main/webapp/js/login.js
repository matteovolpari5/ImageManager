/**
* Login and signup management.
*/

(function() {
	// link html elements

	var login_button = document.getElementById("login_button");
	var login_error_message = document.getElementById("login_error_message");

	var signup_button = document.getElementById("signup_button");
	var signup_error_message = document.getElementById("signup_error_message");
	var password = signup_button.closest("form").querySelector('input[name="password"]');
	var repeat_password = signup_button.closest("form").querySelector('input[name="repeatPassword"]');

	// add event listener to login button
	login_button.addEventListener("click", (e) => {
		// get login form 
		var form = e.target.closest("form");
		if (form.checkValidity()) {
			// after html checks
			// send to server, uses makeCall util
			sendToServer(form, login_error_message, "CheckLogin");
		} else {
			form.reportValidity();
		}
	});

	// add event listener to signup button
	signup_button.addEventListener("click", (e) => {
		// get signup form
		var form = e.target.closest("form");
		if (form.checkValidity()) {
			// after html checks

			// check password equals repeat password
			if (password.value != repeat_password.value) {
				signup_error_message.textContent = "Different values for password and repeat password";
				return;
			}

			// send to server, uses makeCall util
			sendToServer(form, signup_error_message, "CheckSignup");
			
		} else {
			// display input not valid
			form.reportValidity();
		}
	});

	function sendToServer(form, error_message, request_url) {
		// function because the call back is the same
		makeCall("POST", request_url, form,
			function(req) {
				// readyState == XMLHttpRequest.DONE
				var message = req.responseText;
				switch (req.status) {
					case 200:
						// ok
						window.location.href = "home.html";
						break;
					case 400: // bad request
					case 401: // unauthorized
					case 500: // server error
						error_message.textContent = message;
						break;
					default:
						error_message.textContent = "An error occurred, retry.";
				}
			}
		);
	}

})();
