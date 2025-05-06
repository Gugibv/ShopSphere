// src/pages/HomePage.js
import React from "react";
import { logout } from "../auth";

const TestPage = () => {
  return (
    <div style={{ padding: "40px", textAlign: "center" }}>
      <h1>没有被保护的页面</h1>
 
    </div>
  );
};

export default TestPage;
