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

    echo json_encode(array('status' => "OK", 'response' =>"API is alive and well!"));
};

/**
 * the list_products endpoint returns a json containing information about all products available with the seller
 */
$endpoints["list_products"] = function(array $requestData, $conn): void{
    $sql = "SELECT * FROM products";
    $result = mysqli_query($conn, $sql);
    if (mysqli_num_rows($result) > 0) {
      // output data of each row
      $products = array();
      while($row = mysqli_fetch_assoc($result)) {
        $product = array(
          "id" => $row["id"],
          "name" => $row["name"],
          "quantity" => $row["quantity"],
          "description" => $row["description"],
          "image_url" => $row["image_url"]
        );
        array_push($products, $product);
      }
      echo json_encode(array('status' => "OK", 'response' =>$products));
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- No products in store", 'details'=>"400" ));
    }
};

/**
 * the list_products endpoint returns a json containing information about all products available with the seller
 * * @param array requestData this array must contain a key `reservation_details` which has a json containing the reservation details
 */
$endpoints["reserve"] = function(array $requestData, $conn): void{
    if (isset($requestData["json"]["reservation_details"]) && isset($requestData["json"]["reservation_id"]) && isset($requestData["json"]["callback"])) {
      $details = $requestData["json"]["reservation_details"];
      $reservation_global_id = $requestData["json"]["reservation_id"];
      $callback_links = $requestData["json"]["callback"];

      // we have a json with reservation details, we need to do the following:
      // Make a reservation
      // try to link items to that reservation 

      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"000" ));
        exit;
      }

      reserve($user_id, $reservation_global_id, $details, $callback_links, $conn);
      
    } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details", 'details'=>"006" ));
    }
};

$endpoints["show_reserve"] = function(array $requestData, $conn): void{
    // get user id
    $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
    $result = mysqli_query($conn, $sql);
    $user_id = -1;

    if (mysqli_num_rows($result) > 0) {
      $row = mysqli_fetch_assoc($result);
      $user_id = $row["id"];
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"300" ));
      exit;
    }


    $reservations = array();
    $sql = "SELECT * FROM reservations WHERE status = 0 AND token_id = " . $user_id;
    $result = mysqli_query($conn, $sql);

    if (mysqli_num_rows($result) > 0) {
      // output data of each row
      while($row = mysqli_fetch_assoc($result)) {
        $res_id = $row["id"]; 
        $glob_res_id = $row["global_order_id"];
        $sql = "SELECT * FROM reservation_tracker WHERE reservation_id = " . $res_id;
        $res_detail = array();

        $detail = mysqli_query($conn, $sql);
        if (mysqli_num_rows($detail) > 0) {
          while($detail_row = mysqli_fetch_assoc($detail)) {
            $ugh = array(
              "id" => $detail_row["product_id"],
              "amount" => $detail_row["amount"]
            ); 
            array_push($res_detail, $ugh);
          }
          $temp = array($glob_res_id => $res_detail);
          array_push($reservations, $temp);
        } 
      }
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- No reservations associated with user", 'details'=>"301" ));
      exit;
    }
    echo json_encode(array('status' => "OK", 'response' =>$reservations));
};

$endpoints["commit"] = function(array $requestData, $conn): void{
    if (isset($requestData["json"]["commit_details"])) {
      $details = $requestData["json"]["commit_details"];
      
      // we have a json with reservation details, we need to do the following:
      // Make a reservation
      // try to link items to that reservation 

      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"100" ));
        exit;
      }

      commit($user_id, $details, $conn);      
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details", 'details'=>"106" ));
    }
};

$endpoints["show_commit"] = function(array $requestData, $conn): void{
    // get user id
    $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
    $result = mysqli_query($conn, $sql);
    $user_id = -1;

    if (mysqli_num_rows($result) > 0) {
      $row = mysqli_fetch_assoc($result);
      $user_id = $row["id"];
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"200" ));
      exit;
    }


    $reservations = array();
    $sql = "SELECT * FROM orders WHERE token_id = " . $user_id;
    $result = mysqli_query($conn, $sql);

    if (mysqli_num_rows($result) > 0) {
      // output data of each row
      while($row = mysqli_fetch_assoc($result)) {
        $res_id = $row["id"]; 
        $glob_res_id = $row["global_order_id"];
        $sql = "SELECT * FROM order_tracker WHERE order_id = " . $res_id;
        $res_detail = array();

        $detail = mysqli_query($conn, $sql);
        if (mysqli_num_rows($detail) > 0) {
          while($detail_row = mysqli_fetch_assoc($detail)) {
            $ugh = array(
              "id" => $detail_row["product_id"],
              "amount" => $detail_row["amount"]
            ); 
            array_push($res_detail, $ugh);
          }
          $temp = array($glob_res_id => $res_detail);
          array_push($reservations, $temp);
        } 
      }
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- No orders found for user", 'details'=>"209" ));
      exit;
    }
    echo json_encode(array('status' => "OK", 'response' =>$reservations));
};

$endpoints["rollback_commit"] = function(array $requestData, $conn): void{
    if (isset($requestData["json"]["rollback_details"])) {
      $details = $requestData["json"]["rollback_details"];
      
      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"500" ));
        exit;
      }

      // start a transaction for stuff
      // If there is any kind of failure during the process, everything will be reverted 
      mysqli_begin_transaction($conn);

      try {
        // add the items back to the stock
        foreach ($details as $entry) {

          $sql = "SELECT * FROM reservations WHERE status = 1 AND global_order_id = '" . $entry["order_id"] . "'";
          $result = mysqli_query($conn, $sql);
          $internal_res_id = -1;

          if (mysqli_num_rows($result) > 0) {
            $row = mysqli_fetch_assoc($result);
            $internal_res_id = $row["id"];
          } else {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not find a reservation with the given global ID", 'details'=>"50A" ));
            exit;
          }

          $sql = "SELECT * FROM orders WHERE global_order_id = '" . $entry["order_id"] . "'";
          $result = mysqli_query($conn, $sql);
          $internal_ord_id = -1;

          if (mysqli_num_rows($result) > 0) {
            $row = mysqli_fetch_assoc($result);
            $internal_ord_id = $row["id"];
          } else {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not find a reservation with the given global ID", 'details'=>"50B" ));
            exit;
          }

          $sql = "SELECT * FROM order_tracker WHERE id = " . $internal_ord_id;

          $detail = mysqli_query($conn, $sql);
          if (mysqli_num_rows($detail) > 0) {
            
            while($detail_row = mysqli_fetch_assoc($detail)) {
              $sql = "UPDATE products SET quantity = quantity + " . $detail_row["amount"] . " WHERE id = " . $detail_row["product_id"];

              if (!mysqli_query($conn, $sql)) {
                mysqli_rollback($conn);
                echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Product could not be restored", 'details'=>"503" ));
                exit;
              }
              if(mysqli_affected_rows($conn) == 0 ){
                mysqli_rollback($conn);
                echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Product could not be restored", 'details'=>"504" ));
                exit;
              }
            }
          } 

          // delete everything
          $sql = "DELETE FROM order_tracker WHERE order_id=" . $internal_ord_id;

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Order could not be deleted", 'details'=>"505" ));
            exit;
          }

          $sql = "DELETE FROM orders WHERE id=" . $internal_ord_id;

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Order could not be deleted", 'details'=>"506" ));
            exit;
          }

          $sql = "UPDATE reservations SET status = 2 WHERE id = " . $internal_res_id;

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly rollback the order", 'details'=>"50C" ));
            exit;
          }
          if(mysqli_affected_rows($conn) == 0 ){
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly rollback the order", 'details'=>"50D" ));
            exit;
          }
        }

        if(!mysqli_commit($conn)){
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"508" ));
          exit;
        }
        echo json_encode(array('status' => "OK" ));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"509" ));
          exit;
      }
      
    } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details provided", 'details'=>"507" ));
    }
};

$endpoints["rollback_reserve"] = function(array $requestData, $conn): void{
    if (isset($requestData["json"]["rollback_details"])) {
      $details = $requestData["json"]["rollback_details"];
      
      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"600" ));
        exit;
      }

      revert_reservation($user_id, $details, $conn);
    } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details provided", 'details'=>"608" ));
    }
};

$endpoints["cleanup_reserve"] = function(array $requestData, $conn): void{
      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
        if ($row["auth_level"] != 2) {
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not allowed to use this endpoint", 'details'=>"701" ));
          exit;
        }
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"700" ));
        exit;
      }



      // start a transaction for stuff
      // If there is any kind of failure during the process, everything will be reverted 
      mysqli_begin_transaction($conn);

      try {
          $sql = "SELECT * FROM reservations WHERE status = 0 AND created < (NOW() - INTERVAL 15 MINUTE)";

          $result = mysqli_query($conn, $sql);
          if (mysqli_num_rows($result) > 0) {
            
            while($row = mysqli_fetch_assoc($result)) {
              $sql = "SELECT * FROM reservation_tracker WHERE reservation_id = " . $row["id"];

              $detail = mysqli_query($conn, $sql);
              if (mysqli_num_rows($detail) > 0) {
                
                while($detail_row = mysqli_fetch_assoc($detail)) {
                  $sql = "UPDATE products SET quantity = quantity + " . $detail_row["amount"] . " WHERE id = " . $detail_row["product_id"];

                  if (!mysqli_query($conn, $sql)) {
                    mysqli_rollback($conn);
                    echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Product could not be restored", 'details'=>"703" ));
                    exit;
                  }
                  if(mysqli_affected_rows($conn) == 0 ){
                    mysqli_rollback($conn);
                    echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Product could not be restored", 'details'=>"704" ));
                    exit;
                  }
                }
              } 

              // delete everything
              /* $sql = "DELETE FROM reservation_tracker WHERE reservation_id=" . $row["id"]; */
              /**/
              /* if (!mysqli_query($conn, $sql)) { */
              /*   mysqli_rollback($conn); */
              /*   echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- reservation could not be deleted", 'details'=>"705" )); */
              /*   exit; */
              /* } */
              $sql = "UPDATE reservations SET status = 3 WHERE id = " . $row["id"];

              if (!mysqli_query($conn, $sql)) {
                mysqli_rollback($conn);
                echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly rollback the order", 'details'=>"70C" ));
                exit;
              }
              if(mysqli_affected_rows($conn) == 0 ){
                mysqli_rollback($conn);
                echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly rollback the order", 'details'=>"70D" ));
                exit;
              }
            }
          } 
        if(!mysqli_commit($conn)){
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"708" ));
          exit;
        }
        echo json_encode(array('status' => "OK" ));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"709" ));
          exit;
      }
};

$endpoints["transaction_check"] = function(array $requestData, $conn): void{
    if (isset($requestData["json"]["transaction_id"])) {
      $details = $requestData["json"]["transaction_id"];
      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
        if ($row["auth_level"] != 3) {
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not allowed to use this endpoint", 'details'=>"A01" ));
          exit;
        }
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"A00" ));
        exit;
      }
      
      // start a transaction for stuff
      // If there is any kind of failure during the process, everything will be reverted 
      mysqli_begin_transaction($conn);

      try {
        $status = array();
        foreach ($details as $entry) {
          $sql = "SELECT * FROM reservations WHERE global_order_id = '" . $entry . "'";

          $detail = mysqli_query($conn, $sql);
          if (mysqli_num_rows($detail) > 0) {
            $detail_row = mysqli_fetch_assoc($detail);
            array_push($status, array($entry => $detail_row["status"]));
          } else {
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Unknown global ID", 'details'=>"A02" ));
            exit;
          }
        }
        if(!mysqli_commit($conn)){
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"A08" ));
          exit;
        }
        echo json_encode(array('status' => "OK", "response" => $status));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"A09" ));
          exit;
      }
    } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details provided", 'details'=>"A08" ));
    }
};

$endpoints["check_other_supp"] = function(array $requestData, $conn): void{
      // get user id
      $sql = "SELECT * FROM authorized_tokens WHERE token = '" . $requestData["token"] . "'";
      $result = mysqli_query($conn, $sql);
      $user_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $user_id = $row["id"];
        if ($row["auth_level"] != 2) {
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not allowed to use this endpoint", 'details'=>"801" ));
          exit;
        }
      } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- User not associated with orders", 'details'=>"800" ));
        exit;
      }


      # SELECT all the transactions which are waiting
      $sql = "SELECT * FROM reservations WHERE status = 0";

      $result = mysqli_query($conn, $sql);
      if (mysqli_num_rows($result) > 0) {
        while($row = mysqli_fetch_assoc($result)) {
          $operation_made_for_order = false;
          $data = array();
          array_push($data, $row["global_order_id"]);

          // code here for grabbing all associated urls
          $sql_link = "SELECT * FROM callback_links WHERE res_id = " . $row["id"]; 
          $result_link = mysqli_query($conn, $sql_link);

          // basically, do nothing if no callbacks are provided
          if (mysqli_num_rows($result_link) > 0) {
            while($row_link = mysqli_fetch_assoc($result_link)) {
              // Only contact the minimal amount of callbacks
              // Once an operation is known, you don't have to ask others
              if ($operation_made_for_order) {
                continue;
              }

              $url = $row_link["url"];

              $auth_token = getenv('SUP_CHECK_AUTH_TOKEN');
              if (!$auth_token) {
                $auth_token = "ha3b2c9c-a96d-48a8-82ad-0cb775dd3e5d";
              }

              $send_data = array("transaction_id" => $data);

              // use key 'http' even if you send the request to https://...
              $options = [
                  'http' => [
                      'header' => array("x-api-key: ". $auth_token , 'Content-type: application/json'),
                      'method' => 'POST',
                      'content' => json_encode($send_data)
                  ],
              ];

              $context = stream_context_create($options);
              $result_call = file_get_contents($url, false, $context);
              if ($result_call === false) {
                  echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during contact with other supplier", 'details'=>"807", "order_id" => $row["id"], "result" => json_decode($result_call) ));
                  continue;
              }

              $res_arr = json_decode($result_call,true);
              if ($res_arr['status'] == "NOK"){
                  echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure at other supplier", 'details'=>"806", "order_id" => $row["id"], "result" => json_decode($result_call) ));
                  continue;
              }

              // if there is no response key
              if (!$res_arr['response']){
                  echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure at other supplier", 'details'=>"805", "order_id" => $row["id"], "result" => json_decode($result_call) ));
                  continue;
              }
              // start a transaction for stuff
              // If there is any kind of failure during the process, everything will be reverted 
              mysqli_begin_transaction($conn);

              try {
                foreach ($res_arr['response'] as $entry){
                  foreach ($entry as $entry_id => $entry_status) {
                    // We need to perform certain actions based upon the return status
                    // 0: Do nothing
                    // 1: Call the commit route for this entry_id, MARK OPERATION MADE
                    // 2: Call the revert commit route for this entry_id, MARK OPERATION MADE
                    // 3: Call the revert reservation route for this entry_id, MARK OPERATION MADE
                    // 4: Call the revert reservation route for this entry_id, MARK OPERATION MADE

                    switch ($entry_status) {
                      case 0:
                        
                        break;
                      
                      case 1:
                        $operation_made_for_order = true;
                        $details = array(array('reservation_id' => $row["global_order_id"]));
                        
                        commit($row["token_id"], $details, $conn);
                        break;

                      case 4:
                        $operation_made_for_order = true;
                        $details = array(array('reservation_id' => $row["global_order_id"]));

                        revert_reservation($row["token_id"], $details, $conn);
                        break;

                      default:
                        # code...
                        break;
                    }
                  }
                }

                if(!mysqli_commit($conn)){
                  echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"808", "order_id" => $row["id"], "result" => json_decode($result_call) ));
                  continue;
                }
                echo json_encode(array('status' => "OK", "order_id" => $row["id"], "result" => json_decode($result_call)));

              } catch (mysqli_sql_exception $exception) {
                  mysqli_rollback($conn);
                  echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"809", "order_id" => $row["id"], "result" => json_decode($result_call) ));
                  continue;
              }
            }  
          }

        }
      } else {
        #Everything is OK if there are no orders waiting
        echo json_encode(array('status' => "OK" ));
        exit;
      } 

};

/**
 * prints a default message if the endpoint path does not exist.
 * @param array $requestData contains the parameters sent in the request, 
 *                           for this endpoint they are ignored.
 * @return void
 */
$endpoints["404"] = function ($requestData, $conn): void {

  echo json_encode(array('status' => "NOK", 'message' => "Invalid endpoint", 'details'=>"1109" ));
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
        echo json_encode(array('status' => "NOK", 'message' => "The token " . $requestData["token"] . " does not exists or is not authorized to perform this operation.", 'details'=>"1008" ));
        exit;
      }
    } else {
        echo json_encode(array('status' => "NOK", 'message' => "The token is not found in the request", 'details'=>"1009" ));
        exit;
    }
};
