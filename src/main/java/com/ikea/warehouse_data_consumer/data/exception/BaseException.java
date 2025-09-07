package com.ikea.warehouse_data_consumer.data.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Getter
public class BaseException extends ResponseStatusException {
    private HttpStatus status;
    private String code;
    private Object[] messageParams;
    private String message;
    private final Optional<Exception> maybeCause;

    public BaseException(HttpStatus status, String code) {
        super(status, code);
        this.code = code;
        this.status = status;
        this.maybeCause = Optional.empty();
    }

    public BaseException(HttpStatus status, String code, Exception t) {
        super(status, code);
        this.code = code;
        this.status = status;
        this.maybeCause = Optional.of(t);
    }

    public BaseException(HttpStatus status, String code, Object[] params){
        super(status, code);
        this.code = code;
        this.status = status;
        this.messageParams = params;
        this.message = String.format(code, params);
        this.maybeCause = Optional.empty();
    }

    public BaseException(HttpStatus status, String code, String message){
        super(status, code);
        this.code = code;
        this.status = status;
        this.message = message;
        this.messageParams = new Object[]{message};
        this.maybeCause = Optional.empty();
    }
}
