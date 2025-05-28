<?php


function reserve($user_id, $reservation_global_id, $details, $conn) {
  // start a transaction for stuff
  // If there is any kind of failure during the process, everything will be reverted 
  mysqli_begin_transaction($conn);

  try {
    // make the reservation
    $sql = "INSERT INTO reservations (token_id, global_order_id, status) VALUES (" . $user_id . ",'" . $reservation_global_id . "',0)";
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
    if(!mysqli_commit($conn)){
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"008" ));
      exit;
    }
    echo json_encode(array('status' => "OK", 'response' =>array('reservation_id' => $reservation_global_id )));

  } catch (mysqli_sql_exception $exception) {
      mysqli_rollback($conn);
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"009" ));
      exit;
  }

}

function commit($user_id, $details, $conn) {
  // start a transaction for stuff
  // If there is any kind of failure during the process, everything will be reverted 
  mysqli_begin_transaction($conn);

  try {

    $orders = array();
    // transfer reservation into order
    foreach ($details as $entry) {
      
      $sql = "SELECT * FROM reservations WHERE status = 0 AND global_order_id = '" . $entry["reservation_id"] . "'";
      $result = mysqli_query($conn, $sql);
      $internal_res_id = -1;

      if (mysqli_num_rows($result) > 0) {
        $row = mysqli_fetch_assoc($result);
        $internal_res_id = $row["id"];
      } else {
        mysqli_rollback($conn);
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not find a reservation with the given global ID", 'details'=>"107" ));
        exit;
      }


      // create order
      $sql = "INSERT INTO orders(token_id, global_order_id) VALUES (" . $user_id . ", '" . $entry["reservation_id"] . "')";
      $ord_id = -1;

      if (mysqli_query($conn, $sql)) {
        $ord_id = mysqli_insert_id($conn);
        array_push($orders, array('order_id' => $entry["reservation_id"]));
      } else {
        mysqli_rollback($conn);
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Could not create order", 'details'=>"101" ));
        exit;
      }

      $sql = "SELECT * FROM reservation_tracker WHERE reservation_id = '" . $internal_res_id. "'";
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
      /* $sql = "DELETE FROM reservation_tracker WHERE reservation_id=" . $internal_res_id; */
      /**/
      /* if (!mysqli_query($conn, $sql)) { */
      /*   mysqli_rollback($conn); */
      /*   echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not delete reservation ". $entry["reservation_id"] . " before commit", 'details'=>"104" )); */
      /*   exit; */
      /* } */
      $sql = "UPDATE reservations SET status = 1 WHERE id = '" . $internal_res_id . "'";

      if (!mysqli_query($conn, $sql)) {
        mysqli_rollback($conn);
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly commit the order", 'details'=>"10A" ));
        exit;
      }
      if(mysqli_affected_rows($conn) == 0 ){
        mysqli_rollback($conn);
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly commit the order", 'details'=>"10B" ));
        exit;
      }
    }



    if(!mysqli_commit($conn)){
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"108" ));
      exit;
    }
    echo json_encode(array('status' => "OK", 'response' =>$orders));

  } catch (mysqli_sql_exception $exception) {
      mysqli_rollback($conn);
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"109" ));
      exit;
  }

}

function revert_reservation($user_id, $details, $conn) {
  // start a transaction for stuff
  // If there is any kind of failure during the process, everything will be reverted 
  mysqli_begin_transaction($conn);

  try {
    // add the items back to the stock
    foreach ($details as $entry) {
      $sql = "SELECT * FROM reservations WHERE status = 0 AND global_order_id = '" . $entry["reservation_id"] . "'";
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

      $sql = "SELECT * FROM reservation_tracker WHERE reservation_id = " . $internal_res_id;

      $detail = mysqli_query($conn, $sql);
      if (mysqli_num_rows($detail) > 0) {
        
        while($detail_row = mysqli_fetch_assoc($detail)) {
          $sql = "UPDATE products SET quantity = quantity + " . $detail_row["amount"] . " WHERE id = " . $detail_row["product_id"];

          if (!mysqli_query($conn, $sql)) {
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Product could not be restored", 'details'=>"603" ));
            exit;
          }
          if(mysqli_affected_rows($conn) == 0 ){
            mysqli_rollback($conn);
            echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Product could not be restored", 'details'=>"604" ));
            exit;
          }
        }
      } 

      // delete everything
      /* $sql = "DELETE FROM reservation_tracker WHERE reservation_id=" . $internal_res_id; */
      /**/
      /* if (!mysqli_query($conn, $sql)) { */
      /*   mysqli_rollback($conn); */
      /*   echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- reservation could not be deleted", 'details'=>"605" )); */
      /*   exit; */
      /* } */
      $sql = "UPDATE reservations SET status = 4 WHERE id = " . $internal_res_id;

      if (!mysqli_query($conn, $sql)) {
        mysqli_rollback($conn);
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly rollback the order", 'details'=>"60C" ));
        exit;
      }
      if(mysqli_affected_rows($conn) == 0 ){
        mysqli_rollback($conn);
        echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- could not properly rollback the order", 'details'=>"60D" ));
        exit;
      }
    }

    if(!mysqli_commit($conn)){
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"608" ));
      exit;
    }
    echo json_encode(array('status' => "OK" ));

  } catch (mysqli_sql_exception $exception) {
      mysqli_rollback($conn);
      echo json_encode(array('status' => "NOK", 'message' => "Something went wrong -- Failure during transaction", 'details'=>"609" ));
      exit;
  }

}
