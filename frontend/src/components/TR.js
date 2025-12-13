import { useNavigate } from "react-router-dom";
import "./TR.css";

export default function TR() {
  const navigate = useNavigate();

  return (
    <div className="tr-container">

      {/* Bloc gauche — Description */}
      <div className="tr-left">
        <h1 className="tr-title">Temps Réel (TR)</h1>

        <p>
          Vivez une expérience interactive instantanée !
          Grâce à votre caméra, observez votre visage analysé en direct : une grille, des repères ou un cadre s’ajustent automatiquement à vos mouvements.
        </p>

        <p>
          Une démonstration immersive parfaite pour comprendre le principe de l’analyse “live”
          utilisée dans les technologies modernes.
        </p>

        <h3>👉 Comment l'utiliser ?</h3>

        <ul>
          <li>Activez votre caméra.</li>
          <li>Approchez-vous du cadre d’analyse.</li>
          <li>Bougez, souriez, tournez la tête et voyez la réaction en temps réel !</li>
        </ul>
      </div>

      {/* Bloc droit — Image ou vidéo d’aperçu */}
      <div className="tr-right">
        <div className="tr-image-overlay">
          <button className="tr-start-btn" onClick={() => navigate("/tr-recognition")}>Commencer</button>
        </div>
      </div>

      {/* Bouton Retour */}
      <button className="tr-back" onClick={() => navigate(-1)}>
        Retour
      </button>
    </div>
  );
}
