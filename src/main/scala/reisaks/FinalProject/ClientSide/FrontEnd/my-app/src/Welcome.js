import { useNavigate } from 'react-router-dom';
import './welcomeStyle.css';

const WelcomePage = () => {
    const navigate = useNavigate();

    return (
                <div id="mainview">
                    <nav className="navbar navbar-landing">
                        <div className="navbar-container">
                            <div>
                                <a className="navbar-brand text me-4 menu-href" href="/GameRules">How To Play</a>
                                <a className="navbar-brand text me-4 menu-href" target="_blank" rel="noopener noreferrer" href="/Assets/documentation.pdf">
                                    Documentation</a>
                                <a className="navbar-brand text me-4 menu-href" href="https://github.com/raimondseisaks/InternshipEvo">Link To The Repo</a>
                            </div>
                        </div>
                    </nav>
                    <div className="welcomeContent">
                        <div className="welcomeContainer welcomeImage">
                            <img
                                className="welcome-logo"
                                src={`${process.env.PUBLIC_URL}/Assets/loginLogo.webp`}
                                alt="Register Logo"
                            />
                        </div>
                        <div className="welcomeContainer welcome-form">
                            <div className="landingBox">
                                <div className="landingText">
                                    <h1>
                                        The Spinning Wheel Gaming Server
                                    </h1>
                                    <p>
                                        This server provides spinning wheel game, where anyone can play.
                                        You can choose different lobbies, check stats, edit profile, and play with friends.
                                    </p>
                                    <div className="introButtons">

                                    <button onClick={() => navigate('/register')}
                                        className="btn btn-outline-light fw-bold me-4 mt-2">
                                        Register
                                    </button>
                                    <button onClick={() => navigate('/login')} className="btn btn-outline-light fw-bold me-4 mt-2">
                                        Login
                                    </button>
                                </div>
                            </div>
                        </div>
                        </div>
                    </div>
                    <div className="welcome-text">
                        <p>Created for demonstration purposes 2024</p>
                    </div>
                </div>
    );
};

export default WelcomePage;


