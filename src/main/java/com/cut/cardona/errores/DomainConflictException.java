package com.cut.cardona.errores;

public class DomainConflictException extends RuntimeException {
    public DomainConflictException(String message) {
        super(message);
    }
}

