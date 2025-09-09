package com.cut.cardona.event;

import com.cut.cardona.modelo.dto.perros.DtoPerro;
import com.cut.cardona.modelo.perros.Perro;
import com.cut.cardona.modelo.perros.RepositorioPerro;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

@Component
@RequiredArgsConstructor
public class PerroCatalogoEventListener {

    private static final Logger log = LoggerFactory.getLogger(PerroCatalogoEventListener.class);
    private final RepositorioPerro repositorioPerro;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @Transactional(readOnly = true)
    public void onNuevo(PerroCatalogoNuevoEvent ev) {
        repositorioPerro.findById(ev.perroId()).ifPresentOrElse(perro -> {
            try {
                // Forzar inicialización de imágenes si es LAZY
                perro.getImagenes().size();
                DtoPerro dto = new DtoPerro(perro);
                messagingTemplate.convertAndSend("/topic/catalogo/nuevo", dto);
            } catch (Exception e) {
                log.error("Error enviando perro nuevo al catálogo", e);
            }
        }, () -> log.warn("Perro no encontrado para evento nuevo: {}", ev.perroId()));
    }

    @EventListener
    public void onRemove(PerroCatalogoRemoveEvent ev) {
        messagingTemplate.convertAndSend("/topic/catalogo/remove", ev.perroId());
    }
}

