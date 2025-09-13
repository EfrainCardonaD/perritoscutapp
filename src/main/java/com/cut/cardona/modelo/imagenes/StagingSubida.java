package com.cut.cardona.modelo.imagenes;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "staging_subidas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StagingSubida {
    @Id
    @Column(name = "uuid", columnDefinition = "CHAR(36)")
    private String uuid;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo; // 'dog' o 'profile'

    @Column(name = "asociado", nullable = false)
    private boolean asociado;

    @Column(name = "info", length = 255)
    private String info;
}

