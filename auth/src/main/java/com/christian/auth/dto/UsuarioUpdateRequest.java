package com.christian.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioUpdateRequest(

        @Size(min = 8, max = 20, message = "La contraseña debe tener entre 8 y 20 caracteres")
        String password,

        @NotEmpty(message = "Los roles son requeridos")
        Set<String> roles
) {}
