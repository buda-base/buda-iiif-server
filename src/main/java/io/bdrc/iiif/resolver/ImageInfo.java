package io.bdrc.iiif.resolver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageInfo {
    @JsonProperty("width")
    public int width;
    @JsonProperty("height")
    public int height;
    @JsonProperty("filename")
    public String filename;
    @JsonProperty("size")
    public Integer size = null;
    
    public Integer imgNum = null;

    public ImageInfo(int width, int height, String filename, Integer size) {
        this.width = width;
        this.height = height;
        this.filename = filename;
        this.size = size;
    }

    public ImageInfo() {
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "toString objectmapper exception, this shouldn't happen";
        }
    }
}