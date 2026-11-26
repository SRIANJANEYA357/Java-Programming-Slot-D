package com.foodshare.exception;

public class FoodExpiredException extends RuntimeException {
    public FoodExpiredException(String message) {
        super(message);
    }
}
