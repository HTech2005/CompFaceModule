import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./SVAnalysis.css";

export default function SVAnalysis() {
    const navigate = useNavigate();
    const videoRef = useRef(null);
    const canvasRef = useRef(null);
    const [isStreaming, setIsStreaming] = useState(false);
    const [features, setFeatures] = useState(null);
    const [error, setError] = useState(null);

    // Start Webcam
    const startCamera = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true });
            if (videoRef.current) {
                videoRef.current.srcObject = stream;
                setIsStreaming(true);
                setError(null);
            }
        } catch (err) {
            console.error("Error accessing webcam:", err);
            setError("Impossible d'accéder à la caméra.");
        }
    };

    // Stop Webcam
    const stopCamera = () => {
        if (videoRef.current && videoRef.current.srcObject) {
            const tracks = videoRef.current.srcObject.getTracks();
            tracks.forEach((track) => track.stop());
            videoRef.current.srcObject = null;
            setIsStreaming(false);
        }
    };

    // Analysis Loop
    useEffect(() => {
        let intervalId;

        if (isStreaming) {
            intervalId = setInterval(async () => {
                if (!videoRef.current || !canvasRef.current) return;

                const video = videoRef.current;
                const canvas = canvasRef.current;
                const ctx = canvas.getContext("2d");

                canvas.width = video.videoWidth;
                canvas.height = video.videoHeight;
                ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

                canvas.toBlob(async (blob) => {
                    if (!blob) return;
                    const formData = new FormData();
                    formData.append("image", blob, "capture.jpg");

                    try {
                        const response = await fetch("http://localhost:4567/api/analyze", {
                            method: "POST",
                            body: formData,
                        });

                        if (response.ok) {
                            const data = await response.json();
                            if (data.found) {
                                setFeatures(data.features);
                            }
                        }
                    } catch (err) {
                        console.error("API Error", err);
                    }
                }, "image/jpeg");

            }, 1000);
        }

        return () => clearInterval(intervalId);
    }, [isStreaming]);

    // Cleanup
    useEffect(() => {
        return () => stopCamera();
    }, []);

    return (
        <div className="sv-analysis-container">
            <button className="sv-back-btn" onClick={() => navigate(-1)}>← Retour</button>

            <div className="sv-content">

                {/* Left: Video Panel (Similar to TR now) */}
                <div className="sv-video-panel">
                    <h1 className="sv-title" style={{ color: '#00d2ff' }}>Scanner Visage & Caractéristiques</h1>
                    <div className="sv-video-frame">
                        <video ref={videoRef} autoPlay playsInline muted className="sv-video-feed" />
                        {!isStreaming && <div className="video-placeholder">Caméra désactivée</div>}
                    </div>

                    <div className="sv-controls">
                        {!isStreaming ? (
                            <button className="sv-btn start" onClick={startCamera}>Démarrer Analyse</button>
                        ) : (
                            <button className="sv-btn stop" onClick={stopCamera}>Arrêter</button>
                        )}
                    </div>
                </div>

                {/* Right: Results Panel (Similar to TR now) */}
                <div className="sv-result-panel">
                    <h2>Métriques Détectées</h2>

                    {features && isStreaming ? (
                        <div className="sv-metrics-card">
                            <div className="sv-metric-row">
                                <span className="label">Largeur Visage</span>
                                <span className="value">{features.faceWidth} px</span>
                                <div className="bar-bg"><div className="bar-fill" style={{ width: `${Math.min(features.faceWidth / 3, 100)}%` }}></div></div>
                            </div>

                            <div className="sv-metric-row">
                                <span className="label">Hauteur Visage</span>
                                <span className="value">{features.faceHeight} px</span>
                                <div className="bar-bg"><div className="bar-fill" style={{ width: `${Math.min(features.faceHeight / 3, 100)}%` }}></div></div>
                            </div>

                            <div className="sv-metric-row">
                                <span className="label">Distance Yeux</span>
                                <span className="value">{features.eyeDistance} px</span>
                                <div className="bar-bg"><div className="bar-fill" style={{ width: `${Math.min(features.eyeDistance / 1.5, 100)}%` }}></div></div>
                            </div>

                            <div className="sv-metric-grid">
                                <div className="mini-card">
                                    <span>Bouche L</span>
                                    <strong>{features.mouthWidth}</strong>
                                </div>
                                <div className="mini-card">
                                    <span>Bouche H</span>
                                    <strong>{features.mouthHeight}</strong>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="sv-waiting">
                            {isStreaming ? "Scan en cours..." : "En attente..."}
                        </div>
                    )}
                </div>

            </div>

            <canvas ref={canvasRef} style={{ display: 'none' }} />
        </div>
    );
}
