package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

const val configBucketName = "dhiffiechat-config-bucket"
const val encryptedFileBucket = "dhiffiechat-encrypted-file-bucket"

object Singletons {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerKotlinModule()

    val dynamoDB: DynamoDB = DynamoDB(AmazonDynamoDBClientBuilder.defaultClient())
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()
    val credentials: Credentials = Credentials()
    val firebase: FirebaseManager by lazy { FirebaseManager(credentials) }
}