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
        return String.format(
                "\n" +
                        "   ╔════════════════════════════════════════╗\n" +
                        "   ║       CARACTÉRISTIQUES DU VISAGE       ║\n" +
                        "   ╠══════════════════════════╦═════════════╣\n" +
                        "   ║  Largeur Visage          ║  %4d px   ║\n" +
                        "   ║  Hauteur Visage          ║  %4d px   ║\n" +
                        "   ║  Distance Yeux           ║  %4d px   ║\n" +
                        "   ║  Largeur Bouche          ║  %4d px   ║\n" +
                        "   ║  Hauteur Bouche          ║  %4d px   ║\n" +
                        "   ╚══════════════════════════╩═════════════╝",
                faceWidth, faceHeight, eyeDistance, mouthWidth, mouthHeight);
    }
}
