package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

const val configBucketSuffix = "dhiffiechat-config-bucket"
const val encryptedFileBucketSuffix = "dhiffiechat-encrypted-file-bucket"
val environment = System.getenv("DHIFFIE_ENVIRONMENT")
val configBucket = "${environment}-$configBucketSuffix"
val encryptedFileBucket = "${environment}-$encryptedFileBucketSuffix"

object Singletons {
    val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerKotlinModule()

    val dynamoDB: DynamoDB = DynamoDB(AmazonDynamoDBClientBuilder.defaultClient())
    val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()
    val credentials: Credentials = Credentials(s3)
    val firebase: FirebaseManager by lazy { FirebaseManager(credentials) }
}

fun DynamoDB.messageTable(): Table = this.getTable("${environment}.Message")
fun DynamoDB.userTable(): Table = this.getTable("${environment}.User")