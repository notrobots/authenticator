package dev.notrobots.authenticator.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(primaryKeys = ["name", "issuer"])
class Account(
    /**
     * Name associated with this account
     */
    val name: String,
    /**
     * Account issuer, should be the company's website
     */
    val issuer: String,
    /**
     * Additional naming for the account, should be the company's name
     */
    val label: String,
    /**
     * Account secret
     */
    val secret: String,
    /**
     * OTP type
     */
    val type: OTPType,
    /**
     * String used to restore this account
     */
    val backup: String
)