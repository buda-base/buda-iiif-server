package io.bdrc.iiif.controllers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public class IIIFImageApiControllerTest {

    @Test
    public void inputIsAcquiredOnlyWhenStreamingWorkerRuns() throws IOException {
        AtomicBoolean acquired = new AtomicBoolean();
        byte[] content = new byte[] { 1, 2, 3 };
        StreamingResponseBody response = IIIFImageApiController.streamingResponseFrom(() -> {
            acquired.set(true);
            return new ByteArrayInputStream(content);
        });

        assertFalse(acquired.get());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.writeTo(output);

        assertTrue(acquired.get());
        assertArrayEquals(content, output.toByteArray());
    }

    @Test
    public void streamingResponseClosesInputWhenCopyFails() throws IOException {
        AtomicBoolean closed = new AtomicBoolean();
        try (InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("client disconnected");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        }) {
            assertThrows(IOException.class, () ->
                    IIIFImageApiController.streamingResponseFrom(input)
                            .writeTo(new ByteArrayOutputStream()));
        }
        assertTrue(closed.get());
    }
}
