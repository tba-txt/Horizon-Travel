package br.com.horizon.horizon_api.domain.port;

public interface EmailPort {
    void sendEmail(String to, String subject, String body);
}
