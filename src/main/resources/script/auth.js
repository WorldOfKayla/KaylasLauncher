function authorize(authCredentialsJson, POSTrequest) {
    let authCredentials = JSON.parse(authCredentialsJson);
    authCredentials['userAction'] = 'auth';
    let response = POSTrequest.send(authCredentials);
    let responseJSON = JSON.parse(response);
    return response;
}
