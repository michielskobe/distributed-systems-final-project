#!/bin/bash

echo ""
echo "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
echo "%%% Commit a transaction and sync                         %%%"
echo "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"

RESID=$(uuidgen)
echo $RESID

echo ""
echo "Making reservations:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/reserve -d "{\"reservation_details\":[{\"id\":\"1\", \"amount\":\"2\"}], \"reservation_id\":\"$RESID\", \"callback\":[\"https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/transaction_check\"]}"
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/reserve -d "{\"reservation_details\":[{\"id\":\"1\", \"amount\":\"2\"}], \"reservation_id\":\"$RESID\", \"callback\":[\"https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/transaction_check\"]}"

echo ""
echo "Show reservations:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/show_reserve
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/show_reserve

echo ""
echo "Commit on wankers and sync via check other supp:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/commit -d "{\"commit_details\":[{\"reservation_id\":\"$RESID\"}]}"
echo ""
curl -H 'x-api-key: ga3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/check_other_supp


echo ""
echo "Show reservations:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/show_reserve
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/show_reserve

echo ""
echo "Show commits:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/show_commit
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/show_commit

echo ""
echo ""
echo "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
echo "%%% Revert a transaction and sync                         %%%"
echo "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"

RESID=$(uuidgen)
echo $RESID

echo ""
echo "Making reservations:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/reserve -d "{\"reservation_details\":[{\"id\":\"1\", \"amount\":\"2\"}], \"reservation_id\":\"$RESID\", \"callback\":[\"https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/transaction_check\"]}"
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/reserve -d "{\"reservation_details\":[{\"id\":\"1\", \"amount\":\"2\"}], \"reservation_id\":\"$RESID\", \"callback\":[\"https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/transaction_check\"]}"

echo ""
echo "Show reservations:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/show_reserve
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/show_reserve

echo ""
echo "revert on wankers and sync via check other supp:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/rollback_reserve -d "{\"rollback_details\":[{\"reservation_id\":\"$RESID\"}]}"
echo ""
curl -H 'x-api-key: ga3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/check_other_supp


echo ""
echo "Show reservations:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/show_reserve
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/show_reserve

echo ""
echo "Show commits:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/show_commit
echo ""
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/show_commit

