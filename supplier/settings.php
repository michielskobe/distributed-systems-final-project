<?php
// try to get settings from environment (for opsec), otherwise use dev settings.
// IMPORTANT: Never use the standard settings in production, if that was not obvious already
$servername = getenv('SUP_SERVERNAME');
if (!$servername) {
  $servername = "localhost";
}
$username = getenv('SUP_USERNAME');
if (!$username) {
  $username = "root";
}
$password = getenv('SUP_PASSWORD');
if (!$password) {
  $password = "Nalu123!";
}
$dbname = getenv('SUP_DBNAME');
if (!$dbname) {
  $dbname = "dapp";
}


// Create connection
try {
  $conn = mysqli_connect($servername, $username, $password, $dbname);
  // Check connection
  if (!$conn) {
    die(json_encode(array('status' => "NOK", 'message' =>"Connection failed: " . mysqli_connect_error(), 'details'=>"1201")));
  }
} catch (Exception $e) {
  echo json_encode(array('status' => "NOK", 'message' =>"DB Connection failed", 'details'=>"1202"));
  exit;
}

