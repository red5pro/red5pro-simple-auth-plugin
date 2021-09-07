/*
Copyright © 2015 Infrared5, Inc. All rights reserved.

The accompanying code comprising examples for use solely in conjunction with Red5 Pro (the "Example Code") 
is  licensed  to  you  by  Infrared5  Inc.  in  consideration  of  your  agreement  to  the  following  
license terms  and  conditions.  Access,  use,  modification,  or  redistribution  of  the  accompanying  
code  constitutes your acceptance of the following license terms and conditions.

Permission is hereby granted, free of charge, to you to use the Example Code and associated documentation 
files (collectively, the "Software") without restriction, including without limitation the rights to use, 
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit 
persons to whom the Software is furnished to do so, subject to the following conditions:

The Software shall be used solely in conjunction with Red5 Pro. Red5 Pro is licensed under a separate end 
user  license  agreement  (the  "EULA"),  which  must  be  executed  with  Infrared5,  Inc.   
An  example  of  the EULA can be found on our website at: https://account.red5pro.com/assets/LICENSE.txt.

The above copyright notice and this license shall be included in all copies or portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,  INCLUDING  BUT  
NOT  LIMITED  TO  THE  WARRANTIES  OF  MERCHANTABILITY, FITNESS  FOR  A  PARTICULAR  PURPOSE  AND  
NONINFRINGEMENT.   IN  NO  EVENT  SHALL INFRARED5, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
WHETHER IN  AN  ACTION  OF  CONTRACT,  TORT  OR  OTHERWISE,  ARISING  FROM,  OUT  OF  OR  IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
var express = require('express');
var bodyParser = require('body-parser');
var fs = require('fs');
var net = require('net');
var app = express();
const http = require('http').Server(app);


/*
*   BEGINNING OF CONFIGURATION
*
*/
/*
*   HOST
*
*/
var host = "localhost";
/*
*   PORT
*
*/
var port = 3000;
/*
*   Optional url resource for authenticated client
*
*/
var optionalURLResource = ""; // optional
/*
*   END OF CONFIGURATION
*
*/



app.use(bodyParser.urlencoded({
	extended: true
}));
app.use(bodyParser.json());


var useToken = false;

app.get('/', (req, res) => res.sendFile(__dirname + '/index.html'));

// get POST to log In
app.post('/logIn', function (request, response) {

	console.log(request.body.username);
	console.log(request.body.password);

	response.send(JSON.stringify({ "username": "streamUsername", "password": "streamPassword", "streamID": "streamID" }));

});


// get POST to validate the credentials for a stream
app.post('/validateCredentials', function (request, response) {

	console.log('\n\nvalidate credentials called');
	console.log('type: ' + request.body.type);
	console.log('username: ' + request.body.username);
	console.log('password: ' + request.body.password);
	console.log('streamID: ' + request.body.streamID);
	if (useToken)
		console.log('token: ' + request.body.token);

	var type = request.body.type;

	if (type == "publisher" || type == "websocket")
		response.send(JSON.stringify({ "result": true, "url": optionalURLResource }));
	else if (type == "subscriber")
		response.send(JSON.stringify({ "result": true }));
	else {
		console.log('invalid type supplied');
		response.send(JSON.stringify({ "result": false }));
	}
});

// get POST to invalidate the credentials for a stream
app.post('/invalidateCredentials', function (request, response) {

	console.log('\n\ninvalidate credentials called');
	console.log('type: ' + request.body.type);
	console.log('username: ' + request.body.username);
	console.log('password: ' + request.body.password);
	console.log('streamID: ' + request.body.streamID);
	if (useToken)
		console.log('token: ' + request.body.token);

	response.send(JSON.stringify({ "result": true }));

});


app.listen(port, host);

