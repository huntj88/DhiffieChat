#!/bin/bash
DHIFFIE_ENV=$1
aws s3 cp ../firebaseConfig.json "s3://$DHIFFIE_ENV-dhiffiechat-config-bucket/firebaseConfig.json"
