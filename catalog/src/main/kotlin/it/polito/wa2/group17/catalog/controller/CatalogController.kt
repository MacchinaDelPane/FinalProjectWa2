package it.polito.wa2.group17.catalog.controller

import it.polito.wa2.group17.catalog.security.OnlyAdmins
import it.polito.wa2.group17.catalog.security.OnlyEnabledUsers
import it.polito.wa2.group17.catalog.service.CatalogService
import it.polito.wa2.group17.common.dto.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping (
    value = ["/catalog"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class CatalogController {

    @Autowired
    private lateinit var catalogService: CatalogService

    // order-service ***

    @GetMapping ("/orders")
    @OnlyEnabledUsers
    fun getMyOrders(): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(catalogService.getOrders())
    }

    @GetMapping ("/orders/{orderId}")
    @OnlyEnabledUsers
    fun getOrderById(@PathVariable orderId: Long): ResponseEntity<OrderDto> {
        return ResponseEntity.ok(catalogService.getOrderById(orderId))
    }

    @GetMapping("/orders/{orderId}/status")
    @OnlyEnabledUsers
    fun getOrderStatus(@PathVariable orderId: Long): ResponseEntity<OrderStatus?>{
        return ResponseEntity.ok(catalogService.getOrderStatus(orderId))
    }

    @PostMapping("/orders")
    @OnlyEnabledUsers
    fun addOrder(@RequestBody @Valid order: NewOrderRequest): Long {
        return catalogService.addNewOrder(order)
    }

    @PatchMapping("/orders/{orderId}/status")
    @OnlyAdmins
    fun changeOrderStatus(@PathVariable orderId: Long, @RequestBody @Valid status: OrderPatchRequest): ResponseEntity<Long> {
        return ResponseEntity.ok(catalogService.changeOrderStatus(orderId, status))
    }

    @DeleteMapping("/orders/{orderId}")
    @OnlyEnabledUsers
    fun cancelOrder(@PathVariable orderId: Long): ResponseEntity<Unit> {
        return ResponseEntity.ok(catalogService.cancelUserOrder(orderId))
    }

    // ******

    // wallet-service ***

    @GetMapping("/wallets")
    @OnlyEnabledUsers
    fun getMyWallets(): ResponseEntity<Wallet> {
        return ResponseEntity.ok(catalogService.getWallets())
    }

    // ******

    // warehouse-service / products ***

    @GetMapping("/products")
    fun getProductsByCategory(@RequestParam(name = "category", required = false) category:String?) = ResponseEntity.ok(catalogService.getProductsByCategory(category))

    @GetMapping("/products/{productId}")
    fun getProductById(@PathVariable productId: Long) =
        ResponseEntity.ok(catalogService.getProduct(productId))

    @GetMapping("/products/{productId}/picture")
    fun getPicture(@PathVariable productId: Long) =
        ResponseEntity.ok(catalogService.getPicture(productId))

    @PostMapping("/products")
    @OnlyAdmins
    fun addProduct(@RequestBody @Valid newProductRequest: NewProductRequest) = catalogService.addProduct(newProductRequest)

    @PostMapping("/products/{productId}/rating")
    @OnlyEnabledUsers
    fun rateProduct(@PathVariable productId: Long, @RequestBody @Valid ratingRequest: RatingRequest): ResponseEntity<Long?> {
        //val rating = RatingDto(null, ratingRequest.stars, ratingRequest.comment)
        return ResponseEntity.ok(catalogService.rateProduct(productId, ratingRequest))
    }

    @PutMapping("/products/{productId}/picture")
    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    fun addPicture(@PathVariable productId: Long, @RequestBody @Valid picture: PostPicture) =
        ResponseEntity.ok(catalogService.addProductPicture(productId, picture))

    //@PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/products/{productId}")
    fun patchProductById(@PathVariable productId: Long, @RequestBody @Valid product: PatchProductRequest) =
        ResponseEntity.ok(catalogService.patchProductById(productId,product))

    @DeleteMapping("/products/{productId}")
    @OnlyAdmins
    fun deleteProduct(@PathVariable productId: Long) = catalogService.deleteProduct(productId)

    // ******

    // warehouse-service / warehouses ***

    @GetMapping("/warehouses")
    @OnlyAdmins
    fun getWarehouses() = ResponseEntity.ok(catalogService.getWarehouses())

    @PostMapping("/warehouses")
    @OnlyAdmins
    fun addWarehouse(@RequestBody @Valid warehouseRequest: WarehouseRequest) =
        ResponseEntity.ok(catalogService.addWarehouse(warehouseRequest))

    @PostMapping("/warehouses/{warehouseId}/products")
    @OnlyAdmins
    fun addProductToWarehouse(@PathVariable warehouseId: Long, @RequestBody @Valid addProductRequest: AddProductRequest
    ): ResponseEntity<StoredProductDto?> {
        return ResponseEntity.ok(catalogService.addProductToWarehouse(warehouseId, addProductRequest))
    }

    @DeleteMapping("/warehouses/{warehouseId}")
    @OnlyAdmins
    fun deleteWarehouse(@PathVariable warehouseId: Long) =
        catalogService.deleteWarehouse(warehouseId)

    // ******

}
