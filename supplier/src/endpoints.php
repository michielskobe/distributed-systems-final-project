<?php
// closures to define each endpoint logic, 
// I know, this can be improved with some OOP but this is a basic example, 
// don't do this at home, well, or if you want to do it, don't feel judged.

/**
 * prints a default message if the API base path is queried.
 * @param array $requestData contains the parameters sent in the request, for this endpoint they are ignored.
 * @return void
 */
$endpoints["/"] = function (array $requestData, $conn): void {

    echo json_encode("Welcome to my API!");
};

/**
 * prints a greeting message with the name specified in the $requestData["name"] item.
 * if the variable is empty a default name is used.
 * @param array $requestData this array must contain an item with key "name" 
 *                           if you want to display a custom name in the greeting.
 * @return void
 */
$endpoints["sayhello"] = function (array $requestData, $conn): void {

    if (!isset($requestData["name"])) {
        $requestData["name"] = "Misterious masked individual";
    }

    echo json_encode("hello! " . $requestData["name"]);
};

/**
 * prints a default message if the endpoint path does not exist.
 * @param array $requestData contains the parameters sent in the request, 
 *                           for this endpoint they are ignored.
 * @return void
 */
$endpoints["404"] = function ($requestData, $conn): void {

    echo json_encode("Endpoint " . $requestData["endpointName"] . " not found.");
};

/**
 * checks if the token is valid, and prevents the execution of 
 * the requested endpoint.
 * @param array $requestData contains the parameters sent in the request, 
 *                           for this endpoint is required an item with 
 *                           key "token" that contains the token
 *                           received to authenticate and authorize 
 *                           the request.
 * @return void
 */
$endpoints["checktoken"] = function ($requestData, $conn): void {
    if (isset($requestData["token"])) {

      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        write_log("Valid token of user " . $row['associated_entity_name']);
      } else {
        echo json_encode("The token " . $requestData["token"] . " does not exists or is not authorized to perform this operation.");
        exit;
      }
    } else {
        echo json_encode("No token was received to authorize the operation. Verify the information sent");
        exit;
    }
};
