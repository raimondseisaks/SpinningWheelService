import React, { useState } from 'react';
import { useWebSocket, sendMeesage } from './WebSocketHandler';  // Assuming useWebSocket is a custom hook you have created
import { useNavigate } from 'react-router-dom';
import './loginStyle.css';

const LoginPage = () => {
    const [playerId, setPlayerId] = useState('');
    const [password, setPassword] = useState('');
    const { initializeSocket } = useWebSocket();
    const navigate = useNavigate();
    const {sendMessage } = useWebSocket();
    const [loginError, setLoginError] = useState('');

    const handleSubmit = (event) => {
        event.preventDefault();

        initializeSocket(
            `ws://localhost:8080/${encodeURIComponent("login")}/${encodeURIComponent(playerId.replace(/ /g, ''))}/${encodeURIComponent(password)}`,
            () => {
                console.log('WebSocket connection established');
                sendMessage("get-user-data")

                navigate('/lobby');
            },
            () => {
                setLoginError("Incorrect user name or password")
            }
        );
    };

    return (
        <div
        className="loginContainers">
            <div className="loginImage loginContainer">
                <img className="login-logo" src="../Assets/loginLogo.webp" alt="table1"/>
            </div>
            <div className="loginContainer">
                <div id="login">
                    <h2>Join the gaming service</h2>
            <form id="loginForm" onSubmit={handleSubmit}>
                {loginError && <p className="text-danger fw-bold">{loginError}</p>}
                <input
                    type="text"
                    id="playerId"
                    className="form-control login-control"
                    placeholder="Unique Player ID"
                    value={playerId}
                    onChange={(e) => setPlayerId(e.target.value)}
                    required
                />
                <input
                    type="password"
                    id="password"
                    className="form-control login-control"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />
                <p className="signup-text">
                    Don't have an account? <a href="/register" className="signup-link">Make it here</a>
                </p>
                <button type="submit" className="btn btn-outline-danger button">Connect</button>
            </form>
            </div>
        </div>
        </div>
    );
};

export default LoginPage;


