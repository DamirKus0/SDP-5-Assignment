package kz.sdp.delivery.domain;

/** Shared validation for numeric domain values. */
public final class Checks {
    private Checks() { }

    public static double nonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and nonnegative");
        }
        return value;
    }

    public static String nonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
