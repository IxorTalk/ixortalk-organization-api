/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-present IxorTalk CVBA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ixortalk.organization.api.error;

import com.ixortalk.organization.api.OrganizationApiApplication;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolationException;

import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpStatus.*;

@ControllerAdvice(basePackageClasses = { OrganizationApiApplication.class, RepositoryRestExceptionHandler.class })
public class GenericExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericExceptionHandler.class);

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(Exception e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Access denied - " + errorUUID, new HttpHeaders(), FORBIDDEN);
    }

    @ExceptionHandler(value = FeignException.class)
    public ResponseEntity<String> handleFeignException(FeignException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Feign Error - " + errorUUID, new HttpHeaders(), valueOf(e.status()));
    }

    @ExceptionHandler(value = ConflictException.class)
    public ResponseEntity<String> handleConflictException(ConflictException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Conflict - " + errorUUID, new HttpHeaders(), CONFLICT);
    }
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Data Integrity Violation - " + errorUUID, new HttpHeaders(), CONFLICT);
    }

    @ExceptionHandler(value = BadRequestException.class)
    public ResponseEntity<String> handleBadRequestException(BadRequestException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Bad Request - " + errorUUID, new HttpHeaders(), BAD_REQUEST);
    }

    @ExceptionHandler(value = MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingServletRequestPartExceptionException(MissingServletRequestPartException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Bad Request - " + errorUUID + " - missing parameter " + e.getRequestPartName(), new HttpHeaders(), BAD_REQUEST);
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Not Found - " + errorUUID, new HttpHeaders(), NOT_FOUND);
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Constraint violation - " + e.getMessage() + " - " + errorUUID, new HttpHeaders(), BAD_REQUEST);
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Missing request param - " + e.getMessage() + " - " + errorUUID, new HttpHeaders(), BAD_REQUEST);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        String errorUUID = logError(e);
        return new ResponseEntity<>("Internal Server Error - " + errorUUID, new HttpHeaders(), INTERNAL_SERVER_ERROR);
    }

    private static String logError(Exception e) {
        String errorUUID = randomUUID().toString();
        LOGGER.error("Error - {}: {}", errorUUID, e.getMessage(), e);
        return errorUUID;
    }
}