package ejemplo1;

/**
 * Algoritmo 14: V.3 SEQUENTIAL BLOCK (Column × Column)
 * Complejidad: O(n³) — acceso Column×Column: A[k][i] += B[k][j] * C[j][i]
 * Peor localidad de caché (acceso columna en C y A).
 */
public class SeqBlockCC {

    private static final int BLOCK_SIZE = 64;

    public static long[][] run(long[][] B, long[][] C) {
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
}
