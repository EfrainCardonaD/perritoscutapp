package com.cut.cardona.modelo.usuarios;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tokens_reset")
@Getter
@Setter
@NoArgsConstructor
public class ResetToken {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String token;

    @Column(name = "expira_en", nullable = false)
    private Timestamp expiraEn;

    @Column(name = "usado_en")
    private Timestamp usadoEn;

    @Column(name = "creado_en")
    private Timestamp creadoEn;

    @PrePersist
    private void prePersist() {
        if (this.id == null || this.id.isBlank()) this.id = UUID.randomUUID().toString();
        if (this.creadoEn == null) this.creadoEn = new Timestamp(System.currentTimeMillis());
    }

    public boolean isUsado() { return this.usadoEn != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResetToken that = (ResetToken) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

