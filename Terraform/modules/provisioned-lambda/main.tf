resource "aws_lambda_function" "lambda_func" {
  description = var.function_name
  function_name = "${terraform.workspace}_${var.function_name}"
  filename = "../../Functions/build/distributions/Functions-1.0-SNAPSHOT.zip"
  source_code_hash = filebase64sha256("../../Functions/build/distributions/Functions-1.0-SNAPSHOT.zip")
  handler = "me.jameshunt.dhiffiechat.${var.function_name}::handleRequest"
  role = var.role
  runtime = "java8"
  timeout = 30
  memory_size = 1024
}

resource "aws_api_gateway_resource" "gateway_resource" {
  rest_api_id = var.gateway_id
  parent_id   = var.gateway_root_resource_id
  path_part   = var.function_name
}

resource "aws_api_gateway_method" "gateway_method" {
  rest_api_id   = var.gateway_id
  resource_id   = aws_api_gateway_resource.gateway_resource.id
  http_method   = var.http_method
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "gateway_integration" {
  rest_api_id = var.gateway_id
  resource_id = aws_api_gateway_method.gateway_method.resource_id
  http_method = aws_api_gateway_method.gateway_method.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  content_handling        = var.content_handling
  uri                     = aws_lambda_function.lambda_func.invoke_arn
}

resource "aws_lambda_permission" "gw_permission" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.lambda_func.function_name
  principal     = "apigateway.amazonaws.com"

  # The "/*/*" portion grants access from any method on any resource
  # within the API Gateway REST API.
  source_arn = "${var.gateway_execution_arn}/*/*"
}
