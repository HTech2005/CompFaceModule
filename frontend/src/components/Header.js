import { Link } from "react-router-dom";
import { useState } from "react";
import './Header.css';

export default function Header() {
  const [menuOpen, setMenuOpen] = useState(false);

  const toggleMenu = () => setMenuOpen(!menuOpen);

  return (
    <>
      <header className="header">
        <h1 className="title">CompFace</h1>

        {/* Menu desktop */}
        <nav className="menu">
          <Link to="/">Accueil</Link>
          <Link to="/cdv">CDV</Link>
          <Link to="/tr">TR</Link>
          <Link to="/cv">CV</Link>
          <Link to="/dashboard">Dashboard</Link>
        </nav>

        {/* Burger */}
        <div className={`burger ${menuOpen ? "toggle" : ""}`} onClick={toggleMenu}>
          <span></span>
          <span></span>
          <span></span>
        </div>
      </header>

      {/* Menu mobile */}
      <div className={`menu-mobile ${menuOpen ? "active" : ""}`}>
        <Link to="/" onClick={toggleMenu}>Accueil</Link>
        <Link to="/cdv" onClick={toggleMenu}>CDV</Link>
        <Link to="/tr" onClick={toggleMenu}>TR</Link>
        <Link to="/cv" onClick={toggleMenu}>CV</Link>
        <Link to="/dashboard" onClick={toggleMenu}>Dashboard</Link>
      </div>

      {/* Overlay */}
      {menuOpen && <div className="overlay" onClick={toggleMenu}></div>}
    </>
  );
}

