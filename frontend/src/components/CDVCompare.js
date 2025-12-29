import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./CDVCompare.css";

export default function CDV() {
  const navigate = useNavigate();
  const [img1, setImg1] = useState(null);
  const [img2, setImg2] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCompare = async () => {
    if (!img1 || !img2) {
      alert("Veuillez sélectionner deux images.");
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append("image1", img1);
    formData.append("image2", img2);

    try {
      const response = await fetch("http://localhost:4567/api/compare", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        if (data.error) {
          alert(data.error);
        } else {
          setResult(data);
        }
      } else {
        alert("Erreur serveur.");
      }
    } catch (error) {
      console.error(error);
      alert("Erreur réseau.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="cdv-container">
      <button className="cdv-back-btn" onClick={() => navigate(-1)}>← Retour</button>

      <div className="cdv-content">

        {/* LEFT: Controls */}
        <div className="cdv-controls">
          <h1 className="cdv-title">Comparaison Faciale</h1>

          <div className="cdv-uploads">
            {/* Upload Box 1 */}
            <div className="cdv-upload-box">
              {img1 ? (
                <img src={URL.createObjectURL(img1)} alt="1" className="cdv-preview-thumb" />
              ) : (
                <div className="cdv-upload-placeholder">
                  <span>📷<br />Visage 1</span>
                </div>
              )}
              <input type="file" onChange={(e) => setImg1(e.target.files[0])} className="cdv-file-input" accept="image/*" />
            </div>

            {/* Upload Box 2 */}
            <div className="cdv-upload-box">
              {img2 ? (
                <img src={URL.createObjectURL(img2)} alt="2" className="cdv-preview-thumb" />
              ) : (
                <div className="cdv-upload-placeholder">
                  <span>📷<br />Visage 2</span>
                </div>
              )}
              <input type="file" onChange={(e) => setImg2(e.target.files[0])} className="cdv-file-input" accept="image/*" />
            </div>
          </div>

          <button className="cdv-compare-btn" onClick={handleCompare} disabled={loading || !img1 || !img2}>
            {loading ? "Calcul en cours..." : "LANCER COMPARAISON"}
          </button>
        </div>

        {/* RIGHT: Results */}
        <div className="cdv-results">
          {!result ? (
            <div style={{ color: '#555', fontSize: '1.5rem', fontWeight: 'bold' }}>
              En attente de résultat...
            </div>
          ) : (
            <div className="cdv-result-card">
              <span className={`cdv-verdict ${result.match ? "match" : "no-match"}`}>
                {result.match ? "COMPATIBLE" : "NON COMPATIBLE"}
              </span>

              <div className="cdv-scores">
                <div className="score-item">
                  <span className="score-value" style={{ color: '#00ff88' }}>
                    {result.scoreChi2.toFixed(1)}%
                  </span>
                  <span className="score-label">Taux Chi-Carré</span>
                </div>
                <div className="score-item">
                  <span className="score-value" style={{ color: '#00c6ff' }}>
                    {result.scoreEuclidien.toFixed(1)}%
                  </span>
                  <span className="score-label">Taux Euclidien</span>
                </div>
                <div className="score-item">
                  <span className="score-value" style={{ color: '#a29bfe' }}>
                    {result.scoreCosinus.toFixed(1)}%
                  </span>
                  <span className="score-label">Similitude Cosinus</span>
                </div>
                <div className="score-item global">
                  <span className="score-value" style={{ color: '#ffcc00' }}>
                    {(result.scoreGlobal).toFixed(1)}%
                  </span>
                  <span className="score-label">Score Global</span>
                </div>
              </div>

              {/* NEW: Detected Faces Section */}
              <div className="cdv-detected-faces">
                <h4 className="detected-title">Visages Isolés (Preuve Détection)</h4>
                <div className="faces-row">
                  <div className="face-item">
                    <img src={`data:image/jpeg;base64,${result.face1}`} alt="Face 1" className="detected-face-img" />
                    <span className="face-tag">Face 1</span>
                  </div>
                  <div className="face-item">
                    <img src={`data:image/jpeg;base64,${result.face2}`} alt="Face 2" className="detected-face-img" />
                    <span className="face-tag">Face 2</span>
                  </div>
                </div>
              </div>

            </div>
          )}
        </div>

      </div>
    </div>
  );
}
