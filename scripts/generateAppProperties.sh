#!/bin/bash
DHIFFIE_ENV=$1
BASE_URL=$2

# cd to path of this file
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1 ; pwd -P )
cd "$parent_path" || exit 1

# service generates its own credentials on first launch
# retry in case Terraform just applied changes
curl --retry 8 --retry-connrefused --retry-delay 5 -X POST "$BASE_URL/PerformRequest?type=Init" &&

# copy public key back for provisioning app
aws s3 cp "s3://$DHIFFIE_ENV-dhiffiechat-config-bucket/serverKeyPair.public" serverKeyPair.public

echo "# GENERATED FILE (scripts/generateAppProperties.sh)
BASE_URL=$BASE_URL
SERVER_PUBLIC_KEY=$(cat serverKeyPair.public)" > "../Android/${DHIFFIE_ENV}Dhiffie.properties"

rm serverKeyPair.public
