package eu.maveniverse.maven.njord.shared.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CloseableSupport implements Closeable {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final AtomicBoolean closed;

    protected CloseableSupport() {
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            doClose();
        }
    }

    protected void doClose() throws IOException {
        // nothing; override if needed
    }

    protected void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException(getClass().getSimpleName() + " is closed");
        }
    }
}
