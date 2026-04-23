package ejemplo1;

import java.util.*;
import java.io.*;

/**
 * Multiplicación de Matrices Grandes - Universidad del Quindío
 * Orquesta los 15 algoritmos de multiplicación de matrices.
 *
 * Arquitectura objetivo: Mac M2 (ARM64)
 * Compilar: javac com/ejemplo/*.java
 * Ejecutar:  java com.ejemplo.Main [n1] [n2]
 *   Ejemplo: java com.ejemplo.Main 256 512
 */
public class Main {

    static final String DIR_SALIDA = "resultados/";

    // =================== GENERACIÓN Y PERSISTENCIA ===================

    /** Genera una matriz n×n con valores aleatorios de 6 dígitos (100000–999999). */
    public static long[][] generarMatriz(int n, long semilla) {
        Random rand = new Random(semilla);
        long[][] m = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                m[i][j] = 100000L + (long) rand.nextInt(900000);
        return m;
    }

    /** Persiste una matriz en CSV (primera línea: n). */
    public static void guardarMatriz(long[][] m, String ruta) throws IOException {
        new File(ruta).getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(ruta)))) {
            int n = m.length;
            pw.println(n);
            for (int i = 0; i < n; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < n; j++) {
                    if (j > 0) sb.append(',');
                    sb.append(m[i][j]);
                }
                pw.println(sb);
            }
        }
    }

    /** Carga una matriz desde CSV. */
    public static long[][] cargarMatriz(String ruta) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
            int n = Integer.parseInt(br.readLine().trim());
            long[][] m = new long[n][n];
            for (int i = 0; i < n; i++) {
                String[] p = br.readLine().split(",");
                for (int j = 0; j < n; j++)
                    m[i][j] = Long.parseLong(p[j]);
            }
            return m;
        }
    }

    /** Guarda los tiempos de ejecución en CSV persistente. */
    public static void guardarTiempos(List<String[]> filas, String ruta) throws IOException {
        new File(ruta).getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(ruta)))) {
            pw.println("Caso,n,Algoritmo,Tiempo_ms");
            for (String[] f : filas) pw.println(String.join(",", f));
        }
    }

    // =================== INFRAESTRUCTURA DE MEDICIÓN ===================

    @FunctionalInterface
    interface Algoritmo { long[][] run() throws Exception; }

    static long[][] resultado; // evita que el JIT optimice la llamada

    static long medir(Algoritmo alg) throws Exception {
        System.gc();
        long inicio = System.currentTimeMillis();
        resultado = alg.run();
        return System.currentTimeMillis() - inicio;
    }

    // =================== MAIN ===================

    public static void main(String[] args) throws Exception {
        int n1 = (args.length > 0) ? Integer.parseInt(args[0]) : 256;
        int n2 = (args.length > 1) ? Integer.parseInt(args[1]) : n1 * 2;
        int[] tamanos = {n1, n2};

        System.out.println("=== Multiplicación de Matrices Grandes ===");
        System.out.println("Arquitectura: Mac M2 (ARM64 / Apple Silicon)");
        System.out.printf ("Casos: n1=%d  n2=%d%n", n1, n2);
        System.out.println("Directorio de salida: " + DIR_SALIDA);

        new File(DIR_SALIDA).mkdirs();
        List<String[]> tiempos = new ArrayList<>();

        String[] nombres = {
            "1-NaivOnArray", "2-NaivLoopUnrollingTwo", "3-NaivLoopUnrollingFour",
            "4-WinogradOriginal", "5-WinogradScaled",
            "6-StrassenNaiv", "7-StrassenWinograd",
            "8-III3-SeqBlockRC", "9-III4-ParallelBlockRC", "10-III5-EnhParallelBlockRC",
            "11-IV3-SeqBlockRR", "12-IV4-ParallelBlockRR", "13-IV5-EnhParallelBlockRR",
            "14-V3-SeqBlockCC", "15-V4-ParallelBlockCC"
        };

        for (int caso = 0; caso < tamanos.length; caso++) {
            int n = tamanos[caso];
            System.out.printf("%n=== CASO %d  (n=%d) ===%n", caso + 1, n);

            String rutaB = DIR_SALIDA + "caso" + (caso+1) + "_B_n" + n + ".csv";
            String rutaC = DIR_SALIDA + "caso" + (caso+1) + "_C_n" + n + ".csv";
            long[][] B, C;
            if (new File(rutaB).exists() && new File(rutaC).exists()) {
                System.out.println("  Cargando matrices desde disco...");
                B = cargarMatriz(rutaB);
                C = cargarMatriz(rutaC);
            } else {
                System.out.println("  Generando matrices aleatorias (6 dígitos mínimo)...");
                B = generarMatriz(n, (caso + 1) * 7919L);
                C = generarMatriz(n, (caso + 1) * 6271L);
                guardarMatriz(B, rutaB);
                guardarMatriz(C, rutaC);
                System.out.println("  Matrices guardadas en disco.");
            }

            // Los 15 algoritmos delegando a sus clases respectivas
            Algoritmo[] algoritmos = {
                () -> NaivOnArray.run(B, C),
                () -> NaivLoopUnrollingTwo.run(B, C),
                () -> NaivLoopUnrollingFour.run(B, C),
                () -> WinogradOriginal.run(B, C),
                () -> WinogradScaled.run(B, C),
                () -> StrassenNaiv.run(B, C),
                () -> StrassenWinograd.run(B, C),
                () -> SeqBlockRC.run(B, C),
                () -> ParallelBlockRC.run(B, C),
                () -> EnhancedParallelBlockRC.run(B, C),
                () -> SeqBlockRR.run(B, C),
                () -> ParallelBlockRR.run(B, C),
                () -> EnhancedParallelBlockRR.run(B, C),
                () -> SeqBlockCC.run(B, C),
                () -> ParallelBlockCC.run(B, C)
            };

            System.out.println();
            for (int i = 0; i < algoritmos.length; i++) {
                String nombre = nombres[i];
                try {
                    System.out.printf("  %-38s", nombre + "...");
                    long ms = medir(algoritmos[i]);
                    System.out.printf("%6d ms%n", ms);
                    tiempos.add(new String[]{
                        "Caso" + (caso + 1), String.valueOf(n), nombre, String.valueOf(ms)
                    });
                } catch (Exception e) {
                    System.out.println(" ERROR: " + e.getMessage());
                }
            }
        }

        String rutaTiempos = DIR_SALIDA + "tiempos_ejecucion.csv";
        guardarTiempos(tiempos, rutaTiempos);
        System.out.println("\nTiempos guardados en: " + rutaTiempos);
        System.out.println("\n=== Fin de la ejecución ===");
    }
}
