package io.bdrc.iiif.core;

import java.util.Collections;
import java.util.Map;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OverloadExceptionHandler {

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<Map<String, String>> serviceOverloaded() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.set(HttpHeaders.RETRY_AFTER, "5");
        return new ResponseEntity<>(
                Collections.singletonMap("error", "Image service is temporarily overloaded"),
                headers,
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
