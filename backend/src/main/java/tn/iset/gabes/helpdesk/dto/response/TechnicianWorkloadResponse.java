package tn.iset.gabes.helpdesk.dto.response;

/**
 * Charge de travail d'un technicien, utilisee par l'administrateur au
 * moment de choisir a qui affecter un nouveau ticket.
 */
public record TechnicianWorkloadResponse(
        Long technicianId,
        String fullName,
        String speciality,
        long openTickets,
        long breachedTickets
) {
}
