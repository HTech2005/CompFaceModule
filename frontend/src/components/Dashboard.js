import { useNavigate } from "react-router-dom";
import './Dashboard.css';

export default function Dashboard() {
  const navigate = useNavigate();

  return (
    <div className="dashboard-container">

    

      {/* Zone droite — Cartes de gestion */}
      <div className="dashboard-right">
        <div className="dashboard-card">
          <h3>Visage Enregistré</h3>
          <p className="dashboard-count">0</p>
          <button className="dashboard-btn">Enregistrer</button>
        </div>

        <div className="dashboard-card">
          <h3>Visage Modifié</h3>
          <p className="dashboard-count">0</p>
          <button className="dashboard-btn">Modifier</button>
        </div>

        <div className="dashboard-card">
          <h3>Visage Supprimé</h3>
          <p className="dashboard-count">0</p>
          <button className="dashboard-btn">Supprimer</button>
        </div>
      </div>

      {/* Bouton Retour */}
      <button className="dashboard-back" onClick={() => navigate(-1)}>
        Retour
      </button>

    </div>
  );
}
