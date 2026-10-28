package br.com.horizon.horizon_api.domain.model;

import java.time.OffsetDateTime;

public class SupportTicket {

    private Long id;
    private String ticketCode;
    private Long userId;
    private String userName;
    private String userEmail;
    private String subject;
    private Integer slaHours;
    private String slaDescription;
    private String description;
    private String attachmentName;
    private Long attachmentSize;
    private String attachmentType;
    private String attachmentData;
    private String status; // "EM_ATENDIMENTO", "RESOLVIDO"
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public SupportTicket() {
    }

    public SupportTicket(Long id, String ticketCode, Long userId, String userName, String userEmail,
                         String subject, Integer slaHours, String slaDescription, String description,
                         String attachmentName, Long attachmentSize, String attachmentType, String attachmentData,
                         String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.ticketCode = ticketCode;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.subject = subject;
        this.slaHours = slaHours;
        this.slaDescription = slaDescription;
        this.description = description;
        this.attachmentName = attachmentName;
        this.attachmentSize = attachmentSize;
        this.attachmentType = attachmentType;
        this.attachmentData = attachmentData;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public String getSlaDescription() {
        return slaDescription;
    }

    public void setSlaDescription(String slaDescription) {
        this.slaDescription = slaDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentData() {
        return attachmentData;
    }

    public void setAttachmentData(String attachmentData) {
        this.attachmentData = attachmentData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
