import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./CVAnalysis.css";

export default function CV() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState(null);
  const [features, setFeatures] = useState(null);
  const [loading, setLoading] = useState(false);
  const [imageSize, setImageSize] = useState({ width: 0, height: 0 });
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });

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
          setImageSize({ width: data.imageWidth, height: data.imageHeight });
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

  const renderBox = (rect, color, label) => {
    if (!rect || !imageSize.width) return null;

    // Calcul du ratio d'affichage (simplifié, on suppose que l'image remplit le conteneur via contain)
    // Pour être précis, il faudrait mesurer l'élément img réel
    const imgElement = document.querySelector(".cv-preview-img");
    if (!imgElement) return null;

    const displayW = imgElement.clientWidth;
    const displayH = imgElement.clientHeight;
    const ratioX = displayW / imageSize.width;
    const ratioY = displayH / imageSize.height;

    // Offset si contain centre l'image
    const rectStyle = {
      position: "absolute",
      left: rect[0] * ratioX,
      top: rect[1] * ratioY,
      width: rect[2] * ratioX,
      height: rect[3] * ratioY,
      border: `2px solid ${color}`,
      boxShadow: `0 0 10px ${color}`,
      zIndex: 10,
      pointerEvents: "none"
    };

    return (
      <div style={rectStyle} key={label}>
        <span style={{
          position: "absolute",
          top: "-20px",
          left: "0",
          backgroundColor: color,
          color: "white",
          fontSize: "10px",
          padding: "2px 5px",
          borderRadius: "3px",
          fontWeight: "bold"
        }}>{label}</span>
      </div>
    );
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
              <span className="cv-card-value">{features.faceWidth}x{features.faceHeight}</span>
              <span className="cv-card-label">Visage (px)</span>
            </div>
            <div className="cv-card">
              <span className="cv-card-value">{features.eyeDistance}px</span>
              <span className="cv-card-label">Distance Yeux</span>
            </div>
            <div className="cv-card">
              <span className="cv-card-value">{features.noseWidth}px</span>
              <span className="cv-card-label">Largeur Nez</span>
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
          <div className="cv-image-container" style={{ position: "relative", display: "inline-block" }}>
            <img
              src={URL.createObjectURL(selectedFile)}
              alt="Preview"
              className="cv-preview-img"
              onLoad={(e) => {
                // Force re-render once image is loaded to calc boxes
                setContainerSize({ width: e.target.clientWidth, height: e.target.clientHeight });
              }}
            />
            {features && (
              <>
                {renderBox(features.faceRect, "#007bff", "VISAGE")}
                {renderBox(features.eyeLeftRect, "#ff4e4e", "OEIL G")}
                {renderBox(features.eyeRightRect, "#ff4e4e", "OEIL D")}
                {renderBox(features.noseRect, "#00ff88", "NEZ")}
                {renderBox(features.mouthRect, "#ffcc00", "BOUCHE")}
              </>
            )}
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
