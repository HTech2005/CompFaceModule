import { useNavigate } from "react-router-dom";
import "./CV.css";

export default function CV() {
    const navigate = useNavigate();

    return (
        <div className="cv-container">

            {/* Bloc gauche — Description */}
            <div className="cv-left">
                <h1 className="cv-title">Caractéristiques du Visage (CV)</h1>

                <p>
                    Importez une photo et obtenez instantanément une estimation visuelle de ses caractéristiques (yeux, bouche, dimensions).
                </p>

                <h3>👉 Comment l’utiliser ?</h3>
                <ul>
                    <li>Importez une image de visage.</li>
                    <li>Cliquez sur <strong>Analyser</strong>.</li>
                    <li>Observez les mesures biométriques détectées.</li>
                </ul>
            </div>

            {/* Bloc droit — Image + bouton commencer */}
            <div className="cv-right">
                <div className="cv-image-overlay">
                    <button className="cv-start-btn" onClick={() => navigate("/cv-analysis")}>Commencer</button>
                </div>
            </div>

            {/* Bouton Retour */}
            <button className="cv-back" onClick={() => navigate(-1)}>
                Retour
            </button>

        </div>
    );
}
