package org.backend.backend.url.dtos.requestDtos;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUrlRequest {

    private Boolean isActive;
    private String expiresAt; // ISO string
}