package com.cut.cardona.infra.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResult {
    private String id;
    private String filename;
    private String url;
    private String contentType;
    private long size;
}

