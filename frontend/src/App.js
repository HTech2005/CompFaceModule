import './App.css';
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Header from "./components/Header";
import Home from "./components/Home";
import CDV from "./components/CDV";
import CDVCompare from "./components/CDVCompare";
import TR from "./components/TR";
import TRRecognition from "./components/TRRecognition";
import CV from "./components/CV";
import CVAnalysis from "./components/CVAnalysis";
import Dashboard from "./components/Dashboard";

function App() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/cdv" element={<CDV />} />
        <Route path="/cdv-compare" element={<CDVCompare />} />
        <Route path="/tr" element={<TR />} />
        <Route path="/tr-recognition" element={<TRRecognition />} />
        <Route path="/cv" element={<CV />} />
        <Route path="/cv-analysis" element={<CVAnalysis />} />
        <Route path="/Dashboard" element={<Dashboard />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
