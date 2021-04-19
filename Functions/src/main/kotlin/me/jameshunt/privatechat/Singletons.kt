package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object Singletons {
    val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    val dynamoDB = DynamoDB(AmazonDynamoDBClientBuilder.defaultClient())
}