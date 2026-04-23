package ejemplo1;

import java.util.stream.IntStream;

/**
 * Algoritmo 12: IV.4 PARALLEL BLOCK (Row × Row)
 * Complejidad: O(n³/p)
 */
public class ParallelBlockRR {

    private static final int BLOCK_SIZE = 64;

    public static long[][] run(long[][] B, long[][] C) {
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
}
