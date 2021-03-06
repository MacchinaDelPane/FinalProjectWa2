package it.polito.wa2.group17.catalog.service


import it.polito.wa2.group17.catalog.domain.EmailVerificationToken
import it.polito.wa2.group17.catalog.domain.User
import it.polito.wa2.group17.catalog.dto.BooleanValueClass
import it.polito.wa2.group17.catalog.dto.ConvertibleDto.Factory.fromEntity
import it.polito.wa2.group17.catalog.dto.PutSetAdmin
import it.polito.wa2.group17.catalog.dto.UserDetailsDto
import it.polito.wa2.group17.catalog.exceptions.auth.EmailAlreadyPresentException
import it.polito.wa2.group17.catalog.exceptions.auth.UserAlreadyPresentException
import it.polito.wa2.group17.catalog.exceptions.auth.UserAlreadyVerifiedException
import it.polito.wa2.group17.catalog.repository.UserRepository
import it.polito.wa2.group17.catalog.security.RoleName
import it.polito.wa2.group17.common.exception.GenericBadRequestException
import it.polito.wa2.group17.common.mail.MailRequestDto
import it.polito.wa2.group17.common.mail.MailService
import it.polito.wa2.group17.common.transaction.MultiserviceTransactional
import it.polito.wa2.group17.common.transaction.Rollback
import it.polito.wa2.group17.catalog.security.OnlyAdmins
import it.polito.wa2.group17.common.utils.converter.convert
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.Inet4Address
import javax.persistence.EntityNotFoundException

interface UserDetailsServiceExtended : UserDetailsService {
    fun createUser(
        username: String, password: String, email: String,
        name: String, surname: String, address: String,
    )

    @Throws(EntityNotFoundException::class, UserAlreadyVerifiedException::class)
    fun createTokenForUser(username: String, email: String? = null)

    @Throws(EntityNotFoundException::class)
    fun addRoleToUser(username: String, role: String): PutSetAdmin

    @Throws(EntityNotFoundException::class)
    fun setUserEnabled(username: String, enabled: Boolean): BooleanValueClass

    fun enableUser(username: String, enabled: Boolean)

    @Throws(EntityNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetailsDto
    fun verifyToken(token: String)

    @Throws(EntityNotFoundException::class)
    fun getCustomerIdForUser(username: String): Long?

    @Throws(it.polito.wa2.group17.common.exception.EntityNotFoundException::class)
    fun setUserAsAdmin(username: String, value: Boolean): PutSetAdmin?

    fun getAdmins(): List<UserDetailsDto>

    fun getCustomers(): List<UserDetailsDto>

}

@Service
@Transactional
class UserDetailsServiceExtendedImpl(private val userRepository: UserRepository) : UserDetailsServiceExtended {
    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var mailService: MailService


    @Value("\${server.port:8080}")
    private lateinit var localPort: String

    @Value("\${notifications.tokenVerification.subject}")
    private lateinit var tokenMessageSubject: String

    @Value("\${notifications.tokenVerification.body}")
    private lateinit var tokenMessageBody: String

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        val localAddress: String = Inet4Address.getLocalHost().hostName
    }


    private fun computeTokenEndpoint(token: EmailVerificationToken) =
        "http://$localAddress:$localPort/auth/registrationConfirm?token=${
            token.getId().toString()
        }"

    @MultiserviceTransactional
    override fun createUser(
        username: String, password: String, email: String,
        name: String, surname: String, address: String,
    ) {
        if (userRepository.findByUsername(username).isPresent) {
            throw UserAlreadyPresentException(username)
        }
        if (userRepository.findByEmail(email).isPresent) {
            throw EmailAlreadyPresentException(email)
        }
        logger.info("Creating user {}", username)
        val user = userRepository.save(User(username, password, email, name, surname, address, false))
        logger.info("User {} created", username)
        createTokenForUser(username, email)
        addRoleToUser(user.username, RoleName.CUSTOMER.name)
        setUserEnabled(user.username, false)
    }

    @Rollback
    private fun rollbackForCreateUser(username: String, password: String, email: String, name: String, surname: String, address: String) {
        print("Rollback create user\n")
        userRepository.deleteUserByEmail(email)
    }

    override fun createTokenForUser(username: String, email: String?) {
        val user = userRepository.findByUsername(username).orElseThrow { EntityNotFoundException("username $username") }
        if (user.isEnabled)
            throw UserAlreadyVerifiedException(username)

        val existingEmail = email ?: user.email

        logger.info("Creating token for user $username sending it to $existingEmail")
        val token = notificationService.createTokenForUser(username)
        mailService.sendMessage(
            existingEmail,
            tokenMessageSubject,
            String.format(tokenMessageBody, computeTokenEndpoint(token), token.expireDate))
    }

    override fun loadUserByUsername(username: String): UserDetailsDto {
        val user = userRepository.findByUsername(username)
        return fromEntity(user.orElseThrow { EntityNotFoundException("username $username") })
    }

    override fun verifyToken(token: String) {
        val username = notificationService.verifyToken(token)
        setUserEnabled(username, true)
    }

    override fun getCustomerIdForUser(username: String): Long? =
        userRepository.findByUsername(username).orElseThrow { EntityNotFoundException("username $username") }
            .let {
                if (it.getRoleNames().contains(RoleName.CUSTOMER))
                    it.getId()
                else null
            }

    override fun getAdmins(): List<UserDetailsDto> {
        return userRepository.findAdmin(listOf("ADMIN", "CUSTOMER ADMIN", "ADMIN CUSTOMER")).map { it -> UserDetailsDto(it.getId(), it.username, it.password, it.email, it.isEnabled, it.getRoleNames(), it.name, it.surname, it.deliveryAddr) }
    }

    override fun getCustomers(): List<UserDetailsDto> {
        return userRepository.findAll().filter { it.roles.contains("CUSTOMER") }.map { UserDetailsDto(it.getId(), it.username, it.password, it.email, it.isEnabled, it.getRoleNames(), it.name, it.surname, it.deliveryAddr) }
    }

    @MultiserviceTransactional
    override fun addRoleToUser(username: String, role: String): PutSetAdmin {
        logger.info("Adding role {} to {}", role, username)
        val user = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("username $username") }
        val putSetAdmin = PutSetAdmin(user.getId()!!, user.roles)
        user.addRoleName(role)
        return putSetAdmin
    }

    @Rollback
    private fun rollbackForAddRoleToUser(username: String, role: String, putSetAdmin: PutSetAdmin) {
        print("Rollback add role\n")
        val user = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("username $username") }
        user.roles = putSetAdmin.prev_value
    }

    @OnlyAdmins
    override fun enableUser(username: String, enabled: Boolean) {
        logger.info("enableUser started")
        userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("username $username") }
            .isEnabled = enabled
    }

    @MultiserviceTransactional
    override fun setUserEnabled(username: String, enabled: Boolean): BooleanValueClass {
        logger.info("{} user {}", if (enabled) "Enabling" else "Disabling", username)
        val user = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("username $username") }
        val booleanValue = BooleanValueClass(user.isEnabled)
        user.isEnabled = enabled
        return booleanValue
    }

    @Rollback
    private fun rollbackForSetUserEnabled(username: String, enabled: Boolean, booleanValue: BooleanValueClass) {
        print ("Rollback set enabled\n")
        val user = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("username $username") }
        user.isEnabled = booleanValue.prev_value
    }

    @OnlyAdmins
    @MultiserviceTransactional
    override fun setUserAsAdmin(username: String, value: Boolean): PutSetAdmin {
        if (SecurityContextHolder.getContext().authentication.name == username) {
            throw GenericBadRequestException("You cannot modify your roles!")
        }
        print("inside UserDetails")
        val user = userRepository.findByUsername(username)
            .orElseThrow { it.polito.wa2.group17.common.exception.EntityNotFoundException("username $username") }
        val putSetAdmin = PutSetAdmin(user.getId()!!, user.roles)

        if (value) {
            if (user.roles.contains("ADMIN")) {
                logger.info("The user is already an ADMIN!")
            } else {
                user.addRoleName(RoleName.ADMIN.name)
                logger.info("ADMIN added to the roles of the user")
            }
        }
        else {
            user.removeRoleName(RoleName.ADMIN.name)
            logger.info("ADMIN removed from the roles of the user")
        }
        return putSetAdmin
    }

    @Rollback
    private fun rollbackForSetUserAsAdmin(username: String, value: Boolean,  putSetAdmin: PutSetAdmin) {
        val user = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("username $username") }
        user.roles = putSetAdmin.prev_value
    }
}
