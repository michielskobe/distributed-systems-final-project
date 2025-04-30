#!/bin/bash


echo "Testing example Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -d "name=john" -X POST http://localhost:8069/seller-api/sayhello

echo ""
echo "Testing list_product Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/list_products

echo ""
echo "Testing list_product Route (with wrong API key):"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dz3e5d' -X POST http://localhost:8069/seller-api/list_products

echo ""
echo "Testing reserve Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/reserve -d '{"reservation_details":[{"id":"1", "amount":"2"}]}'

echo ""
echo "Testing reserve Route (Invalid product):"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/reserve -d '{"reservation_details":[{"id":"1", "amount":"2"}, {"id":"200000", "amount":"5"}]}'
