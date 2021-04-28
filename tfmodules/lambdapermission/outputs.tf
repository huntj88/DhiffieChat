output "redeploy_hash" {
  value = sha1(jsonencode([
    aws_lambda_function.lambda_func.last_modified,
    aws_lambda_permission.gw_permission.id,
    aws_api_gateway_resource.gateway_resource.id,
    aws_api_gateway_method.gateway_method.id,
    aws_api_gateway_integration.gateway_integration.id
  ]))
}
