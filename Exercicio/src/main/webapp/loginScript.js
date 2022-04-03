function loginForm() {
	xmlhttp = new XMLHttpRequest();
	
	if (xmlhttp) {

		var myObjRegister;
		var myJSONRegister;

		loginUsername = document.getElementById("username").value;
		loginPassword = document.getElementById("password").value;
		
		myObjRegister = { "username": loginUsername, "password": loginPassword };
		myJSONRegister = JSON.stringify(myObjRegister);

		xmlhttp.open("POST", "./rest/users/login", true);
		xmlhttp.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
		xmlhttp.send(myJSONRegister);
		
		xmlhttp.onreadystatechange = function() {
			if(xmlhttp.readyState == 4 && xmlhttp.status == 200) {
				window.location.assign('/loggedIn.html');
			}
		}
	}
}