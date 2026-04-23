package ejemplo1;

/**
 * Algoritmo 6: STRASSEN NAIV
 * Complejidad: O(n^2.807) — divide y vencerás con 7 multiplicaciones
 * recursivas en lugar de 8. Caso base: NaivOnArray.
 * Rellena a potencia de 2 para simplificar la recursión.
 */
public class StrassenNaiv {

    private static final int STRASSEN_THRESHOLD = 64;

    public static long[][] run(long[][] A, long[][] B) {
        int n = A.length, p = MatrizHelper.nextP2(n);
        return MatrizHelper.unpad(rec(MatrizHelper.pad(A, p), MatrizHelper.pad(B, p), p), n);
    }

    private static long[][] rec(long[][] A, long[][] B, int n) {
        if (n <= STRASSEN_THRESHOLD) return NaivOnArray.run(A, B);
        int h = n >> 1;
        long[][] A11 = MatrizHelper.getSub(A,0,0,h), A12 = MatrizHelper.getSub(A,0,h,h);
        long[][] A21 = MatrizHelper.getSub(A,h,0,h), A22 = MatrizHelper.getSub(A,h,h,h);
        long[][] B11 = MatrizHelper.getSub(B,0,0,h), B12 = MatrizHelper.getSub(B,0,h,h);
        long[][] B21 = MatrizHelper.getSub(B,h,0,h), B22 = MatrizHelper.getSub(B,h,h,h);

        long[][] M1 = rec(MatrizHelper.addM(A11,A22), MatrizHelper.addM(B11,B22), h);
        long[][] M2 = rec(MatrizHelper.addM(A21,A22), B11,                         h);
        long[][] M3 = rec(A11,                         MatrizHelper.subM(B12,B22), h);
        long[][] M4 = rec(A22,                         MatrizHelper.subM(B21,B11), h);
        long[][] M5 = rec(MatrizHelper.addM(A11,A12), B22,                         h);
        long[][] M6 = rec(MatrizHelper.subM(A21,A11), MatrizHelper.addM(B11,B12), h);
        long[][] M7 = rec(MatrizHelper.subM(A12,A22), MatrizHelper.addM(B21,B22), h);

        long[][] C = new long[n][n];
        MatrizHelper.setSub(C, MatrizHelper.addM(MatrizHelper.subM(MatrizHelper.addM(M1,M4),M5),M7), 0, 0);
        MatrizHelper.setSub(C, MatrizHelper.addM(M3,M5),                                              0, h);
        MatrizHelper.setSub(C, MatrizHelper.addM(M2,M4),                                              h, 0);
        MatrizHelper.setSub(C, MatrizHelper.addM(MatrizHelper.subM(MatrizHelper.addM(M1,M3),M2),M6), h, h);
        return C;
    }
}
