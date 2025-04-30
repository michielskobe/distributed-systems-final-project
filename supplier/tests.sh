#!/bin/bash

curl -H 'x-api-key: fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d' -d "name=john" -X POST http://localhost:8069/seller-api/sayhello
