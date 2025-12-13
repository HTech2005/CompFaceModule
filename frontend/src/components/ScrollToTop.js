import { useEffect } from "react";
import { useLocation } from "react-router-dom";

export default function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    // Défilement sur le body ou window
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [pathname]);

  return null; // aucun rendu visible
}

