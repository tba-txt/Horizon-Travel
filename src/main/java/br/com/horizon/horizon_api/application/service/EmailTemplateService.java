package br.com.horizon.horizon_api.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Serviço centralizado para geração de templates HTML de e-mails do ecossistema Horizon.
 * Padrão visual estrito (Design System Horizon):
 * - Marca: estritamente Horizon
 * - Paleta oficial: Azul Horizon (#5BA4D8), Verde Horizon (#65A30D), Dark/Slate (#0F172A)
 * - Fundo externo: 100% Branco (#FFFFFF), sem molduras ou tons cinzas ao redor
 * - Cabeçalho padrão: Banner oficial com Logo Horizon, paisagem e curva fluida (S-wave)
 * - Rodapé padrão: Ilustração oficial Horizon com colinas verdes, sol e montanhas
 * - ZERO pills / badges formato cápsula
 * - ZERO linhas divisórias horizontais (dividers / border-top / border-bottom / <hr>)
 * - ZERO setas (->, bi-arrow-right) em botões ou textos
 */
public final class EmailTemplateService {

    private EmailTemplateService() {
    }

    private static final String FONT_FAMILY = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif";
    private static final String COLOR_BLUE = "#5BA4D8";
    private static final String COLOR_GREEN = "#65A30D";
    private static final String COLOR_DARK = "#0F172A";

    private static final String HEADER_HERO_URL = "https://res.cloudinary.com/horizon-travel/image/upload/v1793050504/hz_std_email_header.png";
    private static final String FOOTER_HERO_URL = "https://res.cloudinary.com/horizon-travel/image/upload/v1793050506/hz_std_email_footer.png";

    /**
     * Layout base padrão para todos os e-mails disparados pela plataforma Horizon.
     * Fundo 100% limpo em branco (sem cinza ao redor), largura máxima de 570px,
     * cabeçalho com logo + foto em curva e rodapé com sol e montanhas.
     */
    private static String buildStandardLayout(String pageTitle, String contentHtml) {
        return "<!DOCTYPE html>" +
                "<html lang=\"pt-BR\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <meta name=\"color-scheme\" content=\"light only\">" +
                "  <meta name=\"supported-color-schemes\" content=\"light only\">" +
                "  <title>" + pageTitle + "</title>" +
                "  <style>" +
                "    :root { color-scheme: light only !important; supported-color-schemes: light only !important; }" +
                "    body, table, td { margin: 0; padding: 0; border-collapse: collapse; }" +
                "    img { border: 0; outline: none; text-decoration: none; display: block; -ms-interpolation-mode: bicubic; }" +
                "    [data-ogsc] .hz-body, [data-ogsb] .hz-body { background-color: #ffffff !important; }" +
                "    [data-ogsc] .hz-card, [data-ogsb] .hz-card { background-color: #ffffff !important; }" +
                "  </style>" +
                "</head>" +
                "<body class=\"hz-body\" style=\"margin: 0; padding: 0; background-color: #ffffff; font-family: " + FONT_FAMILY + "; -webkit-font-smoothing: antialiased;\">" +
                "  <table role=\"presentation\" class=\"hz-body\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color: #ffffff; padding: 24px 12px; margin: 0;\">" +
                "    <tr><td align=\"center\">" +
                "      <table role=\"presentation\" class=\"hz-card\" width=\"570\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width: 100%; max-width: 570px; background-color: #ffffff; border-radius: 18px; box-shadow: 0 4px 24px rgba(15, 23, 42, 0.05); overflow: hidden; border-collapse: collapse; margin: 0 auto;\">" +
                "        <!-- HEADER COM LOGO HORIZON + HERO PAISAGEM + CURVA S-WAVE -->" +
                "        <tr><td style=\"padding: 0; margin: 0; line-height: 0; font-size: 0; background-color: #ffffff;\">" +
                "          <img src=\"" + HEADER_HERO_URL + "\" width=\"570\" alt=\"Horizon\" style=\"display: block; width: 100%; max-width: 570px; height: auto; border: 0; margin: 0; padding: 0; border-top-left-radius: 18px; border-top-right-radius: 18px;\">" +
                "        </td></tr>" +
                "        <!-- CORPO DO E-MAIL COM O CONTEÚDO ESPECÍFICO -->" +
                "        <tr><td style=\"padding: 34px 44px 28px 44px; background-color: #ffffff;\">" +
                contentHtml +
                "        </td></tr>" +
                "        <!-- RODAPÉ OFICIAL HORIZON COM SOL E MONTANHAS -->" +
                "        <tr><td style=\"padding: 0; margin: 0; line-height: 0; font-size: 0; background-color: #ffffff;\">" +
                "          <img src=\"" + FOOTER_HERO_URL + "\" width=\"570\" alt=\"Horizon • Descubra. Planeje. Viva.\" style=\"display: block; width: 100%; max-width: 570px; height: auto; border: 0; margin: 0; padding: 0; border-bottom-left-radius: 18px; border-bottom-right-radius: 18px;\">" +
                "        </td></tr>" +
                "      </table>" +
                "    </td></tr>" +
                "  </table>" +
                "</body>" +
                "</html>";
    }

    private static String extractFirstName(String socialName, String name) {
        String rawCandidate = null;
        if (name != null && !name.isBlank()) {
            rawCandidate = name.trim();
        } else if (socialName != null && !socialName.isBlank()) {
            rawCandidate = socialName.trim();
        }

        if (rawCandidate == null || rawCandidate.isEmpty()) {
            return "viajante";
        }
        String firstWord = rawCandidate.split("\\s+")[0];
        if (firstWord.isEmpty()) {
            return "viajante";
        }
        return firstWord.substring(0, 1).toUpperCase() + firstWord.substring(1);
    }

    /**
     * E-mail para recuperação / redefinição de senha com a identidade visual Horizon.
     * Fundo 100% branco ao redor (sem cinza), layout oficial da marca.
     */
    public static String buildPasswordResetEmail(String socialName, String name, String resetUrl) {
        String firstName = extractFirstName(socialName, name);
        String safeUrl = (resetUrl != null && !resetUrl.isBlank()) ? resetUrl.trim() : "http://localhost:4200/reset-password";

        String contentHtml =
                "<h1 style=\"margin: 0 0 14px 0; font-size: 26px; font-weight: 800; color: " + COLOR_DARK + "; letter-spacing: -0.5px;\">Redefinição de senha</h1>" +
                "<div style=\"font-size: 17px; font-weight: 600; color: " + COLOR_DARK + "; margin-bottom: 14px;\">Olá, " + firstName + "!</div>" +
                "<p style=\"margin: 0 0 28px 0; font-size: 15px; color: #334155; line-height: 1.65;\">" +
                "  Recebemos uma solicitação para redefinir a senha da sua conta no Horizon. Clique no botão abaixo para criar uma nova senha." +
                "</p>" +
                "<div align=\"center\" style=\"padding-bottom: 28px;\">" +
                "  <a href=\"" + safeUrl + "\" target=\"_blank\" style=\"display: inline-block; background-color: " + COLOR_BLUE + "; color: #ffffff; padding: 15px 38px; font-size: 16px; font-weight: 700; text-decoration: none; border-radius: 10px; box-shadow: 0 6px 18px rgba(91, 164, 216, 0.35);\">Redefinir minha senha</a>" +
                "</div>" +
                "<div style=\"background-color: #e8f4fc; border-radius: 14px; padding: 18px 22px; margin-bottom: 26px;\">" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">" +
                "    <tr>" +
                "      <td width=\"42\" valign=\"middle\" style=\"padding-right: 14px;\">" +
                "        <div style=\"width: 38px; height: 38px; background-color: #ffffff; border-radius: 10px; text-align: center; line-height: 38px;\">" +
                "          <img src=\"https://res.cloudinary.com/horizon-travel/image/upload/v1793051758/hz_lock_icon.png\" width=\"20\" height=\"24\" alt=\"Seguro\" style=\"display: inline-block; vertical-align: middle;\">" +
                "        </div>" +
                "      </td>" +
                "      <td valign=\"middle\">" +
                "        <span style=\"display: block; font-size: 15px; font-weight: 700; color: " + COLOR_DARK + "; margin-bottom: 3px;\">Seu link é seguro e temporário</span>" +
                "        <span style=\"display: block; font-size: 13px; color: #334155; line-height: 1.45;\">Este link expira em 1 hora e só pode ser usado uma vez.</span>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</div>" +
                "<p style=\"margin: 0 0 18px 0; font-size: 14px; color: #334155; line-height: 1.6;\">" +
                "  Se você não solicitou a redefinição de senha, pode ignorar este e-mail.<br>" +
                "  Sua conta continuará segura." +
                "</p>" +
                "<div style=\"font-size: 15px; color: " + COLOR_DARK + "; line-height: 1.5; padding-bottom: 10px;\">" +
                "  Um abraço,<br>" +
                "  <strong>Equipe Horizon</strong>" +
                "</div>";

        return buildStandardLayout("Horizon - Redefinição de Senha", contentHtml);
    }

    /**
     * E-mail de confirmação de senha alterada com sucesso.
     */
    public static String buildPasswordChangedConfirmationEmail(String socialName, String name) {
        String firstName = extractFirstName(socialName, name);
        String loginUrl = "http://localhost:4200/login";

        String contentHtml =
                "<div style=\"margin-bottom: 16px;\">" +
                "  <span style=\"display: inline-block; background-color: #f0fdf4; color: #166534; font-size: 12px; font-weight: 700; letter-spacing: 0.5px; padding: 6px 14px; border-radius: 8px; text-transform: uppercase;\">Segurança da Conta</span>" +
                "</div>" +
                "<h1 style=\"margin: 0 0 14px 0; font-size: 26px; font-weight: 800; color: " + COLOR_DARK + "; letter-spacing: -0.5px;\">Senha alterada com sucesso</h1>" +
                "<div style=\"font-size: 17px; font-weight: 600; color: " + COLOR_DARK + "; margin-bottom: 14px;\">Olá, " + firstName + "!</div>" +
                "<p style=\"margin: 0 0 24px 0; font-size: 15px; color: #334155; line-height: 1.65;\">" +
                "  Confirmamos que a senha de acesso à sua conta na Horizon foi alterada com sucesso." +
                "</p>" +
                "<div style=\"background-color: #f0f7fc; border-radius: 14px; border: 1.5px solid rgba(91, 164, 216, 0.25); padding: 18px 22px; margin-bottom: 26px;\">" +
                "  <span style=\"font-size: 14px; color: #334155; line-height: 1.5; display: block;\">" +
                "    Se você mesmo realizou essa alteração, nenhuma ação adicional é necessária. Caso você <strong>não</strong> tenha solicitado essa mudança, entre em contato imediatamente com o suporte da Horizon para proteger seu acesso." +
                "  </span>" +
                "</div>" +
                "<div align=\"center\" style=\"padding-bottom: 26px;\">" +
                "  <a href=\"" + loginUrl + "\" target=\"_blank\" style=\"display: inline-block; background-color: " + COLOR_BLUE + "; color: #ffffff; padding: 14px 34px; font-size: 15px; font-weight: 700; text-decoration: none; border-radius: 10px; box-shadow: 0 4px 14px rgba(91, 164, 216, 0.35);\">Acessar Minha Conta</a>" +
                "</div>" +
                "<div style=\"font-size: 15px; color: " + COLOR_DARK + "; line-height: 1.5; padding-bottom: 10px;\">" +
                "  Um abraço,<br>" +
                "  <strong>Equipe Horizon</strong>" +
                "</div>";

        return buildStandardLayout("Horizon - Senha Alterada com Sucesso", contentHtml);
    }

    /**
     * Sobrecarga mantida para compatibilidade direta.
     */
    public static String buildPasswordResetEmail(String name, String resetUrl) {
        return buildPasswordResetEmail(null, name, resetUrl);
    }

    /**
     * E-mail para confirmação de pagamento e emissão de bilhetes.
     */
    public static String buildPaymentConfirmationEmail(String name, Long reservationId, String destinationName, String paymentMethod, String reservationsUrl) {
        String safeName = extractFirstName(null, name);
        String safeDest = (destinationName != null && !destinationName.isBlank()) ? destinationName.trim() : "seu destino";
        String safeMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod.trim() : "Confirmado";
        String safeUrl = (reservationsUrl != null && !reservationsUrl.isBlank()) ? reservationsUrl.trim() : "http://localhost:4200/profile";

        String contentHtml =
                "<div style=\"margin-bottom: 16px;\">" +
                "  <span style=\"display: inline-block; background-color: #f0fdf4; color: #166534; font-size: 12px; font-weight: 700; letter-spacing: 0.5px; padding: 6px 14px; border-radius: 8px; text-transform: uppercase;\">Pagamento Confirmado</span>" +
                "</div>" +
                "<h1 style=\"margin: 0 0 14px 0; font-size: 26px; font-weight: 800; color: " + COLOR_DARK + "; letter-spacing: -0.5px;\">Tudo pronto para sua viagem!</h1>" +
                "<div style=\"font-size: 17px; font-weight: 600; color: " + COLOR_DARK + "; margin-bottom: 14px;\">Olá, " + safeName + "!</div>" +
                "<p style=\"margin: 0 0 24px 0; font-size: 15px; color: #334155; line-height: 1.65;\">" +
                "  O pagamento da sua reserva foi confirmado com sucesso. Seus bilhetes de embarque oficiais já foram emitidos e estão liberados para consulta e download na sua conta Horizon." +
                "</p>" +
                "<div style=\"background-color: #f0f7fc; border-radius: 14px; border: 1.5px solid rgba(91, 164, 216, 0.25); padding: 20px 22px; margin-bottom: 26px;\">" +
                "  <div style=\"margin-bottom: 12px;\">" +
                "    <span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 700; display: block; margin-bottom: 2px;\">Destino</span>" +
                "    <span style=\"font-size: 17px; font-weight: 800; color: " + COLOR_DARK + ";\">" + safeDest + "</span>" +
                "  </div>" +
                "  <div style=\"margin-bottom: 12px;\">" +
                "    <span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 700; display: block; margin-bottom: 2px;\">Código da Reserva</span>" +
                "    <span style=\"font-size: 15px; font-weight: 600; color: #334155;\">#" + reservationId + "</span>" +
                "  </div>" +
                "  <div style=\"margin-bottom: 12px;\">" +
                "    <span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 700; display: block; margin-bottom: 2px;\">Forma de Pagamento</span>" +
                "    <span style=\"font-size: 15px; font-weight: 600; color: #334155;\">" + safeMethod + "</span>" +
                "  </div>" +
                "  <div>" +
                "    <span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 700; display: block; margin-bottom: 2px;\">Status da Reserva</span>" +
                "    <span style=\"font-size: 15px; font-weight: 700; color: " + COLOR_GREEN + ";\">Confirmada</span>" +
                "  </div>" +
                "</div>" +
                "<div align=\"center\" style=\"padding-bottom: 26px;\">" +
                "  <a href=\"" + safeUrl + "\" target=\"_blank\" style=\"display: inline-block; background-color: " + COLOR_BLUE + "; color: #ffffff; padding: 14px 34px; font-size: 15px; font-weight: 700; text-decoration: none; border-radius: 10px; box-shadow: 0 4px 14px rgba(91, 164, 216, 0.35);\">Visualizar Meus Bilhetes</a>" +
                "</div>" +
                "<div style=\"background-color: #f8fafc; border-radius: 10px; padding: 16px 20px; margin-bottom: 24px;\">" +
                "  <p style=\"margin: 0 0 6px 0; font-size: 13px; font-weight: 700; color: #334155;\">Orientações de Embarque</p>" +
                "  <p style=\"margin: 0; font-size: 13px; color: #64748b; line-height: 1.5;\">Apresente seu cartão de embarque digital ou impresso diretamente no portão do aeroporto. Chegue com antecedência mínima de 2 horas para voos domésticos e 3 horas para voos internacionais.</p>" +
                "</div>" +
                "<div style=\"font-size: 15px; color: " + COLOR_DARK + "; line-height: 1.5; padding-bottom: 10px;\">" +
                "  Um abraço,<br>" +
                "  <strong>Equipe Horizon</strong>" +
                "</div>";

        return buildStandardLayout("Horizon - Pagamento Confirmado", contentHtml);
    }

    /**
     * E-mail para cancelamento de reserva.
     */
    public static String buildReservationCancellationEmail(String name, Long reservationId, String destinationName, String reason, String destinationsUrl) {
        String safeName = extractFirstName(null, name);
        String safeDest = (destinationName != null && !destinationName.isBlank()) ? destinationName.trim() : "seu destino";
        String safeReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Cancelamento solicitado pelo usuário.";
        String safeUrl = (destinationsUrl != null && !destinationsUrl.isBlank()) ? destinationsUrl.trim() : "http://localhost:4200/destinations";

        String contentHtml =
                "<div style=\"margin-bottom: 16px;\">" +
                "  <span style=\"display: inline-block; background-color: #fef2f2; color: #991b1b; font-size: 12px; font-weight: 700; letter-spacing: 0.5px; padding: 6px 14px; border-radius: 8px; text-transform: uppercase;\">Reserva Cancelada</span>" +
                "</div>" +
                "<h1 style=\"margin: 0 0 14px 0; font-size: 26px; font-weight: 800; color: " + COLOR_DARK + "; letter-spacing: -0.5px;\">Cancelamento Confirmado</h1>" +
                "<div style=\"font-size: 17px; font-weight: 600; color: " + COLOR_DARK + "; margin-bottom: 14px;\">Olá, " + safeName + "!</div>" +
                "<p style=\"margin: 0 0 20px 0; font-size: 15px; color: #334155; line-height: 1.65;\">" +
                "  Confirmamos o cancelamento da sua reserva <strong>#" + reservationId + "</strong> para <strong>" + safeDest + "</strong>." +
                "</p>" +
                "<div style=\"background-color: #f8fafc; border-radius: 12px; padding: 18px 20px; margin-bottom: 24px;\">" +
                "  <span style=\"font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 700; display: block; margin-bottom: 6px;\">Detalhes do Cancelamento</span>" +
                "  <span style=\"font-size: 14px; color: #334155; line-height: 1.5; display: block;\">" + safeReason + "</span>" +
                "</div>" +
                "<p style=\"margin: 0 0 26px 0; font-size: 14px; line-height: 1.6; color: #64748b;\">" +
                "  Os assentos foram liberados em nossa malha aérea. Quando estiver pronto para planejar sua próxima experiência de viagem, estaremos prontos para atender você." +
                "</p>" +
                "<div align=\"center\" style=\"padding-bottom: 26px;\">" +
                "  <a href=\"" + safeUrl + "\" target=\"_blank\" style=\"display: inline-block; background-color: " + COLOR_BLUE + "; color: #ffffff; padding: 14px 34px; font-size: 15px; font-weight: 700; text-decoration: none; border-radius: 10px; box-shadow: 0 4px 14px rgba(91, 164, 216, 0.35);\">Explorar Novos Destinos</a>" +
                "</div>" +
                "<div style=\"font-size: 15px; color: " + COLOR_DARK + "; line-height: 1.5; padding-bottom: 10px;\">" +
                "  Um abraço,<br>" +
                "  <strong>Equipe Horizon</strong>" +
                "</div>";

        return buildStandardLayout("Horizon - Cancelamento da Reserva", contentHtml);
    }

    /**
     * E-mail para avaliação de viagem e plataforma (NPS e CSAT).
     */
    public static String buildTripFeedbackInvitationEmail(String name, Long reservationId, String destinationName, String feedbackUrl) {
        String safeName = extractFirstName(null, name);
        String safeDest = (destinationName != null && !destinationName.isBlank()) ? destinationName.trim() : "seu destino";
        String safeUrl = (feedbackUrl != null && !feedbackUrl.isBlank()) ? feedbackUrl.trim() : "http://localhost:4200/profile";

        String contentHtml =
                "<div style=\"margin-bottom: 16px;\">" +
                "  <span style=\"display: inline-block; background-color: #f0fdf4; color: #166534; font-size: 12px; font-weight: 700; letter-spacing: 0.5px; padding: 6px 14px; border-radius: 8px; text-transform: uppercase;\">Avaliação de Viagem</span>" +
                "</div>" +
                "<h1 style=\"margin: 0 0 14px 0; font-size: 26px; font-weight: 800; color: " + COLOR_DARK + "; letter-spacing: -0.5px;\">Como foi sua experiência?</h1>" +
                "<div style=\"font-size: 17px; font-weight: 600; color: " + COLOR_DARK + "; margin-bottom: 14px;\">Olá, " + safeName + "!</div>" +
                "<p style=\"margin: 0 0 24px 0; font-size: 15px; color: #334155; line-height: 1.65;\">" +
                "  Esperamos que sua viagem para <strong>" + safeDest + "</strong> tenha sido inesquecível! Na Horizon, cada recomendação é construída para combinar com seu perfil. Saber a sua opinião é essencial para calibrarmos cada vez melhor suas próximas descobertas." +
                "</p>" +
                "<div align=\"center\" style=\"padding-bottom: 24px;\">" +
                "  <a href=\"" + safeUrl + "\" target=\"_blank\" style=\"display: inline-block; background-color: " + COLOR_GREEN + "; color: #ffffff; padding: 14px 34px; font-size: 15px; font-weight: 700; text-decoration: none; border-radius: 10px; box-shadow: 0 4px 14px rgba(101, 163, 13, 0.35);\">Avaliar Minha Experiência</a>" +
                "</div>" +
                "<p style=\"margin: 0 0 24px 0; font-size: 13px; color: #64748b; text-align: center; line-height: 1.5;\">" +
                "  A avaliação leva menos de 2 minutos e nos ajuda a transformar o turismo personalizado." +
                "</p>" +
                "<div style=\"font-size: 15px; color: " + COLOR_DARK + "; line-height: 1.5; padding-bottom: 10px;\">" +
                "  Um abraço,<br>" +
                "  <strong>Equipe Horizon</strong>" +
                "</div>";

        return buildStandardLayout("Horizon - Avalie sua Viagem", contentHtml);
    }

    /**
     * E-mail executivo consolidando o relatório de NPS e CSAT (Batch a cada 3 dias).
     */
    public static String buildWeeklyFeedbackReportEmail(String periodStr, long totalResponses,
                                                        long npsTotal, double npsScore, long promoters, long passives, long detractors,
                                                        long csatTotal, double csatMedia, double csatPct,
                                                        long platCsatTotal, double platCsatMedia, long platNpsTotal, double platNpsScore,
                                                        long tripCsatTotal, double tripCsatMedia, long tripNpsTotal, double tripNpsScore,
                                                        List<String> comments) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1 style=\"margin: 0 0 6px 0; font-size: 26px; font-weight: 800; color: ").append(COLOR_DARK).append("; letter-spacing: -0.5px;\">Síntese de Avaliações (NPS e CSAT)</h1>");
        sb.append("<div style=\"font-size: 13px; color: #64748b; margin-bottom: 24px;\">").append(periodStr).append("</div>");

        // KPI Box
        sb.append("<div style=\"background-color: #f8fafc; border-radius: 12px; padding: 20px 22px; margin-bottom: 24px;\">");
        sb.append("  <p style=\"margin: 0 0 16px 0; font-size: 13px; font-weight: 700; color: #334155; text-transform: uppercase;\">Resumo Geral Consolidado</p>");
        sb.append("  <div style=\"margin-bottom: 12px;\">");
        sb.append("    <span style=\"font-size: 13px; color: #64748b;\">Total de Respostas Captadas: </span>");
        sb.append("    <strong style=\"font-size: 15px; color: ").append(COLOR_DARK).append(";\">").append(totalResponses).append("</strong>");
        sb.append("  </div>");

        if (npsTotal > 0) {
            String sign = npsScore > 0 ? "+" : "";
            sb.append("  <div style=\"margin-bottom: 12px;\">");
            sb.append("    <span style=\"font-size: 13px; color: #64748b;\">NPS Geral: </span>");
            sb.append("    <strong style=\"font-size: 16px; color: ").append(COLOR_BLUE).append(";\">")
              .append(sign).append(String.format("%.1f%%", npsScore)).append("</strong>");
            sb.append("    <span style=\"font-size: 12px; color: #94a3b8; display: block; margin-top: 2px;\">")
              .append("Promotores: ").append(promoters).append(" | Neutros: ").append(passives).append(" | Detratores: ").append(detractors).append("</span>");
            sb.append("  </div>");
        }

        if (csatTotal > 0) {
            sb.append("  <div>");
            sb.append("    <span style=\"font-size: 13px; color: #64748b;\">CSAT Médio (Satisfação): </span>");
            sb.append("    <strong style=\"font-size: 16px; color: ").append(COLOR_GREEN).append(";\">")
              .append(String.format("%.2f / 5.00", csatMedia)).append("</strong>");
            sb.append("    <span style=\"font-size: 12px; color: #94a3b8; display: block; margin-top: 2px;\">")
              .append(String.format("%.1f%% dos respondentes satisfeitos (notas 4 ou 5)", csatPct)).append("</span>");
            sb.append("  </div>");
        }

        if (totalResponses == 0) {
            sb.append("  <p style=\"margin: 4px 0 0 0; font-size: 13px; color: #64748b;\">Nenhuma avaliação foi registrada pelos usuários neste período.</p>");
        }
        sb.append("</div>");

        // Detalhamento por canal
        sb.append("<div style=\"background-color: #f0f7fc; border-radius: 12px; padding: 20px 22px; margin-bottom: 24px;\">");
        sb.append("  <p style=\"margin: 0 0 14px 0; font-size: 13px; font-weight: 700; color: #334155; text-transform: uppercase;\">Detalhamento por Canal</p>");

        sb.append("  <div style=\"margin-bottom: 16px; background-color: #ffffff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04);\">");
        sb.append("    <strong style=\"font-size: 14px; color: ").append(COLOR_DARK).append("; display: block; margin-bottom: 6px;\">Plataforma Horizon</strong>");
        if (platCsatTotal > 0) {
            sb.append("    <div style=\"font-size: 13px; color: #475569;\">CSAT: ").append(String.format("%.2f / 5.00 (%d avaliações)", platCsatMedia, platCsatTotal)).append("</div>");
        }
        if (platNpsTotal > 0) {
            String sign = platNpsScore > 0 ? "+" : "";
            sb.append("    <div style=\"font-size: 13px; color: #475569; margin-top: 2px;\">NPS: ").append(sign).append(String.format("%.1f%% (%d avaliações)", platNpsScore, platNpsTotal)).append("</div>");
        }
        if (platCsatTotal == 0 && platNpsTotal == 0) {
            sb.append("    <div style=\"font-size: 13px; color: #94a3b8;\">Nenhuma avaliação no período.</div>");
        }
        sb.append("  </div>");

        sb.append("  <div style=\"background-color: #ffffff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04);\">");
        sb.append("    <strong style=\"font-size: 14px; color: ").append(COLOR_DARK).append("; display: block; margin-bottom: 6px;\">Viagens Realizadas</strong>");
        if (tripCsatTotal > 0) {
            sb.append("    <div style=\"font-size: 13px; color: #475569;\">CSAT: ").append(String.format("%.2f / 5.00 (%d avaliações)", tripCsatMedia, tripCsatTotal)).append("</div>");
        }
        if (tripNpsTotal > 0) {
            String sign = tripNpsScore > 0 ? "+" : "";
            sb.append("    <div style=\"font-size: 13px; color: #475569; margin-top: 2px;\">NPS: ").append(sign).append(String.format("%.1f%% (%d avaliações)", tripNpsScore, tripNpsTotal)).append("</div>");
        }
        if (tripCsatTotal == 0 && tripNpsTotal == 0) {
            sb.append("    <div style=\"font-size: 13px; color: #94a3b8;\">Nenhuma avaliação no período.</div>");
        }
        sb.append("  </div>");
        sb.append("</div>");

        // Comentários
        if (comments != null && !comments.isEmpty()) {
            sb.append("<div style=\"margin-bottom: 24px;\">");
            sb.append("  <p style=\"margin: 0 0 12px 0; font-size: 13px; font-weight: 700; color: #334155; text-transform: uppercase;\">Comentários dos Viajantes</p>");
            for (String comment : comments) {
                sb.append("  <div style=\"background-color: #f8fafc; border-radius: 8px; padding: 14px 16px; margin-bottom: 10px;\">");
                sb.append("    <p style=\"margin: 0; font-size: 13px; color: #334155; line-height: 1.5; font-style: italic;\">\"").append(comment).append("\"</p>");
                sb.append("  </div>");
            }
            sb.append("</div>");
        }

        sb.append("<div style=\"font-size: 15px; color: ").append(COLOR_DARK).append("; line-height: 1.5; padding-bottom: 10px;\">");
        sb.append("  Um abraço,<br>");
        sb.append("  <strong>Equipe de Operações Horizon</strong>");
        sb.append("</div>");

        return buildStandardLayout("Horizon - Síntese de Avaliações", sb.toString());
    }

    /**
     * E-mail de confirmação de abertura de chamado de atendimento (Suporte Horizon).
     * Informa o número do protocolo/ticket (ex: HZ-2026-01), o assunto selecionado,
     * o tempo de SLA para retorno de um atendente humano e as orientações para acompanhamento.
     * Segue estritamente as diretrizes da Horizon: sem pills, sem divisores e sem setas.
     */
    public static String buildSupportTicketEmail(String name, String ticketCode, String subject, String slaDescription, String description) {
        String safeName = extractFirstName(null, name);
        String safeTicket = (ticketCode != null && !ticketCode.isBlank()) ? ticketCode.trim() : "HZ-2026-01";
        String safeSubject = (subject != null && !subject.isBlank()) ? subject.trim() : "Atendimento Geral";
        String safeSla = (slaDescription != null && !slaDescription.isBlank()) ? slaDescription.trim() : "até 24 horas úteis";
        String safeDesc = (description != null) ? description.trim() : "";
        if (safeDesc.length() > 300) {
            safeDesc = safeDesc.substring(0, 297) + "...";
        }

        StringBuilder content = new StringBuilder();
        content.append("<h1 style=\"margin: 0 0 14px 0; font-size: 26px; font-weight: 800; color: ").append(COLOR_DARK).append("; letter-spacing: -0.5px;\">Chamado registrado com sucesso</h1>");
        content.append("<div style=\"font-size: 17px; font-weight: 600; color: ").append(COLOR_DARK).append("; margin-bottom: 14px;\">Olá, ").append(safeName).append("!</div>");
        content.append("<p style=\"margin: 0 0 24px 0; font-size: 15px; color: #334155; line-height: 1.65;\">");
        content.append("Recebemos sua mensagem em nossa Central de Atendimento. Um atendente humano entrará em contato diretamente por este e-mail dentro do prazo previsto para o seu assunto.");
        content.append("</p>");

        // Destaque tipográfico do Protocolo e SLA (sem caixas transparentes/IA Slop)
        content.append("<div style=\"margin: 28px 0 20px 0;\">");
        content.append("  <span style=\"font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 1.2px; display: block; margin-bottom: 4px;\">Protocolo de Atendimento</span>");
        content.append("  <span style=\"font-size: 30px; font-weight: 800; color: ").append(COLOR_BLUE).append("; letter-spacing: 0.5px; line-height: 1.2; display: block;\">").append(safeTicket).append("</span>");
        content.append("</div>");

        content.append("<div style=\"margin-bottom: 24px; font-size: 15px; line-height: 1.85;\">");
        content.append("  <div><span style=\"color: #64748b;\">Assunto:</span> <strong style=\"color: ").append(COLOR_DARK).append("; margin-left: 4px;\">").append(safeSubject).append("</strong></div>");
        content.append("  <div><span style=\"color: #64748b;\">Previsão de retorno:</span> <strong style=\"color: ").append(COLOR_GREEN).append("; margin-left: 4px;\">").append(safeSla).append("</strong></div>");
        content.append("  <div><span style=\"color: #64748b;\">Status:</span> <strong style=\"color: ").append(COLOR_DARK).append("; margin-left: 4px;\">Em atendimento</strong></div>");
        content.append("</div>");

        if (!safeDesc.isEmpty()) {
            content.append("<div style=\"margin: 24px 0;\">");
            content.append("  <span style=\"font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 1px; display: block; margin-bottom: 6px;\">Mensagem enviada:</span>");
            content.append("  <p style=\"margin: 0; font-size: 14px; color: #475569; line-height: 1.6; font-style: italic;\">\"").append(safeDesc).append("\"</p>");
            content.append("</div>");
        }

        content.append("<p style=\"margin: 0 0 20px 0; font-size: 14px; color: #334155; line-height: 1.6;\">");
        content.append("Você pode acompanhar todos os seus chamados a qualquer momento na seção <strong>Meus Chamados</strong> em seu perfil no Horizon.<br><br>");
        content.append("Guarde o número do seu protocolo <strong>").append(safeTicket).append("</strong> para futuras consultas.");
        content.append("</p>");

        content.append("<div style=\"font-size: 15px; color: ").append(COLOR_DARK).append("; line-height: 1.5; padding-bottom: 10px;\">");
        content.append("  Um abraço,<br>");
        content.append("  <strong>Equipe de Atendimento Horizon</strong>");
        content.append("</div>");

        return buildStandardLayout("Horizon - Chamado Aberto (" + safeTicket + ")", content.toString());
    }
}

