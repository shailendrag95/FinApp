package com.android.skg.finapp.data.repository

import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import android.util.Base64
import com.android.skg.finapp.domain.model.AccountType
import com.android.skg.finapp.domain.model.CardNetwork
import com.android.skg.finapp.domain.model.CreditCard
import com.android.skg.finapp.domain.model.BankAccount
import com.android.skg.finapp.domain.model.Transaction
import com.android.skg.finapp.domain.model.TransactionCategory

class ExportRepository(
    private val creditCardRepository: CreditCardRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun buildExportJson(): JSONObject {
        val cards = creditCardRepository.getAllCards()
        val accounts = bankAccountRepository.getAllAccounts()
        val transactions = transactionRepository.getAll()

        return JSONObject().apply {
            put("version", EXPORT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("creditCards", JSONArray(cards.map { it.toJson() }))
            put("bankAccounts", JSONArray(accounts.map { it.toJson() }))
            put("transactions", JSONArray(transactions.map { it.toJson() }))
        }
    }

    suspend fun exportEncryptedJson(password: String): String {
        val payload = buildExportJson().toString()
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        return JSONObject().apply {
            put("version", EXPORT_VERSION)
            put("encrypted", true)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }.toString()
    }

    suspend fun exportCsv(): String {
        val cards = creditCardRepository.getAllCards()
        val accounts = bankAccountRepository.getAllAccounts()
        val transactions = transactionRepository.getAll()
        val sb = StringBuilder()
        sb.appendLine("type,nickname,number,holder,extra1,extra2,extra3,notes")
        cards.forEach { card ->
            sb.appendLine(
                listOf(
                    "card",
                    csvEscape(card.nickname),
                    csvEscape(card.cardNumber),
                    csvEscape(card.holderName),
                    csvEscape(card.network.displayName),
                    csvEscape(card.bank),
                    csvEscape(card.expiry),
                    csvEscape(card.notes.orEmpty()),
                ).joinToString(","),
            )
        }
        accounts.forEach { account ->
            sb.appendLine(
                listOf(
                    "bank",
                    csvEscape(account.nickname),
                    csvEscape(account.accountNumber),
                    csvEscape(account.holderName),
                    csvEscape(account.bankName),
                    csvEscape(account.ifscOrSwift),
                    csvEscape(account.accountType.displayName),
                    csvEscape(account.notes.orEmpty()),
                ).joinToString(","),
            )
        }
        sb.appendLine("type,cardNickname,amount,date,merchant,category,notes")
        val cardNicknames = cards.associate { it.id to it.nickname }
        transactions.forEach { tx ->
            sb.appendLine(
                listOf(
                    "transaction",
                    csvEscape(cardNicknames[tx.cardId].orEmpty()),
                    tx.amount.toString(),
                    tx.date.toString(),
                    csvEscape(tx.merchant),
                    csvEscape(tx.category?.displayName.orEmpty()),
                    csvEscape(tx.notes.orEmpty()),
                ).joinToString(","),
            )
        }
        return sb.toString()
    }

    suspend fun parseEncryptedImport(content: String, password: String): JSONObject {
        val wrapper = JSONObject(content)
        val salt = Base64.decode(wrapper.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(wrapper.getString("iv"), Base64.NO_WRAP)
        val data = Base64.decode(wrapper.getString("data"), Base64.NO_WRAP)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, javax.crypto.spec.GCMParameterSpec(128, iv))
        val decrypted = String(cipher.doFinal(data), Charsets.UTF_8)
        return JSONObject(decrypted)
    }

    suspend fun importData(json: JSONObject, replaceExisting: Boolean) {
        if (replaceExisting) {
            transactionRepository.deleteAll()
            creditCardRepository.deleteAll()
            bankAccountRepository.deleteAll()
        }

        val cardIdMap = mutableMapOf<Long, Long>()
        val cardsArray = json.optJSONArray("creditCards") ?: JSONArray()
        for (i in 0 until cardsArray.length()) {
            val obj = cardsArray.getJSONObject(i)
            val oldId = obj.getLong("id")
            val card = CreditCard(
                nickname = obj.getString("nickname"),
                cardNumber = obj.getString("cardNumber"),
                holderName = obj.getString("holderName"),
                expiry = obj.getString("expiry"),
                cvv = obj.getString("cvv"),
                network = CardNetwork.valueOf(obj.getString("network")),
                bank = obj.getString("bank"),
                dueDateDay = obj.optInt("dueDateDay").takeIf { obj.has("dueDateDay") && !obj.isNull("dueDateDay") },
                creditLimit = obj.optDouble("creditLimit").takeIf { obj.has("creditLimit") && !obj.isNull("creditLimit") },
                notes = obj.optString("notes").takeIf { it.isNotBlank() },
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            val newId = creditCardRepository.insert(card)
            cardIdMap[oldId] = newId
        }

        val accountsArray = json.optJSONArray("bankAccounts") ?: JSONArray()
        for (i in 0 until accountsArray.length()) {
            val obj = accountsArray.getJSONObject(i)
            val account = BankAccount(
                nickname = obj.getString("nickname"),
                bankName = obj.getString("bankName"),
                accountNumber = obj.getString("accountNumber"),
                holderName = obj.getString("holderName"),
                ifscOrSwift = obj.getString("ifscOrSwift"),
                accountType = AccountType.valueOf(obj.getString("accountType")),
                notes = obj.optString("notes").takeIf { it.isNotBlank() },
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
            bankAccountRepository.insert(account)
        }

        val txArray = json.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            val oldCardId = obj.getLong("cardId")
            val newCardId = cardIdMap[oldCardId] ?: continue
            val categoryName = obj.optString("category").takeIf { it.isNotBlank() }
            transactionRepository.insert(
                Transaction(
                    cardId = newCardId,
                    amount = obj.getDouble("amount"),
                    date = obj.getLong("date"),
                    merchant = obj.getString("merchant"),
                    category = categoryName?.let { TransactionCategory.valueOf(it) },
                    notes = obj.optString("notes").takeIf { it.isNotBlank() },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                ),
            )
        }
    }

    suspend fun importCsv(content: String) {
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) {
            error("CSV file is empty or invalid")
        }

        val cardNicknameToId = mutableMapOf<String, Long>()
        var currentSection = ""

        for (i in 1 until lines.size) {
            val line = lines[i]
            val values = parseCsvLine(line)
            if (values.isEmpty()) continue

            val type = values.getOrNull(0)?.lowercase() ?: continue

            when (type) {
                "card" -> {
                    if (values.size < 8) continue
                    val card = CreditCard(
                        nickname = csvUnescape(values[1]),
                        cardNumber = csvUnescape(values[2]),
                        holderName = csvUnescape(values[3]),
                        expiry = csvUnescape(values[6]),
                        cvv = "000",
                        network = try { CardNetwork.valueOf(csvUnescape(values[4]).uppercase().replace(" ", "")) } catch (e: Exception) { CardNetwork.OTHER },
                        bank = csvUnescape(values[5]),
                        notes = csvUnescape(values[7]).takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis(),
                    )
                    val newId = creditCardRepository.insert(card)
                    cardNicknameToId[csvUnescape(values[1])] = newId
                }
                "bank" -> {
                    if (values.size < 8) continue
                    val account = BankAccount(
                        nickname = csvUnescape(values[1]),
                        accountNumber = csvUnescape(values[2]),
                        holderName = csvUnescape(values[3]),
                        bankName = csvUnescape(values[4]),
                        ifscOrSwift = csvUnescape(values[5]),
                        accountType = try { AccountType.valueOf(csvUnescape(values[6]).uppercase()) } catch (e: Exception) { AccountType.SAVINGS },
                        notes = csvUnescape(values[7]).takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis(),
                    )
                    bankAccountRepository.insert(account)
                }
                "transaction" -> {
                    if (values.size < 7) continue
                    val cardNickname = csvUnescape(values[1])
                    val cardId = cardNicknameToId[cardNickname] ?: continue
                    val category = csvUnescape(values[5]).takeIf { it.isNotBlank() }
                    transactionRepository.insert(
                        Transaction(
                            cardId = cardId,
                            amount = values[2].toDoubleOrNull() ?: 0.0,
                            date = values[3].toLongOrNull() ?: System.currentTimeMillis(),
                            merchant = csvUnescape(values[4]),
                            category = category?.let { try { TransactionCategory.valueOf(it.uppercase()) } catch (e: Exception) { null } },
                            notes = csvUnescape(values[6]).takeIf { it.isNotBlank() },
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val c = line[i]
            when {
                c == '"' -> {
                    inQuotes = !inQuotes
                    current.append(c)
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun csvUnescape(value: String): String {
        return value.trim().removeSurrounding("\"").replace("\"\"", "\"")
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun CreditCard.toJson() = JSONObject().apply {
        put("id", id)
        put("nickname", nickname)
        put("cardNumber", cardNumber)
        put("holderName", holderName)
        put("expiry", expiry)
        put("cvv", cvv)
        put("network", network.name)
        put("bank", bank)
        put("dueDateDay", dueDateDay)
        put("creditLimit", creditLimit)
        put("notes", notes)
        put("createdAt", createdAt)
    }

    private fun BankAccount.toJson() = JSONObject().apply {
        put("id", id)
        put("nickname", nickname)
        put("bankName", bankName)
        put("accountNumber", accountNumber)
        put("holderName", holderName)
        put("ifscOrSwift", ifscOrSwift)
        put("accountType", accountType.name)
        put("notes", notes)
        put("createdAt", createdAt)
    }

    private fun Transaction.toJson() = JSONObject().apply {
        put("id", id)
        put("cardId", cardId)
        put("amount", amount)
        put("date", date)
        put("merchant", merchant)
        put("category", category?.name)
        put("notes", notes)
        put("createdAt", createdAt)
    }

    companion object {
        const val EXPORT_VERSION = 1
        private const val PBKDF2_ITERATIONS = 100_000
    }

    suspend fun buildSampleCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("type,nickname,number,holder,extra1,extra2,extra3,notes")

        // Sample card
        sb.appendLine(
            listOf(
                "card",
                csvEscape("Personal Visa"),
                csvEscape("4111111111111111"),
                csvEscape("Jane Doe"),
                csvEscape("Visa"),
                csvEscape("Sample Bank"),
                csvEscape("1226"),
                csvEscape("Sample card for import"),
            ).joinToString(","),
        )

        // Sample bank account
        sb.appendLine(
            listOf(
                "bank",
                csvEscape("Main Account"),
                csvEscape("1234567890"),
                csvEscape("Jane Doe"),
                csvEscape("Sample Bank"),
                csvEscape("SBIN0000001"),
                csvEscape("Savings"),
                csvEscape("Sample account"),
            ).joinToString(","),
        )

        sb.appendLine("type,cardNickname,amount,date,merchant,category,notes")

        // Sample transaction
        sb.appendLine(
            listOf(
                "transaction",
                csvEscape("Personal Visa"),
                "42.5",
                System.currentTimeMillis().toString(),
                csvEscape("Coffee Shop"),
                csvEscape("Food"),
                csvEscape("Sample transaction"),
            ).joinToString(","),
        )

        return sb.toString()
    }
}
