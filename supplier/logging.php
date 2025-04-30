<?php

function write_log($log_msg){
    
  file_put_contents('../log_'.date("j.n.Y").'.log',date("Y-m-d H:i:s"). " == " . $log_msg.PHP_EOL, FILE_APPEND);
}
