import React, { useEffect, useState } from 'react';
import { useWebSocket } from './WebSocketHandler';
import './StatsStyle.css';
import { useNavigate } from "react-router-dom";
import Footer from "./Footer";

const StatsPage = () => {
    const navigate = useNavigate();
    const { socket, sendMessage } = useWebSocket();
    const [userDataArray, setUserDataArray] = useState([]);
    const [serverDataArray, setServerDataArray] = useState([]);

    useEffect(() => {
        if (socket) {
            sendMessage("Get-Current-Server-Stats");
            sendMessage("Get-Current-Player-Stats");

            socket.onmessage = (event) => {
                const message = event.data;
                if (message.startsWith("Player-Stats")) {
                    setUserDataArray(message.split(" ").slice(1));
                } else if (message.startsWith("Server-Stats")) {
                    setServerDataArray(message.split(" ").slice(1));
                }
            };
        }
    }, [socket]);

    const handleStatsExit = () => {
        navigate("/lobby");
    };

    return (
        <div>
            <nav className="navbar bg-danger">
                <div className="container-fluid">
                    <a className="navbar-brand text-light" href="#">Spinning Wheel Server</a>
                    <div className="nav-controls">
                        <button
                            id="exit-server"
                            className="btn btn-outline-light"
                            onClick={handleStatsExit}
                        >
                            Back To The Lobby
                        </button>
                    </div>
                </div>
            </nav>

            <div className="stats-vh-container">
                <h3 className="section-title">Personal Stats</h3>
                <div className="stats-container">
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Count of your bets</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{userDataArray[0]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Your winning amount</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{userDataArray[1]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Your average bet amount</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{userDataArray[2]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="stats-container">
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Total played games</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{userDataArray[3]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Biggest bet</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{userDataArray[4]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Total wins</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{userDataArray[5]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <h3 className="section-title">Server Stats</h3>
                <div className="stats-container">
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Registered players</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{serverDataArray[0]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Most active player<br/>
                                    (Player bet at least 1 bet<br/> in round)</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{serverDataArray[1]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Average amount of bet</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{serverDataArray[2]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div className="stats-container">
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Amount of bets</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{serverDataArray[3]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Biggest win</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{serverDataArray[4]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stats-section">
                        <div className="stat-item">
                            <div className="stat-inner">
                                <div className="stat-front">
                                    <div className="stat-label">Biggest bet</div>
                                </div>
                                <div className="stat-back">
                                    <div className="stat-value">{serverDataArray[5]}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <Footer />
        </div>
    );
};

export default StatsPage;
