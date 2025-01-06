import React from 'react';
import './GameRulesStyle.css';
import {useNavigate} from "react-router-dom";
import Footer from "./Footer";

const GameRules = () => {
    const navigate = useNavigate();

    return (
        <div>
            <nav className="navbar bg-danger">
                <div className="container-fluid">
                    <a className="navbar-brand text-light text" href="#">Spinning Wheel Server</a>
                    <div className="nav-controls">
                        <button id="exit-server" className="btn btn-outline-light" onClick={() => navigate("/")}>Return Back
                        </button>
                    </div>
                </div>
            </nav>
            <div>
                <div className="allTables">
                    <h1 className="text">RULES OF SPINNING WHEEL GAME SERVER</h1>
                    <div className="text-center">
                        <div className="text-center">

                            <div className="row mt-5 rules-card-container">

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Game round</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. Betting round duration is 10 seconds<br />
                                            2. Wheel spins 5 seconds and stops on random sector
                                            3.

                                        </p>
                                    </div>
                                </div>

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Betting</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. You can bet on one sector once at round<br/>
                                            2. In game there is 100 sectors<br/>
                                            3. You can bet and delete bets only in betting phase
                                        </p>
                                    </div>
                                </div>

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Winning Multipliers</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. Odd Sectors - Winning multiplier: Bet x 2<br/>
                                            2. Even Sectors (except 100) - Winning multiplier: Bet x 3<br/>
                                            3. Special Sectors: 100 - Winning multiplier: Bet x 50
                                        </p>
                                    </div>
                                </div>

                            </div>

                            <div className="row mt-5 rules-card-container">

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Balance</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. When you register you have 100$ on balance<br/>
                                            2. Every day you can claim free 10$
                                        </p>
                                    </div>
                                </div>

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Private Table</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. You can create private table which has 100 seats<br/>
                                            2. It has same functionality as public table<br/>
                                            3. To your private table can join users which know name of table and your set password
                                            4. Table deletes when last player leaves table
                                        </p>
                                    </div>
                                </div>

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Public tables</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. In server there is 9 public tables<br/>
                                            2. You can chat with others in table<br/>
                                            3. You can see you round results<br/>
                                            4. You can see your bets of current round<br/>
                                            5. You can see state of table (table state, your balance an players count)
                                        </p>
                                    </div>
                                </div>

                            </div>

                            <div className="row mt-5 rules-card-container">

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Stats</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. Server provides statistic about game style<br/>
                                            2. Server provides statistic about all players<br/>
                                        </p>
                                    </div>
                                </div>

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Profile data</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. You can see your profile data<br/>
                                            2. You can change you profile data: password, email and user name<br/>
                                            3. Also you can delete you account<br/>
                                            4. Email and user name must be unique!<br/>
                                            5. Password min length - 8
                                        </p>
                                    </div>
                                </div>

                                <div className="card text-dark bg-light mb-3">
                                    <div className="card-header">Sound effects</div>
                                    <div className="card-body">
                                        <p className="card-text">
                                            1. Spinning wheel game has sound effects for different game scenario<br/>
                                            2. You can leave your suggestions in profile edit section
                                        </p>
                                    </div>
                                </div>

                            </div>


                        </div>
                    </div>
                </div>

                <div className="video-container">
                    <h2 className="video-heading">Watch Our Demo</h2>
                    <video controls width="1080">
                        <source src="/Assets/preview-video.mp4" type="video/mp4"/>
                        Your browser does not support the video tag.
                    </video>
                </div>

            </div>
            <Footer/>
        </div>
    );
};

export default GameRules;
