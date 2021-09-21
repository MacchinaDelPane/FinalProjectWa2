package it.polito.wa2.group17.catalog.service

import it.polito.wa2.group17.catalog.connector.OrderConnector
import it.polito.wa2.group17.catalog.connector.OrderConnectorMocked
import it.polito.wa2.group17.catalog.connector.WarehouseConnector
import it.polito.wa2.group17.catalog.connector.WarehouseConnectorMocked
import it.polito.wa2.group17.catalog.dto.ConvertibleDto.Factory.fromEntity
import it.polito.wa2.group17.catalog.dto.UserDetailsDto
import it.polito.wa2.group17.catalog.repository.UserRepository
import it.polito.wa2.group17.common.dto.*
import it.polito.wa2.group17.common.exception.EntityNotFoundException
import it.polito.wa2.group17.common.transaction.MultiserviceTransactional
import it.polito.wa2.group17.common.transaction.Rollback
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RequestBody
import javax.validation.Valid

interface CatalogService {

    @Throws(EntityNotFoundException::class)
    fun getOrders(): List<OrderDto>

    @Throws(EntityNotFoundException::class)
    fun getOrderById(orderId: Long): OrderDto?

    @Throws(EntityNotFoundException::class)
    fun addNewOrder(order: OrderDto): Long

    @Throws(EntityNotFoundException::class)
    fun listProducts(): List<StoredProductDto>

    @Throws(EntityNotFoundException::class)
    fun getProduct(productId: Long): StoredProductDto?

    @Throws(EntityNotFoundException::class)
    fun getWallets(): Wallet?

    @Throws(EntityNotFoundException::class)
    fun getUserInformation(): UserDetailsDto?

    @Throws(EntityNotFoundException::class)
    fun cancelUserOrder(orderId: Long)

    @Throws(EntityNotFoundException::class)
    fun getPicture(productId: Long): PostPicture?

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @Throws(EntityNotFoundException::class)
    fun addProductPicture(productId: Long, picture: PostPicture):ProductDto?

    @Throws(EntityNotFoundException::class)
    fun patchProductById(productId: Long, product: PatchProductRequest): ProductDto

    @Throws(EntityNotFoundException::class)
    fun addProductToWarehouse(warehouseId: Long, addProductRequest: AddProductRequest): StoredProductDto?

    @Throws(EntityNotFoundException::class)
    fun getOrderStatus(orderId: Long): OrderStatus?


}


@Service
@Transactional
private open class CatalogServiceImpl(
    val userRepository: UserRepository
) : CatalogService {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var warehouseConnector: WarehouseConnectorMocked

    @Autowired
    private lateinit var orderConnector: OrderConnectorMocked

    override fun getOrders(): List<OrderDto> {
        val username = SecurityContextHolder.getContext().authentication.name
        logger.info("Searching for the orders of the user with username {}", username)
        val user = userRepository.findByUsername(username)
        if (!user.isEmpty) {
            val userId = user.get().getId()
            return orderConnector.getOrdersByUsername(userId)
        }
        return listOf()
    }

    override fun getOrderById(orderId: Long): OrderDto? {
        logger.info("Searching for the order with id {}", orderId)
        return orderConnector.getOrderById(orderId)
    }

    @MultiserviceTransactional
    override fun addNewOrder(order: OrderDto): Long {
        logger.info("Adding a new order...")
        return orderConnector.addOrder(order)
    }

    @Rollback
    private fun rollbackForAddOrder(order: OrderDto, id: Long){
        logger.warn("Rollback of order with ID ${order.id}")
    }

    override fun listProducts(): List<StoredProductDto> {
        logger.info("Listing all the products...")
        return warehouseConnector.getProducts()
    }

    override fun getProduct(productId: Long): StoredProductDto? {
        logger.info("Searching for product with id {}", productId)
        return warehouseConnector.getProductById(productId)
    }

    override fun getWallets(): Wallet? {
        val username = SecurityContextHolder.getContext().authentication.name
        logger.info("Searching for the wallets of the user with username {}", username)
        val wallets = warehouseConnector.getWalletsByUsername(username)
        return wallets
    }

    override fun getUserInformation(): UserDetailsDto? {
        val username = SecurityContextHolder.getContext().authentication.name
        val userInfo = userRepository.findByUsername(username)
        return if(!userInfo.isEmpty) {
            fromEntity(userInfo.get())
        } else null
    }

    @MultiserviceTransactional
    override fun cancelUserOrder(orderId: Long) {
        logger.info("Cancelling order {}", orderId)
        return orderConnector.cancelOrder(orderId)
    }

    @Rollback
    private fun rollbackForCancelUserOrder(orderId: Long){
        logger.warn("Rollback of cancelling order with ID $orderId")
    }

    override fun getPicture(productId: Long): PostPicture? {
        return warehouseConnector.getProductPicture(productId)
    }

    @MultiserviceTransactional
    override fun addProductPicture(productId: Long, picture: PostPicture): ProductDto? {
        return warehouseConnector.setProductPicture(productId, picture)
    }

    @MultiserviceTransactional
    override fun patchProductById(productId: Long, product: PatchProductRequest): ProductDto {
        return warehouseConnector.patchProductById(productId, product)
    }

    @MultiserviceTransactional
    override fun addProductToWarehouse(warehouseId: Long, addProductRequest: AddProductRequest): StoredProductDto? {
        return warehouseConnector.addProduct(warehouseId, addProductRequest)
    }

    override fun getOrderStatus(orderId: Long): OrderStatus? {
        return orderConnector.getStatus(orderId)?.status
    }

    @Rollback
    private fun rollbackForAddProductToWarehouse(warehouseId: Long, addProductRequest: AddProductRequest, Dto: StoredProductDto?) {
        logger.info("Rollback for addProductToWarehouse")
    }

    @Rollback
    private fun rollbackForPatchProductById(productId: Long, product: PatchProductRequest, Dto: ProductDto) {
        logger.info("Rollback for patchProductById")
    }

    @Rollback
    private fun rollbackForAddProductPicture(productId: Long, picture: PostPicture, Dto: ProductDto?) {
        logger.info("Rollback for addProductPicture")
    }


}
