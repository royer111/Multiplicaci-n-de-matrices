package ejemplo1;

/** Métodos auxiliares compartidos por múltiples algoritmos. */
public class MatrizHelper {

    public static long[][] addM(long[][] A, long[][] B) {
        int n = A.length; long[][] C = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) C[i][j] = A[i][j] + B[i][j];
        return C;
    }

    public static long[][] subM(long[][] A, long[][] B) {
        int n = A.length; long[][] C = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    public static long[][] getSub(long[][] M, int r, int c, int sz) {
        long[][] S = new long[sz][sz];
        for (int i = 0; i < sz; i++) System.arraycopy(M[r + i], c, S[i], 0, sz);
        return S;
    }

    public static void setSub(long[][] M, long[][] S, int r, int c) {
        for (int i = 0; i < S.length; i++) System.arraycopy(S[i], 0, M[r + i], c, S.length);
    }

    public static int nextP2(int n) { int p = 1; while (p < n) p <<= 1; return p; }

    public static long[][] pad(long[][] M, int p) {
        if (M.length == p) return M;
        long[][] P = new long[p][p];
        for (int i = 0; i < M.length; i++) System.arraycopy(M[i], 0, P[i], 0, M.length);
        return P;
    }

    public static long[][] unpad(long[][] M, int n) {
        if (M.length == n) return M;
        long[][] R = new long[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(M[i], 0, R[i], 0, n);
        return R;
    }
}
