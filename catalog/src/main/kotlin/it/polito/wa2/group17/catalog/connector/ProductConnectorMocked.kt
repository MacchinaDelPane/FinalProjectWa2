package it.polito.wa2.group17.catalog.connector

import it.polito.wa2.group17.common.connector.Connector
import it.polito.wa2.group17.common.dto.RatingDto
import it.polito.wa2.group17.common.dto.RatingRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary

@Connector
@Primary
@ConditionalOnProperty(prefix = "connectors.warehouse.mock", name = ["enabled"], havingValue = "true")

class ProductConnectorMocked: ProductConnector() {
    override fun rateProductById(productId: Long, ratingDto: RatingRequest) = 0L
}
