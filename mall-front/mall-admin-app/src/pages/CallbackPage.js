// src/pages/CallbackPage.js
import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";

const CallbackPage = () => {
  const navigate = useNavigate();

  useEffect(() => {
    // 稍微等待 cookie 写入后再跳转
    const timer = setTimeout(() => {
      navigate("/home");
    }, 300); // 可调时间
    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div style={{ padding: "50px", textAlign: "center" }}>
      <h2>登录成功，正在跳转首页...</h2>
    </div>
  );
};

export default CallbackPage;
