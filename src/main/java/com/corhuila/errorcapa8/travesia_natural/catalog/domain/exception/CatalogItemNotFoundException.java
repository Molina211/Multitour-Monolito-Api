package com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception;

public class CatalogItemNotFoundException extends RuntimeException {

    public CatalogItemNotFoundException(String catalogItemId) {
        super("catalog item not found: " + catalogItemId);
    }
}
