import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { WebSocketProvider } from './WebSocketHandler';
import WelcomePage from './Welcome';
import LobbyPage from './LobbyPage';
import TablePage from './TablePage'
import LoginPage from "./LoginPage";
import RegisterPage from "./RegisterFile";
import GameRules from "./GameRules";
import ProfilePage from "./ProfilePage";
import StatsPage from "./Stats";

const App = () => {
    return (
        <WebSocketProvider>
            <Router>
                <Routes>
                    <Route path="/" element={<WelcomePage />} />
                    <Route path="/lobby" element={<LobbyPage />} />
                    <Route path="/table" element={<TablePage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/gameRules" element={<GameRules />} />
                    <Route path="/profile" element={<ProfilePage />} />
                    <Route path="/stats" element={<StatsPage />} />
                </Routes>
            </Router>
        </WebSocketProvider>
    );
};

export default App;



