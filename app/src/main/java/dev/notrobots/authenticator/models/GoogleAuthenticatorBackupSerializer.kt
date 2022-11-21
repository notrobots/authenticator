package dev.notrobots.authenticator.models

import com.google.protobuf.MessageLite
import dev.notrobots.authenticator.data.GOOGLE_AUTHENTICATOR_PROTO_VERSION
import dev.notrobots.authenticator.proto.GoogleAuthenticatorOuterClass.*
import dev.notrobots.authenticator.util.BackupData
import dev.notrobots.authenticator.util.ProtobufUtil
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm

class GoogleAuthenticatorBackupSerializer : BackupMessageSerializer() {
    override fun serialize(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>,
        maxBytes: Int
    ): List<String> {
        if (maxBytes > 0) {
            val chunk = accounts.map { serializeAccount(it) }

            return listOf(
                ProtobufUtil.serializeMessage(serializeBackup(chunk, 1, 1))
            )
        } else {
            val chunks = mutableSetOf<Set<MessageLite>>()
            var currentChunk = mutableSetOf<MessageLite>()
            var currentChunkSize = 0
            val clearChunk = {
                currentChunk = mutableSetOf()
                currentChunkSize = 0
            }

            for (cursor in accounts.indices) {
                val accountMessage = serializeAccount(accounts[cursor])
                val accountMessageLength = ProtobufUtil.getMessageLength(accountMessage)

                if (accountMessageLength + currentChunkSize > maxBytes) {
                    chunks.add(currentChunk)
                    clearChunk()
                }

                currentChunk.add(accountMessage)
                currentChunkSize += accountMessageLength
            }

            chunks.add(currentChunk)
            clearChunk()

            return chunks.mapIndexed { i, chunk ->
                val backupMessage = serializeBackup(chunk, i, chunks.size)

                ProtobufUtil.serializeMessage(backupMessage)
            }
        }
    }

    override fun deserialize(data: String): BackupData {
        val backupMessage = GoogleAuthenticator.Backup.parseFrom(ProtobufUtil.deserializeMessage(data))

        return BackupData(
            backupMessage.accountsList.map {
                val path = Account.parsePath(it.name)

                Account(
                    path.groupValues[2],
                    ProtobufUtil.deserializeSecret(it.secret)
                ).apply {
                    issuer = it.issuer
                    label = path.groupValues[1]
                    type = when (it.type) {
                        GoogleAuthenticator.OTPType.OTP_TYPE_TOTP -> OTPType.TOTP
                        GoogleAuthenticator.OTPType.OTP_TYPE_HOTP -> OTPType.HOTP

                        else -> OTPType.TOTP
                    }
                    counter = it.counter
                    digits = it.digits  //FIXME Google Auth only supports 8 and 6
                    algorithm = when (it.algorithm) {
                        GoogleAuthenticator.Algorithm.ALGORITHM_SHA1 -> HmacAlgorithm.SHA1

                        else -> HmacAlgorithm.SHA1
                    }
                }
            }
        )
    }

    private fun serializeBackup(chunk: Iterable<MessageLite>, chunkIndex: Int, chunkCount: Int): MessageLite {
        val chunkId = System.currentTimeMillis().toInt()
        val backupMessage = GoogleAuthenticator.Backup.newBuilder()
            .setVersion(GOOGLE_AUTHENTICATOR_PROTO_VERSION)
            .setBatchIndex(chunkIndex)
            .setBatchSize(chunkCount)
            .setBatchId(chunkId)

        for (message in chunk) {
            backupMessage.addAccounts(message as GoogleAuthenticator.Account)
        }

        return backupMessage.build()
    }

    private fun serializeAccount(account: Account): MessageLite {
        return GoogleAuthenticator.Account.newBuilder()
            .setName(account.path)
            .setSecret(ProtobufUtil.serializeSecret(account.secret))
            .setIssuer(account.issuer)
            .setOTPType(account.type)
            .setCounter(account.counter)
            .setDigits(account.digits)
            .setAlgorithm(account.algorithm)
            .build()
    }

    private fun GoogleAuthenticator.Account.Builder.setOTPType(otpType: OTPType) = apply {
        type = when (otpType) {
            OTPType.TOTP -> GoogleAuthenticator.OTPType.OTP_TYPE_TOTP
            OTPType.HOTP -> GoogleAuthenticator.OTPType.OTP_TYPE_HOTP
        }
    }

    private fun GoogleAuthenticator.Account.Builder.setAlgorithm(algorithm: HmacAlgorithm) = apply {
        this.algorithm = when (algorithm) {
            HmacAlgorithm.SHA1 -> GoogleAuthenticator.Algorithm.ALGORITHM_SHA1

            else -> GoogleAuthenticator.Algorithm.ALGORITHM_UNSPECIFIED
        }
    }
}