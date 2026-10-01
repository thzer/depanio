package tn.iset.gabes.helpdesk.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Enveloppe de pagination renvoyee par l'API.
 * <p>
 * Elle expose uniquement les informations utiles au frontend, au lieu de
 * serialiser directement l'objet {@code Page} de Spring Data dont la
 * structure interne peut changer d'une version a l'autre.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Convertit une page d'entites en page de DTO.
     *
     * @param page   page issue du repository
     * @param mapper fonction de transformation entite vers DTO
     */
    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
