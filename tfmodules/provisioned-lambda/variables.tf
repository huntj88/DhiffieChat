variable "function_name" {
  type = string
}

variable "http_method" {
  type = string
}

variable "content_handling" {
  type = string
  default = "CONVERT_TO_TEXT"
}

variable "role" {
  type = string
}

variable "gateway_id" {
  type = string
}

variable "gateway_root_resource_id" {
  type = string
}

variable "gateway_execution_arn" {
  type = string
}