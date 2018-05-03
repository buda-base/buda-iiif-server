package de.digitalcollections.core.model.impl.resource;

import java.net.URI;
import java.util.UUID;

import de.digitalcollections.core.model.api.MimeType;
import de.digitalcollections.core.model.api.resource.Resource;

public class S3Resource implements Resource{

    private String filenameExtension;
    private long lastModified = -1;
    private MimeType mimeType;
    private long size = -1;
    private UUID uuid = UUID.randomUUID();
    private boolean readonly = false;
    private URI uri;
    private String identifier;
    
        
    public S3Resource() {
        super();        
    }

    @Override
    public String getFilename() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFilenameExtension() {
      return filenameExtension;
    }

    @Override
    public void setFilenameExtension(String filenameExtension) {
      this.filenameExtension = filenameExtension;
    }

    @Override
    public long getLastModified() {
      return this.lastModified;
    }

    @Override
    public void setLastModified(long lastModified) {
      this.lastModified = lastModified;
    }

    @Override
    public MimeType getMimeType() {
      return this.mimeType;
    }

    @Override
    public void setMimeType(MimeType mimeType) {
      this.mimeType = mimeType;
    }

    @Override
    public boolean isReadonly() {
      return this.readonly;
    }

    @Override
    public void setReadonly(boolean readonly) {
      this.readonly = readonly;
    }

    @Override
    public long getSize() {
      return size;
    }

    @Override
    public void setSize(long size) {
      this.size = size;
    }

    @Override
    public URI getUri() {
      return this.uri;
    }

    @Override
    public void setUri(URI uri) {
      this.uri = uri;
    }

    @Override
    public UUID getUuid() {
      return uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
      this.uuid = uuid;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "S3Resource [filenameExtension=" + filenameExtension + ", lastModified=" + lastModified + ", mimeType="
                + mimeType + ", size=" + size + ", uuid=" + uuid + ", readonly=" + readonly + ", uri=" + uri
                + ", identifier=" + identifier + "]";
    }
    

}
