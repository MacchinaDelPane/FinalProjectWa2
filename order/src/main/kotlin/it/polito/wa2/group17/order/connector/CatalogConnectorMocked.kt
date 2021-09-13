package it.polito.wa2.group17.order.connector

import it.polito.wa2.group17.common.connector.Connector
import it.polito.wa2.group17.order.model.UserModel
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary

@Connector
@Primary
@ConditionalOnProperty(prefix = "connectors.catalog.mock", name = ["enabled"], havingValue = "true")
class CatalogConnectorMocked: CatalogConnector() {
    override fun getUserInfo(userId: Long): UserModel = UserModel(userId, "via DiProva 42")
}