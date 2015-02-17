package es.tor.controller;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

public class GenericController {
    protected static Logger logger = Logger.getLogger(GenericController.class);
    
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void handleAllException(Throwable e) {
        if (e instanceof AccessDeniedException) {
            throw (AccessDeniedException) e;
        }
        
        logger.error("Unexpected error", e);
    }
}