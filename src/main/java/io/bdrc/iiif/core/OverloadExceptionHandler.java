package io.bdrc.iiif.core;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.bdrc.auth.AccessInfo;

@RestControllerAdvice
public class OverloadExceptionHandler {

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<Map<String, String>> serviceOverloaded(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.set(HttpHeaders.RETRY_AFTER, "5");

        Object access = request.getAttribute("access");
        boolean loggedIn = access instanceof AccessInfo && ((AccessInfo) access).isLogged();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Image service is temporarily overloaded");
        body.put("message", loggedIn
                ? "Please retry shortly"
                : "Please log in for priority access, or retry shortly");
        return new ResponseEntity<>(body, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
