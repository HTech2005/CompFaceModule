import { useNavigate } from "react-router-dom";
import "./SV.css";

export default function SV() {
  const navigate = useNavigate();

  return (
    <div className="sv-container">

      {/* Bloc Gauche — Description */}
      <div className="sv-left">
        <h1 className="sv-title">Scanner un Visage (SV)</h1>

        <p>
          Explorez une simulation de scan facial moderne !
          Chargez une photo ou activez votre webcam pour voir un scan animé qui détecte automatiquement le visage à l’écran.
        </p>

        <p>
          Ce module est parfait pour comprendre comment les systèmes analysent
          un visage : orientation, contours, zones clés… le tout de manière simple,
          fluide et pédagogique.
        </p>

        <h3>👉 Comment l'utiliser ?</h3>

        <ul>
          <li>Choisissez : <strong>Importer une photo</strong> ou <strong>Activer la caméra</strong>.</li>
          <li>Laissez le scan s’effectuer automatiquement.</li>
          <li>Découvrez les repères visuels détectés (contours, zones analysées, etc.).</li>
        </ul>
      </div>

      {/* Bloc Droit — Image + bouton commencer */}
      <div className="sv-right">
        <div className="sv-image-overlay">
          <button className="sv-start-btn" onClick={() => navigate("/sv-analysis")}>Commencer</button>
        </div>
      </div>

      {/* Bouton Retour */}
      <button className="sv-back" onClick={() => navigate(-1)}>
        Retour
      </button>
    </div>
  );
}
