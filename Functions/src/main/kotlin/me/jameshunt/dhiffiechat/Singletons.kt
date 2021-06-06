package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object Singletons {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerKotlinModule()

    val dynamoDB = DynamoDB(AmazonDynamoDBClientBuilder.defaultClient())

    val encryptedFileBucket = "dhiffiechat-encrypted-file-bucket"
    val s3 = AmazonS3ClientBuilder.standard().build()

    val firebase by lazy { FirebaseManager() }
}