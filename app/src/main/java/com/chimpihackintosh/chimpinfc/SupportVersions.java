// No Android dependencies: this logic is exercised by ordinary JVM unit tests.
package com.chimpihackintosh.chimpinfc;

final class SupportVersions {
    private SupportVersions() {}
    static int[] parse(String raw) {
        if (raw == null || raw.length() > 100 || !raw.matches("v?[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?")) return null;
        String[] parts = raw.replaceFirst("^v", "").split("[-+]", 2)[0].split("\\.");
        try { return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])}; }
        catch (NumberFormatException error) { return null; }
    }
    static boolean older(String current, String minimum) {
        int[] left = parse(current), right = parse(minimum);
        if (left == null || right == null) return false;
        for (int i=0; i<3; i++) if (left[i] != right[i]) return left[i] < right[i];
        return false;
    }
    static boolean ended(long now) { return now >= 1793750400000L; } // 2026-11-04 00:00 UTC
}
