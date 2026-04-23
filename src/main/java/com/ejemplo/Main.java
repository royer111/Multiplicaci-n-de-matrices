package com.ejemplo;

import java.util.*;
import java.io.*;
import java.util.stream.*;

/**
 * Multiplicación de Matrices Grandes - Universidad del Quindío
 * Implementación de 15 algoritmos de multiplicación de matrices.
 *
 * Arquitectura objetivo: Mac M2 (ARM64)
 * Compilar: javac MatrizMultiplicacion.java
 * Ejecutar:  java MatrizMultiplicacion [n1] [n2]
 *   Ejemplo: java MatrizMultiplicacion 256 512
 */
public class Main {

    // =================== CONFIGURACIÓN ===================
    static final int BLOCK_SIZE        = 64;   // Tamaño de bloque para caché (óptimo M2: L1=128KB)
    static final int STRASSEN_THRESHOLD = 64;  // Umbral recursivo para Strassen
    static final String DIR_SALIDA     = "resultados/";

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

    // =================== 1. NAIV ON ARRAY ===================
    // Complejidad: O(n³) — triple bucle directo sobre arreglos.

    public static long[][] naivOnArray(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++)
                    A[i][j] += B[i][k] * C[k][j];
        return A;
    }

    // =================== 2. NAIV LOOP UNROLLING TWO ===================
    // Complejidad: O(n³) — igual que Naiv pero el compilador/CPU aprovecha
    // mejor el pipeline al desdoblar el bucle interno de a 2.

    public static long[][] naivLoopUnrollingTwo(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = 0;
                int k = 0;
                for (; k <= n - 2; k += 2)
                    s += B[i][k] * C[k][j] + B[i][k + 1] * C[k + 1][j];
                if (k < n) s += B[i][k] * C[k][j];
                A[i][j] = s;
            }
        return A;
    }

    // =================== 3. NAIV LOOP UNROLLING FOUR ===================
    // Complejidad: O(n³) — desdoblamiento por 4 para mayor aprovechamiento
    // del pipeline de ejecución superescalar (M2 tiene 8 unidades ALU).

    public static long[][] naivLoopUnrollingFour(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = 0;
                int k = 0;
                for (; k <= n - 4; k += 4)
                    s += B[i][k]     * C[k][j]     + B[i][k + 1] * C[k + 1][j]
                            + B[i][k + 2] * C[k + 2][j] + B[i][k + 3] * C[k + 3][j];
                for (; k < n; k++) s += B[i][k] * C[k][j];
                A[i][j] = s;
            }
        return A;
    }

    // =================== 4. WINOGRAD ORIGINAL ===================
    // Complejidad: O(n³) pero con ~n³/2 multiplicaciones (la mitad).
    // Precalcula factores de fila y columna para reducir multiplicaciones
    // en el bucle principal.

    public static long[][] winogradOriginal(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        int half = n >> 1;
        long[] rf = new long[n], cf = new long[n];

        for (int i = 0; i < n; i++)
            for (int k = 0; k < half; k++)
                rf[i] += B[i][2 * k] * B[i][2 * k + 1];

        for (int j = 0; j < n; j++)
            for (int k = 0; k < half; k++)
                cf[j] += C[2 * k][j] * C[2 * k + 1][j];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = -rf[i] - cf[j];
                for (int k = 0; k < half; k++)
                    s += (B[i][2*k] + C[2*k+1][j]) * (B[i][2*k+1] + C[2*k][j]);
                if ((n & 1) == 1) s += B[i][n - 1] * C[n - 1][j];
                A[i][j] = s;
            }
        return A;
    }

    // =================== 5. WINOGRAD SCALED ===================
    // Complejidad: O(n³) — variante de Winograd con precómputo de factores
    // desdobladopor 2 para mayor rendimiento en el pipeline.

    public static long[][] winogradScaled(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        int half = n >> 1;
        long[] rf = new long[n], cf = new long[n];

        // Factores de fila con unrolling x2
        for (int i = 0; i < n; i++) {
            long r = 0; int k = 0;
            for (; k <= half - 2; k += 2)
                r += B[i][2*k] * B[i][2*k+1] + B[i][2*k+2] * B[i][2*k+3];
            for (; k < half; k++) r += B[i][2*k] * B[i][2*k+1];
            rf[i] = r;
        }

        // Factores de columna con unrolling x2
        for (int j = 0; j < n; j++) {
            long c = 0; int k = 0;
            for (; k <= half - 2; k += 2)
                c += C[2*k][j] * C[2*k+1][j] + C[2*k+2][j] * C[2*k+3][j];
            for (; k < half; k++) c += C[2*k][j] * C[2*k+1][j];
            cf[j] = c;
        }

        // Cómputo principal con unrolling x2
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = -rf[i] - cf[j];
                int k = 0;
                for (; k <= half - 2; k += 2) {
                    s += (B[i][2*k]   + C[2*k+1][j]) * (B[i][2*k+1] + C[2*k][j]);
                    s += (B[i][2*k+2] + C[2*k+3][j]) * (B[i][2*k+3] + C[2*k+2][j]);
                }
                for (; k < half; k++)
                    s += (B[i][2*k] + C[2*k+1][j]) * (B[i][2*k+1] + C[2*k][j]);
                if ((n & 1) == 1) s += B[i][n-1] * C[n-1][j];
                A[i][j] = s;
            }
        return A;
    }

    // =================== AUXILIARES STRASSEN ===================

    private static long[][] addM(long[][] A, long[][] B) {
        int n = A.length; long[][] C = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) C[i][j] = A[i][j] + B[i][j];
        return C;
    }

    private static long[][] subM(long[][] A, long[][] B) {
        int n = A.length; long[][] C = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    private static long[][] getSub(long[][] M, int r, int c, int sz) {
        long[][] S = new long[sz][sz];
        for (int i = 0; i < sz; i++) System.arraycopy(M[r + i], c, S[i], 0, sz);
        return S;
    }

    private static void setSub(long[][] M, long[][] S, int r, int c) {
        for (int i = 0; i < S.length; i++) System.arraycopy(S[i], 0, M[r + i], c, S.length);
    }

    private static int nextP2(int n) { int p = 1; while (p < n) p <<= 1; return p; }

    private static long[][] pad(long[][] M, int p) {
        if (M.length == p) return M;
        long[][] P = new long[p][p];
        for (int i = 0; i < M.length; i++) System.arraycopy(M[i], 0, P[i], 0, M.length);
        return P;
    }

    private static long[][] unpad(long[][] M, int n) {
        if (M.length == n) return M;
        long[][] R = new long[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(M[i], 0, R[i], 0, n);
        return R;
    }

    // =================== 6. STRASSEN NAIV ===================
    // Complejidad: O(n^2.807) — divide y vencerás con 7 multiplicaciones
    // recursivas en lugar de 8. Caso base: NaivOnArray.
    // Rellena a potencia de 2 para simplificar la recursión.

    public static long[][] strassenNaiv(long[][] A, long[][] B) {
        int n = A.length, p = nextP2(n);
        return unpad(strassenNaivRec(pad(A, p), pad(B, p), p), n);
    }

    private static long[][] strassenNaivRec(long[][] A, long[][] B, int n) {
        if (n <= STRASSEN_THRESHOLD) return naivOnArray(A, B);
        int h = n >> 1;
        long[][] A11 = getSub(A,0,0,h), A12 = getSub(A,0,h,h);
        long[][] A21 = getSub(A,h,0,h), A22 = getSub(A,h,h,h);
        long[][] B11 = getSub(B,0,0,h), B12 = getSub(B,0,h,h);
        long[][] B21 = getSub(B,h,0,h), B22 = getSub(B,h,h,h);

        // 7 productos de Strassen
        long[][] M1 = strassenNaivRec(addM(A11,A22), addM(B11,B22), h); // (A11+A22)(B11+B22)
        long[][] M2 = strassenNaivRec(addM(A21,A22), B11,            h); // (A21+A22)B11
        long[][] M3 = strassenNaivRec(A11,           subM(B12,B22),  h); // A11(B12-B22)
        long[][] M4 = strassenNaivRec(A22,           subM(B21,B11),  h); // A22(B21-B11)
        long[][] M5 = strassenNaivRec(addM(A11,A12), B22,            h); // (A11+A12)B22
        long[][] M6 = strassenNaivRec(subM(A21,A11), addM(B11,B12),  h); // (A21-A11)(B11+B12)
        long[][] M7 = strassenNaivRec(subM(A12,A22), addM(B21,B22),  h); // (A12-A22)(B21+B22)

        long[][] C = new long[n][n];
        setSub(C, addM(subM(addM(M1,M4),M5),M7), 0, 0); // C11 = M1+M4-M5+M7
        setSub(C, addM(M3,M5),                   0, h); // C12 = M3+M5
        setSub(C, addM(M2,M4),                   h, 0); // C21 = M2+M4
        setSub(C, addM(subM(addM(M1,M3),M2),M6), h, h); // C22 = M1-M2+M3+M6
        return C;
    }

    // =================== 7. STRASSEN WINOGRAD ===================
    // Complejidad: O(n^2.807) — misma estructura que StrassenNaiv pero el
    // caso base usa WinogradOriginal (menos multiplicaciones en hoja).

    public static long[][] strassenWinograd(long[][] A, long[][] B) {
        int n = A.length, p = nextP2(n);
        return unpad(strassenWinogradRec(pad(A, p), pad(B, p), p), n);
    }

    private static long[][] strassenWinogradRec(long[][] A, long[][] B, int n) {
        if (n <= STRASSEN_THRESHOLD) return winogradOriginal(A, B); // caso base: Winograd
        int h = n >> 1;
        long[][] A11 = getSub(A,0,0,h), A12 = getSub(A,0,h,h);
        long[][] A21 = getSub(A,h,0,h), A22 = getSub(A,h,h,h);
        long[][] B11 = getSub(B,0,0,h), B12 = getSub(B,0,h,h);
        long[][] B21 = getSub(B,h,0,h), B22 = getSub(B,h,h,h);

        long[][] M1 = strassenWinogradRec(addM(A11,A22), addM(B11,B22), h);
        long[][] M2 = strassenWinogradRec(addM(A21,A22), B11,            h);
        long[][] M3 = strassenWinogradRec(A11,           subM(B12,B22),  h);
        long[][] M4 = strassenWinogradRec(A22,           subM(B21,B11),  h);
        long[][] M5 = strassenWinogradRec(addM(A11,A12), B22,            h);
        long[][] M6 = strassenWinogradRec(subM(A21,A11), addM(B11,B12),  h);
        long[][] M7 = strassenWinogradRec(subM(A12,A22), addM(B21,B22),  h);

        long[][] C = new long[n][n];
        setSub(C, addM(subM(addM(M1,M4),M5),M7), 0, 0);
        setSub(C, addM(M3,M5),                   0, h);
        setSub(C, addM(M2,M4),                   h, 0);
        setSub(C, addM(subM(addM(M1,M3),M2),M6), h, h);
        return C;
    }

    // =================== 8. III.3 SEQUENTIAL BLOCK (Row × Column) ===================
    // Complejidad: O(n³) — mejora localidad de caché al procesar bloques
    // contiguos. A[i][j] += B[i][k] * C[k][j]

    public static long[][] seqBlockRC(long[][] B, long[][] C) {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE;
        for (int i1 = 0; i1 < n; i1 += bs)
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1, iLim = Math.min(i1+bs,n); i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][j] += B[i][k] * C[k][j];
        return A;
    }

    // =================== 9. III.4 PARALLEL BLOCK (Row × Column) ===================
    // Complejidad: O(n³/p) donde p = núcleos disponibles.
    // Paraleliza sobre bloques de filas (i1), sin conflictos de escritura.

    public static long[][] parallelBlockRC(long[][] B, long[][] C) {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE;
        IntStream.range(0, (n + bs - 1) / bs).parallel().forEach(bi -> {
            int i1 = bi * bs, iLim = Math.min(i1 + bs, n);
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1; i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][j] += B[i][k] * C[k][j];
        });
        return A;
    }

    // =================== 10. III.5 ENHANCED PARALLEL BLOCK (Row × Column) ===================
    // Complejidad: O(n³/2) — divide el rango de filas en 2 mitades y las
    // ejecuta en 2 hilos simultáneos (equivalente a Parallel.Do de C#).

    public static long[][] enhancedParallelBlockRC(long[][] B, long[][] C) throws InterruptedException {
            int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE; int half = n / 2;
            Runnable r1 = () -> blockRC(A, B, C, 0,    half, n, bs);
            Runnable r2 = () -> blockRC(A, B, C, half, n,    n, bs);
            Thread t1 = new Thread(r1), t2 = new Thread(r2);
            t1.start(); t2.start(); t1.join(); t2.join();
            return A;
    }



    private static void blockRC(long[][] A, long[][] B, long[][] C, int iStart, int iEnd, int n, int bs) {
        for (int i1 = iStart; i1 < iEnd; i1 += bs)
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1, iLim = Math.min(i1+bs,iEnd); i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][j] += B[i][k] * C[k][j];
    }

    // =================== 11. IV.3 SEQUENTIAL BLOCK (Row × Row) ===================
    // Complejidad: O(n³) — acceso Row×Row: A[i][k] += B[i][j] * C[j][k]
    // Mejor localidad en B (acceso fila) comparado con Row×Column.

    public static long[][] seqBlockRR(long[][] B, long[][] C) {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE;
        for (int i1 = 0; i1 < n; i1 += bs)
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1, iLim = Math.min(i1+bs,n); i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][k] += B[i][j] * C[j][k];
        return A;
    }

    // =================== 12. IV.4 PARALLEL BLOCK (Row × Row) ===================
    // Complejidad: O(n³/p)

    public static long[][] parallelBlockRR(long[][] B, long[][] C) {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE;
        IntStream.range(0, (n + bs - 1) / bs).parallel().forEach(bi -> {
            int i1 = bi * bs, iLim = Math.min(i1 + bs, n);
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1; i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][k] += B[i][j] * C[j][k];
        });
        return A;
    }

    // =================== 13. IV.5 ENHANCED PARALLEL BLOCK (Row × Row) ===================
    // Complejidad: O(n³/2) — 2 hilos sobre mitades del rango i.

    public static long[][] enhancedParallelBlockRR(long[][] B, long[][] C) throws InterruptedException {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE; int half = n / 2;
        Thread t1 = new Thread(() -> blockRR(A, B, C, 0,    half, n, bs));
        Thread t2 = new Thread(() -> blockRR(A, B, C, half, n,    n, bs));
        t1.start(); t2.start(); t1.join(); t2.join();
        return A;
    }

    private static void blockRR(long[][] A, long[][] B, long[][] C, int iStart, int iEnd, int n, int bs) {
        for (int i1 = iStart; i1 < iEnd; i1 += bs)
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1, iLim = Math.min(i1+bs,iEnd); i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][k] += B[i][j] * C[j][k];
    }

    // =================== 14. V.3 SEQUENTIAL BLOCK (Column × Column) ===================
    // Complejidad: O(n³) — acceso Column×Column: A[k][i] += B[k][j] * C[j][i]
    // Peor localidad de caché (acceso columna en C y A).

    public static long[][] seqBlockCC(long[][] B, long[][] C) {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE;
        for (int i1 = 0; i1 < n; i1 += bs)
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1, iLim = Math.min(i1+bs,n); i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[k][i] += B[k][j] * C[j][i];
        return A;
    }

    // =================== 15. V.4 PARALLEL BLOCK (Column × Column) ===================
    // Complejidad: O(n³/p) — paraleliza sobre bloques de columna (i1).
    // Hilos distintos acceden a columnas distintas de A → sin condición de carrera.

    public static long[][] parallelBlockCC(long[][] B, long[][] C) {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE;
        IntStream.range(0, (n + bs - 1) / bs).parallel().forEach(bi -> {
            int i1 = bi * bs, iLim = Math.min(i1 + bs, n);
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1; i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[k][i] += B[k][j] * C[j][i];
        });
        return A;
    }

    // =================== INFRAESTRUCTURA DE MEDICIÓN ===================

    @FunctionalInterface
    interface Algoritmo { long[][] run() throws Exception; }

    static long[][] resultado;  // evita que el JIT optimice la llamada

    static long medir(Algoritmo alg) throws Exception {
        System.gc();
        long inicio = System.currentTimeMillis();
        resultado = alg.run();
        return System.currentTimeMillis() - inicio;
    }

    // =================== MAIN ===================

    public static void main(String[] args) throws Exception {
        // Tamaños por defecto: n y 2n
        int n1 = (args.length > 0) ? Integer.parseInt(args[0]) : 4096;
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

            // Persistencia de matrices de entrada
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

            // Definir algoritmos en orden
            Algoritmo[] algoritmos = {
                    ()  -> naivOnArray(B, C),
                    ()  -> naivLoopUnrollingTwo(B, C),
                    ()  -> naivLoopUnrollingFour(B, C),
                    ()  -> winogradOriginal(B, C),
                    ()  -> winogradScaled(B, C),
                    ()  -> strassenNaiv(B, C),
                    ()  -> strassenWinograd(B, C),
                    ()  -> seqBlockRC(B, C),
                    ()  -> parallelBlockRC(B, C),
                    ()  -> enhancedParallelBlockRC(B, C),
                    ()  -> seqBlockRR(B, C),
                    ()  -> parallelBlockRR(B, C),
                    ()  -> enhancedParallelBlockRR(B, C),
                    ()  -> seqBlockCC(B, C),
                    ()  -> parallelBlockCC(B, C)
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

        // Guardar tiempos de forma persistente
        String rutaTiempos = DIR_SALIDA + "tiempos_ejecucion.csv";
        guardarTiempos(tiempos, rutaTiempos);
        System.out.println("\nTiempos guardados en: " + rutaTiempos);
        System.out.println("\n=== Fin de la ejecución ===");
    }
}