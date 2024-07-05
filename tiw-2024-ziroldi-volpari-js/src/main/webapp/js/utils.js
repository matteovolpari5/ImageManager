/**
* Utils functions.
*/

// function used to make Ajax calls: GET requests and forms POST requests
function makeCall(method, url, formElement, callback, reset = true) {
    var req = new XMLHttpRequest();
     
    req.onreadystatechange = function() {
        if(req.readyState == XMLHttpRequest.DONE) {
            callback(req);
        }
    };
    
    req.open(method, url);	// asynchronous
    
    if (formElement == null) {  
      // for GET requestes
        req.send();
    } else { 
		req.send(new FormData(formElement));
    }
    
    if (formElement !== null && reset === true) {
		formElement.reset();
    }
}

// function used to make Ajax calls: POST requests with Json data 
function makeCallJson(url, jsonData, callback) {
    var req = new XMLHttpRequest();
     
    req.onreadystatechange = function() {
        if(req.readyState == XMLHttpRequest.DONE) {
            callback(req);
        }
    };
    
    req.open("POST", url);	// asynchronous
    
    req.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
	req.send(jsonData);
}

 
// IIFE used to implement automatic form submission when the user presses enter button
(function() {
	// get all forms in the document
	var forms = Array.from(document.getElementsByTagName("form"));
   	forms.forEach((form) => {
        var inputFields = form.querySelectorAll('input:not([type="button"]):not([type="hidden"])');
        var submitButton = form.querySelector('input[type="button"]');
        inputFields.forEach((input) => {
			// add event listener 
            input.addEventListener("keydown", (e) => {
				// if the key is enter
                if(e.keyCode == 13) {
                    // prevent default submission, needed for simple forms with one input field
                    e.preventDefault(); 
                    // generate a click event
                    let click = new Event("click");
                    submitButton.dispatchEvent(click);
                }
            });
        });
    });
})();

