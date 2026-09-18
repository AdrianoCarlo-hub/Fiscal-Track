package mg.dgi.fiscaltrack.domain.enums;

public enum TypeRelance {
    J_MOINS_7("J-7"),
    J_MOINS_1("J-1"),
    J_PLUS_1("J+1"),
    J_PLUS_8("J+8");

    private final String code;

    TypeRelance(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
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
