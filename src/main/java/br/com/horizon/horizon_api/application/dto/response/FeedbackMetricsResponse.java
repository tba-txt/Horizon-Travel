package br.com.horizon.horizon_api.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FeedbackMetricsResponse {
    private long platformTotalFeedbacks;
    private double platformNps;
    private double platformCsat;

    private long tripTotalFeedbacks;
    private double tripNps;
    private double tripCsat;
}
