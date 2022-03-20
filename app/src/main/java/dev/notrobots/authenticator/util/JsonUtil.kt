package dev.notrobots.authenticator.util

import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.json.JSONArray
import org.json.JSONObject

object JsonUtil {
    fun serialize(accounts: List<Account>): JSONObject {
        val json = JSONObject()
        val accountObjects = accounts.map {
            serializeAccount(it)
        }

        json.put("accounts", JSONArray(accountObjects))

        return json
    }

    fun deserializeAccounts(json: JSONObject): List<Account> {
        require(json.has("accounts")) {
            "JSON object is missing the 'accounts' field"
        }

        val accountArray = json.getJSONArray("accounts")

        return List(accountArray.length()) {
            deserializeAccount(accountArray.getJSONObject(it))
        }
    }

//    fun deserializeTags(json: JSONObject): List<Tag> {
//
//    }

    fun serializeAccount(account: Account): JSONObject {
        return JSONObject().apply {
            put("name", account.name)
            put("secret", account.secret)
            put("issuer", account.issuer)
            put("label", account.label)
            put("type", account.type.toString())
            put("counter", account.counter)
            put("digits", account.digits)
            put("period", account.period)
            put("algorithm", account.algorithm.toString())
        }
    }

    fun deserializeAccount(json: JSONObject): Account {
        return Account(
            json.getString("name"),
            json.getString("secret")
        ).apply {
            issuer = json.getString("issuer")
            label = json.getString("label")
            type = parseEnum<OTPType>(json.getString("type"))!!
            counter = json.getLong("counter")
            digits = json.getInt("digits")
            period = json.getLong("period")
            algorithm = parseEnum<HmacAlgorithm>(json.getString("algorithm"))!!
        }
    }

    fun deserializeAccount(json: String): Account {
        return deserializeAccount(JSONObject(json))
    }
}