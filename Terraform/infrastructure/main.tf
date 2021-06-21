terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.37.0"
    }
  }
}

provider "aws" {
  profile = "default"
  region  = "us-east-1"
}

resource "aws_s3_bucket" "encrypted_media_bucket" {
  bucket = "${terraform.workspace}-dhiffiechat-encrypted-file-bucket"
  acl    = "private"

  lifecycle_rule {
    id      = "clear_all_after_two_weeks"
    enabled = true

    expiration {
      days = 14
    }
  }

  tags = {
    Name = "Encrypted file bucket"
  }
}

resource "aws_s3_bucket" "config_bucket" {
  bucket = "${terraform.workspace}-dhiffiechat-config-bucket"
  acl    = "private"

  tags = {
    Name = "config bucket"
  }
}

resource "aws_dynamodb_table" "user-dynamodb-table" {
  name           = "${terraform.workspace}.User"
  billing_mode   = "PAY_PER_REQUEST"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "userId"

  attribute {
    name = "userId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "message-dynamodb-table" {
  name           = "${terraform.workspace}.Message"
  billing_mode   = "PAY_PER_REQUEST"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "to"
  range_key      = "messageCreatedAt"

  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  attribute {
    name = "to"
    type = "S"
  }

  attribute {
    name = "messageCreatedAt"
    type = "S"
  }
}

resource "aws_iam_role" "function_role" {
  name = "${terraform.workspace}_function_role"

  # Terraform's "jsonencode" function converts a
  # Terraform expression result to valid JSON syntax.
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "s3.amazonaws.com"
        }
      },
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      },
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "dynamodb.amazonaws.com"
        }
      },
    ]
  })
}


resource "aws_iam_policy" "function_policy" {
  name        = "${terraform.workspace}_s3_bucket_policy"
  path        = "/"
  description = "s3 bucket policy"

  # Terraform's "jsonencode" function converts a
  # Terraform expression result to valid JSON syntax.
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "s3:ListBucket",
        ]
        Effect = "Allow"
        Resource = [
          aws_s3_bucket.encrypted_media_bucket.arn,
          aws_s3_bucket.config_bucket.arn
        ]
      },
      {
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Effect = "Allow"
        Resource = [
          "${aws_s3_bucket.encrypted_media_bucket.arn}/*",
          "${aws_s3_bucket.config_bucket.arn}/*"
        ]
      },
      {
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Effect = "Allow"
        Resource = [
          "arn:aws:logs:*:*:*"
        ]
      },
      {
        Sid = "ReadWriteTable"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:PutItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
        ]
        Effect = "Allow"
        Resource = [
          aws_dynamodb_table.user-dynamodb-table.arn,
          aws_dynamodb_table.message-dynamodb-table.arn
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "function-attach" {
  role       = aws_iam_role.function_role.name
  policy_arn = aws_iam_policy.function_policy.arn
}

resource "aws_api_gateway_rest_api" "chat_gateway" {
  name        = "${terraform.workspace}_chat_gateway"
  description = "Chat Gateway"
}

module "perform_request" {
  source                   = "../modules/provisioned-lambda"
  function_name            = "PerformRequest"
  gateway_execution_arn    = aws_api_gateway_rest_api.chat_gateway.execution_arn
  gateway_id               = aws_api_gateway_rest_api.chat_gateway.id
  gateway_root_resource_id = aws_api_gateway_rest_api.chat_gateway.root_resource_id
  http_method              = "POST"
  role                     = aws_iam_role.function_role.arn
}

resource "aws_lambda_permission" "allow_bucket_event" {
  statement_id  = "AllowExecutionFromS3Bucket"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.handle_s3_upload.arn
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.encrypted_media_bucket.arn
}

resource "aws_lambda_function" "handle_s3_upload" {
  description      = "handle s3 upload"
  function_name    = "${terraform.workspace}_HandleS3Upload"
  filename         = "../../Lambda/build/distributions/Lambda-1.0-SNAPSHOT.zip"
  source_code_hash = filebase64sha256("../../Lambda/build/distributions/Lambda-1.0-SNAPSHOT.zip")
  handler          = "me.jameshunt.dhiffiechat.HandleS3Upload::handleRequest"
  role             = aws_iam_role.function_role.arn
  runtime          = "java8"
  timeout          = 30
  memory_size      = 1024
}

resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = aws_s3_bucket.encrypted_media_bucket.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.handle_s3_upload.arn
    events              = ["s3:ObjectCreated:*"]
  }

  depends_on = [aws_lambda_permission.allow_bucket_event]
}

resource "aws_api_gateway_deployment" "chat_deployment" {

  triggers = {
    redeployment = sha1(jsonencode([
      module.perform_request.redeploy_hash,
    ]))
  }

  stage_name  = terraform.workspace
  rest_api_id = aws_api_gateway_rest_api.chat_gateway.id
}

output "base_url" {
  value = aws_api_gateway_deployment.chat_deployment.invoke_url
}
