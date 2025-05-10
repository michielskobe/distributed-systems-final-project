#!/bin/bash

echo ""
echo "Testing list_product Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/list_products

echo ""
echo "Testing list_product Route (with wrong API key):"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dz3e5d' -X POST http://localhost:8069/seller-api/list_products

echo ""
echo "Testing reserve Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/reserve -d '{"reservation_details":[{"id":"1", "amount":"2"}], "reservation_id":"lol"}'

echo ""
echo "Testing reserve Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/reserve -d '{"reservation_details":[{"id":"1", "amount":"2"}], "reservation_id":"lol5"}'

echo ""
echo "Testing reserve Route (Invalid product):"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/reserve -d '{"reservation_details":[{"id":"111111", "amount":"2"}], "reservation_id":"lol1"}'

echo ""
echo "Testing show_reserve Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/show_reserve

echo ""
echo "Testing rollback_reserve Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/rollback_reserve -d '{"rollback_details":[{"reservation_id":"lol"}]}'

echo ""
echo "Testing show_reserve Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/show_reserve

echo ""
echo "Testing commit Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/commit -d '{"commit_details":[{"reservation_id":"lol3"}, {"reservation_id":"78"}]}'

echo ""
echo "Testing commit Route 2:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/commit -d '{"commit_details":[{"reservation_id":"lol5"}]}'

echo ""
echo "Testing show_commit Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/show_commit

echo ""
echo "Testing rollback_commit Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -H 'Content-Type: application/json' -X POST http://localhost:8069/seller-api/rollback_commit -d '{"rollback_details":[{"order_id":"lol5"}]}'

echo ""
echo "Testing show_commit Route:"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/show_commit

echo ""
echo "Testing cleanup_reserve Route(Wrong API key):"
curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/cleanup_reserve

echo ""
echo "Testing cleanup_reserve Route:"
curl -H 'x-api-key: ga3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -X POST http://localhost:8069/seller-api/cleanup_reserve
