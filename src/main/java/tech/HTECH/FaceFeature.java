package tech.HTECH;

public class FaceFeature {
    public int faceWidth;
    public int faceHeight;

    public int eyeDistance;

    public int mouthWidth;
    public int mouthHeight;

    public FaceFeature(int faceWidth, int faceHeight, int eyeDistance, int mouthWidth, int mouthHeight) {
        this.faceWidth = faceWidth;
        this.faceHeight = faceHeight;
        this.eyeDistance = eyeDistance;
        this.mouthWidth = mouthWidth;
        this.mouthHeight = mouthHeight;
    }

    @Override
    public String toString() {
        return "FaceFeatures{" +
                "faceWidth=" + faceWidth +
                ", faceHeight=" + faceHeight +
                ", eyeDistance=" + eyeDistance +
                ", mouthWidth=" + mouthWidth +
                ", mouthHeight=" + mouthHeight +
                '}';
    }
}

