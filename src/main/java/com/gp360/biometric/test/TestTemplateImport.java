package com.gp360.biometric.test;

import com.digitalpersona.uareu.*;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;

public class TestTemplateImport {
    public static void main(String[] args) {
        try {
            // Inicializar
            Engine engine = UareUGlobal.GetEngine();
            System.out.println("Motor inicializado");

            // Leer template de archivo
            String testFile = "template_inmate6_right_thumb.dat";
            byte[] templateData = Files.readAllBytes(Paths.get(testFile));
            System.out.println("\nTemplate leído: " + testFile);
            System.out.println("Tamaño: " + templateData.length + " bytes");

            // Mostrar primeros bytes
            System.out.print("Primeros 20 bytes (hex): ");
            for (int i = 0; i < Math.min(20, templateData.length); i++) {
                System.out.printf("%02X ", templateData[i]);
            }
            System.out.println();

            // Verificar header
            String header = new String(templateData, 0, 3);
            System.out.println("Header: " + header);

            // Intentar importar de diferentes formas
            System.out.println("\n=== Pruebas de importación ===");

            // Prueba 1: ImportFmd directo
            try {
                System.out.println("\n1. ImportFmd con ANSI_378_2004:");
                Fmd fmd1 = UareUGlobal.GetImporter().ImportFmd(
                    templateData,
                    Fmd.Format.ANSI_378_2004,
                    Fmd.Format.ANSI_378_2004
                );
                System.out.println("  ✓ ÉXITO - FMD importado");
                System.out.println("  Formato: " + fmd1.getFormat());
                System.out.println("  Tamaño FMD: " + fmd1.getData().length);
            } catch (UareUException e) {
                System.out.println("  ✗ ERROR: " + e.getMessage());
                System.out.println("  Código: " + e.getCode());
            }

            // Prueba 2: ImportFmd manteniendo formato
            try {
                System.out.println("\n2. ImportFmd sin conversión:");
                Fmd fmd2 = UareUGlobal.GetImporter().ImportFmd(
                    templateData,
                    Fmd.Format.ANSI_378_2004,
                    Fmd.Format.ANSI_378_2004  // Mantener el mismo formato
                );
                System.out.println("  ✓ ÉXITO - FMD importado");
                System.out.println("  Formato: " + fmd2.getFormat());

                // Intentar obtener información del FMD
                System.out.println("  Tamaño datos FMD: " + fmd2.getData().length);
                System.out.println("  Ancho: " + fmd2.getWidth());
                System.out.println("  Alto: " + fmd2.getHeight());
                System.out.println("  Resolución: " + fmd2.getResolution() + " DPI");
            } catch (UareUException e) {
                System.out.println("  ✗ ERROR: " + e.getMessage());
                System.out.println("  Código error: " + e.getCode());
            }

            // Prueba 3: ImportFmd desde Base64
            try {
                System.out.println("\n3. ImportFmd desde Base64:");
                // Leer el template en base64 del archivo de texto
                String base64Template = new String(Files.readAllBytes(Paths.get("test_template.txt")));
                byte[] decodedTemplate = Base64.getDecoder().decode(base64Template);

                Fmd fmd3 = UareUGlobal.GetImporter().ImportFmd(
                    decodedTemplate,
                    Fmd.Format.ANSI_378_2004,
                    Fmd.Format.ANSI_378_2004
                );
                System.out.println("  ✓ ÉXITO - FMD importado desde Base64");
            } catch (Exception e) {
                System.out.println("  ✗ ERROR: " + e.getMessage());
            }

            // Prueba 4: Comparación entre dos templates
            try {
                System.out.println("\n4. Comparación de templates:");

                // Cargar dos templates
                byte[] template1 = Files.readAllBytes(Paths.get("template_inmate6_right_thumb.dat"));
                byte[] template2 = Files.readAllBytes(Paths.get("template_inmate6_right_index.dat"));

                Fmd fmd1 = UareUGlobal.GetImporter().ImportFmd(
                    template1, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);
                Fmd fmd2 = UareUGlobal.GetImporter().ImportFmd(
                    template2, Fmd.Format.ANSI_378_2004, Fmd.Format.ANSI_378_2004);

                int score = engine.Compare(fmd1, 0, fmd2, 0);
                System.out.println("  Score de comparación: " + score);
                System.out.println("  (Menor = más similar, típicamente < 50000 para match)");

                // Comparar el mismo template consigo mismo
                int selfScore = engine.Compare(fmd1, 0, fmd1, 0);
                System.out.println("  Score consigo mismo: " + selfScore);

            } catch (Exception e) {
                System.out.println("  ✗ ERROR en comparación: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error general: " + e.getMessage());
            e.printStackTrace();
        }
    }
}