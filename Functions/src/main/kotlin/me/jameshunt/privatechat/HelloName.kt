package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler


data class Name(var name: String = "")
data class Response(var message: String = "")

class HelloName : RequestHandler<Name, Response> {
    override fun handleRequest(name: Name, context: Context): Response {
        val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
        val test = DynamoDB(defaultClient)
            .getTable("User")
            .getItem(PrimaryKey("HashedIdentity", "wow"))
            .asMap()

        return Response("Hello $test")
    }
}