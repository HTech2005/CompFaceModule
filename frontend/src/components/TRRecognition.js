import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./TRRecognition.css";

export default function TRRecognition() {
    const navigate = useNavigate();
    const videoRef = useRef(null);
    const canvasRef = useRef(null);
    const [isStreaming, setIsStreaming] = useState(false);
    const [searchResult, setSearchResult] = useState(null);
    const [error, setError] = useState(null);

    // Timer State
    const [timeLeft, setTimeLeft] = useState(10);
    const [isScanComplete, setIsScanComplete] = useState(false);
    const [isFaceDetected, setIsFaceDetected] = useState(false);

    // Start Webcam
    const startCamera = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: true });
            if (videoRef.current) {
                videoRef.current.srcObject = stream;
                setIsStreaming(true);
                setError(null);
                // Reset state
                setTimeLeft(10);
                setIsScanComplete(false);
                setIsFaceDetected(false);
                setSearchResult(null);
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
        }
        setIsStreaming(false); // Update state outside to ensure re-renders
    };

    // Analysis Loop
    useEffect(() => {
        let intervalId;

        if (isStreaming && !isScanComplete) {
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
                        const response = await fetch("http://localhost:4567/api/search", {
                            method: "POST",
                            body: formData,
                        });

                        if (response.ok) {
                            const data = await response.json();

                            if (data.found) {
                                setIsFaceDetected(true);
                                setSearchResult(data);

                                // Decrement timer only if face found
                                setTimeLeft(prev => {
                                    if (prev <= 1) {
                                        setIsScanComplete(true);
                                        stopCamera();
                                        return 0;
                                    }
                                    return prev - 1;
                                });
                            } else {
                                setIsFaceDetected(false);
                            }
                        }
                    } catch (err) {
                        console.error("API Error", err);
                    }
                }, "image/jpeg");

            }, 1000); // 1s interval matches countdown logic nicely
        }

        return () => clearInterval(intervalId);
    }, [isStreaming, isScanComplete]); // Depend on isScanComplete to stop loop

    // Cleanup
    useEffect(() => {
        return () => stopCamera();
    }, []);

    return (
        <div className="tr-recognition-container">
            <button className="tr-back-btn" onClick={() => navigate(-1)}>← Retour</button>

            <div className="tr-rec-content">

                {/* Left: Video */}
                <div className="tr-rec-video-panel">
                    <h1 className="tr-rec-title">Identification Temps Réel</h1>
                    <div className="tr-video-frame">
                        <video ref={videoRef} autoPlay playsInline muted className="tr-video-feed" />
                        {!isStreaming && <div className="video-placeholder">Caméra désactivée</div>}

                        {/* Visual Overlay Frame */}
                        {isStreaming && !isScanComplete && (
                            <div className={`scan-overlay ${isFaceDetected ? 'detected' : 'searching'}`}>
                                <div className="scan-corner top-left"></div>
                                <div className="scan-corner top-right"></div>
                                <div className="scan-corner bottom-left"></div>
                                <div className="scan-corner bottom-right"></div>
                                {isFaceDetected && <div className="scan-line"></div>}
                            </div>
                        )}
                    </div>

                    <div className="tr-controls">
                        {!isStreaming ? (
                            !isScanComplete ? (
                                <button className="tr-btn start" onClick={startCamera}>Démarrer Scan</button>
                            ) : (
                                <button className="tr-btn start" onClick={startCamera}>Nouveau Scan</button>
                            )
                        ) : (
                            <div className="tr-timer-display">
                                <span className="timer-label">Analyse en cours...</span>
                                <span className="timer-value">{timeLeft}s</span>
                                <button className="tr-btn stop-small" onClick={stopCamera}>Annuler</button>
                            </div>
                        )}
                    </div>
                </div>

                {/* Right: Results */}
                <div className="tr-rec-result-panel">
                    <h2>Résultat d'Identification</h2>

                    {isScanComplete && searchResult ? (
                        <div className={`tr-id-card ${searchResult.isMatch ? 'match' : 'no-match'} fade-in`}>
                            <div className="tr-id-avatar">
                                {searchResult.isMatch ? "👤" : "🚫"}
                            </div>
                            <div className="tr-id-info">
                                <h3>{searchResult.isMatch ? searchResult.bestMatch : "Inconnu"}</h3>
                                <div className="tr-score-bar">
                                    <div className="tr-score-fill" style={{ width: `${searchResult.score}%`, background: searchResult.isMatch ? '#ffcc00' : '#ff416c' }}></div>
                                </div>
                                <div className="tr-detailed-scores">
                                    <div className="tr-detail-item">
                                        <span className="detail-label">Global:</span>
                                        <span className="detail-value">{Math.round(searchResult.score)}%</span>
                                    </div>
                                    <div className="tr-detail-item">
                                        <span className="detail-label">Euclidien:</span>
                                        <span className="detail-value">{Math.round(searchResult.scoreEuclidien)}%</span>
                                    </div>
                                    <div className="tr-detail-item">
                                        <span className="detail-label">Cosinus:</span>
                                        <span className="detail-value">{Math.round(searchResult.scoreCosinus)}%</span>
                                    </div>
                                </div>

                                <div className="tr-status-badge">
                                    {searchResult.isMatch ? "✅ ACCÈS AUTORISÉ" : "⛔ NON RECONNU"}
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="tr-waiting">
                            {isStreaming ? (
                                <div className="scanning-text">
                                    Analyse biométrique en cours...<br />
                                    Veuillez garder le visage stable.<br />
                                    <strong>{10 - timeLeft}/10s</strong>
                                </div>
                            ) : (
                                isScanComplete ? "Scan terminé." : "En attente de démarrage..."
                            )}
                        </div>
                    )}
                </div>

            </div>

            <canvas ref={canvasRef} style={{ display: 'none' }} />
        </div>
    );
}
