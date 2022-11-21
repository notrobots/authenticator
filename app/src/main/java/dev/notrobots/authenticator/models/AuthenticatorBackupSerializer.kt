package dev.notrobots.authenticator.models

import com.google.protobuf.ByteString
import dev.notrobots.authenticator.proto.AuthenticatorOuterClass.*
import dev.notrobots.authenticator.util.*
import dev.notrobots.authenticator.util.MutableAccountsWithTags
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm

class AuthenticatorBackupSerializer : BackupMessageSerializer() {
    override fun serialize(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>,
        maxBytes: Int
    ): List<String> {
        val backupMessage = Authenticator.Backup.newBuilder()

        for (account in accounts) {
            backupMessage.addAccounts(serializeAccount(account, accountsWithTags))
        }

        for (tag in tags) {
            backupMessage.addTags(
                Authenticator.Tag.newBuilder()
                    .setName(tag.name)
                    .build()
            )
        }

        return ProtobufUtil.serializeMessage(backupMessage.build()).chunked(maxBytes)
    }

    override fun deserialize(data: String): BackupData {
        val backupMessage = Authenticator.Backup.parseFrom(ProtobufUtil.deserializeMessage(data))
        val accountsWithTags: MutableAccountsWithTags = mutableMapOf()
        val accounts = Set(backupMessage.accountsCount) {
            val accountMessage = backupMessage.getAccounts(it)

            Account(
                accountMessage.name,
                accountMessage.secret.toString(Charsets.UTF_8)
            ).apply {
                order = accountMessage.order
                issuer = accountMessage.issuer
                label = accountMessage.label
                type = when (accountMessage.type) {
                    Authenticator.OTPType.OTP_TYPE_TOTP -> OTPType.TOTP
                    Authenticator.OTPType.OTP_TYPE_HOTP -> OTPType.HOTP

                    else -> OTPType.TOTP
                }
                counter = accountMessage.counter
                digits = accountMessage.digits
                period = accountMessage.period
                algorithm = when (accountMessage.algorithm) {
                    Authenticator.Algorithm.ALGORITHM_SHA1 -> HmacAlgorithm.SHA1
                    Authenticator.Algorithm.ALGORITHM_SHA256 -> HmacAlgorithm.SHA256
                    Authenticator.Algorithm.ALGORITHM_SHA512 -> HmacAlgorithm.SHA512

                    else -> HmacAlgorithm.SHA1
                }

                if (accountMessage.tagsCount > 0) {
                    accountsWithTags[this] = accountMessage.tagsList.toSet()   // tagList is a ProtobufArrayList which is not serializable
                }
            }
        }
        val tags = Set(backupMessage.tagsCount) {
            val tagMessage = backupMessage.getTags(it)

            Tag(tagMessage.name)
        }

        return BackupData(accounts, tags, accountsWithTags)
    }

    private fun serializeAccount(account: Account, accountsWithTags: List<AccountWithTags>): Authenticator.Account {
        val accountWithTags = accountsWithTags.find { a -> a.account == account }
        val tagsNames = accountWithTags?.tags?.map { it.name }

        return Authenticator.Account.newBuilder()
            .setName(account.name)
            .setOrder(account.order)
            .setSecret(ByteString.copyFrom(account.secret.toByteArray()))
            //                        .setSecret(encodeSecret(account.secret))
            .setIssuer(account.issuer)
            .setLabel(account.label)
            .setOTPType(account.type)
            .setCounter(account.counter)
            .setDigits(account.digits)
            .setPeriod(account.period)
            .setAlgorithm(account.algorithm)
            .apply {
                tagsNames?.let {
                    addAllTags(tagsNames)
                }
            }
            .build()
    }

    private fun Authenticator.Account.Builder.setOTPType(otpType: OTPType) = apply {
        type = when (otpType) {
            OTPType.TOTP -> Authenticator.OTPType.OTP_TYPE_TOTP
            OTPType.HOTP -> Authenticator.OTPType.OTP_TYPE_HOTP
        }
    }

    private fun Authenticator.Account.Builder.setAlgorithm(algorithm: HmacAlgorithm) = apply {
        setAlgorithm(
            when (algorithm) {
                HmacAlgorithm.SHA1 -> Authenticator.Algorithm.ALGORITHM_SHA1
                HmacAlgorithm.SHA256 -> Authenticator.Algorithm.ALGORITHM_SHA256
                HmacAlgorithm.SHA512 -> Authenticator.Algorithm.ALGORITHM_SHA512
            }
        )
    }
}