package ejemplo1;

/**
 * Algoritmo 10: III.5 ENHANCED PARALLEL BLOCK (Row × Column)
 * Complejidad: O(n³/2) — divide el rango de filas en 2 mitades y las
 * ejecuta en 2 hilos simultáneos (equivalente a Parallel.Do de C#).
 */
public class EnhancedParallelBlockRC {

    private static final int BLOCK_SIZE = 64;

    public static long[][] run(long[][] B, long[][] C) throws InterruptedException {
        int n = B.length; long[][] A = new long[n][n]; int bs = BLOCK_SIZE; int half = n / 2;
        Thread t1 = new Thread(() -> blockRC(A, B, C, 0,    half, n, bs));
        Thread t2 = new Thread(() -> blockRC(A, B, C, half, n,    n, bs));
        t1.start(); t2.start(); t1.join(); t2.join();
        return A;
    }

    private static void blockRC(long[][] A, long[][] B, long[][] C,
                                int iStart, int iEnd, int n, int bs) {
        for (int i1 = iStart; i1 < iEnd; i1 += bs)
            for (int j1 = 0; j1 < n; j1 += bs)
                for (int k1 = 0; k1 < n; k1 += bs)
                    for (int i = i1, iLim = Math.min(i1+bs,iEnd); i < iLim; i++)
                        for (int j = j1, jLim = Math.min(j1+bs,n); j < jLim; j++)
                            for (int k = k1, kLim = Math.min(k1+bs,n); k < kLim; k++)
                                A[i][j] += B[i][k] * C[k][j];
    }
}
