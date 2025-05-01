<?php
$servername = "localhost";
$username = "root";
$password = "Nalu123!";
$dbname = "dapp";
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

