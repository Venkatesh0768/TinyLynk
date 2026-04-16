package org.backend.backend.url.dtos.responseDto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlAnalyticsResponse {

    private Long urlId;
    private Long totalClicks;

    // later:
    // private Map<String, Long> countryStats;
    // private Map<String, Long> deviceStats;
}