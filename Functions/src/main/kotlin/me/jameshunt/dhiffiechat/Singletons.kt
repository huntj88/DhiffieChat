package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object Singletons {
    val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    val dynamoDB = DynamoDB(AmazonDynamoDBClientBuilder.defaultClient())
    val s3 = AmazonS3ClientBuilder.standard().build()
}