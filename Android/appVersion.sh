DHIFFIE_ENV=$1
MASTER_COUNT=$(git rev-list --first-parent --count origin/master)
HEAD_COUNT=$(git rev-list --first-parent --count HEAD)
COUNT_DIFF=$((HEAD_COUNT-MASTER_COUNT))
BRANCH_NAME=$(git branch --show-current)

echo "${MASTER_COUNT}.${COUNT_DIFF}.${BRANCH_NAME}.${DHIFFIE_ENV}"