package it.polito.wa2.group17.wallet.model

import it.polito.wa2.group17.common.utils.converter.ConvertibleAlias
import it.polito.wa2.group17.common.utils.converter.CustomConversion
import it.polito.wa2.group17.common.utils.converter.impl.IdAnnotatedExtractor
import it.polito.wa2.group17.wallet.entity.TransactionEntity
import java.time.Instant


data class Transaction(
    val id: Long,
    val timeInstant: Instant,
    val amount: Double = 0.0,
    val reason: String,
    @param:CustomConversion(CustomConversion.Using(IdAnnotatedExtractor::class, TransactionEntity::class))
    @param:ConvertibleAlias(ConvertibleAlias.From(TransactionEntity::class,"source"))
    val sourceWallet: Long,
)
