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
          "description" => $row["description"]
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
    if (isset($requestData["json"]["reservation_details"])) {
      $details = $requestData["json"]["reservation_details"];
      
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


      // start a transaction for stuff
      // If there is any kind of failure during the process, everything will be reverted 
      mysqli_begin_transaction($conn);

      try {
        // make the reservation
        $sql = "INSERT INTO reservations (token_id) VALUES (" . $user_id . ")";
        $res_id = -1;

        if (mysqli_query($conn, $sql)) {
          $res_id = mysqli_insert_id($conn);
        } else {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not make reservation", 'details'=>"001" ));
          exit;
        }

        foreach ($details as $entry) {
          // update the quantity
          $sql = "UPDATE products SET quantity = quantity - " . $entry["amount"] . " WHERE id = " . $entry["id"] . " AND quantity >= " . $entry["amount"] . " ";

          // add products to the reservation
          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not add product ". $entry["id"] ." to reservation, does the product exist?", 'details'=>"002" ));
            exit;
          }
          if(mysqli_affected_rows($conn) == 0 ){
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not add product ". $entry["id"] ." to reservation, is the product still in stock?", 'details'=>"003" ));
            exit;
          }
          
          
          $sql = "INSERT INTO reservation_tracker (amount, product_id, reservation_id) VALUES (" . $entry["amount"] . ", " . $entry["id"] . "," . $res_id . ")";

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not add product ". $entry["id"] ." to reservation", 'details'=>"004" ));
            exit;
          }
        }
        
        /* If code reaches this point without errors then commit the data in the database */
        mysqli_commit($conn);
        echo json_encode(array('status' => "OK", 'response' =>array('reservation_id' => $res_id )));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"009" ));
          exit;
      }
      
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
    $sql = "SELECT * FROM reservations WHERE token_id = " . $user_id;
    $result = mysqli_query($conn, $sql);

    if (mysqli_num_rows($result) > 0) {
      // output data of each row
      while($row = mysqli_fetch_assoc($result)) {
        $res_id = $row["id"]; 
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
          $temp = array($res_id => $res_detail);
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

      // start a transaction for stuff
      // If there is any kind of failure during the process, everything will be reverted 
      mysqli_begin_transaction($conn);

      try {

        $orders = array();
        // transfer reservation into order
        foreach ($details as $entry) {
          // create order
          $sql = "INSERT INTO orders(token_id) VALUES (" . $user_id . ")";
          $ord_id = -1;

          if (mysqli_query($conn, $sql)) {
            $ord_id = mysqli_insert_id($conn);
            array_push($orders, array('order_id' => $ord_id));
          } else {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not create order", 'details'=>"101" ));
            exit;
          }

          $sql = "SELECT * FROM reservation_tracker WHERE reservation_id = " . $entry["reservation_id"];
          $result = mysqli_query($conn, $sql);

          if (mysqli_num_rows($result) > 0) {
            // output data of each row
            while($row = mysqli_fetch_assoc($result)) {
                           
              $sql = "INSERT INTO order_tracker (amount, product_id, order_id) VALUES (" . $row["amount"] . ", " . $row["product_id"] . ", " . $ord_id . ")";
              $res_id = -1;

              if (mysqli_query($conn, $sql)) {
                $res_id = mysqli_insert_id($conn);
              } else {
                mysqli_rollback($conn);
                echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not tranfer product ". $row["product_id"] . " to commit", 'details'=>"102" ));
                exit;
              }
            }
          } else {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not tranfer reservation ". $entry["reservation_id"] . " to commit", 'details'=>"103" ));
            exit;
          }

          // remove reservation
          // sql to delete a record
          $sql = "DELETE FROM reservation_tracker WHERE reservation_id=" . $entry["reservation_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not delete reservation ". $entry["reservation_id"] . " before commit", 'details'=>"104" ));
            exit;
          }
          $sql = "DELETE FROM reservations WHERE id=" . $entry["reservation_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not delete reservation ". $entry["reservation_id"] . " before commit", 'details'=>"105" ));
            exit;
          }
        }



        mysqli_commit($conn);
        echo json_encode(array('status' => "OK", 'response' =>$orders));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"109" ));
          exit;
      }
      
    } else {
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details", 'details'=>"108" ));
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
          $temp = array($res_id => $res_detail);
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
          $sql = "SELECT * FROM order_tracker WHERE order_id = " . $entry["order_id"];

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
          $sql = "DELETE FROM order_tracker WHERE order_id=" . $entry["order_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Order could not be deleted", 'details'=>"505" ));
            exit;
          }
          $sql = "DELETE FROM orders WHERE id=" . $entry["order_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Order could not be deleted", 'details'=>"506" ));
            exit;
          }
        }

        mysqli_commit($conn);
        echo json_encode(array('status' => "OK" ));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"509" ));
          exit;
      }
      
    } else {
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Invalid details provided", 'details'=>"508" ));
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
