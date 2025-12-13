import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./CVAnalysis.css";

export default function CV() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState(null);
  const [features, setFeatures] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleFileChange = (event) => {
    if (event.target.files && event.target.files[0]) {
      setSelectedFile(event.target.files[0]);
      setFeatures(null); // Reset prev results
    }
  };

  const handleAnalyze = async () => {
    if (!selectedFile) return;

    setLoading(true);
    const formData = new FormData();
    formData.append("image", selectedFile);

    try {
      const response = await fetch("http://localhost:4567/api/analyze", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        if (data.found) {
          setFeatures(data.features);
        } else {
          alert("Erreur : Aucun visage détecté ! Assurez-vous que le visage est bien éclairé et de face.");
        }
      } else {
        alert("Erreur serveur.");
      }
    } catch (error) {
      console.error(error);
      alert("Impossible de joindre le serveur.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="cv-container">
      <button className="cv-back-btn" onClick={() => navigate(-1)}>← Retour</button>

      {/* LEFT PANEL: Controls & Analysis */}
      <div className="cv-left">
        <h1 className="cv-title">Analyse Faciale</h1>
        <p className="cv-description">
          Importez une image pour extraire instantanément les mesures biométriques clés :
          distance inter-pupillaire, dimensions, et proportions.
        </p>

        {/* Custom Upload Zone */}
        <div className="cv-upload-zone">
          <div className="cv-upload-icon">📂</div>
          <p>{selectedFile ? selectedFile.name : "Cliquez ou Glissez votre image ici"}</p>
          <input
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            className="cv-file-input"
          />
        </div>

        <button
          className="cv-analyze-btn"
          onClick={handleAnalyze}
          disabled={!selectedFile || loading}
        >
          {loading ? "Calcul en cours..." : "LANCER L'ANALYSE"}
        </button>

        {/* Results Grid */}
        {features && (
          <div className="cv-results-grid">
            <div className="cv-card">
              <span className="cv-card-value">{features.faceWidth}px</span>
              <span className="cv-card-label">Largeur</span>
            </div>
            <div className="cv-card">
              <span className="cv-card-value">{features.faceHeight}px</span>
              <span className="cv-card-label">Hauteur</span>
            </div>
            <div className="cv-card">
              <span className="cv-card-value">{features.eyeDistance}px</span>
              <span className="cv-card-label">Yeux</span>
            </div>
            <div className="cv-card">
              <span className="cv-card-value">{features.mouthWidth}px</span>
              <span className="cv-card-label">Bouche (L)</span>
            </div>
          </div>
        )}
      </div>

      {/* RIGHT PANEL: Preview */}
      <div className="cv-right">
        {selectedFile ? (
          <div className="cv-image-container">
            <img
              src={URL.createObjectURL(selectedFile)}
              alt="Preview"
              className="cv-preview-img"
            />
          </div>
        ) : (
          <div className="cv-placeholder">
            <span>Aperçu de l'image</span>
          </div>
        )}
      </div>
    </div>
  );
}
