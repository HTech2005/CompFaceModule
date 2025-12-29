package tech.HTECH;

public class FaceFeature {
    // Basic metrics
    public int faceWidth;
    public int faceHeight;
    public int eyeDistance;
    public int mouthWidth;
    public int mouthHeight;
    public int noseWidth; // New to fix redundancy

    // Coordinates for drawing (x, y, w, h)
    public int[] faceRect;
    public int[] eyeLeftRect;
    public int[] eyeRightRect;
    public int[] mouthRect;
    public int[] noseRect;

    public FaceFeature(int faceWidth, int faceHeight, int eyeDistance, int mouthWidth, int mouthHeight) {
        this.faceWidth = faceWidth;
        this.faceHeight = faceHeight;
        this.eyeDistance = eyeDistance;
        this.mouthWidth = mouthWidth;
        this.mouthHeight = mouthHeight;
    }

    @Override
    public String toString() {
        return String.format(
                "Face: %dx%d, EyeDist: %d, Mouth: %dx%d",
                faceWidth, faceHeight, eyeDistance, mouthWidth, mouthHeight);
    }
}
