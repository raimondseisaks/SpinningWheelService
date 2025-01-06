import React, { createContext, useContext, useState, useEffect } from 'react';

const WebSocketContext = createContext();

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [message, setMessage] = useState('');
    const [userData, setUserData] = useState(null);
    const [pingInterval, setPingInterval] = useState(null); // For ping/pong mechanism

    const initializeSocket = (url, onOpen, onClose) => {
        const ws = new WebSocket(url);

        ws.onmessage = (event) => {
            if (event.data === "pong") {
                console.log("Received pong response from server");
            } else if (event.data.startsWith("user-data")) {
                const userDataArray = event.data.split(" ").slice(1);
                userDataArray[2] = parseFloat(userDataArray[2]).toFixed(2)
                setUserData(userDataArray);
            } else {
                setMessage(event.data);
            }
        };

        ws.onopen = () => {
            console.log('WebSocket connection established');
            onOpen();
        };

        ws.onclose = (event) => {
            console.error(`WebSocket closed: Code=${event.code}, Reason=${event.reason}`);
            onClose();
        };

        ws.onerror = (error) => {
            console.error('WebSocket error', error);
        };

        setSocket(ws);

        return () => {
            ws.close();
        };
    };

    useEffect(() => {
        return () => {
            if (socket) {
                socket.close();
            }
        };
    }, [socket]);

    const sendMessage = (msg) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(msg);
        }
    };

    return (
        <WebSocketContext.Provider value={{
            socket,
            message,
            sendMessage,
            userData,
            initializeSocket }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);





