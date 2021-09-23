package it.polito.wa2.group17.login.exceptions.security

import it.polito.wa2.group17.common.exception.AutoLoggableException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class UserNotAllowedException(username: String) :
    AutoLoggableException("User $username is not allowed to perform the required operation")
