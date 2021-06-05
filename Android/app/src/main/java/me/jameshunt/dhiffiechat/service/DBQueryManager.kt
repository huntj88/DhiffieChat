package me.jameshunt.dhiffiechat.service;

import com.squareup.sqldelight.android.AndroidSqliteDriver
import me.jameshunt.dhiffiechat.AliasQueries
import me.jameshunt.dhiffiechat.Database
import me.jameshunt.dhiffiechat.DhiffieChatApp
import me.jameshunt.dhiffiechat.Encryption_keyQueries
import net.sqlcipher.database.SupportFactory

class DBQueryManager(application: DhiffieChatApp, private val prefManager: PrefManager) {
    private val db = Database(AndroidSqliteDriver(
        schema = Database.Schema,
        context = application,
        name = "dhiffiechat.db",
        factory = { SupportFactory(prefManager.getDBPassword().toByteArray()).create(it) }
    ))

    fun getAliasQueries(): AliasQueries {
        return db.aliasQueries
    }

    fun getEncryptionKeyQueries(): Encryption_keyQueries {
        return db.encryption_keyQueries
    }
}
