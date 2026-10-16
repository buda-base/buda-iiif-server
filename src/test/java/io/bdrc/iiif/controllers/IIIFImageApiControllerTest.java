package io.bdrc.iiif.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class IIIFImageApiControllerTest {

    @Test
    public void streamingResponseClosesInputWhenCopyFails() {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("client disconnected");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        assertThrows(IOException.class, () ->
                IIIFImageApiController.streamingResponseFrom(input)
                        .writeTo(new ByteArrayOutputStream()));
        assertTrue(closed.get());
    }
}
