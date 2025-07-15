package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;
import java.util.UUID;

/**
 * ✅ Utilitaires pour conversion ULID ↔ UUID
 */
public class UlidHelper {

    /**
     * Convertit un String ID vers UUID (gère ULID, UUID, hex)
     */
    public static UUID stringToUuid(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            return null;
        }

        idString = idString.trim();

        try {
            // Format ULID standard (26 caractères)
            if (idString.length() == 26 && idString.matches("[0-9A-Z]+")) {
                Ulid ulid = Ulid.from(idString);
                return ulid.toUuid();
            }

            // Format UUID avec tirets (36 caractères)
            if (idString.length() == 36 && idString.contains("-")) {
                return UUID.fromString(idString);
            }

            // Format Hex sans tirets (32 caractères)
            if (idString.length() == 32 && idString.matches("[0-9A-Fa-f]+")) {
                String formatted = idString.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }

            throw new IllegalArgumentException("Format ID non reconnu: " + idString);

        } catch (Exception e) {
            System.err.println("❌ Erreur conversion ID: " + idString + " - " + e.getMessage());
            throw new IllegalArgumentException("ID invalide: " + idString, e);
        }
    }

    /**
     * Convertit un UUID vers String ULID pour affichage
     */
    public static String uuidToUlidString(UUID uuid) {
        if (uuid == null) return null;
        try {
            Ulid ulid = Ulid.from(uuid);
            return ulid.toString();
        } catch (Exception e) {
            return uuid.toString();
        }
    }
}
