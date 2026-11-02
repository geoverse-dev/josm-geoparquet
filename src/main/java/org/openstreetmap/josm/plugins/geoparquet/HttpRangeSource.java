package org.openstreetmap.josm.plugins.geoparquet;

import io.tileverse.parquetry.io.ByteRangeSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.MemorySegment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Read-only HTTP(S) byte-range source. Requires RFC 7233 byte ranges; never downloads whole objects. */
public final class HttpRangeSource implements ByteRangeSource {
    private final URI uri;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final long length;
    public HttpRangeSource(URI uri) throws IOException {
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
            throw new IOException("Only HTTP(S) URLs are supported. Use a presigned HTTPS URL for private S3 objects.");
        this.uri = uri;
        try {
            HttpRequest req = HttpRequest.newBuilder(uri).header("Range", "bytes=0-0")
                    .timeout(Duration.ofSeconds(45)).GET().build();
            HttpResponse<byte[]> response = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 206) throw new IOException("Server must support HTTP byte ranges (206); got " + response.statusCode());
            String contentRange = response.headers().firstValue("Content-Range").orElseThrow(
                    () -> new IOException("Missing Content-Range response header"));
            int slash = contentRange.lastIndexOf('/');
            if (slash < 0) throw new IOException("Invalid Content-Range: " + contentRange);
            this.length = Long.parseLong(contentRange.substring(slash + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IOException("Interrupted while checking remote GeoParquet", e);
        } catch (NumberFormatException e) { throw new IOException("Invalid remote file size", e); }
    }
    @Override public long size() { return length; }
    @Override public int read(long offset, MemorySegment dst) {
        if (offset < 0) throw new IllegalArgumentException("Negative offset");
        if (offset >= length) return -1;
        if (dst.byteSize() == 0) return 0;
        int count = Math.toIntExact(Math.min(dst.byteSize(), length - offset));
        try {
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .header("Range", "bytes=" + offset + "-" + (offset + count - 1))
                    .timeout(Duration.ofSeconds(90)).GET().build();
            HttpResponse<byte[]> response = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 206 || response.body().length != count)
                throw new IOException("Invalid HTTP range response: status=" + response.statusCode()
                        + ", bytes=" + response.body().length + ", expected=" + count);
            dst.asSlice(0, count).copyFrom(MemorySegment.ofArray(response.body()));
            return count;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new UncheckedIOException(new IOException("Interrupted", e));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Override public void close() { }
}
