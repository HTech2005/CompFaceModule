import { useNavigate } from "react-router-dom";
import "./CDV.css";
// Import image safely
import cvBackground from "../assets/cv-background.jpg";

export default function CDV() {
    const navigate = useNavigate();

    return (
        <div className="cdv-container">

            {/* Zone gauche — Description */}
            <div className="cdv-left">
                <h1 className="cdv-title">Comparer Deux Visages (CDV)</h1>

                <h2>Découvrez la Similarité entre Deux Visages</h2>
                <p>
                    Comparez facilement deux visages grâce à une interface simple, intuitive
                    et visuellement agréable. Importez deux photos et obtenez instantanément
                    une estimation visuelle de leur ressemblance.
                </p>

                <h3>👉 Comment l'utiliser ?</h3>
                <ul>
                    <li>Importez deux images de visages.</li>
                    <li>Cliquez sur <strong>Comparer</strong>.</li>
                    <li>Observez la similarité et les traits mis en avant.</li>
                </ul>
            </div>

            {/* Zone droite — Image */}
            <div className="cdv-right">
                <div className="cdv-image" style={{ backgroundImage: `url(${cvBackground})` }}>
                    <button className="cdv-start" onClick={() => navigate("/cdv-compare")}>Commencer</button>
                </div>
            </div>

            {/* Bouton Retour */}
            <button className="cdv-back" onClick={() => navigate(-1)}>
                Retour
            </button>

        </div>
    );
}
