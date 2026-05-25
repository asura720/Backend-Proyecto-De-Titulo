package com.cuidapp.autenticacioncuidapp.model;

public enum UserRole {
    INDEPENDIENTE,  // usuario normal, sin vinculación
    TITULAR,        // cuidador que gestiona pacientes
    PACIENTE        // adulto mayor vinculado a un titular
}
