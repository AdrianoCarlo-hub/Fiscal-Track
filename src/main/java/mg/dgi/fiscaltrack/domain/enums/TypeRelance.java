package mg.dgi.fiscaltrack.domain.enums;

/**
 * Types de notification. Les quatre premiers codes (J-7, J-1, J+1, J+8)
 * sont ceux des relances automatiques d'echeance ; les suivants couvrent
 * les evenements du cycle de vie (declaration, paiement, penalite, amende)
 * et les messages manuels.
 */
public enum TypeRelance {
    J_MOINS_7("J-7"),
    J_MOINS_1("J-1"),
    J_PLUS_1("J+1"),
    J_PLUS_8("J+8"),
    DECLARATION_VALIDEE("DECLARATION_VALIDEE"),
    DECLARATION_REJETEE("DECLARATION_REJETEE"),
    PAIEMENT_RECU("PAIEMENT_RECU"),
    PENALITE_APPLIQUEE("PENALITE_APPLIQUEE"),
    AMENDE_APPLIQUEE("AMENDE_APPLIQUEE"),
    MESSAGE_LIBRE("MESSAGE_LIBRE");

    private final String code;

    TypeRelance(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** Un avis legal est toujours publie sur le portail. */
    public boolean estAvisLegal() {
        return this == J_PLUS_1 || this == J_PLUS_8
                || this == PENALITE_APPLIQUEE || this == AMENDE_APPLIQUEE;
    }

    public static TypeRelance fromCode(String code) {
        for (TypeRelance t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Code inconnu : " + code);
    }
}
