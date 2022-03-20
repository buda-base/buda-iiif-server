package io.bdrc.iiif.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class IIIFException extends Exception {
    public static final int GENERIC_APP_ERROR_CODE = 5001;
    private static final Logger logger = LoggerFactory.getLogger(Exception.class);
    private static final long serialVersionUID = -5379981810772284216L;
    int status;
    int code;
    String link = null;
    String developerMessage = null;
    String message = null;

    public IIIFException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public IIIFException() {
    }

    public IIIFException(String message) {
        super(message);
    }

    public IIIFException(int status, int code, String message, String developerMessage, String link) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = developerMessage;
        this.link = link;
        if (status == 500) {
            logger.error("error status {}, code {}, message: {}, details {}", status, code, message, developerMessage);
        } else {
            logger.warn(message);
            logger.warn(developerMessage);
        }
        
    }

    public IIIFException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = null;
        if (status == 500) {
            logger.error("error status {}, code {}, message: {}", status, code, message);
        }
    }

    public IIIFException(int status, int code, Exception e) {
        super(e.getMessage());
        this.status = status;
        this.code = code;
        this.message = e.getMessage();
        if (e instanceof IIIFException && ((IIIFException) e).developerMessage != null) {
            this.developerMessage = ((IIIFException) e).developerMessage;
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            this.developerMessage = sw.toString();
        }
        if (status == 500) {
            logger.error("error status {}, code {}", status, code, e);
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
    
    public HttpStatus getHttpStatus() {
        return HttpStatus.resolve(status);
    }
    
    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}