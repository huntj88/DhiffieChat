#!/bin/bash
DHIFFIE_ENV=$1

# cd to path of this file
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1 ; pwd -P )
cd "$parent_path" || exit 1

aws s3 cp ../firebaseConfig.json "s3://$DHIFFIE_ENV-dhiffiechat-config-bucket/firebaseConfig.json"
