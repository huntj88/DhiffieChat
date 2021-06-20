## Generating config files for app build types
* ./generateAppProperties.sh \<ENV> <BASE_URL>
* ./generateAppProperties.sh stage https://81xwkk1nbe.execute-api.us-east-1.amazonaws.com/stage
* ./generateAppProperties.sh prod https://awesaw0atb.execute-api.us-east-1.amazonaws.com/prod/

# Upload firebase config to a specific env
* ./uploadFirebaseConfig.sh \<ENV>
* ./uploadFirebaseConfig.sh stage
* ./uploadFirebaseConfig.sh prod
