package mg.dgi.fiscaltrack.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contribuables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContribuableEntity {

    @Id
    @Column(name = "nif", length = 10)
    private String nif;

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Column(name = "forme_juridique")
    private String formeJuridique;

    @Column(name = "nom_dirigeant")
    private String nomDirigeant;

    @Column(name = "prenom_dirigeant")
    private String prenomDirigeant;

    @Column(name = "cin_dirigeant")
    private String cinDirigeant;

    @Column(name = "email_contribuable")
    private String emailContribuable;

    @Column(name = "telephone_contribuable")
    private String telephoneContribuable;

    @Column(name = "adresse_contribuable")
    private String adresseContribuable;

    @Column(name = "commune_contribuable")
    private String communeContribuable;

    @Column(name = "mot_de_passe_hash_contribuable", nullable = false, columnDefinition = "TEXT")
    private String motDePasseHashContribuable;

    @Column(name = "regime_imposition")
    private String regimeImposition;

    @Column(name = "obligation_comptable")
    private String obligationComptable;

    @Column(name = "statut_activite", nullable = false)
    private String statutActivite;

    @Column(name = "date_immatriculation", nullable = false)
    private LocalDate dateImmatriculation;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
