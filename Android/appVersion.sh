#!/bin/bash
DHIFFIE_ENV=$1
NEAREST_COMMON_ANCESTOR=$(git merge-base origin/master HEAD)
MASTER_COUNT=$(git rev-list --first-parent --count "$NEAREST_COMMON_ANCESTOR")
HEAD_COUNT=$(git rev-list --first-parent --count HEAD)
COUNT_DIFF=$((HEAD_COUNT-MASTER_COUNT))
SHORT_COMMIT_HASH=$(git rev-parse --short HEAD)

echo "${MASTER_COUNT}.${COUNT_DIFF}.${SHORT_COMMIT_HASH}.${DHIFFIE_ENV}"
