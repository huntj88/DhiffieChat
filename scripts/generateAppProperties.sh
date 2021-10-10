#!/bin/bash
DHIFFIE_ENV=$1
BASE_URL=$2

# cd to path of this file
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1 ; pwd -P )
cd "$parent_path" || exit 1

echo "# GENERATED FILE (scripts/generateAppProperties.sh)
BASE_URL=$BASE_URL" > "../Android/${DHIFFIE_ENV}Dhiffie.properties"
