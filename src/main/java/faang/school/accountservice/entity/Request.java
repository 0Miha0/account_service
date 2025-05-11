package faang.school.accountservice.entity;

import faang.school.accountservice.enums.request.RequestStatus;
import faang.school.accountservice.enums.request.RequestType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {

    @Id
    @Column(name = "idempotent_token", nullable = false, unique = true)
    private UUID idempotentToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 64)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 64)
    private RequestStatus requestStatus;

    @Column(name = "context", length = 128)
    private String context;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RequestTask> requestTasks;
}
