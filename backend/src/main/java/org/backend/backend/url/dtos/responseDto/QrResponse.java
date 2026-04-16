package org.backend.backend.url.dtos.responseDto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrResponse {

    private Long urlId;
    private String qrImageUrl;
}