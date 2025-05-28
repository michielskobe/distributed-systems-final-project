<?php
header("Content-Type: application/json; charset=UTF-8");

include "../settings.php";
include '../logging.php';
include "util.php";

write_log("User request from IP: ".$_SERVER['REMOTE_ADDR']);

$BASE_URI = getenv('SUP_BASEURI');
if (!$BASE_URI) {
  $BASE_URI = "/seller-api/";
}

$endpoints = array();
$requestData = array();

require_once("endpoints.php");

/*
 * API data handler
 */
switch ($_SERVER['REQUEST_METHOD']) {
    case 'POST':
        $requestData = $_POST;
        break;
    case 'GET':
        $requestData = $_GET;
        break;
    case 'DELETE':
        $requestData = $_DELETE;
        break;
    case 'PUT':
    case 'PATCH':
        parse_str(file_get_contents('php://input'), $requestData);

        //if the information received cannot be interpreted as an arrangement it is ignored.
        if (!is_array($requestData)) {
            $requestData = array();
        }

        break;
    default:
        //TODO: implement here any other type of request method that may arise.
        break;
}

$json_params = file_get_contents("php://input");
$requestData["json"] = json_decode($json_params, true);

/*
 * API security
 */

//If the token is sent in a header X-API-KEY
if (isset($_SERVER["HTTP_X_API_KEY"])) {
    $requestData["token"] = $_SERVER["HTTP_X_API_KEY"];
}


/*
 * API Router
 */
$parsedURI = parse_url($_SERVER["REQUEST_URI"]);
$endpointName = str_replace($BASE_URI, "", $parsedURI["path"]);

if (empty($endpointName)) {
    $endpointName = "/";
}
// main router 
if (isset($endpoints[$endpointName])) {
    // Received token validation.
    $endpoints["checktoken"]($requestData, $conn);
    // run the request if succesfull
    $endpoints[$endpointName]($requestData, $conn);
} else {
		write_log("No endpoint found for " . $endpointName);
    $endpoints["404"](array("endpointName" => $endpointName), $conn);
}

