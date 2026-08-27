package com.example.flightticketbooking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * The single error shape returned by every failing request.
 *
 * <p>{@code fieldErrors} is only present on validation failures. Nothing here
 * carries a stack trace or any other internal detail.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }
}
