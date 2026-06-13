package com.reggie.api_gateway.math;

public final class VectorMath {
    private VectorMath() {}

    public static float dot(float[] a, float[] b) {
        requireSameLength(a, b);
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public static float l2Squared(float[] a, float[] b) {
        requireSameLength(a, b);
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return sum;
    }

    public static float norm(float[] a) {
        float sum = 0.0f;
        for (float v : a) {
            sum += v * v;
        }
        return (float) Math.sqrt(sum);
    }

    public static float cosine(float[] a, float[] b) {
        requireSameLength(a, b);
        float denom = norm(a) * norm(b);
        if (denom == 0.0f) return 0.0f;
        return dot(a, b) / denom;
    }

    private static void requireSameLength(float[] a, float[] b) {
        if (a == null || b == null) throw new IllegalArgumentException("Vectors must not be null");
        if (a.length != b.length) throw new IllegalArgumentException("Vector lengths differ: " + a.length + " vs " + b.length);
        if (a.length == 0) throw new IllegalArgumentException("Vector length must be > 0");
    }
}
