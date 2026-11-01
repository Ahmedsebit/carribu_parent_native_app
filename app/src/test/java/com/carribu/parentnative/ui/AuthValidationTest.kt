package com.carribu.parentnative.ui

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {
    @Test
    fun `password validation enforces length match and change`() {
        assertNotNull(AuthViewModel.validatePassword("123", "123", "old"))
        assertNotNull(AuthViewModel.validatePassword("newpass", "different", "old"))
        assertNotNull(AuthViewModel.validatePassword("samepass", "samepass", "samepass"))
        assertNull(AuthViewModel.validatePassword("newpass", "newpass", "oldpass"))
    }
}
