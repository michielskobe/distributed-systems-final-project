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
      echo json_encode($products);
    } else {
      echo json_encode("0 results");
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
        echo json_encode("Something went wrong -- CODE: 001");
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
          echo json_encode("Something went wrong -- CODE: 002");
          exit;
        }

        foreach ($details as $entry) {
          // update the quantity
          $sql = "UPDATE products SET quantity = quantity - " . $entry["amount"] . " WHERE id = " . $entry["id"] . " AND quantity >= " . $entry["amount"] . " ";

          // add products to the reservation
          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode("Something went wrong -- CODE: 003");
            exit;
          }
          if(mysqli_affected_rows($conn) == 0 ){
            mysqli_rollback($conn);
            echo json_encode("Something went wrong -- CODE: 004");
            exit;
          }
          
          
          $sql = "INSERT INTO reservation_tracker (amount, product_id, reservation_id) VALUES (" . $entry["amount"] . ", " . $entry["id"] . "," . $res_id . ")";

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode("Something went wrong -- CODE: 005");
            exit;
          }
        }
        
        /* If code reaches this point without errors then commit the data in the database */
        mysqli_commit($conn);
        echo json_encode(array('reservation_id' => $res_id ));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode("Something went wrong -- CODE: 009");
          exit;
      }
      
    } else {
      echo json_encode("Invalid reservation details");
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
      echo json_encode("Something went wrong -- CODE: 201");
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
      echo json_encode("Something went wrong -- CODE: 204");
      exit;
    }
    echo json_encode($reservations);
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
        echo json_encode("Something went wrong -- CODE: 101");
        exit;
      }

      // start a transaction for stuff
      // If there is any kind of failure during the process, everything will be reverted 
      mysqli_begin_transaction($conn);

      try {
        // create order
        $sql = "INSERT INTO orders(token_id) VALUES (" . $user_id . ")";
        $ord_id = -1;

        if (mysqli_query($conn, $sql)) {
          $ord_id = mysqli_insert_id($conn);
        } else {
          mysqli_rollback($conn);
          echo json_encode("Something went wrong -- CODE: 102");
          exit;
        }

        // transfer reservation into order
        foreach ($details as $entry) {
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
                echo json_encode("Something went wrong -- CODE: 103");
                exit;
              }
            }
          } else {
            mysqli_rollback($conn);
            echo json_encode("Something went wrong -- CODE: 104");
            exit;
          }

          // remove reservation
          // sql to delete a record
          $sql = "DELETE FROM reservation_tracker WHERE reservation_id=" . $entry["reservation_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode("Something went wrong -- CODE: 105");
            exit;
          }
          $sql = "DELETE FROM reservations WHERE id=" . $entry["reservation_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode("Something went wrong -- CODE: 106");
            exit;
          }
        }



        mysqli_commit($conn);
        echo json_encode(array('order_id' => $ord_id ));

      } catch (mysqli_sql_exception $exception) {
          mysqli_rollback($conn);
          echo json_encode("Something went wrong -- CODE: 109");
          exit;
      }
      
    } else {
      echo json_encode("Invalid commit details");
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
      echo json_encode("Something went wrong -- CODE: 201");
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
      echo json_encode("Something went wrong -- CODE: 204");
      exit;
    }
    echo json_encode($reservations);
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
