package me.jameshunt.dhiffiechat;

import com.squareup.sqldelight.android.AndroidSqliteDriver
import net.sqlcipher.database.SupportFactory

class DBQueryManager(application: DhiffieChatApp, private val prefManager: PrefManager) {
    private val db = Database(AndroidSqliteDriver(
        schema = Database.Schema,
        context = application,
        name = "dhiffiechat.db",
        factory = { SupportFactory(prefManager.getDBPassword().toByteArray()).create(it) }
    ))

    fun getAliasQueries():AliasQueries {
        return db.aliasQueries
    }

    fun getEncryptionKeyQueries():Encryption_keyQueries {
        return db.encryption_keyQueries
    }
}
