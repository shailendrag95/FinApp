package com.example.finapp.data.repository

import com.example.finapp.data.local.dao.CreditCardDao
import com.example.finapp.data.local.entity.CreditCardEntity
import com.example.finapp.domain.model.CardNetwork
import com.example.finapp.domain.model.CardType
import com.example.finapp.domain.model.CreditCard
import com.example.finapp.security.EncryptionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreditCardRepository(
    private val dao: CreditCardDao,
    private val encryptionHelper: EncryptionHelper,
) {
    fun observeCards(): Flow<List<CreditCard>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCard(id: Long): CreditCard? =
        dao.getById(id)?.toDomain()

    suspend fun getAllCards(): List<CreditCard> =
        dao.getAll().map { it.toDomain() }

    suspend fun insert(card: CreditCard): Long {
        val id = dao.insert(card.toEntity())
        return if (card.id == 0L) id else card.id
    }

    suspend fun update(card: CreditCard) {
        dao.update(card.toEntity())
    }

    suspend fun delete(card: CreditCard) {
        dao.delete(card.toEntity())
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun CreditCardEntity.toDomain() = CreditCard(
        id = id,
        nickname = nickname,
        cardNumber = encryptionHelper.decrypt(cardNumberEnc),
        holderName = holderName,
        expiry = expiry,
        cvv = encryptionHelper.decrypt(cvvEnc),
        network = CardNetwork.valueOf(network),
        bank = bank,
        dueDateDay = dueDateDay,
        creditLimit = creditLimit,
        notes = notes,
        createdAt = createdAt,
        isLTF = isLTF,
        cardType = try { CardType.valueOf(cardType) } catch (e: Exception) { CardType.OTHER },
    )

    private fun CreditCard.toEntity() = CreditCardEntity(
        id = id,
        nickname = nickname,
        cardNumberEnc = encryptionHelper.encrypt(cardNumber),
        holderName = holderName,
        expiry = expiry,
        cvvEnc = encryptionHelper.encrypt(cvv),
        network = network.name,
        bank = bank,
        dueDateDay = dueDateDay,
        creditLimit = creditLimit,
        notes = notes,
        createdAt = createdAt,
        isLTF = isLTF,
        cardType = cardType.name,
    )
}
