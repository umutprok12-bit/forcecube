package com.forcecube;

/**
 * Tiny quaternion helper for the cube's visual rotation. Quaternions are double[]{w, x, y, z}.
 * Because the cube is symmetric, any of the 24 "face-up" orientations looks identical, so the cube
 * can roll freely and then settle to the nearest one without a visible pop.
 */
public class CubeOrientation {
    private static final double[][] GROUP = buildGroup();

    public static double[] identity() {
        return new double[]{1.0D, 0.0D, 0.0D, 0.0D};
    }

    public static double[] axisAngle(double ax, double ay, double az, double angle) {
        double s = Math.sin(angle * 0.5D);
        return new double[]{Math.cos(angle * 0.5D), ax * s, ay * s, az * s};
    }

    /** Hamilton product a * b (apply b first, then a). */
    public static double[] mul(double[] a, double[] b) {
        double[] r = new double[]{
                a[0] * b[0] - a[1] * b[1] - a[2] * b[2] - a[3] * b[3],
                a[0] * b[1] + a[1] * b[0] + a[2] * b[3] - a[3] * b[2],
                a[0] * b[2] - a[1] * b[3] + a[2] * b[0] + a[3] * b[1],
                a[0] * b[3] + a[1] * b[2] - a[2] * b[1] + a[3] * b[0]
        };
        return normalize(r);
    }

    public static double[] normalize(double[] q) {
        double n = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (n < 1.0E-9D) return identity();
        return new double[]{q[0] / n, q[1] / n, q[2] / n, q[3] / n};
    }

    public static double[] nlerp(double[] a, double[] b, double t) {
        double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
        double sign = dot < 0.0D ? -1.0D : 1.0D;
        return normalize(new double[]{
                a[0] + (b[0] * sign - a[0]) * t,
                a[1] + (b[1] * sign - a[1]) * t,
                a[2] + (b[2] * sign - a[2]) * t,
                a[3] + (b[3] * sign - a[3]) * t
        });
    }

    /** The closest of the 24 axis-aligned orientations. */
    public static double[] nearest(double[] q) {
        double best = -1.0D;
        double[] bestQ = GROUP[0];
        for (double[] g : GROUP) {
            double d = Math.abs(g[0] * q[0] + g[1] * q[1] + g[2] * q[2] + g[3] * q[3]);
            if (d > best) {
                best = d;
                bestQ = g;
            }
        }
        return bestQ;
    }

    private static double[][] buildGroup() {
        int[][] perms = {{0, 1, 2}, {1, 2, 0}, {2, 0, 1}, {0, 2, 1}, {1, 0, 2}, {2, 1, 0}};
        int[] parity = {1, 1, 1, -1, -1, -1};
        double[][] out = new double[24][];
        int n = 0;
        for (int pi = 0; pi < 6; pi++) {
            for (int sMask = 0; sMask < 8; sMask++) {
                int s0 = (sMask & 1) == 0 ? 1 : -1;
                int s1 = (sMask & 2) == 0 ? 1 : -1;
                int s2 = (sMask & 4) == 0 ? 1 : -1;
                if (parity[pi] * s0 * s1 * s2 != 1) continue; // proper rotations only
                int[][] m = new int[3][3];
                m[0][perms[pi][0]] = s0;
                m[1][perms[pi][1]] = s1;
                m[2][perms[pi][2]] = s2;
                out[n++] = fromMatrix(m);
            }
        }
        return out;
    }

    private static double[] fromMatrix(int[][] m) {
        double tr = m[0][0] + m[1][1] + m[2][2];
        double w, x, y, z, s;
        if (tr > 0) {
            s = Math.sqrt(tr + 1.0D) * 2.0D;
            w = 0.25D * s;
            x = (m[2][1] - m[1][2]) / s;
            y = (m[0][2] - m[2][0]) / s;
            z = (m[1][0] - m[0][1]) / s;
        } else if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
            s = Math.sqrt(1.0D + m[0][0] - m[1][1] - m[2][2]) * 2.0D;
            w = (m[2][1] - m[1][2]) / s;
            x = 0.25D * s;
            y = (m[0][1] + m[1][0]) / s;
            z = (m[0][2] + m[2][0]) / s;
        } else if (m[1][1] > m[2][2]) {
            s = Math.sqrt(1.0D + m[1][1] - m[0][0] - m[2][2]) * 2.0D;
            w = (m[0][2] - m[2][0]) / s;
            x = (m[0][1] + m[1][0]) / s;
            y = 0.25D * s;
            z = (m[1][2] + m[2][1]) / s;
        } else {
            s = Math.sqrt(1.0D + m[2][2] - m[0][0] - m[1][1]) * 2.0D;
            w = (m[1][0] - m[0][1]) / s;
            x = (m[0][2] + m[2][0]) / s;
            y = (m[1][2] + m[2][1]) / s;
            z = 0.25D * s;
        }
        return normalize(new double[]{w, x, y, z});
    }
}
