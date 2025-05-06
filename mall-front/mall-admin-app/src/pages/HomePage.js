// src/pages/HomePage.js
import React, { useEffect, useState } from "react";
import axios from "axios";

const HomePage = () => {
  const [username, setUsername] = useState("");

  useEffect(() => {
    axios.get("https://cls.loc.lhubsg.com:8080/api/sg/wb/v1/common/oidc/me", {
      withCredentials: true
    })
      .then(res => {
        setUsername(res.data.username);
      })
      .catch(() => {
        setUsername("未登录");
      });
  }, []);

  return (
    <div style={{ padding: "40px", textAlign: "center" }}>
      <h1>欢迎来到主页 🎉</h1>
      <p>当前登录用户：<strong>{username}</strong></p>
    </div>
  );
};

export default HomePage;
