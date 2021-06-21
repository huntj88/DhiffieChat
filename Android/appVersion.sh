#!/bin/bash
DHIFFIE_ENV=$1
NEAREST_COMMON_ANCESTOR=$(git merge-base origin/master HEAD)
MASTER_COUNT=$(git rev-list --first-parent --count "$NEAREST_COMMON_ANCESTOR")
HEAD_COUNT=$(git rev-list --first-parent --count HEAD)
COUNT_DIFF=$((HEAD_COUNT-MASTER_COUNT))
BRANCH_NAME=$(git branch --show-current)

# If not on a branch, use short commit hash
if [[ "$BRANCH_NAME" == "" ]]; then
  BRANCH_NAME=$(git rev-parse --short HEAD)
fi

echo "${MASTER_COUNT}.${COUNT_DIFF}.${BRANCH_NAME}.${DHIFFIE_ENV}"
